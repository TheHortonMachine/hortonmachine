/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.jgrassgears.io.grass;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.geometry.Envelope2D;

import eu.hydrologis.jgrass.jgrassgears.io.grass.core.GrassBinaryRasterWriteHandler;
import eu.hydrologis.jgrass.jgrassgears.io.grass.spi.GrassBinaryImageWriterSpi;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.IHMProgressMonitor;

/**
 * Coverage Writer class for writing GRASS raster maps.
 * <p>
 * The class writes a GRASS raster map to a GRASS workspace (see package documentation for further
 * info). The writing is really done via Imageio extended classes.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 3.0
 * @see GrassBinaryImageWriter
 * @see GrassBinaryRasterWriteHandler
 */
public class GrassCoverageWriter {
    private File output;
    private final IHMProgressMonitor monitor;

    /**
     * Constructor for the {@link GrassCoverageWriter}.
     */
    public GrassCoverageWriter( File output, IHMProgressMonitor monitor ) {
        this.output = output;
        this.monitor = monitor;
    }

    /**
     * Writes the {@link GridCoverage2D supplied coverage} to disk.
     * <p>
     * Note that this also takes care to cloes the file handle after writing to disk.
     * </p>
     * 
     * @param gridCoverage2D the coverage to write.
     * @throws IOException
     */
    public void write( GridCoverage2D gridCoverage2D ) throws IOException {
        try {
            Envelope2D env = gridCoverage2D.getEnvelope2D();
            GridEnvelope2D worldToGrid = gridCoverage2D.getGridGeometry().worldToGrid(env);

            double xRes = env.getWidth() / worldToGrid.getWidth();
            double yRes = env.getHeight() / worldToGrid.getHeight();

            JGrassRegion region = new JGrassRegion(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), xRes, yRes);

            GrassBinaryImageWriterSpi writerSpi = new GrassBinaryImageWriterSpi();
            GrassBinaryImageWriter writer = new GrassBinaryImageWriter(writerSpi, monitor);
            monitor.beginTask("Retrieving image from coverage.", IHMProgressMonitor.UNKNOWN);
            RenderedImage renderedImage = gridCoverage2D.view(ViewType.GEOPHYSICS).getRenderedImage();
            monitor.done();
            writer.setOutput(output, region);
            writer.write(renderedImage);
            writer.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void write( GridCoverage2D gridCoverage2D, JGrassRegion writeRegion ) throws IOException {
        GrassBinaryImageWriterSpi writerSpi = new GrassBinaryImageWriterSpi();
        GrassBinaryImageWriter writer = new GrassBinaryImageWriter(writerSpi, monitor);
        monitor.beginTask("Retrieving image from coverage.", IHMProgressMonitor.UNKNOWN);
        RenderedImage renderedImage = gridCoverage2D.view(ViewType.GEOPHYSICS).getRenderedImage();
        monitor.done();
        writer.setOutput(output, writeRegion);
        writer.write(renderedImage);
        writer.dispose();
    }

}
