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
package org.jgrasstools.gears.modules.v.featurereprojector;

import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

@Description("Module for vector reprojection")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Crs, Reprojection, Vector")
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class FeatureReprojector extends JGTModel {

    @Description("The feature collection that has to be reprojected.")
    @In
    public SimpleFeatureCollection inGeodata;

    @Description("The code defining the target coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @In
    public String pCode;

    @Description("Switch that set to true allows for some error due to different datums. If set to false, it won't reproject without Bursa Wolf parameters.")
    @In
    public boolean doLenient = true;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The reprojected feature collection.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }

        outGeodata = FeatureCollections.newCollection();
        SimpleFeatureType featureType = inGeodata.getSchema();

        CoordinateReferenceSystem dataCRS = featureType.getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCrs = CRS.decode(pCode);

        MathTransform transform = CRS.findMathTransform(dataCRS, targetCrs, doLenient);

        FeatureIterator<SimpleFeature> inFeatureIterator = inGeodata.features();
        int id = 0;
        while( inFeatureIterator.hasNext() ) {
            // copy the contents of each feature and transform the geometry
            SimpleFeature feature = inFeatureIterator.next();
            List<Object> attributesList = feature.getAttributes();

            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
            builder.addAll(attributesList);
            SimpleFeature newFeature = builder.buildFeature(featureType.getTypeName() + "." + id++);

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Geometry reprojectedGeometry = JTS.transform(geometry, transform);

            newFeature.setDefaultGeometry(reprojectedGeometry);
            outGeodata.add(newFeature);
        }
        inGeodata.close(inFeatureIterator);
    }

}
