package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hillshade.Hillshade;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Hillshade} module.
 * 
 * @author Daniele Andreis
 */
public class TestHillshade extends HMTestCase {


	public void testHillshade() throws Exception {

		// Locale.setDefault(Locale.ITALIAN);

		double[][] elevationData = HMTestMaps.mapData;
		HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3004");
		GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage(
				"elevation", elevationData, envelopeParams, crs, true);

		PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(
				System.out, System.out);

		Hillshade hillshade = new Hillshade();
		hillshade.inElevation = elevationCoverage;
		hillshade.defaultElevation = 45.0;
		hillshade.defaultAzimuth = 315;

		hillshade.pm = pm;

		hillshade.process();

		GridCoverage2D hillshadeCoverage = hillshade.outMap;

		checkMatrixEqual(hillshadeCoverage.getRenderedImage(),
				HMTestMaps.outHillshade, 0.1);
	}

}
