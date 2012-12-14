/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules.v.vectorconverter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("A simple middleman module to do feature conversion.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Feature, Vector, Convert")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.EXPERIMENTAL)
@UI(JGTConstants.HIDE_UI_HINT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VectorConverter extends JGTModel {
    @Description("The input features.")
    @In
    public SimpleFeatureCollection inGeodata;

    @Description("The output features.")
    @Out
    public SimpleFeatureCollection outGeodata;

    @Execute
    public void process() throws Exception {
        checkNull(inGeodata);
        outGeodata = inGeodata;
    }

}
