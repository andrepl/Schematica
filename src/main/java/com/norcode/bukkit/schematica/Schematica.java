package com.norcode.bukkit.schematica;
import com.norcode.bukkit.schematica.exceptions.EmptyClipboardException;
import com.norcode.bukkit.schematica.exceptions.IncompleteSelectionException;
import com.norcode.bukkit.schematica.exceptions.SchematicLoadException;
import com.norcode.bukkit.schematica.exceptions.SchematicSaveException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Schematica extends JavaPlugin implements Listener {

    static Schematica getInstance() {
        return instance;
    }

    HashMap<String, Session> sessions = new HashMap<String, Session>();
    private static Schematica instance;
    @Override
    public void onEnable() {
        this.instance = this;
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LinkedList<String> params = new LinkedList<String>();
        for (int i=0;i<args.length;i++) { params.add(args[i]); }
        if (params.size() == 0) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cannot be run from console.");
            return true;
        }
        Player player = (Player) sender;
        sessions.get(player.getName().toLowerCase());
        String action = params.pop().toLowerCase();
        if (action.equals("copy")) {
            cmdCopy(player, "copy", params);
        } else if (action.equals("paste")) {
            cmdPaste(player, "paste", params);
        } else if (action.equals("load")) {
            cmdLoad(player, "load", params);
        } else if (action.equals("save")) {
            cmdSave(player, "save", params);
        } else if (action.equals("info")) {
            cmdInfo(player, "info", params);
        } else {
            return false;
        }
        return true;
    }

    public void cmdCopy(Player player, String label, LinkedList<String> params) {
        Session session = sessions.get(player.getName().toLowerCase());
        try {
            session.copy();
            player.sendMessage("Selection copied to clipboard.");
        } catch (IncompleteSelectionException ex) {
            player.sendMessage("You haven't made a selection.");
        }
    }

    public void cmdPaste(Player player, String label, LinkedList<String> params) {
        Session session = sessions.get(player.getName().toLowerCase());
        try {
            session.paste();
            player.sendMessage("Clipboard contents pasted into world.");
        } catch (EmptyClipboardException ex) {
            player.sendMessage("The clipboard is empty.");
        }
    }

    public void cmdLoad(Player player, String label, LinkedList<String> params) {
        Session session = sessions.get(player.getName().toLowerCase());
        if (params.size() < 1) {
            player.sendMessage("Filename expected.");
            return;
        }
        File file = new File(getDataFolder(), params.get(0));
        byte[] schematicBytes;
        try {
            schematicBytes = new byte[(int)file.length()];
            DataInputStream is = new DataInputStream(new FileInputStream(file));
            is.readFully(schematicBytes);
            is.close();
            session.setClipboard(Clipboard.fromSchematic(schematicBytes));
        } catch (FileNotFoundException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            player.sendMessage("Failed to load '" + params.get(0) + "'");
            return;
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            player.sendMessage("Failed to load '" + params.get(0) + "'");
            return;
        } catch (SchematicLoadException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            player.sendMessage("Failed to load '" + params.get(0) + "'");
            return;
        }        
        player.sendMessage("Schematic Loaded.");
    }

    public void cmdSave(Player player, String label, LinkedList<String> params) {
        if (params.size() < 1) {
            player.sendMessage("Filename Expected.");
            return;
        }
        Session sess = sessions.get(player.getName().toLowerCase());
        DataOutputStream os = null;
        File file = new File(getDataFolder(), params.get(0));
        try {
            os = new DataOutputStream(new FileOutputStream(file));
            if (sess.getClipboard() == null) {
                player.sendMessage("The clipboard is empty.");
                return;
            }
            os.write(sess.getClipboard().toSchematic());
            player.sendMessage("Schematic saved to " + file.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Schematica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchematicSaveException ex) {
            Logger.getLogger(Schematica.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Schematica.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { os.close(); } catch (IOException ex) {}
        }
    }

    public void cmdInfo(Player player, String label, LinkedList<String> params) {
        Session sess = sessions.get(player.getName().toLowerCase());
        if (sess == null) {
            player.sendMessage("No selection or clipboard.");
            return;
        }
        if (sess.getSelection() != null) {
            Selection sel = sess.getSelection();
            player.sendMessage("Selection Min: " + sel.getMin());
            player.sendMessage("Selection Max: " + sel.getMax());
        }
        if (sess.getClipboard() != null) {
            player.sendMessage("Clipboard Width: " + sess.getClipboard().getSize().getBlockX());
            player.sendMessage("Clipboard Height: " + sess.getClipboard().getSize().getBlockY());
            player.sendMessage("Clipboard Length: " + sess.getClipboard().getSize().getBlockZ());
            player.sendMessage("Clipboard Offset: " + sess.getClipboard().getOffset());
            player.sendMessage("Clipboard Origin: " + sess.getClipboard().getOrigin());
        }
        
    }
        
    @EventHandler(ignoreCancelled=false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer().hasPermission("schematica.select")) {
            if (event.getItem() != null && event.getItem().getType().equals(Material.GOLD_HOE)) {
                Session sess = sessions.get(event.getPlayer().getName().toLowerCase());
                if (sess == null) {
                    sess = new Session(event.getPlayer().getName());
                    sessions.put(event.getPlayer().getName().toLowerCase(), sess);
                }
                if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                    sess.getSelection().setPt1(event.getClickedBlock().getLocation());
                    event.getPlayer().sendMessage("Selection Pt.1 set to " + event.getClickedBlock().getLocation().toVector());
                    event.setCancelled(true);
                    event.setUseInteractedBlock(Result.DENY);
                } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    sess.getSelection().setPt2(event.getClickedBlock().getLocation());
                    event.getPlayer().sendMessage("Selection Pt.2 set to " + event.getClickedBlock().getLocation().toVector());
                    event.setCancelled(true);
                    event.setUseInteractedBlock(Result.DENY);
                }
            }
        }
    }
}
