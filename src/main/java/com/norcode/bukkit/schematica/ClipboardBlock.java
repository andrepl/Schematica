/* This file is part of Schematica.
 * Copyright (C) 2013 metalhedd <https://github.com/andrepl/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Schematica.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This code is based heavily on WorldEdit by sk89q <http://www.sk89q.com>
 */
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
