/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.lesto.modules.filter;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("A module that splits a las file into smaller pieces.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, split")
@Label(JGTConstants.LESTO + "/filter")
@Name("lassplitter")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasSplitter extends JGTModel {
    @Description("A las file to split.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description("The rows to split into.")
    @In
    public int pRows;

    @Description("The cols to split into.")
    @In
    public int pCols;

    @SuppressWarnings("unchecked")
    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        File inLas = new File(inFile);
        String lasName = FileUtilities.getNameWithoutExtention(inLas);
        try (ALasReader reader = ALasReader.getReader(inLas, null)) {
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsNum = header.getRecordsCount();
            double[] xyzScale = header.getXYZScale();
            double[] xyzOffset = header.getXYZOffset();
            ReferencedEnvelope3D env = header.getDataEnvelope();

            double[] xRange = NumericsUtilities.range2Bins(env.getMinX(), env.getMaxX(), pCols);
            double[] yRange = NumericsUtilities.range2Bins(env.getMinY(), env.getMaxY(), pRows);

            STRtree envelopeTree = new STRtree();
            HashMap<Envelope, ALasWriter> env2LaswriterMap = new HashMap<>();
            int fileCount = 1;
            for( int x = 0; x < xRange.length - 1; x++ ) {
                double minX = xRange[x];
                double maxX = xRange[x + 1];
                for( int y = 0; y < yRange.length - 1; y++ ) {
                    double minY = yRange[y];
                    double maxY = yRange[y + 1];

                    Envelope envelope = new Envelope(new Coordinate(minX, minY), new Coordinate(maxX, maxY));

                    File outLasPiece = new File(inLas.getParentFile(), lasName + "_" + fileCount + ".las");
                    ALasWriter writer = ALasWriter.getWriter(outLasPiece, env.getCoordinateReferenceSystem());
                    writer.setOffset(xyzOffset[0], xyzOffset[1], xyzOffset[2]);
                    writer.setScales(xyzScale[0], xyzScale[1], xyzScale[2]);
                    writer.setBounds(minX, maxX, minY, maxY, env.getMinZ(), env.getMaxZ());
                    writer.open();

                    envelopeTree.insert(envelope, writer);
                    env2LaswriterMap.put(envelope, writer);

                    fileCount++;
                }
            }

            pm.beginTask("Split file...", (int) recordsNum);
            while( reader.hasNextPoint() ) {
                LasRecord dot = reader.getNextPoint();
                Coordinate coord = new Coordinate(dot.x, dot.y);

                List<ALasWriter> result = envelopeTree.query(new Envelope(coord));
                int size = result.size();
                if (size == 0) {
                    continue;
                }

                ALasWriter aLasWriter = result.get(0);
                aLasWriter.addPoint(dot);
                pm.worked(1);
            }
            pm.done();

            for( ALasWriter writer : env2LaswriterMap.values() ) {
                writer.close();
            }
        }
    }

}
