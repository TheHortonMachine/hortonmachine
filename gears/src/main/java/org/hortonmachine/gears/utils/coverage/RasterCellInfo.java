package org.hortonmachine.gears.utils.coverage;

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
    private RegionMap regionMap;
	private Coordinate coordinate;

    public RasterCellInfo( int col, int row, GridCoverage2D... coverage2ds ) {
        this.coverage2ds = new HMRaster[coverage2ds.length];
        for( int i = 0; i < coverage2ds.length; i++ ) {
			this.coverage2ds[i] = HMRaster.fromGridCoverage(coverage2ds[i]);
		}
        coordinate = CoverageUtilities.coordinateFromColRow(col, row, coverage2ds[0].getGridGeometry());
        lon = coordinate.x;
        lat = coordinate.y;
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage2ds[0]);
    }

    public RasterCellInfo( double lon, double lat, GridCoverage2D... coverage2ds ) {
        this.lon = lon;
        this.lat = lat;
        this.coordinate = new Coordinate(lon, lat);
        this.coverage2ds = new HMRaster[coverage2ds.length];
        for( int i = 0; i < coverage2ds.length; i++ ) {
			this.coverage2ds[i] = HMRaster.fromGridCoverage(coverage2ds[i]);
        }
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage2ds[0]);
    }
    
    public RasterCellInfo( int col, int row, HMRaster... coverage2ds ) {
        this.coverage2ds = coverage2ds;
        coordinate = coverage2ds[0].getWorld(col, row);	
        lon = coordinate.x;
        lat = coordinate.y;
        regionMap = coverage2ds[0].getRegionMap();
    }

    public RasterCellInfo( double lon, double lat, HMRaster... coverage2ds ) {
        this.lon = lon;
        this.lat = lat;
        this.coordinate = new Coordinate(lon, lat);
        this.coverage2ds = coverage2ds;
        regionMap = coverage2ds[0].getRegionMap();
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

        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

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
        for (int r = bufferCells; r >= -bufferCells; r--) {
            double la = lat + r * yres;
            sb.append(sep).append("\n");
            for (HMRaster g2d : coverage2ds) {
                String name = StringUtilities.trimOrPadToCount(g2d.getName().toString(), cellChars);
                sb.append("| ").append(name).append(" | ");
                for (int c = -bufferCells; c <= bufferCells; c++) {
                    double lo = lon + c * xres;
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
