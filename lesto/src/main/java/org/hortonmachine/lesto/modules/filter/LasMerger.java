/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.lesto.modules.filter;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

@Description("A module that merges las files to a single one.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, merge")
@Label(HMConstants.LESTO + "/filter")
@Name("lasmerge")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class LasMerger extends HMModel {
    @Description("A folder of las files to merge.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description("The merged las output file.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLas;

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, outLas);
        CoordinateReferenceSystem crs = null;

        File inFolderFile = new File(inFolder);
        File[] lasList = inFolderFile.listFiles(new FilenameFilter(){
            public boolean accept( File arg0, String arg1 ) {
                return arg1.toLowerCase().endsWith(".las");
            }
        });

        StringBuilder sb = new StringBuilder("Merging files:");
        for( File file : lasList ) {
            sb.append("\n").append(file.getAbsolutePath());
        }
        pm.message(sb.toString());

        // create readers and calculate bounds
        List<ALasReader> readers = new ArrayList<ALasReader>();
        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;
        int count = 0;
        for( File lasFile : lasList ) {
            ALasReader reader = ALasReader.getReader(lasFile, crs);
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsNum = header.getRecordsCount();
            count = (int) (count + recordsNum);
            ReferencedEnvelope3D envelope = header.getDataEnvelope();
            xMin = min(xMin, envelope.getMinX());
            yMin = min(yMin, envelope.getMinY());
            zMin = min(zMin, envelope.getMinZ());
            xMax = max(xMax, envelope.getMaxX());
            yMax = max(yMax, envelope.getMaxY());
            zMax = max(zMax, envelope.getMaxZ());
            readers.add(reader);
        }

        File outFile = new File(outLas);
        ALasWriter writer = ALasWriter.getWriter(outFile, crs);
        writer.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
        writer.open();

        pm.beginTask("Merging...", count);
        for( ALasReader reader : readers ) {
            while( reader.hasNextPoint() ) {
                LasRecord readNextLasDot = reader.getNextPoint();
                writer.addPoint(readNextLasDot);
                pm.worked(1);
            }
            reader.close();
        }
        writer.close();
        pm.done();
    }

}
