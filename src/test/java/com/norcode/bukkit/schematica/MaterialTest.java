package com.norcode.bukkit.schematica;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 23/09/13
 * Time: 11:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class MaterialTest extends TestCase {
    public MaterialTest(String name) {
        super(name);

    }

    public void testMissingIds() throws Exception {
        HashMap<Integer, Field> matIdMap = new HashMap<Integer, Field>();

        Field[] declaredFields = MaterialID.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                if (field.getType().equals(Integer.TYPE)) {
                    matIdMap.put((Integer) field.get(null), field);
                } else {
                    System.out.println("Skipping static " + field.getType() + " field.");
                }
            }
        }

        for (Material m: Material.values()) {
            Assert.assertTrue("MaterialID not defined for " + m + " (" + m.getId() + ")", matIdMap.containsKey(m.getId()));
        }
    }
}
