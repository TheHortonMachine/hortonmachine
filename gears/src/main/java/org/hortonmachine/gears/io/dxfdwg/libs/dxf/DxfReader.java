package org.hortonmachine.gears.io.dxfdwg.libs.dxf;
///*
// * Library name : dxf
// * (C) 2006 Micha�l Michaud
// * 
// * This program is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// * 
// * For more information, contact:
// *
// * michael.michaud@free.fr
// *
// */
//
//package fr.michaelm.jump.drivers.dxf;
//
//import org.locationtech.jts.geom.*;
//
//import java.io.File;
//import java.io.FileOutputStream;
//
//import org.geotools.feature.FeatureCollection;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.feature.simple.SimpleFeatureType;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
///**
// *DataProperties for the JCSReader load(DataProperties) interface:<br><br>
// *<table border=1><tr><th>Parameter</th><th>Meaning</th></tr>
// *<tr><td>InputFile or DefaultValue</td><td>File name for the input .shp file</td></tr>
// *</table><br>
// *NOTE: The input .dbf is assumed to be 'beside' (in the same directory) as the .shp file.
// *<tr><td>CompressedFileTemplate</td><td>File name (.zip NOT a .gz) with a .shp and .dbf file inside</td></tr>
// *
// * Uses a modified version of geotools to do the .dbf and .shp file reading.
// * If you are reading from a .zip file, the dbf file will be copied to your temp directory and deleted after being read.
// */
///**
// * DXF reader.
// * Use the file name to read in the DriverProperties parameter, read the file
// * and return a FeatureCollection.
// * @author Micha�l Michaud
// * @version 0.5.0
// */
//// History
//public class DxfReader {
//    DxfFile dxfFile = null;
//
//    /** Creates new DxfReader */
//    public DxfReader() {
//    }
//
//    /**
//     * Main method to read a DXF file. 
//     * @param dp 'InputFile' or 'DefaultValue' to specify input .dxf file.
//     *
//     */
//    public SimpleFeatureCollection read( File file,
//            CoordinateReferenceSystem crs ) throws Exception {
//        SimpleFeatureCollection result;
//        GeometryFactory factory = new GeometryFactory();
//        dxfFile = DxfFile.createFromFile(file, crs);
//        result = dxfFile.read(factory);
//        System.gc();
//        return result;
//    }
//}
