package org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.LinkedHashMap;

import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;

/**
 * Implements the InterpolationDataProvider interface for raster data. This
 * class extracts the coordinates and cell values from a GridCoverage2D.
 */
public class RasterInterpolationProvider implements InterpolationDataProvider {
	private GridCoverage2D gridCoverage;

	/**
	 * Constructor to initialize the raster interpolation provider.
	 *
	 * @param gridCoverage The input raster data as a GridCoverage2D.
	 */
	public RasterInterpolationProvider(GridCoverage2D gridCoverage) {
		this.gridCoverage = gridCoverage;
	}

	/**
	 * Extracts the coordinate of each pixel from the input grid. Each pixel's
	 * center is calculated, transformed to grid coordinates, and its value is
	 * retrieved from a WritableRaster.
	 *
	 * @return A LinkedHashMap where each key is a unique integer identifier and
	 *         each value is a Coordinate (x, y, and z where z is the cell value).
	 */
	@Override
	public LinkedHashMap<Integer, Coordinate> getCoordinates() {
		// Retrieve the grid geometry from the raster.
		GridGeometry2D grid = gridCoverage.getGridGeometry();
		LinkedHashMap<Integer, Coordinate> out = new LinkedHashMap<>();
		int count = 0;

		// Convert the grid geometry into a region map containing parameters like rows,
		// columns, resolution, etc.
		RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(grid);
		int cols = regionMap.getCols();
		int rows = regionMap.getRows();
		double south = regionMap.getSouth();
		double west = regionMap.getWest();
		double xres = regionMap.getXres();
		double yres = regionMap.getYres();

		// Obtain the transform to convert world coordinates to grid coordinates.
		final Position gridPoint = new Position2D();
		WritableRaster demWR = mapsTransform(gridCoverage);

		// Prepare the WritableRaster by replacing no-value (-9999.0) if needed.
		MathTransform transf = grid.getCRSToGrid2D();

		// Loop through each column and row to compute the coordinate of each pixel.
		for (int i = 0; i < cols; i++) {
			// For each column, compute the x coordinate (center of the pixel).
			for (int j = 0; j < rows; j++) {
				// Create a new coordinate object.
				Coordinate coordinate = new Coordinate();
				// Compute the pixel's center. Adjusting by xres and yres centers the coordinate
				// within the cell.
				coordinate.x = west + i * xres;
				coordinate.y = south + j * yres;
				// Create a DirectPosition with the calculated coordinate in the grid's CRS.
				Position point = new Position2D(grid.getCoordinateReferenceSystem(), coordinate.x,
						coordinate.y);
				try {
					// Transform the world coordinate to grid coordinate space.
					// Potential improvements: If transformation fails, consider throwing a custom
					// runtime exception.
					// You might throw a custom exception, e.g., new
					// InterpolationDataException("Transformation error", e);
					transf.transform(point, gridPoint);
				} catch (MismatchedDimensionException e) {
					// Log and handle the exception appropriately.
					// Consider rethrowing a runtime exception instead of just printing the stack
					// trace.
					e.printStackTrace();
				} catch (TransformException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Retrieve the transformed grid coordinates.
				double[] gridCoord = gridPoint.getCoordinate();
				// Cast the grid coordinates to int, as they represent pixel indices.
				int x = (int) gridCoord[0];
				int y = (int) gridCoord[1];
				// Get the sample value (e.g., elevation) from the raster at the calculated grid
				// indices.
				coordinate.z = demWR.getSample(x, y, 0);
				out.put(count, coordinate);
				count++;
			}
		}

		return out;
	}

	/**
	 * Maps reader transform the GrifCoverage2D in to the writable raster and
	 * replace the -9999.0 value with no value.
	 *
	 * @param inValues: the input map values
	 * @return the writable raster of the given map
	 */
	private WritableRaster mapsTransform(GridCoverage2D inValues) {
		RenderedImage inValuesRenderedImage = inValues.getRenderedImage();
		WritableRaster inValuesWR = CoverageUtilities.replaceNovalue(inValuesRenderedImage, -9999.0);
		inValuesRenderedImage = null;
		return inValuesWR;
	}

	@Override
	public double getValueAt(Coordinate coordinate) {
		// In many cases, the value is already the z-value from the cell center.
		return coordinate.z;
	}
}
