package org.hortonmachine.mapcalc;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import it.geosolutions.jaiext.jiffle.JiffleBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MapcalcJiffler {

    public static final String RESULT_MAP_NAME = "result";

    private String function;
    private HashMap<String, RenderedImage> sourceImgParams = new HashMap<String, RenderedImage>();

    private String errorMessage = null;
    private HashMap<String, String> name2PathMap;
    private String resultPath;

    /**
     * Constructor.
     * 
     * @param function the script.
     * @param resultPath the path to the return map. The map name has to be RESULT.
     * @param name2PathMap the map of names of maps and their paths to be read.
     * @throws IOException
     */
    public MapcalcJiffler( String function, String resultPath, HashMap<String, String> name2PathMap ) throws IOException {
        this.function = function;
        this.resultPath = resultPath;
        this.name2PathMap = name2PathMap;
    }

    public String exec( IHMProgressMonitor pm ) throws Exception {
        ReferencedEnvelope bounds = null;
        RegionMap regionMap = null;
        CoordinateReferenceSystem crs = null;

        try {
            pm.beginTask("Reading input maps...", name2PathMap.size());
            for( Entry<String, String> entry : name2PathMap.entrySet() ) {
                String mapName = entry.getKey();
                if (function.indexOf(mapName) != -1) {
                    GridCoverage2D gridCoverage2D = OmsRasterReader.readRaster(entry.getValue());
                    RenderedImage renderedImage = gridCoverage2D.getRenderedImage();
                    sourceImgParams.put(mapName, renderedImage);

                    if (bounds == null) {
                        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(gridCoverage2D);
                        crs = gridCoverage2D.getCoordinateReferenceSystem();
                        ReferencedEnvelope renv = new ReferencedEnvelope(regionMap.getWest(), regionMap.getEast(),
                                regionMap.getSouth(), regionMap.getNorth(), crs);
                        bounds = renv;
                    }
                }
                pm.worked(1);
            }
            pm.done();
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = "An error occurred while reading the input maps.";
            return errorMessage;
        }

        pm.beginTask("Adding source maps...", sourceImgParams.size());
        JiffleBuilder interp = new JiffleBuilder();
        interp.script(function);
        for( Entry<String, RenderedImage> entry : sourceImgParams.entrySet() ) {
            interp.source(entry.getKey(), entry.getValue());
            pm.worked(1);
        }
        pm.done();
        pm.beginTask("Building output map...", 1);
        interp.dest(RESULT_MAP_NAME, regionMap.getCols(), regionMap.getRows());
        pm.worked(1);
        pm.done();

        pm.beginTask("Executing mapcalc...", IHMProgressMonitor.UNKNOWN);
        interp.run();
        RenderedImage image = interp.getImage(RESULT_MAP_NAME);
        pm.done();

        GridCoverage2D coverage = CoverageUtilities.buildCoverage(RESULT_MAP_NAME, image, regionMap, crs);
        OmsRasterWriter.writeRaster(resultPath, coverage);

        return errorMessage;

    }

}