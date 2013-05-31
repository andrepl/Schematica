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

import org.bukkit.Location;
import org.bukkit.util.BlockVector;

public class Selection {
    Location pt1 = null;
    Location pt2 = null;

    public Selection() {}
    public Selection(Location pt1) {
        this.pt1 = pt1.clone();
    }

    public Selection(Location pt1, Location pt2) {
        this.pt1 = pt1.clone();
        this.pt2 = pt2.clone();
    }

    public Location getPt1() {
        return pt1;
    }

    public void setPt1(Location pt1) {
        this.pt1 = pt1;
    }

    public Location getPt2() {
        return pt2;
    }

    public void setPt2(Location pt2) {
        this.pt2 = pt2;
    }

    public Location getMin() {
        if (pt1 == null) {
            return pt2;
        } else if (pt2 == null) {
            return pt1;
        }
        return new Location(pt1.getWorld(), 
                Math.min(pt1.getBlockX(), pt2.getBlockX()),
                Math.min(pt1.getBlockY(), pt2.getBlockY()),
                Math.min(pt1.getBlockZ(), pt2.getBlockZ()));
    }

    public Location getMax() {
        if (pt1 == null) {
            return pt2;
        } else if (pt2 == null) {
            return pt1;
        }
        return new Location(pt1.getWorld(), 
                Math.max(pt1.getBlockX(), pt2.getBlockX()),
                Math.max(pt1.getBlockY(), pt2.getBlockY()),
                Math.max(pt1.getBlockZ(), pt2.getBlockZ()));
    }

    public BlockVector getSize() {
        Location min = getMin();
        Location max = getMax();
        return new BlockVector(max.getBlockX() - min.getBlockX() + 1, max.getBlockY() - min.getBlockY() + 1, max.getBlockZ() - min.getBlockZ() + 1);
    }

    public boolean isComplete() {
        return pt1 != null && pt2 != null && pt2.getWorld().getName().equals(pt1.getWorld().getName()); 
    }
}
