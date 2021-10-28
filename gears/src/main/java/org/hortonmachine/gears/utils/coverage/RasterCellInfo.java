package org.hortonmachine.gears.utils.coverage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.StringUtilities;
import org.locationtech.jts.geom.Coordinate;

public class RasterCellInfo {

    private GridCoverage2D[] coverage2ds;
    private double lon;
    private double lat;
    private int bufferCells = 1;
    private RegionMap regionMap;

    public RasterCellInfo( int col, int row, GridCoverage2D... coverage2ds ) {
        this.coverage2ds = coverage2ds;
        Coordinate coordinate = CoverageUtilities.coordinateFromColRow(col, row, coverage2ds[0].getGridGeometry());
        lon = coordinate.x;
        lat = coordinate.y;
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage2ds[0]);
    }

    public RasterCellInfo( double lon, double lat, GridCoverage2D... coverage2ds ) {
        this.lon = lon;
        this.lat = lat;
        this.coverage2ds = coverage2ds;
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage2ds[0]);
    }

    /**
     * @return the array of values for the requested cell.
     */
    public double[] getValues() {
        double[] values = new double[coverage2ds.length];
        int i = 0;
        for( GridCoverage2D g2d : coverage2ds ) {
            double v = CoverageUtilities.getValue(g2d, lon, lat);
            values[i++] = v;
        }
        return values;
    }

    public void setBufferCells( int bufferCells ) {
        this.bufferCells = bufferCells;
    }

    @Override
    public String toString() {
        int cellChars = 40;

        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        double fromLon = lon - xres * bufferCells;
        double toLon = lon + xres * bufferCells;
        double fromLat = lat + yres * bufferCells;
        double toLat = lat - yres * bufferCells;
        
        StringBuilder sepSb = new StringBuilder();
        sepSb.append("+");
        for( int i = 0; i < cellChars+2; i++ ) {
            sepSb.append("-");
        }
        sepSb.append("+");
        for( int i = 0; i < bufferCells * 2 + 1; i++ ) {
            sepSb.append("-------------------+");
        }
        String sep = sepSb.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for( double la = fromLat; la >= toLat; la -= yres ) {
            sb.append(sep + "\n");
            for( GridCoverage2D g2d : coverage2ds ) {
                String name = StringUtilities.trimOrPadToCount(g2d.getName().toString(), cellChars);
                sb.append("| ").append(name).append(" | ");
                double nv = HMConstants.getNovalue(g2d);
                for( double lo = fromLon; lo <= toLon; lo += xres ) {
                    double v = CoverageUtilities.getValue(g2d, lo, la);
                    if (HMConstants.isNovalue(v, nv)) {
                        sb.append("       -nv-       | ");
                    } else {
                        sb.append(String.format("%17.8f", v)).append(" | ");
                    }
                }
                sb.append("\n");
            }
        }
        sb.append(sep + "\n");

        return sb.toString();
    }

}
