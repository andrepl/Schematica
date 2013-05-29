package com.norcode.bukkit.schematica;
import java.util.*;

import net.minecraft.server.v1_5_R3.NBTBase;
import net.minecraft.server.v1_5_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import net.minecraft.server.v1_5_R3.NBTTagInt;
import net.minecraft.server.v1_5_R3.NBTTagList;
import net.minecraft.server.v1_5_R3.TileEntity;
import net.minecraft.server.v1_5_R3.TileEntityBeacon;
import net.minecraft.server.v1_5_R3.TileEntityBrewingStand;
import net.minecraft.server.v1_5_R3.TileEntityChest;
import net.minecraft.server.v1_5_R3.TileEntityCommand;
import net.minecraft.server.v1_5_R3.TileEntityComparator;
import net.minecraft.server.v1_5_R3.TileEntityDispenser;
import net.minecraft.server.v1_5_R3.TileEntityDropper;
import net.minecraft.server.v1_5_R3.TileEntityEnchantTable;
import net.minecraft.server.v1_5_R3.TileEntityEnderChest;
import net.minecraft.server.v1_5_R3.TileEntityEnderPortal;
import net.minecraft.server.v1_5_R3.TileEntityFurnace;
import net.minecraft.server.v1_5_R3.TileEntityHopper;
import net.minecraft.server.v1_5_R3.TileEntityLightDetector;
import net.minecraft.server.v1_5_R3.TileEntityMobSpawner;
import net.minecraft.server.v1_5_R3.TileEntityNote;
import net.minecraft.server.v1_5_R3.TileEntityPiston;
import net.minecraft.server.v1_5_R3.TileEntityRecordPlayer;
import net.minecraft.server.v1_5_R3.TileEntitySign;
import net.minecraft.server.v1_5_R3.TileEntitySkull;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;
import org.bukkit.util.BlockVector;

import com.norcode.bukkit.schematica.exceptions.SchematicLoadException;
import com.norcode.bukkit.schematica.exceptions.SchematicSaveException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

public class Clipboard {
    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;

    ClipboardBlock[][][] data;
    BlockVector origin;
    BlockVector offset;
    BlockVector size;

    private static HashSet<Integer> onlyAir = new HashSet<Integer>();
    static {
        onlyAir.add(0);
    }
    public Clipboard(BlockVector size) {
        this.size = new BlockVector(size);
        this.offset = new BlockVector();
        this.origin = new BlockVector();
        this.data = new ClipboardBlock[this.size.getBlockX()][this.size.getBlockY()][this.size.getBlockZ()];
    }

    public BlockVector getOrigin() {
        return origin;
    }

    public void setOrigin(BlockVector origin) {
        this.origin = origin;
    }


    public BlockVector getOffset() {
        return offset;
    }


    public void setOffset(BlockVector offset) {
        this.offset = offset;
    }


    public BlockVector getSize() {
        return size;
    }


    public void setSize(BlockVector size) {
        this.size = size;
    }


    public static Clipboard fromSchematic(byte[] input) throws SchematicLoadException {
        NBTTagCompound tag = NBTCompressedStreamTools.a(input);
        
        short width = tag.getShort("Width");
        short length = tag.getShort("Length");
        short height = tag.getShort("Height");
        BlockVector size = new BlockVector(width, height, length);
        
        int originX = tag.getInt("WEOriginX");
        int originY = tag.getInt("WEOriginY");
        int originZ = tag.getInt("WEOriginZ");
        BlockVector origin = new BlockVector(originX, originY, originZ);

        int offsetX = tag.getInt("WEOffsetX");
        int offsetY = tag.getInt("WEOffsetY");
        int offsetZ = tag.getInt("WEOffsetZ");
        BlockVector offset = new BlockVector(offsetX, offsetY, offsetZ);
        
        byte[] blockIds = tag.getByteArray("Blocks");
        byte[] data = tag.getByteArray("Data");
        byte[] addId = new byte[0];
        short[] blocks = new short[blockIds.length];

        if (tag.hasKey("AddBlocks")) {
            addId = tag.getByteArray("AddBlocks");
        }
        // Combine the AddBlocks data with the first 8-bit block ID
        for (int index = 0; index < blockIds.length; index++) {
            if ((index >> 1) >= addId.length) { // No corresponding AddBlocks index
                blocks[index] = (short) (blockIds[index] & 0xFF);
            } else {
                if ((index & 1) == 0) {
                    blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blockIds[index] & 0xFF));
                } else {
                    blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blockIds[index] & 0xFF));
                }
            }
        }

        // Need to pull out tile entities
        NBTTagList tileEntities = tag.getList("TileEntities");

        Map<BlockVector, NBTTagCompound> tileEntitiesMap =
                new HashMap<BlockVector, NBTTagCompound>();

        NBTTagCompound cTag;
        for (int index=0;index<tileEntities.size();index++) {
            try {
                cTag = (NBTTagCompound) tileEntities.get(index);
            } catch (ClassCastException ex) {
                continue;
            }
            int x = 0;
            int y = 0;
            int z = 0;
            for (NBTBase v: (List<NBTBase>) cTag.c()) {
                if (v instanceof NBTTagInt) {
                    NBTTagInt intTag = (NBTTagInt) v;
                    if (v.getName().equals("x")) {
                        x = intTag.data;
                    } else if (v.getName().equals("y")) {
                        y = intTag.data;
                    } else if(v.getName().equals("z")) {
                        z = intTag.data;
                    }
                }
            }
            tileEntitiesMap.put(new BlockVector(x,y,z), cTag);
        }

        Clipboard cb = new Clipboard(size);
        cb.origin = origin;
        cb.offset = offset;
        int idx;
        BlockVector vec;
        ClipboardBlock block;
        for (int x=0;x<width;++x) {
            for (int y=0;y<height;++y) {
                for (int z=0;z<length;++z) {
                    idx = y*width*length+z*width+x;
                    vec = new BlockVector(x,y,z);
                    block = new ClipboardBlock(blocks[idx], data[idx]);
                    if (tileEntitiesMap.containsKey(vec)) {
                        block.setTag(tileEntitiesMap.get(vec));
                    }
                    cb.data[x][y][z] = block;
                }
            }
        }
        return cb;
    }

    public byte[] toSchematic() throws SchematicSaveException {
        int width = getWidth();
        int height = getHeight();
        int length = getLength();
        if (width > MAX_SIZE) {
            throw new SchematicSaveException("Width " + width + " exceeds max size of "  + MAX_SIZE);
        }
        if (height > MAX_SIZE) {
            throw new SchematicSaveException("Height " + height + " exceeds max size of "  + MAX_SIZE);
        }
        if (length > MAX_SIZE) {
            throw new SchematicSaveException("Length " + length + " exceeds max size of "  + MAX_SIZE);
        }

        NBTTagCompound tag = new NBTTagCompound("Schematic");
        tag.setShort("Width",  (short) length);
        tag.setShort("Length", (short) length);
        tag.setShort("Height", (short) height);
        tag.setString("Materials",  "Alpha");
        tag.setInt("WEOriginX", getOrigin().getBlockX());
        tag.setInt("WEOriginY", getOrigin().getBlockY());
        tag.setInt("WEOriginZ", getOrigin().getBlockZ());
        tag.setInt("WEOffsetX", getOffset().getBlockX());
        tag.setInt("WEOffsetY", getOffset().getBlockY());
        tag.setInt("WEOffsetZ", getOffset().getBlockZ());

        // Copy
        byte[] blocks = new byte[width * height * length];
        byte[] addBlocks = null;
        byte[] blockData = new byte[width * height * length];
        NBTTagList tileEntities = new NBTTagList();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    int index = y * width * length + z * width + x;
                    ClipboardBlock block = getBlock(x, y, z);

                    // Save 4096 IDs in an AddBlocks section
                    if (block.getType() > 255) {
                        if (addBlocks == null) { // Lazily create section
                            addBlocks = new byte[(blocks.length >> 1) + 1];
                        }

                        addBlocks[index >> 1] = (byte) (((index & 1) == 0) ?
                                addBlocks[index >> 1] & 0xF0 | (block.getType() >> 8) & 0xF
                                : addBlocks[index >> 1] & 0xF | ((block.getType() >> 8) & 0xF) << 4);
                    }

                    blocks[index] = (byte) block.getType();
                    blockData[index] = (byte) block.getData();

                    if (block.hasTag()) {
                        NBTTagCompound rawTag = (NBTTagCompound) block.getTag().clone();
                        rawTag.setInt("x", x);
                        rawTag.setInt("y", y);
                        rawTag.setInt("z", z);
                        tileEntities.add(rawTag);
                    }
                }
            }
        }


        tag.setByteArray("Blocks", blocks);
        tag.setByteArray("Data", blockData);
        tag.set("Entities", new NBTTagList("Entities"));
        tag.set("TileEntities", tileEntities);
        if (addBlocks != null) {
            tag.setByteArray("AddBlocks", addBlocks);
        }
        // Build and output
        tag.setName("Schematic");
        return NBTCompressedStreamTools.a(tag);
    }

    private static BlockVector rotateBlockVector(BlockVector v, double angle) {
        angle = Math.toRadians(angle);
        int x = v.getBlockX();
        int z = v.getBlockZ();
        double x2 = x * Math.cos(angle) - z * Math.sin(angle);
        double z2 = x * Math.sin(angle) + z * Math.cos(angle);
        return new BlockVector(x2,v.getBlockY(),z2);
    }

    public void rotate2D(int angle) {
        angle = angle % 360;
        if (angle % 90 != 0) {
            return;
        }
        boolean reverse = angle < 0;
        int numRotations = Math.abs((int) Math.floor(angle / 90.0));

        int width = getWidth();
        int length = getLength();
        int height = getHeight();
        BlockVector sizeRotated = rotateBlockVector(size, angle);
        int shiftX = sizeRotated.getX() < 0 ? -sizeRotated.getBlockX() - 1 : 0;
        int shiftZ = sizeRotated.getZ() < 0 ? -sizeRotated.getBlockZ() - 1 : 0;

        ClipboardBlock newData[][][] = new ClipboardBlock
                [Math.abs(sizeRotated.getBlockX())]
                [Math.abs(sizeRotated.getBlockY())]
                [Math.abs(sizeRotated.getBlockZ())];

        for (int x = 0; x < width; ++x) {
            for (int z = 0; z < length; ++z) {
                BlockVector v = rotateBlockVector(new BlockVector(x, 0, z), angle);
                int newX = v.getBlockX();
                int newZ = v.getBlockZ();
                for (int y = 0; y < height; ++y) {
                    ClipboardBlock block = data[x][y][z];
                    newData[shiftX + newX][y][shiftZ + newZ] = block;
                    if (reverse) {
                        for (int i = 0; i < numRotations; ++i) {
                            block.rotate90Reverse();
                        }
                    } else {
                        for (int i = 0; i < numRotations; ++i) {
                            block.rotate90();
                        }
                    }
                }
            }
        }

        data = newData;
        size = new BlockVector(Math.abs(sizeRotated.getBlockX()),
                          Math.abs(sizeRotated.getBlockY()),
                          Math.abs(sizeRotated.getBlockZ()));
        offset = new BlockVector(rotateBlockVector(offset, angle).subtract(new org.bukkit.util.Vector(shiftX, 0, shiftZ)));
    }

    public ClipboardBlock getBlock(int x, int y, int z) {
        return data[x][y][z];
    }

    public ClipboardBlock getBlock(BlockVector vec) {
        return getBlock(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
    }

    public void setBlock(int x, int y, int z, ClipboardBlock block) {
        data[x][y][z] = block;
    }

    public void setBlock(BlockVector vec, ClipboardBlock block) {
        setBlock(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ(), block);
    }

    private int getHeight() {
        return size.getBlockY();
    }


    private int getLength() {
        return size.getBlockZ();
    }


    private int getWidth() {
        return size.getBlockX();
    }

    public void copyBlockFromWorld(Block block, int x, int y, int z) {
        Schematica.getInstance().getLogger().info("Copying " + block + " to clipboard " + x + "," + y + "," + z);
        ClipboardBlock cbb = new ClipboardBlock(block.getTypeId(), block.getData());
        if (MaterialID.isTileEntityBlock(block.getTypeId())) {
            CraftWorld cw = (CraftWorld) block.getWorld();
            TileEntity te = cw.getTileEntityAt(block.getX(), block.getY(), block.getZ());
            NBTTagCompound tag = new NBTTagCompound(MaterialID.getTileEntityId(block.getTypeId()));
            te.b(tag);
            tag.setInt("x", x);
            tag.setInt("y", y);
            tag.setInt("z", z);
            cbb.setTag(tag);
        }
        data[x][y][z] = cbb;
    }

    public void copyBlockFromWorld(Block block, BlockVector vec) {
        copyBlockFromWorld(block, vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
    }

    public void copyBlockToWorld(ClipboardBlock block, Location loc) {
        BlockState state = loc.getBlock().getState();
        Material old = state.getType();
        
        state.setTypeId(block.getType());
        state.setRawData(block.getData());
        state.update(true, false);

        if (block.hasTag()) {
            Schematica.getInstance().getLogger().info("Copying TileEntity Data: " + block.getTag());
            TileEntity te;
            NBTTagCompound btag = (NBTTagCompound) block.getTag().clone();
            btag.setName(MaterialID.getTileEntityId(block.getType()));
            btag.setInt("x", loc.getBlockX());
            btag.setInt("y", loc.getBlockY());
            btag.setInt("z", loc.getBlockZ());
            te = ((CraftWorld) loc.getWorld()).getTileEntityAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()); // MaterialID.getTileEntityClass(block.getType()).newInstance();
            te.a(btag);
            te.update();
            //((CraftWorld) loc.getWorld()).getHandle().setTileEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), te);

        }
    }

    public List<BlockVector> getPasteQueue(boolean shuffle, Set<Integer> ignoreTypes) {
        if (ignoreTypes == null) {
            ignoreTypes = onlyAir;
        }
        List<BlockVector> queue = new ArrayList<BlockVector>(this.getWidth()*this.getHeight()*this.getLength());
        List<BlockVector> layerQueue = new ArrayList<BlockVector>(this.getWidth()*this.getLength());
        List<BlockVector> yBuffer = new ArrayList<BlockVector>();
        List<BlockVector> finishBuffer = new ArrayList<BlockVector>();
        ClipboardBlock cb;
        for (int y=0;y<getHeight();y++) {
            for (int z=0;z<getSize().getBlockZ();z++) {
                for (int x=0;x<getSize().getBlockX();x++) {
                    cb = data[x][y][z];
                    if (ignoreTypes.contains(cb.getType())) continue;
                    switch (cb.getType()) {
                        case MaterialID.TORCH:
                        case MaterialID.BED_BLOCK:
                        case MaterialID.COCOA:
                        case MaterialID.WOOD_BUTTON:
                        case MaterialID.STONE_BUTTON:
                        case MaterialID.SKULL:
                        case MaterialID.REDSTONE_TORCH_OFF:
                        case MaterialID.REDSTONE_TORCH_ON:
                        case MaterialID.TRAP_DOOR:
                        case MaterialID.WALL_SIGN:
                        case MaterialID.TRIPWIRE_HOOK:
                            yBuffer.add(new BlockVector(x,y,z));
                            break;
                        case MaterialID.PISTON_EXTENSION:
                        case MaterialID.VINE:
                        case MaterialID.IRON_DOOR_BLOCK:
                        case MaterialID.WOODEN_DOOR:
                        case MaterialID.LEVER:
                            finishBuffer.add(new BlockVector(x,y,z));
                            break;
                        default:
                            layerQueue.add(new BlockVector(x,y,z));
                    }
                }
            }
            if (shuffle) {
                Collections.shuffle(layerQueue);
                Collections.shuffle(yBuffer);
            }
            queue.addAll(layerQueue);
            if (!yBuffer.isEmpty()) {
                queue.addAll(yBuffer);
                yBuffer.clear();
            }
            layerQueue.clear();
        }
        if (shuffle) {
            Collections.shuffle(finishBuffer);
        }
        queue.addAll(finishBuffer);
        return queue;
    }

    public List<BlockVector> getCutQueue(boolean shuffle, HashSet<Integer> ignoreBlocks) {
        List<BlockVector> queue = getPasteQueue(shuffle, ignoreBlocks);
        Collections.reverse(queue);
        return queue;
    }

    @Override
    public String toString() {
        BlockVector s = getSize();
        return "{Clipboard[" + s.getBlockX() + "x" + s.getBlockY() + "x" + s.getBlockZ() + "]}";
    }

}
