package com.norcode.bukkit.schematica;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.norcode.bukkit.schematica.exceptions.IncompleteSelectionException;
import com.norcode.bukkit.schematica.exceptions.EmptyClipboardException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import java.util.HashSet;

public class Session {

    String playerName;
    Selection selection = null;
    Clipboard clipboard = null;

    public Session(String name) {
        this.playerName = name;
    }
    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public Selection getSelection() {
        if (selection == null) {
            selection = new Selection();
        }
        return selection;
    }
    public void setSelection(Selection selection) {
        this.selection = selection;
    }
    public Clipboard getClipboard() {
        return this.clipboard;
    }
    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public void copy() throws IncompleteSelectionException {
        if (!getSelection().isComplete()) {
            throw new IncompleteSelectionException("No selection made");
        }
        this.clipboard = new Clipboard(getSelection().getSize());
        Schematica.getInstance().getLogger().info("created clipboard: " + getSelection().getSize());
        Location min = getSelection().getMin();
        Block b;
        for (int x=0;x<getSelection().getSize().getBlockX();x++) {
            for (int y=0;y<getSelection().getSize().getBlockY();y++) {
                for (int z=0;z<getSelection().getSize().getBlockZ();z++) {
                    b = min.clone().add(x,y,z).getBlock();
                    this.clipboard.copyBlockFromWorld(b, x, y, z);  
                }
            }
        }
        BlockVector playerPos = Bukkit.getServer().getPlayer(playerName).getLocation().toVector().toBlockVector();
        this.clipboard.setOffset(getSelection().getMin().subtract(playerPos).toVector().toBlockVector());
    }

    public void paste() throws EmptyClipboardException {
        if (clipboard == null) {
            throw new EmptyClipboardException("The clipboard is empty.");
        }
        Player player = Bukkit.getServer().getPlayer(playerName);
        World world = player.getWorld();
        BlockVector origin = clipboard.getOffset().clone().add(player.getLocation().toVector()).toBlockVector();
        
        for (BlockVector pos: clipboard.getPasteQueue(false, null)) {
            clipboard.copyBlockToWorld(clipboard.getBlock(pos),
                    new Location(world,
                            origin.getBlockX() + pos.getBlockX(),
                            origin.getBlockY() + pos.getBlockY(),
                            origin.getBlockZ() + pos.getBlockZ()));
        }
    }
}
