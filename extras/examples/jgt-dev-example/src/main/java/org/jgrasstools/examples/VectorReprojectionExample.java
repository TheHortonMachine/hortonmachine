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
package org.jgrasstools.examples;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorreprojector.VectorReprojector;

public class VectorReprojectionExample extends JGTModel {
    public static void main( String[] args ) throws Exception {

        String inputVector = "your input path here";
        String outputVector = "your output path here";

        // read the vector
        SimpleFeatureCollection readFeatureCollection = VectorReader.readVector(inputVector);

        // reproject
        VectorReprojector vectorReprojector = new VectorReprojector();
        vectorReprojector.inVector = readFeatureCollection;
        vectorReprojector.pCode = "EPSG:32632";
        vectorReprojector.process();
        SimpleFeatureCollection outVector = vectorReprojector.outVector;

        // write the vetor
        VectorWriter.writeVector(outputVector, outVector);

    }
}
