package org.hortonmachine.hmachine.modules.geomorphology.multitca;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_inCp9_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_outMultiTca_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithm;

@Description(OMSMULTITCA_DESCRIPTION)
@Documentation(OMSMULTITCA_DOCUMENTATION)
@Author(name = OMSMULTITCA_AUTHORNAMES, contact = OMSMULTITCA_AUTHORCONTACTS)
@Keywords(OMSMULTITCA_KEYWORDS)
@Label(OMSMULTITCA_LABEL)
@Name(OMSMULTITCA_NAME)
@Status(OMSMULTITCA_STATUS)
@License(OMSMULTITCA_LICENSE)
public class OmsMultiTca extends HMModel {
    @Description(OMSMULTITCA_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSMULTITCA_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSMULTITCA_inCp9_DESCRIPTION)
    @In
    public GridCoverage2D inCp9 = null;

    @Description(OMSMULTITCA_outMultiTca_DESCRIPTION)
    @Out
    public GridCoverage2D outMultiTca = null;

    // the flow direction.
    private int[][] dir = ModelsSupporter.DIR_WITHFLOW_EXITING;
    // the incoming flow direction.
    private int[][] dirIn = ModelsSupporter.DIR_WITHFLOW_ENTERING;

    @Execute
    public void process() {
        if (!concatOr(outMultiTca == null, doReset)) {
            return;
        }
        checkNull(inPit, inFlow, inCp9);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        // pm.message();

        @SuppressWarnings("unused")
        int ipos, jpos, i, j, ncicli = 0;
        double sum, delta, pos;

        // create new matrix
        double[] elevationArray = new double[cols * rows];
        double[] indexOfElevation = new double[cols * rows];

        // pm.message();

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        RandomIter pitIter = CoverageUtilities.getRandomIterator(inPit);
        RandomIter cp9Iter = CoverageUtilities.getRandomIterator(inCp9);
        WritableRaster alreadyDonePixelWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, 0.0);
        WritableRandomIter alreadyDoneIter = RandomIterFactory.createWritable(alreadyDonePixelWR, null);
        WritableRaster multiTcaWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, 1.0);
        WritableRandomIter multiTcaIter = RandomIterFactory.createWritable(multiTcaWR, null);

        /*
         * store the value of elevation in an array
         */
        for( int t = 0; t < rows; t++ ) {
            for( int s = 0; s < cols; s++ ) {
                elevationArray[((t) * cols) + s] = pitIter.getSampleDouble(s, t, 0);
                indexOfElevation[((t) * cols) + s] = ((t) * cols) + s + 1;
            }
        }

        /*
         * sorted the array of elevation.
         */
        // pm.message();
        try {
            QuickSortAlgorithm sortAlgorithm = new QuickSortAlgorithm(pm);
            sortAlgorithm.sort(elevationArray, indexOfElevation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * start to working with the highest value of elevation.
         */
        for( int l = cols * rows - 1; l >= 0; l-- ) {
            ncicli = cols * rows - l;
            if (elevationArray[l] <= 0) {
                break;
            } else {

                pos = indexOfElevation[l];
                // extract the index of the matrix from the arrays index.
                i = (int) (pos - 1) % cols;
                j = (int) (pos - 1) / cols;

                if (alreadyDoneIter.getSampleDouble(i, j, 0) == 0.0) {

                    alreadyDoneIter.setSample(i, j, 0, 1.0);
                    if (cp9Iter.getSampleDouble(i, j, 0) == 10 || cp9Iter.getSampleDouble(i, j, 0) == 20
                            || cp9Iter.getSampleDouble(i, j, 0) == 30 || cp9Iter.getSampleDouble(i, j, 0) == 40
                            || cp9Iter.getSampleDouble(i, j, 0) == 50 || cp9Iter.getSampleDouble(i, j, 0) == 60) {
                        sum = 0;
                        for( int k = 1; k <= 8; k++ ) {
                            ipos = i + dir[k][0];
                            jpos = j + dir[k][1];
                            delta = pitIter.getSampleDouble(i, j, 0) - pitIter.getSampleDouble(ipos, jpos, 0);
                            if (delta == 0) {
                                if (alreadyDoneIter.getSampleDouble(ipos, jpos, 0) == 0.0
                                        && flowIter.getSampleDouble(ipos, jpos, 0) == dirIn[k][2]) {
                                    resolveFlat(ipos, jpos, cols, rows, pitIter, multiTcaIter, alreadyDoneIter, flowIter, cp9Iter);
                                }
                            }
                            if (delta > 0.0 && pitIter.getSampleDouble(ipos, jpos, 0) > 0.0) {
                                sum += delta;
                            }
                        }
                        for( int k = 1; k <= 8; k++ ) {
                            ipos = i + dir[k][0];
                            jpos = j + dir[k][1];
                            delta = pitIter.getSampleDouble(i, j, 0) - pitIter.getSampleDouble(ipos, jpos, 0);
                            if (delta > 0.0 && pitIter.getSampleDouble(ipos, jpos, 0) > 0.0) {
                                multiTcaIter.setSample(ipos, jpos, 0,
                                        multiTcaIter.getSampleDouble(ipos, jpos, 0) + multiTcaIter.getSampleDouble(i, j, 0)
                                                * (delta / sum));
                            } else if (delta == 0.0 && flowIter.getSampleDouble(i, j, 0) == dirIn[k][2]) {
                                multiTcaIter.setSample(ipos, jpos, 0,
                                        multiTcaIter.getSampleDouble(ipos, jpos, 0) + multiTcaIter.getSampleDouble(i, j, 0));
                            }
                        }

                    } else if (cp9Iter.getSampleDouble(i, j, 0) == 70 || cp9Iter.getSampleDouble(i, j, 0) == 80
                            || cp9Iter.getSampleDouble(i, j, 0) == 90) {
                        for( int k = 1; k <= 8; k++ ) {
                            ipos = i + dir[k][0];
                            jpos = j + dir[k][1];
                            double delta2 = pitIter.getSampleDouble(i, j, 0) - pitIter.getSampleDouble(ipos, jpos, 0);
                            if (delta2 == 0) {
                                if (alreadyDoneIter.getSampleDouble(ipos, jpos, 0) == 0.0
                                        && flowIter.getSampleDouble(ipos, jpos, 0) == dirIn[k][2]) {

                                    resolveFlat(ipos, jpos, cols, rows, pitIter, multiTcaIter, alreadyDoneIter, flowIter, cp9Iter);
                                }
                            }
                        }
                        for( int k = 1; k <= 8; k++ ) {
                            ipos = i + dir[k][0];
                            jpos = j + dir[k][1];
                            if (flowIter.getSampleDouble(i, j, 0) != 10 && flowIter.getSampleDouble(i, j, 0) == dir[k][2]) {

                                multiTcaIter.setSample(ipos, jpos, 0,
                                        multiTcaIter.getSampleDouble(ipos, jpos, 0) + multiTcaIter.getSampleDouble(i, j, 0));
                                break;
                            }
                        }
                    }

                }

            }
        }
        for( int t = 0; t < rows; t++ ) {
            for( int s = 0; s < cols; s++ ) {
                if (isNovalue(cp9Iter.getSampleDouble(s, t, 0)) || isNovalue(flowIter.getSampleDouble(s, t, 0)))
                    multiTcaIter.setSample(s, t, 0, HMConstants.doubleNovalue);
            }
        }

        outMultiTca = CoverageUtilities.buildCoverage("multiTca", multiTcaWR, regionMap, inFlow.getCoordinateReferenceSystem());

    }
    private int resolveFlat( int ipos, int jpos, int cols, int rows, RandomIter pitRandomIter,
            WritableRandomIter multitcaRandomIter, WritableRandomIter segnaRandomIter, RandomIter flowRandomIter,
            RandomIter cp3RandomIter ) {
        double delta2 = 0;
        double sum2 = 0;
        int count = 0;

        segnaRandomIter.setSample(ipos, jpos, 0, 1.0);
        if (cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 10 || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 20
                || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 30 || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 40
                || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 50 || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 60) {
            for( int k = 1; k <= 8; k++ ) {
                int ipos2 = ipos + dir[k][0];
                int jpos2 = jpos + dir[k][1];
                delta2 = pitRandomIter.getSampleDouble(ipos, jpos, 0) - pitRandomIter.getSampleDouble(ipos2, jpos2, 0);
                if (delta2 == 0) {
                    if (segnaRandomIter.getSampleDouble(ipos2, jpos2, 0) == 0.0
                            && flowRandomIter.getSampleDouble(ipos2, jpos2, 0) == dirIn[k][2]) {
                        resolveFlat(ipos2, jpos2, cols, rows, pitRandomIter, multitcaRandomIter, segnaRandomIter, flowRandomIter,
                                cp3RandomIter);
                    }
                }
                if (delta2 > 0.0 && pitRandomIter.getSampleDouble(ipos2, jpos2, 0) > 0.0) {
                    sum2 += delta2;
                }
            }
            for( int k = 1; k <= 8; k++ ) {
                int ipos2 = ipos + dir[k][0];
                int jpos2 = jpos + dir[k][1];
                delta2 = pitRandomIter.getSampleDouble(ipos, jpos, 0) - pitRandomIter.getSampleDouble(ipos2, jpos2, 0);
                if (delta2 > 0.0 && pitRandomIter.getSampleDouble(ipos2, jpos2, 0) > 0.0) {
                    multitcaRandomIter.setSample(ipos2, jpos2, 0, multitcaRandomIter.getSampleDouble(ipos2, jpos2, 0)
                            + multitcaRandomIter.getSampleDouble(ipos, jpos, 0) * (delta2 / sum2));
                } else if (delta2 == 0.0 && flowRandomIter.getSampleDouble(ipos, jpos, 0) == dir[k][2]) {
                    multitcaRandomIter.setSample(ipos2, jpos2, 0, multitcaRandomIter.getSampleDouble(ipos2, jpos2, 0)
                            + multitcaRandomIter.getSampleDouble(ipos, jpos, 0));
                }
            }
        } else if (cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 70 || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 80
                || cp3RandomIter.getSampleDouble(ipos, jpos, 0) == 90) {

            for( int k = 1; k <= 8; k++ ) {

                int ipos2 = ipos + dir[k][0];
                int jpos2 = jpos + dir[k][1];

                delta2 = pitRandomIter.getSampleDouble(ipos, jpos, 0) - pitRandomIter.getSampleDouble(ipos2, jpos2, 0);
                if (delta2 == 0) {
                    if (segnaRandomIter.getSampleDouble(ipos2, jpos2, 0) == 0.0
                            && flowRandomIter.getSampleDouble(ipos2, jpos2, 0) == dirIn[k][2]) {
                        resolveFlat(ipos2, jpos2, cols, rows, pitRandomIter, multitcaRandomIter, segnaRandomIter, flowRandomIter,
                                cp3RandomIter);
                    }
                }

            }

            for( int k = 1; k <= 8; k++ ) {

                int ipos2 = ipos + dir[k][0];
                int jpos2 = jpos + dir[k][1];

                if (flowRandomIter.getSampleDouble(ipos, jpos, 0) != 10
                        && flowRandomIter.getSampleDouble(ipos, jpos, 0) == dir[k][2]) {

                    multitcaRandomIter.setSample(ipos2, jpos2, 0, multitcaRandomIter.getSampleDouble(ipos2, jpos2, 0)
                            + multitcaRandomIter.getSampleDouble(ipos, jpos, 0));
                    break;
                }
            }
        }

        return count;
    }
}
