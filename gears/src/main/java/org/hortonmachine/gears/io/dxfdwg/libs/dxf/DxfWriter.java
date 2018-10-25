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
//import org.locationtech.jump.feature.*;
//import org.locationtech.jump.io.JUMPWriter;
//import org.locationtech.jump.io.DriverProperties;
//import org.locationtech.jump.io.IllegalParametersException;
//import org.locationtech.jump.io.CompressedFile;
//import org.locationtech.jts.algorithm.CGAlgorithms;
//import org.locationtech.jts.algorithm.RobustCGAlgorithms;
//
//import java.io.*;
//import java.net.URL;
//import java.util.Iterator;
//
//
///**
// * DXF writer
// * @author Micha�l Michaud
// * @version 0.5.0
// */
//// History
//// 2006-11-12 : Much clean-up made on 2006-11-12 for version 0.5
//public class DxfWriter implements JUMPWriter {
//    protected static CGAlgorithms cga = new RobustCGAlgorithms();
//    DxfFile dxfFile = null;
//
//    /** Creates new DxfWriter */
//    public DxfWriter() {}
//
//    /**
//     * Main method - write the featurecollection to a DXF file.
//     *
//     * @param featureCollection collection to write
//     * @param dp 'OutputFile' or 'DefaultValue' to specify where to write.
//     */
//    public void write(FeatureCollection featureCollection, DriverProperties dp)
//        throws IllegalParametersException, Exception {
//        String dxfFileName;
//        String fname;
//        int loc;
//        dxfFileName = dp.getProperty("File");
//        
//        if (dxfFileName == null) {
//            dxfFileName = dp.getProperty("DefaultValue");
//        }
//        if (dxfFileName == null) {
//            throw new IllegalParametersException("no File property specified");
//        }
//        
//        String[] layerNameProp = (String[])dp.get("LAYER_NAME");
//        String[] layerNames = layerNameProp==null?new String[]{}:layerNameProp;
//
//        // Check if header has to be written
//        // Warning : using getProperty instead of get return null
//        // because HEADER is not a String
//        // This option has been moved on 2006-11-12 after the header-bug has
//        // fixed (comments will be removed in version 0.6)
//        /*boolean header = true;
//        if (dp.get("HEADER") != null) {
//            header = ((Boolean)dp.get("HEADER")).booleanValue();
//        }*/
//        
//        // Check if the writer has to create layers with "_" suffix for layers with holes
//        // Warning : using getProperty instead of get return null
//        // because SUFFIX is not a String
//        boolean suffix = true;
//        if (dp.get("SUFFIX") != null) {
//            suffix = ((Boolean)dp.get("SUFFIX")).booleanValue();
//        }
//        
//        loc = dxfFileName.lastIndexOf(File.separatorChar);
//        fname = dxfFileName.substring(loc + 1); // ie. "/data1/hills.dxf" -> "hills.dxf"
//        loc = fname.lastIndexOf(".");
//        if (loc == -1) {
//            throw new IllegalParametersException("Filename must end in '.dxf'");
//        }
//
//        URL url = new URL("file", "localhost", dxfFileName);
//        DxfFile dxfFile = new DxfFile();
//        FileWriter fw = new FileWriter(dxfFileName);
//        DxfFile.write(featureCollection, layerNames, fw, 2, suffix);
//        fw.close();
//    }
//    
//    /*
//    protected DxfFile getDxfFile(String dxfFileName, String compressedFname)
//        throws Exception {
//        java.io.InputStream in = CompressedFile.openFile(dxfFileName,
//                compressedFname);
//        DxfFile dxfFile = new DxfFile(in);
//        return dxfFile;
//    }
//    */
//    
//}
