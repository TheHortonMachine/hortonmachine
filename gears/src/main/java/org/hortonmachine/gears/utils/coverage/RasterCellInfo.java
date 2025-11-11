package org.hortonmachine.gears.utils.coverage;

import java.awt.Point;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.StringUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;

public class RasterCellInfo {

    private HMRaster[] coverage2ds;
    private double lon;
    private double lat;
    private int bufferCells = 1;
    private double xRes;
    private double yRes;
	private Coordinate coordinate;

    public RasterCellInfo( int col, int row, GridCoverage2D... coverage2ds ) {
        this.coverage2ds = new HMRaster[coverage2ds.length];
        for( int i = 0; i < coverage2ds.length; i++ ) {
			this.coverage2ds[i] = HMRaster.fromGridCoverage(coverage2ds[i]);
		}
        coordinate = CoverageUtilities.coordinateFromColRow(col, row, coverage2ds[0].getGridGeometry());
        lon = coordinate.x;
        lat = coordinate.y;
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage2ds[0]);
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();
    }

    public RasterCellInfo( double lon, double lat, GridCoverage2D... coverage2ds ) {
        this.lon = lon;
        this.lat = lat;
        this.coordinate = new Coordinate(lon, lat);
        this.coverage2ds = new HMRaster[coverage2ds.length];
        for( int i = 0; i < coverage2ds.length; i++ ) {
			this.coverage2ds[i] = HMRaster.fromGridCoverage(coverage2ds[i]);
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage2ds[0]);
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();
    }
    
    public RasterCellInfo( int col, int row, HMRaster... coverage2ds ) {
        this.coverage2ds = coverage2ds;
        coordinate = coverage2ds[0].getWorld(col, row);	
        lon = coordinate.x;
        lat = coordinate.y;
        RegionMap regionMap = coverage2ds[0].getRegionMap();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();
    }

    public RasterCellInfo( double lon, double lat, HMRaster... coverage2ds ) {
        this.lon = lon;
        this.lat = lat;
        this.coordinate = new Coordinate(lon, lat);
        this.coverage2ds = coverage2ds;
        RegionMap regionMap = coverage2ds[0].getRegionMap();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();
    }

    /**
     * @return the array of values for the requested cell.
     */
    public double[] getValues() {
        double[] values = new double[coverage2ds.length];
        int i = 0;
        for( HMRaster g2d : coverage2ds ) {
            values[i++] = g2d.getValue(coordinate);
        }
        return values;
    }

    /**
     * @return true if all values are the same.
     */
    public boolean allEqual() {
        double[] values = getValues();
        double v1 = values[0];
        for( int i = 1; i < values.length; i++ ) {
            if (!NumericsUtilities.dEq(v1, values[i])) {
                return false;
            }
        }
        return true;
    }

    public void setBufferCells( int bufferCells ) {
        this.bufferCells = bufferCells;
    }

    @Override
    public String toString() {
        int cellChars = 40;

        StringBuilder sepSb = new StringBuilder();
        sepSb.append("+");
        for( int i = 0; i < cellChars + 2; i++ ) {
            sepSb.append("-");
        }
        sepSb.append("+");
        for( int i = 0; i < bufferCells * 2 + 1; i++ ) {
            sepSb.append("-------------------+");
        }
        String sep = sepSb.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(sep).append("\n");
        // show the cols header 
        sb.append("| ").append(StringUtilities.trimOrPadToCount("  ", cellChars)).append(" | ");
        for (int r = bufferCells; r >= -bufferCells; r--) {
        	double la = lat + r * yRes;
	        for (int c = -bufferCells; c <= bufferCells; c++) {
				double lo = lon + c * xRes;
				Point cell = coverage2ds[0].getCell(new Coordinate(lo, la));
				sb.append(String.format(" %16d | ", cell.x));
			}
        }
        sb.append("\n");
        for (int r = bufferCells; r >= -bufferCells; r--) {
            double la = lat + r * yRes;
            sb.append(sep).append("\n");
            for (HMRaster g2d : coverage2ds) {
                String name = StringUtilities.trimOrPadToCount(g2d.getName().toString(), cellChars);
                sb.append("| ").append(name).append(" | ");
                for (int c = -bufferCells; c <= bufferCells; c++) {
                    double lo = lon + c * xRes;
                    double v = g2d.getValue(new Coordinate(lo, la));
                    if (g2d.isNovalue(v)) {
                        sb.append("       -nv-       | ");
                    } else {
                        sb.append(String.format("%17.8f", v)).append(" | ");
                    }
                }
                sb.append("\n");
            }
        }
        sb.append(sep).append("\n");

        return sb.toString();
    }
    
    public String toString2() {
        int cellChars = 40;
        int rowChars  = 8;   // width for the row index column

        // ---- separators ----
        StringBuilder sepSb = new StringBuilder();
        sepSb.append("+");
        for (int i = 0; i < rowChars + 2; i++) {
            sepSb.append("-");
        }
        sepSb.append("+");
        for (int i = 0; i < cellChars + 2; i++) {
            sepSb.append("-");
        }
        sepSb.append("+");
        for (int i = 0; i < bufferCells * 2 + 1; i++) {
            sepSb.append("-------------------+");
        }
        String sep = sepSb.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(sep).append("\n");

        // ---- header: (blank row label) + (blank name cell) + column numbers ----
        sb.append("| ")
          .append(StringUtilities.trimOrPadToCount("", rowChars)).append(" | ")
          .append(StringUtilities.trimOrPadToCount("Name", cellChars)).append(" | ");

        for (int c = -bufferCells; c <= bufferCells; c++) {
            double lo = lon + c * xRes;
            Point cell = coverage2ds[0].getCell(new Coordinate(lo, lat));
            sb.append(String.format("  c = %-11d", cell.x) + " | "); // column indices
        }
        sb.append("\n");

        // ---- body: for each row (north->south), print row index + each raster line ----
        for (int r = bufferCells; r >= -bufferCells; r--) {
            double la = lat + r * yRes;

            // compute the row index once (any column is fine; we pick c = 0 position)
            Point rowCell = coverage2ds[0].getCell(new Coordinate(lon, la));
            String rowIdxStr = String.valueOf(rowCell.y);

            sb.append(sep).append("\n");

            boolean isFirst = true;
            for (HMRaster g2d : coverage2ds) {
                String name = StringUtilities.trimOrPadToCount(g2d.getName().toString(), cellChars);

                // row index column + raster name column
                String pre = "r = ";
                if( isFirst ) {
					isFirst = false;
				} else {
					rowIdxStr = "";
					pre = "    ";
				}
                sb.append("| ")
                .append(pre + StringUtilities.trimOrPadToCount(rowIdxStr, rowChars-4)).append(" | ")
                .append(name).append(" | ");

                // values across columns
                for (int c = -bufferCells; c <= bufferCells; c++) {
                    double lo = lon + c * xRes;
                    double v = g2d.getValue(new Coordinate(lo, la));
                    if (g2d.isNovalue(v)) {
                        sb.append("       -nv-       | ");
                    } else {
                        sb.append(String.format("%17.8f", v)).append(" | ");
                    }
                }
                sb.append("\n");
            }
        }
        sb.append(sep).append("\n");

        return sb.toString();
    }


}
