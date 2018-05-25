/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import oms3.ComponentException;

/**
 *
 * @author od
 */
public class Files {

    public static String readFully(String name) {
        StringBuilder b = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new FileReader(name));
            String line;
            while ((line = r.readLine()) != null) {
                b.append(line).append('\n');
            }
            r.close();
        } catch (IOException E) {
            throw new ComponentException(E.getMessage());
        }
        return b.toString();
    }
}
