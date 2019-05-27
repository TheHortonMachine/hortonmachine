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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter.OMSLASCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter.OMSLASCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter.OMSLASCONVERTER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter.OMSLASCONVERTER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter.OMSLASCONVERTER_STATUS;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter.OMSLASCONVERTER_inFile_DESCRIPTION;

import java.io.File;
import java.util.function.Predicate;

import org.geotools.referencing.crs.DefaultEngineeringCRS;
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

@Description("Filter and/or modify a las file.")
@Author(name = OMSLASCONVERTER_AUTHORNAMES, contact = OMSLASCONVERTER_AUTHORCONTACTS)
@Keywords("las, filter")
@Label(OMSLASCONVERTER_LABEL)
@Name("lasfilter")
@Status(OMSLASCONVERTER_STATUS)
@License(OMSLASCONVERTER_LICENSE)
public class LasFilter extends HMModel {

    @Description(OMSLASCONVERTER_inFile_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inFile;

    @Description("The filter predicate to apply.")
    @In
    public Predicate<LasRecord> filter;

    @Description("The output file.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    @Execute
    public void process() throws Exception {
        checkNull(inFile, filter, outFile);

        final File lasFile = new File(inFile);
        try (ALasReader lasReader = ALasReader.getReader(lasFile)) {
            lasReader.open();
            ILasHeader header = lasReader.getHeader();
            CoordinateReferenceSystem crs = header.getCrs();
            if (crs == null) {
                pm.errorMessage("No CRS supplies, folding back on Generic 2D");
                crs = DefaultEngineeringCRS.GENERIC_2D;
            }

            int recordsCount = (int) header.getRecordsCount();
            pm.beginTask("Filtering data...", recordsCount);
            try (ALasWriter lasWriter = ALasWriter.getWriter(new File(outFile), crs)) {
                lasWriter.setBounds(header);
                lasWriter.open();
                while( lasReader.hasNextPoint() ) {
                    pm.worked(1);
                    LasRecord lr = lasReader.getNextPoint();
                    if (filter.test(lr)) {
                        lasWriter.addPoint(lr);
                    }
                }
            } finally {
                pm.done();
            }
        }
    }

}
