package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.energyindexcalculator.EnergyIndexCalculator;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.io.eicalculator.EIAltimetry;
import eu.hydrologis.jgrass.jgrassgears.io.eicalculator.EIAreas;
import eu.hydrologis.jgrass.jgrassgears.io.eicalculator.EIEnergy;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

/**
 * Test {@link EnergyIndexCalculator}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestEnergyIndexCalculator extends HMTestCase {

    /**
     * TODO make this test a bit more serious.
     * 
     * @throws Exception
     */
    public void testEnergyIndexCalculator() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        double[][] aspectData = HMTestMaps.aspectDataRadiants;
        GridCoverage2D aspectCoverage = CoverageUtilities.buildCoverage("aspect", aspectData, envelopeParams, crs);
        double[][] nablaData = HMTestMaps.nablaData0;
        GridCoverage2D nablaCoverage = CoverageUtilities.buildCoverage("nabla", nablaData, envelopeParams, crs);
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs);
        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs);
        double[][] subbasinsData = HMTestMaps.basinDataNN0;
        GridCoverage2D subbasinsCoverage = CoverageUtilities.buildCoverage("subbasins", subbasinsData, envelopeParams, crs);

        EnergyIndexCalculator eiCalculator = new EnergyIndexCalculator();
        eiCalculator.inAspect = aspectCoverage;
        eiCalculator.inCurvatures = nablaCoverage;
        eiCalculator.inDem = pitCoverage;
        eiCalculator.inSlope = slopeCoverage;
        eiCalculator.inBasins = subbasinsCoverage;
        eiCalculator.pDt = 1;
        eiCalculator.pEi = 2;
        eiCalculator.pEs = 2;
        eiCalculator.pm = pm;

        eiCalculator.executeEnergyIndexCalculator();

        List<EIAltimetry> altimetricValues = eiCalculator.outAltimetry;
        List<EIEnergy> energeticValues = eiCalculator.outEnergy;
        List<EIAreas> areaValues = eiCalculator.outArea;

        EIAltimetry eiAltimetry = altimetricValues.get(0);
        assertEquals(1, eiAltimetry.basinId);
        assertEquals(0, eiAltimetry.altimetricBandId);
        assertEquals(737.5, eiAltimetry.elevationValue);
        assertEquals(75.0, eiAltimetry.bandRange);

        EIEnergy eiEnergy = energeticValues.get(0);
        assertEquals(1, eiEnergy.basinId);
        assertEquals(0, eiEnergy.energeticBandId);
        assertEquals(0, eiEnergy.virtualMonth);
        assertEquals(0.09808943859674346, eiEnergy.energyValue, 0.0001);

        EIAreas eiAreas = areaValues.get(0);
        assertEquals(1, eiAreas.basinId);
        assertEquals(0, eiAreas.altimetricBandId);
        assertEquals(0, eiAreas.energyBandId);
        assertEquals(0.0, eiAreas.areaValue, 0.0001);

    }

}
