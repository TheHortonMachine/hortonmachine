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
package org.jgrasstools.gears.modules.utils.featureslist;

import java.util.ArrayList;
import java.util.List;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("A module that reads vectors.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Iterator, Vector")
@Label(JGTConstants.LIST_READER)
@Status(Status.CERTIFIED)
@Name("vectorlister")
@License("General Public License Version 3 (GPLv3)")
public class OmsFeaturesLister extends JGTModel {

    @Description("The list of file from which to read features.")
    @UI(JGTConstants.FILESPATHLIST_UI_HINT)
    @In
    public List<String> inFiles;

    @Description("All features read from the input files.")
    @Out
    public List<SimpleFeatureCollection> outFC = null;

    @Execute
    public void process() throws Exception {

        outFC = new ArrayList<SimpleFeatureCollection>();

        for( String file : inFiles ) {
            SimpleFeatureCollection featureCollection = OmsVectorReader.readVector(file);
            outFC.add(featureCollection);
        }

    }

}
