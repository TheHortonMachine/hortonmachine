///*
// * JGrass - Free Open Source Java GIS http://www.jgrass.org 
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * This library is free software; you can redistribute it and/or modify it under
// * the terms of the GNU Library General Public License as published by the Free
// * Software Foundation; either version 2 of the License, or (at your option) any
// * later version.
// * 
// * This library is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
// * details.
// * 
// * You should have received a copy of the GNU Library General Public License
// * along with this library; if not, write to the Free Foundation, Inc., 59
// * Temple Place, Suite 330, Boston, MA 02111-1307 USA
// */
//package org.jgrasstools.hortonmachine.models.hm;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.jgrasstools.gears.io.rasterreader.RasterReader;
//import org.jgrasstools.gears.io.vectorreader.VectorReader;
//import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
//import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras.HecrasInputBuilder;
//import org.jgrasstools.hortonmachine.utils.HMTestCase;
///**
// * Test for the {@link HecrasInputBuilder} module.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestHecrasInputMaker extends HMTestCase {
//
//    public void testHecrasInputMaker() throws Exception {
//
//        String baseFolder = "D:/Dropbox/hydrologis/lavori/2012_11_ponte_arche/shape/";
//        String inElevationPath = baseFolder + "dtm_tratto_finale.asc";
//        String inRiverPath = baseFolder + "tratto_rete_finale.shp";
//        // String inSectionsPath = baseFolder + "valfazzon_sections_30_20.shp";
//        String outHecrasPath = baseFolder + "ciresa2.geo";
//        String outSectionsPath = baseFolder + "ciresa_sections.shp";
//        String outSectionsPointsPath = baseFolder + "ciresa_sectionspath.shp";
//
//        GridCoverage2D elevMap = RasterReader.readRaster(inElevationPath);
//        SimpleFeatureCollection riverMap = VectorReader.readVector(inRiverPath);
//        // SimpleFeatureCollection sectionsMap = VectorReader.readVector(inSectionsPath);
//
//        HecrasInputBuilder hecras = new HecrasInputBuilder();
//        hecras.inElev = elevMap;
//        hecras.inRiver = riverMap;
//        // hecras.inSections = sectionsMap;
//        hecras.inHecras = outHecrasPath;
//        hecras.pTitle = "ciresa";
//        hecras.pSectionsWidth = 5.0;
//        hecras.pSectionsIntervalDistance = 10.0;
//        hecras.inHecras = outHecrasPath;
//        hecras.pm = pm;
//        hecras.process();
//        SimpleFeatureCollection outSections = hecras.outSections;
//        VectorWriter.writeVector(outSectionsPath, outSections);
//        SimpleFeatureCollection outSectionsPoints = hecras.outSectionPoints;
//        VectorWriter.writeVector(outSectionsPointsPath, outSectionsPoints);
//    }
//}