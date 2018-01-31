/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import oms3.dsl.Buildable;
import oms3.ngmf.util.cosu.luca.ParameterData;
import oms3.SimConst;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author od
 */
public class Calibration implements Buildable {

    String strategy = SimConst.MEAN;
    String range = "0-*";
    boolean matchColumn = false; // if false, select based on row index.
    // if true, base on matching column names.
    String file;
    String column;
    String table;

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setMatchColumn(boolean matchColumn) {
        this.matchColumn = matchColumn;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    //  Integer[] idx = rangeparser("1,3-5,6,7-*",20);
    private static List<Integer> parseRange(String range, int max) {
        List<Integer> idx = new ArrayList<Integer>();
        String[] n = range.split(",");
        for (String s : n) {
            String[] d = s.split("-");
            int mi = Integer.parseInt(d[0]);
            if (mi < 0 || mi >= max) {
                throw new IllegalArgumentException(range);
            }
            if (d.length == 2) {
                if (d[1].equals("*")) {
                    d[1] = Integer.toString(max - 1);
                }
                int ma = Integer.parseInt(d[1]);
                if (ma <= mi || ma >= max || ma < 0) {
                    throw new IllegalArgumentException(range);
                }
                for (int i = mi; i <= ma; i++) {
                    idx.add(i);
                }
            } else {
                idx.add(mi);
            }
        }
        return idx;
    }

    public String getStrategy() {
        if (strategy == null) {
            throw new RuntimeException("Missing strategy.");
        }
        if (!strategy.equals(SimConst.MEAN) && !strategy.equals(SimConst.INDIVIDUAL) && !strategy.equals(SimConst.BINARY)) {
            throw new RuntimeException("Strategy " + strategy + "unsupported.");
        }
        return strategy;
    }

    public String getRange() {
        return range;
    }

    public int getStrategyAsInt() {
        if (strategy.equals(SimConst.MEAN)) {
            return ParameterData.MEAN;
        } else if (strategy.equals(SimConst.INDIVIDUAL)) {
            return ParameterData.INDIVIDUAL;
        } else if (strategy.equals(SimConst.BINARY)) {
            return ParameterData.BINARY;
        } else {
            throw new IllegalArgumentException("Calibration strategy " + strategy + "not valid.");
        }
    }

    public boolean[] getCalibrateFlags(int length) {
        boolean[] calibrationFlags = new boolean[length];
        List<Integer> idx = new ArrayList<Integer>();
        if (length > 1) {
            idx = parseRange(range, length);
        } else // parseRange not happy with "0-*" for length of 1.
        {
            idx.add(1);
        }

        if (matchColumn == false) {
            for (int i = 0; i < length; i++) { // match all rows in the range provided
                calibrationFlags[i] = idx.contains(i);
            }
        } else {
            throw new IllegalArgumentException("Selecting calibration flags by matching column data is not yet supported.");
        }

        return calibrationFlags;
    }

    public boolean getMatchColumn() {
        return matchColumn;
    }

    public String getFile() {
        if (file == null) {
            throw new RuntimeException("missing file name.");
        }
        return file;
    }

    public String getTable() {
        return table;   // can be null
    }

    public String getColumn() {
        if (column == null) {
            throw new RuntimeException("missing column name.");
        }
        return column;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
}
