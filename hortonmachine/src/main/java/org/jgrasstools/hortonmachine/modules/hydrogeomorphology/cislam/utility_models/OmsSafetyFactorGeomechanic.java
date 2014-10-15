package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_outSafetyactorGeoMechanic_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inCohesion_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inGamma_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inKsat_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_s_DESCRIPTION;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_NAME)
@Status(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsSafetyFactorGeomechanic extends JGTModel {

	@Description(OMSCISLAM_inSlope_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public GridCoverage2D inSlope = null;

	@Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("m")
	@In
	public GridCoverage2D inSoilThickness = null;

	// #############################################
	// Geo-Techical parameters
	// #############################################
	@Description(OMSCISLAM_inCohesion_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("kPa")
	@In
	public GridCoverage2D inCohesion = null;

	@Description(OMSCISLAM_inPhi_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public GridCoverage2D inPhi = null;

	@Description(OMSCISLAM_inGamma_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public GridCoverage2D inGammaSoil = null;

	@Description(OMSCISLAM_inKsat_DESCRIPTION)
	@Unit("m/s")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public GridCoverage2D inKsat = null;

	@Description(OMSCISLAM_inTheta_s_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public GridCoverage2D inTheta_s = null;

	@Description(OMSCISLAM_inTheta_r_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public GridCoverage2D inTheta_r = null;

	@Description(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_outSafetyactorGeoMechanic_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Out
	public GridCoverage2D outSafetyactorGeoMechanic = null;

	@Execute
	public void process() {

		/*
		 * model.inSlope = inSlope; model.inPhi = inPhi; model.inCohesion =
		 * inCohesion; model.inGammaSoil = inGammaSoil; model.inSoilThickness =
		 * inSoilThickness;
		 */

		/**
		 * Computes a geo-technical SafetyFactor Map This calculation does not
		 * take into account hydrologic factors that may negatively affect the
		 * result. This means that map areas that do not even stand the test of
		 * this geo-technical safety factor will surely result in not stable
		 * areas even when hydrology is taken into account.
		 * 
		 * @param slopeMap
		 *            The user provided slope map: can be computed using
		 *            {@link OmsSlope} model
		 * @param phiMap
		 *            The user provided phi map (effective frictional angle)
		 * @param cochesionMap
		 *            The user provided effective cohesion map
		 * @param gammaMap
		 *            The user provided gamma map (density)
		 * @param soil_thicknessMap
		 *            The user provided soil_thickness map: can be computed
		 *            using {@link OmsSoilThickness} model
		 * @param pm2
		 * @return
		 */
		RegionMap regionMap = CoverageUtilities
				.getRegionParamsFromGridCoverage(inSlope);
		int cols = regionMap.getCols();
		int rows = regionMap.getRows();
		double xRes = regionMap.getXres();
		double yRes = regionMap.getYres();

		CoordinateReferenceSystem crs = inSlope.getCoordinateReferenceSystem();

		RandomIter slopeIter = CoverageUtilities.getRandomIterator(inSlope);
		RandomIter phiIter = CoverageUtilities.getRandomIterator(inPhi);
		RandomIter cochesionIter = CoverageUtilities
				.getRandomIterator(inCohesion);
		RandomIter gammaIter = CoverageUtilities.getRandomIterator(inGammaSoil);
		RandomIter soil_thicknessIter = CoverageUtilities
				.getRandomIterator(inSoilThickness);

		WritableRaster outSafetyFactorGeoTechnicalWR = CoverageUtilities
				.createDoubleWritableRaster(cols, rows, null, null,
						JGTConstants.doubleNovalue);
		WritableRandomIter outSafetyFactorGeoTechnicalIter = RandomIterFactory
				.createWritable(outSafetyFactorGeoTechnicalWR, null);

		pm.beginTask("Start Safety Factor computation..", rows); //$NON-NLS-1$

		double slope, phi, cohesion, gamma, soil_thickness, FSi;
		// Cycling into the valid region.
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {

				if (!isNovalue(slopeIter.getSampleDouble(c, r, 0))) {

					slope = slopeIter.getSampleDouble(c, r, 0);
					phi = phiIter.getSampleDouble(c, r, 0);
					cohesion = cochesionIter.getSampleDouble(c, r, 0);
					gamma = gammaIter.getSampleDouble(c, r, 0);
					soil_thickness = soil_thicknessIter
							.getSampleDouble(c, r, 0);

					// Quickfix to inconsistent input
					// TODO : remove this once model implements input check
					// https://bitbucket.org/mcfoi/jgrasstool-clone/issue/1/quickfix-to-inconsistent-input-in
					double[] args = checkInputQuickfix(new double[] { slope,
							phi, cohesion, gamma, soil_thickness }, pm);
					slope = args[0];
					phi = args[1];
					cohesion = args[2];
					gamma = args[3];
					soil_thickness = args[4];
					// end of Quickfix

					FSi = Math.tan(phi / 180 * Math.PI)
							/ slope
							+ (2 * cohesion)
							/ (gamma * soil_thickness * Math.sin(2 * Math
									.atan(slope)));
					if (Double.isInfinite(FSi))
						System.out.println(FSi);
					else
						outSafetyFactorGeoTechnicalIter.setSample(c, r, 0, FSi);
				}
			}
			pm.worked(1);
		}
		pm.done();

		GridCoverage2D outSafetyFactor = CoverageUtilities.buildCoverage(
				"safetyfactor", outSafetyFactorGeoTechnicalWR, regionMap, crs);
		outSafetyactorGeoMechanic = outSafetyFactor;

	}

	/**
	 * Checks whether Safety Factor inputs satisfy computational requirements
	 * This method is deprecated and has to be replaced with sound input
	 * checking policy at model level
	 * 
	 * @deprecated
	 * @param args
	 *            slope phi cohesion gamma soil_thicknes
	 * @return double[] containing computational safe values
	 */
	private static double[] checkInputQuickfix(double[] args,
			IJGTProgressMonitor pm) {

		for (int i = 0; i < args.length; i++) {
			if (args[i] == 0.0) {
				String message = null;
				switch (i) {
				case 0:
					message = "slope";
					pm.errorMessage("An invalid value of " + message
							+ " has been fixed on the fly! [" + args[i] + "]");
					args[i] = 0.009; // slope
					pm.errorMessage("<==" + args[i]);
					break;
				case 1:
					message = "phi";
					pm.errorMessage("An invalid value of " + message
							+ " has been fixed on the fly! [" + args[i] + "]");
					args[i] = 30.0; // phi
					pm.errorMessage("<==" + args[i]);
					break;
				case 2:
					message = "cohesion";
					pm.errorMessage("An invalid value of " + message
							+ " has been fixed on the fly! [" + args[i] + "]");
					args[i] = 1.0; // cohesion
					pm.errorMessage("<==" + args[i]);
					break;
				case 3:
					message = "gamma";
					pm.errorMessage("An invalid value of " + message
							+ " has been fixed on the fly! [" + args[i] + "]");
					args[i] = 18.0; // gamma soil
					pm.errorMessage("<==" + args[i]);
					break;
				case 4:
					message = "soil_thicknes";
					pm.errorMessage("\nAn invalid value of " + message
							+ " has been fixed on the fly! [" + args[i] + "]");
					args[i] = 0.1; // soil_thicknes
					pm.errorMessage("<==" + args[i] + "\n");
					break;
				default:
					break;
				}
			}
		}
		return args;
	}

}
