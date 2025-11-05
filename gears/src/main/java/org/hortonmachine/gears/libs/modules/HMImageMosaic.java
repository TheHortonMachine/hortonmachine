package org.hortonmachine.gears.libs.modules;

import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.geotools.api.parameter.GeneralParameterValue;

/**
 * Wrapper around the geotools image mosaic reader.
 */
public class HMImageMosaic implements AutoCloseable {

	private AbstractGridCoverage2DReader reader;

	public HMImageMosaic(String mosaicPath) {
		AbstractGridFormat format = GridFormatFinder.findFormat(mosaicPath);
		if (format == null) {
			throw new IllegalArgumentException("Cannot find ImageMosaicReader for path: " + mosaicPath);
		}
		reader = format.getReader(mosaicPath);
		if (reader == null) {
			throw new IllegalArgumentException("Cannot create ImageMosaicReader for path: " + mosaicPath);
		}
		if (!(reader instanceof ImageMosaicReader)) {
			throw new IllegalArgumentException(
					"Reader is not an instance of ImageMosaicReader for path: " + mosaicPath);
		}
	}

	/**
	 * Read a raster from the mosaic reader for the given envelope at the original IM reader resolution.
	 * 
	 * @param envelope the envelope to read
	 * @return the grid coverage.
	 * @throws IOException
	 */
	public HMRaster read(Envelope envelope) throws IOException {
		RegionMap reagionMap = CoverageUtilities.getRegionParamsFromImageMosaicReader((ImageMosaicReader) reader);
		double xres = reagionMap.getXres();
		double yres = reagionMap.getYres();
		return read(envelope, xres, yres);
	}

	/**
	 * Read a raster from the mosaic reader for the given envelope at the specified resolution.
	 * 
	 * @param envelope the envelope to read
	 * @param xres     the x resolution
	 * @param yres     the y resolution
	 * @return the grid coverage.
	 * @throws IOException
	 */
	public HMRaster read(Envelope envelope, double xres, double yres) throws IOException {
		GeneralParameterValue[] readParameters = CoverageUtilities.createGridGeometryGeneralParameter(
				xres, yres, envelope.getMaxY(), envelope.getMinY(), envelope.getMaxX(),
				envelope.getMinX(), reader.getCoordinateReferenceSystem());

		GridCoverage2D gridCoverage2D = reader.read(readParameters);
		return HMRaster.fromGridCoverage(gridCoverage2D);
	}
	
	/**
	 * Get the value at a specific coordinate from the mosaic reader.
	 * 
	 * @param coordinate the coordinate to read
	 * @return the value at the coordinate
	 * @throws IOException if no coverage is found for the coordinate
	 */
	public double getValueAt(Coordinate coordinate) throws IOException {
		HMRaster raster = read(new Envelope(coordinate.x, coordinate.x, coordinate.y, coordinate.y));
		if (raster == null) {
			throw new IOException("No coverage found for coordinate: " + coordinate);
		}
		return raster.getValue(0, 0);	
	}

	@Override
	public void close() throws Exception {
		if (reader != null)
			reader.dispose();
	}

}
