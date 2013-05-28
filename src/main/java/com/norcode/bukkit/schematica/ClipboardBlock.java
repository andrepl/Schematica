package com.norcode.bukkit.schematica;

import net.minecraft.server.v1_5_R3.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.util.BlockVector;

public class ClipboardBlock {
    int type;
    byte data;
    NBTTagCompound tag = null;

    public ClipboardBlock(int type, byte data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte getData() {
        return data;
    }
    
    public void setData(byte data) {
        this.data = data;
    }

    public NBTTagCompound getTag() {
        return tag;
    }

    public void setTag(NBTTagCompound tag) {
        this.tag = tag;
    }

    public boolean hasTag() {
        return this.tag != null;
    }
    public void rotate90() {
        this.data = (byte) MaterialID.rotate90(this.type, (int) this.data);
    }

    public void rotate90Reverse() {
        this.data = (byte) MaterialID.rotate90Reverse(this.type, (int) this.data);
    }

    public String toString() {
        return "{ClipboardBlock[" + Material.getMaterial(this.getType()).name() + ":" + this.getData() + "]";
    }
}
