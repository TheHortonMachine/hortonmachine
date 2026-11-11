package org.hortonmachine.gears.libs.modules;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.io.vrt.VrtImageMosaic;
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

	/**
	 * @param mosaicPath the shapefile of the mosaic index or a GDAL VRT file, served through the
	 *                   mosaic of {@link VrtImageMosaic}.
	 */
	public HMImageMosaic(String mosaicPath) {
		if (mosaicPath.toLowerCase().endsWith(".vrt")) {
			try {
				mosaicPath = VrtImageMosaic.getIndex(new File(mosaicPath)).getAbsolutePath();
			} catch (UnsupportedOperationException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot make the mosaic of the VRT: " + mosaicPath, e);
			}
		}
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
		return HMRaster.fromGridCoverage(toZeroOrigin(gridCoverage2D));
	}

	/**
	 * The mosaic reader gives the windows that are not aligned with its grid as images that don't
	 * start at 0, 0, while {@link HMRaster} reads its cells from 0, 0: the cells are moved there.
	 */
	private static GridCoverage2D toZeroOrigin(GridCoverage2D coverage) {
		RenderedImage image = coverage.getRenderedImage();
		if (image.getMinX() == 0 && image.getMinY() == 0) {
			return coverage;
		}
		Raster data = image.getData();
		WritableRaster raster = data.createCompatibleWritableRaster(image.getWidth(), image.getHeight());
		raster.setRect(-image.getMinX(), -image.getMinY(), data);
		RegionMap region = RegionMap.fromGridGeometry(coverage.getGridGeometry());
		Double novalue = CoverageUtilities.getNovalue(coverage);
		return CoverageUtilities.buildCoverageWithNovalue(coverage.getName().toString(), raster, region,
				coverage.getCoordinateReferenceSystem(), novalue != null ? novalue : HMConstants.doubleNovalue);
	}
	
	/**
	 * Get the value at a specific coordinate from the mosaic reader.
	 * 
	 * @param coordinate the coordinate to read
	 * @return the value at the coordinate
	 * @throws IOException if no coverage is found for the coordinate
	 */
	public double getValueAt(Coordinate coordinate) throws IOException {
		// a window of a few cells around the point: an empty envelope has no grid to read
		RegionMap regionMap = CoverageUtilities.getRegionParamsFromImageMosaicReader((ImageMosaicReader) reader);
		double xres = regionMap.getXres();
		double yres = regionMap.getYres();
		CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
		GeneralParameterValue[] readParameters = CoverageUtilities.createGridGeometryGeneralParameter(xres, yres,
				coordinate.y + yres, coordinate.y - yres, coordinate.x + xres, coordinate.x - xres, crs);
		GridCoverage2D coverage = reader.read(readParameters);
		if (coverage == null) {
			throw new IOException("No coverage found for coordinate: " + coordinate);
		}
		try {
			// the coverage finds the cell also when its image doesn't start at 0, 0
			double[] values = coverage.evaluate((Position) new Position2D(crs, coordinate.x, coordinate.y),
					(double[]) null);
			return values[0];
		} catch (PointOutsideCoverageException e) {
			throw new IOException("No coverage found for coordinate: " + coordinate, e);
		} finally {
			coverage.dispose(true);
		}
	}

	@Override
	public void close() throws Exception {
		if (reader != null)
			reader.dispose();
	}

}
