
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
package org.jgrasstools.gears;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import oms3.Access;
import oms3.ComponentAccess;

import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionReader;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionWriter;
import org.jgrasstools.gears.io.adige.VegetationLibraryReader;
import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageReader;
import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageWriter;
import org.jgrasstools.gears.io.dbf.DbfTableReader;
import org.jgrasstools.gears.io.eicalculator.EIAltimetryReader;
import org.jgrasstools.gears.io.eicalculator.EIAreasReader;
import org.jgrasstools.gears.io.eicalculator.EIEnergyReader;
import org.jgrasstools.gears.io.grass.JGrassCoverageReader;
import org.jgrasstools.gears.io.grass.JGrassCoverageWriter;
import org.jgrasstools.gears.io.id2valuearray.Id2ValueArrayReader;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.io.tiff.GeoTiffCoverageReader;
import org.jgrasstools.gears.io.tiff.GeoTiffCoverageWriter;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepWriterId2Value;
import org.jgrasstools.gears.io.timeseries.TimeseriesReaderArray;
import org.jgrasstools.gears.io.timeseries.TimeseriesWriterArray;
import org.jgrasstools.gears.modules.r.coveragereconverter.CoverageConverter;
import org.jgrasstools.gears.modules.r.coveragereprojector.CoverageReprojector;
import org.jgrasstools.gears.modules.r.mapcalc.Mapcalc;
import org.jgrasstools.gears.modules.r.scanline.ScanLineRasterizer;
import org.jgrasstools.gears.modules.r.summary.CoverageSummary;
import org.jgrasstools.gears.modules.utils.fileiterator.FileIterator;
import org.jgrasstools.gears.modules.v.attributesjoiner.AttributesJoiner;
import org.jgrasstools.gears.modules.v.featurereprojector.FeatureReprojector;
import org.jgrasstools.gears.modules.v.marchingsquares.MarchingSquaresVectorializer;
import org.jgrasstools.gears.modules.v.rastercattofeatureattribute.RasterCatToFeatureAttribute;
import org.jgrasstools.gears.modules.v.simplifier.GeometrySimplifier;
import org.jgrasstools.gears.modules.v.smoothing.LineSmoother;
import org.jgrasstools.gears.modules.v.sourcesdirection.SourcesDirectionCalculator;
import org.jgrasstools.gears.ui.CoverageViewer;

/**
 * Class presenting modules names and classes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class JGrassGears {

    /**
     * A {@link LinkedHashMap map} of all the classes and their simple names.
     */
    public static final LinkedHashMap<String, Class< ? >> moduleName2Class = new LinkedHashMap<String, Class< ? >>();

    /**
     * A {@link LinkedHashMap map} of all the classes and their simple names.
     */
    public static final LinkedHashMap<String, List<String>> moduleName2FieldNames = new LinkedHashMap<String, List<String>>();

    /**
     * An array of all the fields used in the modules.
     */
    public static String[] allFields = null;

    /**
     * An array of all the classes of the modules.
     */
    public static String[] allClasses = null;

    static {
        /*
         * define all modules available
         */
        // r
        moduleName2Class.put("CoverageConverter", CoverageConverter.class);
        moduleName2Class.put("CoverageReprojector", CoverageReprojector.class);
        moduleName2Class.put("Mapcalc", Mapcalc.class);
        moduleName2Class.put("ScanLineRasterizer", ScanLineRasterizer.class);
        moduleName2Class.put("CoverageSummary", CoverageSummary.class);

        // utils
        moduleName2Class.put("FileIterator", FileIterator.class);
        
        // v
        moduleName2Class.put("AttributesJoiner", AttributesJoiner.class);
        moduleName2Class.put("FeatureReprojector", FeatureReprojector.class);
        moduleName2Class.put("MarchingSquaresVectorializer", MarchingSquaresVectorializer.class);
        moduleName2Class.put("RasterCatToFeatureAttribute", RasterCatToFeatureAttribute.class);
        moduleName2Class.put("GeometrySimplifier", GeometrySimplifier.class);
        moduleName2Class.put("LineSmoother", LineSmoother.class);
        moduleName2Class.put("SourcesDirectionCalculator", SourcesDirectionCalculator.class);

        /*
         * I/O
         */
        moduleName2Class.put("AdigeBoundaryConditionReader", AdigeBoundaryConditionReader.class);
        moduleName2Class.put("AdigeBoundaryConditionWriter", AdigeBoundaryConditionWriter.class);
        moduleName2Class.put("VegetationLibraryReader", VegetationLibraryReader.class);
        moduleName2Class.put("ArcgridCoverageReader", ArcgridCoverageReader.class);
        moduleName2Class.put("ArcgridCoverageWriter", ArcgridCoverageWriter.class);
        moduleName2Class.put("RasterReader", RasterReader.class);
        moduleName2Class.put("DbfTableReader", DbfTableReader.class);
        moduleName2Class.put("EIAltimetryReader", EIAltimetryReader.class);
        moduleName2Class.put("EIAreasReader", EIAreasReader.class);
        moduleName2Class.put("EIEnergyReader", EIEnergyReader.class);
        moduleName2Class.put("JGrassCoverageReader", JGrassCoverageReader.class);
        moduleName2Class.put("JGrassCoverageWriter", JGrassCoverageWriter.class);
        moduleName2Class.put("ShapefileFeatureReader", ShapefileFeatureReader.class);
        moduleName2Class.put("ShapefileFeatureWriter", ShapefileFeatureWriter.class);
        moduleName2Class.put("GeoTiffCoverageReader", GeoTiffCoverageReader.class);
        moduleName2Class.put("GeoTiffCoverageWriter", GeoTiffCoverageWriter.class);
        moduleName2Class
                .put("TimeseriesByStepReaderId2Value", TimeseriesByStepReaderId2Value.class);
        moduleName2Class
                .put("TimeseriesByStepWriterId2Value", TimeseriesByStepWriterId2Value.class);
        moduleName2Class.put("TimeseriesReaderArray", TimeseriesReaderArray.class);
        moduleName2Class.put("TimeseriesWriterArray", TimeseriesWriterArray.class);
        moduleName2Class.put("Id2ValueArrayReader", Id2ValueArrayReader.class);
        
        /*
         * GUI
         */
        moduleName2Class.put("CoverageViewer", CoverageViewer.class);

        Set<String> moduleNames = moduleName2Class.keySet();

        /*
         * extract all fields
         */
        if (allFields == null) {

            List<String> completions = new ArrayList<String>();
            for( String moduleName : moduleNames ) {
                try {
                    List<String> tmpfields = new ArrayList<String>();
                    Class< ? > moduleClass = moduleName2Class.get(moduleName);
                    Object annotatedObject = moduleClass.newInstance();
                    ComponentAccess cA = new ComponentAccess(annotatedObject);
                    Collection<Access> inputs = cA.inputs();
                    for( Access access : inputs ) {
                        String name = access.getField().getName();
                        if (!completions.contains(name)) {
                            completions.add(name);
                        }
                        tmpfields.add(name);
                    }
                    Collection<Access> outputs = cA.outputs();
                    for( Access access : outputs ) {
                        String name = access.getField().getName();
                        if (!completions.contains(name)) {
                            completions.add(name);
                        }
                        tmpfields.add(name);
                    }
                    moduleName2FieldNames.put(moduleName, tmpfields);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Collections.sort(completions);
            allFields = (String[]) completions.toArray(new String[completions.size()]);
        }
        /*
         * gather all classes
         */
        if (allClasses == null) {
            List<String> classNames = new ArrayList<String>();
            for( String moduleName : moduleNames ) {
                Class< ? > moduleClass = moduleName2Class.get(moduleName);
                classNames.add(moduleClass.getSimpleName());
            }
            Collections.sort(classNames);
            allClasses = (String[]) classNames.toArray(new String[classNames.size()]);
        }
    }

}
