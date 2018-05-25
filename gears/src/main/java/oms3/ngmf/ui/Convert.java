/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.ui;

import java.io.*;
import java.io.BufferedReader;
import java.util.*;

import oms3.io.DataIO;
import oms3.ngmf.ui.mms.MMSParameterAdapter;
import oms3.ngmf.ui.mms.MMSParameterAdapter.MmsParamInfo;

public class Convert {

    public static void main(String[] args) {
        data("C:/tmp/east.data");
    }

    public static String data(String file) {
        File dataFile = new File(file);
        if (!dataFile.isAbsolute()) {
            String work = System.getProperty("oms3.work");
            if (work == null) {
                return "Error no $work property found, use full qualifies name instead.)";
            }
            dataFile = new File(work + "/data", file);
        }
        String name = dataFile.getName().substring(0, dataFile.getName().indexOf('.'));
        File out = new File(dataFile.getParentFile(), name + ".csv");

        StringBuffer head = new StringBuffer("@H, date");
        StringBuffer type = new StringBuffer(" " +  DataIO.KEY_TYPE + ", Date");
        StringBuffer dout = new StringBuffer(",");

        try {
            FileReader fr = new FileReader(dataFile);
            BufferedReader br = new BufferedReader(fr);
            String h = br.readLine();
            String statHdr = br.readLine();

            while (!statHdr.startsWith("###")) {
                if (statHdr.startsWith("//") || statHdr.isEmpty()) {
                    statHdr = br.readLine();
                    continue;
                }
                String[] st = statHdr.split("\\s+");
                int cols = Integer.parseInt(st[1]);
                for (int i = 0; i < cols; i++) {
                    head.append(", " + st[0] + "[" + i + "]");
                    type.append(", double");
                }
                statHdr = br.readLine();
            }

            PrintWriter w = new PrintWriter(out);
            w.println("@T, \"Table\"");
            w.println(" " + DataIO.KEY_CREATED_AT + ", \"" + new Date() + "\"");
            w.println(" " + DataIO.KEY_CONVERTED_FROM + ", \"" + dataFile + "\"");
            w.println(" " + DataIO.DATE_FORMAT + ", yyyy MM dd H m s");
            w.println(" Title, \"" + h + "\"");
            w.println(head);
            w.println(type);

            String data;
            while ((data = br.readLine()) != null) {
                String[] str = data.split(" ");
                for (int i = 0; i < 6; i++) {
                    dout.append(str[i] + " ");
                }
                for (int i = 6; i < str.length; i++) {
                    dout.append("," + str[i]);
                }
                w.println(dout);
                dout.delete(1, dout.length());
            }
            fr.close();
            w.close();
            return "  Converted: '" + file + "' -> '" + out + "'\n";
        } catch (IOException ioe) {
            return "Error : " + ioe.getMessage() + "\n";
        }
    }

    public static String param(String file) {
        File param = new File(file);
        if (!param.isAbsolute()) {
            String work = System.getProperty("oms3.work");
            if (work == null) {
                return "Error no $work property found, use full qualifies name instead.)";
            }
            param = new File(work + "/data", file);
        }
        String name = param.getName().substring(0, param.getName().indexOf('.'));
        File out = new File(param.getParentFile(), name + ".csv");

        try {
            MmsParamInfo info = MMSParameterAdapter.map(param);
            info.store(new FileOutputStream(out));
            return "  Converted: '" + file + "' -> '" + out + "'\n";
        } catch (IOException ex) {
            return "Error: " + ex.getMessage();
        }
    }

    public static String statvar(String file) {
        StringBuffer head = new StringBuffer("@H, date");
        StringBuffer type = new StringBuffer(" " +  DataIO.KEY_TYPE + ", Date");
        StringBuffer dout = new StringBuffer(",");

        File statvar = new File(file);
        if (!statvar.isAbsolute()) {
            String work = System.getProperty("oms3.work");
            if (work == null) {
                return "Error no $work property found, use full qualifies name instead.)";
            }
            statvar = new File(work + "/data", file);
        }
        String name = statvar.getName().substring(0, statvar.getName().indexOf('.'));
        File out = new File(statvar.getParentFile(), name + ".csv");

        try {
            FileReader fr = new FileReader(statvar);
            BufferedReader br = new BufferedReader(fr);

            String statHdr = br.readLine();
            int varNum = Integer.parseInt(statHdr);
            for (int i = 0; i < varNum; i++) {
                String s = br.readLine();
                String[] st = s.split("\\s+");
                head.append(", " + st[0]);
                type.append(", double");
            }

            PrintWriter w = new PrintWriter(out);
            w.println("@T, \"Table\"");
            w.println(" " + DataIO.KEY_CREATED_AT + ", \"" + new Date() + "\"");
            w.println(" " + DataIO.KEY_CONVERTED_FROM + ", \"" + statvar + "\"");
            w.println(" " + DataIO.DATE_FORMAT + ", yyyy MM dd H m s");
            w.println(head);
            w.println(type);

            String data;
            while ((data = br.readLine()) != null) {
                String[] str = data.split(" ");
                for (int i = 1; i < 7; i++) {
                    dout.append(str[i] + " ");
                }
                dout.append(",");
                for (int i = 7; i < str.length - 1; i++) {
                    dout.append(str[i] + ",");
                }
                dout.append(str[str.length - 1]);
                w.println(dout);
                dout.delete(1, dout.length());
            }
            fr.close();
            w.close();
            return "  Converted: '" + file + "' -> '" + out + "'\n";
        } catch (IOException ioe) {
            return "Error : " + ioe.getMessage() + "\n";
        }
    }
}

