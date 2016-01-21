package org.jgrasstools.hortonmachine.modules.statistics.kriging;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridGeometry2D;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A world coordinate associated with its respective grid point in the context
 * of a certain grid geometry.
 * 
 * <p>
 * This is useful so that there's no need to convert the world point into the
 * grid point by using the mathematical transform, which can lead to rounding
 * errors which in turn invite all kinds of trouble (see
 * https://github.com/moovida/jgrasstools/issues/12, for example)
 * 
 * @author Rafael Almeida (@rafaelalmeida)
 * @since 0.7.9
 *
 */
public class OmsGridPoint {
	private GridGeometry2D gridGeometry;
	private GridCoordinates2D gridCoordinates;
	private Coordinate worldCoordinates;
	
	public GridCoordinates2D getGridCoordinates() {
		return gridCoordinates;
	}
	
	public void setGridCoordinates(GridCoordinates2D gridCoordinates) {
		this.gridCoordinates = gridCoordinates;
	}
	
	public Coordinate getWorldCoordinates() {
		return worldCoordinates;
	}
	
	public void setWorldCoordinates(Coordinate worldCoordinates) {
		this.worldCoordinates = worldCoordinates;
	}

	public GridGeometry2D getGridGeometry() {
		return gridGeometry;
	}

	public void setGridGeometry(GridGeometry2D gridGeometry) {
		this.gridGeometry = gridGeometry;
	}

	@Override
	public String toString() {
		return "OmsGridPoint [gridCoordinates=" + gridCoordinates
				+ ", worldCoordinates=" + worldCoordinates + "]";
	}
	
}
