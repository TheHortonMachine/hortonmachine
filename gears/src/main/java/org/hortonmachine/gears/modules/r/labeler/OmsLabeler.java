/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.modules.r.labeler;

import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_LABEL;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_LICENSE;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_NAME;
import static org.hortonmachine.gears.modules.r.labeler.OmsLabeler.OMSLABELER_STATUS;

import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;

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

@Description(OMSLABELER_DESCRIPTION)
@Documentation(OMSLABELER_DOCUMENTATION)
@Author(name = OMSLABELER_AUTHORNAMES, contact = OMSLABELER_AUTHORCONTACTS)
@Keywords(OMSLABELER_KEYWORDS)
@Label(OMSLABELER_LABEL)
@Name(OMSLABELER_NAME)
@Status(OMSLABELER_STATUS)
@License(OMSLABELER_LICENSE)
public class OmsLabeler extends HMModel {

    @Description(OMSLABELER_IN_MAP_DESCRIPTION)
    @In
    public GridCoverage2D inMap = null;

    @Description(OMSLABELER_OUT_MAP_DESCRIPTION)
    @Out
    public GridCoverage2D outMap = null;

    public static final String OMSLABELER_DESCRIPTION = "Labels the connected components of a raster: every group of valid cells that touch each other gets its own number.";
    public static final String OMSLABELER_DOCUMENTATION = """
            The cells are connected also by the corners, as in the 8 directions of the flow: a \
            diagonal line of cells is a single component. The value of the cells doesn't matter, \
            only if they are valid or novalue, so a raster of classes has to be cut to a class \
            first, for example with RasterNull and its inverse switch. The components are \
            numbered from 1, in the order in which their first cell is met reading the raster \
            from the north-west corner by rows; the novalue cells stay novalue.

            The labels give an identifier to every patch of a mask, as the single tree crowns, \
            the landslides or the lakes of a map, to count them, to measure their area with \
            RasterSummary or to vectorize them.
            """;
    public static final String OMSLABELER_KEYWORDS = "Labeling, Connected components, Patches, Raster";
    public static final String OMSLABELER_LABEL = HMConstants.RASTERPROCESSING;
    public static final String OMSLABELER_NAME = "omslabeler";
    public static final int OMSLABELER_STATUS = 40;
    public static final String OMSLABELER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSLABELER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSLABELER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSLABELER_IN_MAP_DESCRIPTION = "The raster to label: its valid cells are the ones to group.";
    public static final String OMSLABELER_OUT_MAP_DESCRIPTION = "The raster of the labels, the number of the component of each valid cell.";

    // the neighbours already visited reading by rows: west, north-west, north, north-east
    private static final int[] VISITED_COLS = {-1, -1, 0, 1};
    private static final int[] VISITED_ROWS = {0, -1, -1, -1};

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }
        checkNull(inMap);

        try (HMRaster inRaster = HMRaster.fromGridCoverage(inMap);
                HMRaster outRaster = new HMRasterWritableBuilder().setName("labeled").setTemplate(inMap).setDoInteger(true)
                        .setNoValue(HMConstants.intNovalue).setInitialValue(HMConstants.intNovalue).build()) {
            int cols = inRaster.getCols();
            int rows = inRaster.getRows();

            // first pass: provisional labels, with the equivalences of the labels that meet
            int[] provisional = new int[cols * rows];
            // the parent of each label in the union-find, label 0 is the background
            int[] parents = new int[16];
            int nextLabel = 1;
            pm.beginTask("Labeling...", rows);
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    if (inRaster.isNovalue(inRaster.getValue(c, r))) {
                        continue;
                    }
                    int label = 0;
                    for( int i = 0; i < VISITED_COLS.length; i++ ) {
                        int col = c + VISITED_COLS[i];
                        int row = r + VISITED_ROWS[i];
                        if (col < 0 || col >= cols || row < 0) {
                            continue;
                        }
                        int neighbourLabel = provisional[row * cols + col];
                        if (neighbourLabel == 0) {
                            continue;
                        }
                        if (label == 0) {
                            label = neighbourLabel;
                        } else {
                            union(parents, label, neighbourLabel);
                        }
                    }
                    if (label == 0) {
                        if (nextLabel == parents.length) {
                            parents = Arrays.copyOf(parents, parents.length * 2);
                        }
                        label = nextLabel++;
                        parents[label] = label;
                    }
                    provisional[r * cols + c] = label;
                }
                pm.worked(1);
            }
            pm.done();

            // the final labels are consecutive, in the order of the first cell of each component
            int[] finalLabels = new int[nextLabel];
            int componentsCount = 0;
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    int label = provisional[r * cols + c];
                    if (label == 0) {
                        continue;
                    }
                    int root = find(parents, label);
                    if (finalLabels[root] == 0) {
                        finalLabels[root] = ++componentsCount;
                    }
                    outRaster.setValue(c, r, finalLabels[root]);
                }
            }
            pm.message("Components found: " + componentsCount);

            outMap = outRaster.buildCoverage();
        }
    }

    private static int find( int[] parents, int label ) {
        int root = label;
        while( parents[root] != root ) {
            root = parents[root];
        }
        // compress the path, so that the next searches are short
        while( parents[label] != root ) {
            int next = parents[label];
            parents[label] = root;
            label = next;
        }
        return root;
    }

    private static void union( int[] parents, int label1, int label2 ) {
        int root1 = find(parents, label1);
        int root2 = find(parents, label2);
        if (root1 != root2) {
            // the smaller label is the root
            parents[Math.max(root1, root2)] = Math.min(root1, root2);
        }
    }

}
