/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package org.jgrasstools.gui.spatialtoolbox.widgets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class EsriAsciiProcessingRegion {

    public static final String NO_DATA_MARKER = "-9999";

    /** Column number tag in the header file */
    public static final String NCOLS = "NCOLS";

    /** Row number tag in the header file */
    public static final String NROWS = "NROWS";

    /** xll corner coordinate tag in the header file */
    public static final String XLLCORNER = "XLLCORNER";

    /** yll corner coordinate tag in the header file */
    public static final String YLLCORNER = "YLLCORNER";

    /** xll center coordinate tag in the header file */
    public static final String XLLCENTER = "XLLCENTER";

    /** yll center coordinate tag in the header file */
    public static final String YLLCENTER = "YLLCENTER";

    /** cell size tag in the header file */
    public static final String CELLSIZE = "CELLSIZE";

    /** no data tag in the header file */
    public static final String NODATA_VALUE = "NODATA_VALUE";

    private File file;

    private Integer nCols;
    private Integer nRows;
    private Double xllCellCoordinate;
    private Double yllCellCoordinate;
    private Double cellSizeX;
    private Double cellSizeY;
    // private double noData;

    public EsriAsciiProcessingRegion( File file ) {
        this.file = file;
        try {
            parseHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseHeader() throws IOException {
        // /////////////////////////////////////////////////////////////////////
        //
        // This is the ArcInfo ASCII Grid Format
        // nrows XX
        // ncols XX
        // xllcorner | xllcenter XX
        // yllcorner | yllcenter XX
        // cellsize XX
        // NODATA_value XX (Optional)
        // XX XX XX XX... (DATA VALUES)
        //
        // /////////////////////////////////////////////////////////////////////

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));

            String line = null;
            int readMaxLines = 10;
            int linesRead = 0;
            while( (line = br.readLine()) != null ) {
                if (linesRead++ > readMaxLines) {
                    break;
                }

                String[] split = line.trim().split("\\s+");
                if (split.length > 2) {
                    continue;
                }

                String sKey = split[0];
                String valueStr = split[1];
                double value = Double.parseDouble(valueStr);
                if (Double.isNaN(value) || Double.isNaN(value))
                    break;

                if (NCOLS.equalsIgnoreCase(sKey)) {
                    nCols = (int) value;
                } else if (NROWS.equalsIgnoreCase(sKey)) {
                    nRows = (int) value;
                } else if (XLLCORNER.equalsIgnoreCase(sKey)) {
                    xllCellCoordinate = value;
                } else if (YLLCORNER.equalsIgnoreCase(sKey)) {
                    yllCellCoordinate = value;
                } else if (XLLCENTER.equalsIgnoreCase(sKey)) {
                    xllCellCoordinate = value;
                } else if (YLLCENTER.equalsIgnoreCase(sKey)) {
                    yllCellCoordinate = value;
                } else if (CELLSIZE.equalsIgnoreCase(sKey)) {
                    cellSizeX = cellSizeY = value;
                }
                // else if (NODATA_VALUE.equalsIgnoreCase(sKey)) {
                // noData = value;
                // }
            }
        } finally {
            if (br != null)
                br.close();
        }
    }

    /**
     * @return the region info as [w, e, s, n, xRes, yRes, cols, rows]
     */
    public double[] getRegionInfo() {
        if (nCols == null || //
                nRows == null || //
                xllCellCoordinate == null || //
                yllCellCoordinate == null || //
                cellSizeX == null || //
                cellSizeY == null //
        ) {
            return null;
        }

        double width = cellSizeX * nCols;
        double height = cellSizeY * nRows;
        double w = xllCellCoordinate;
        double e = xllCellCoordinate + width;
        double s = yllCellCoordinate;
        double n = yllCellCoordinate + height;
        return new double[]{//
        /*    */w, e, s, n, //
                cellSizeX,//
                cellSizeY,//
                nCols,//
                nRows//
        };
    }
}
