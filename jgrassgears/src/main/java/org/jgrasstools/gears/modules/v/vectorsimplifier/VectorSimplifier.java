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
package org.jgrasstools.gears.modules.v.vectorsimplifier;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Label;
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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

@Description("Collection of Simplification Algorithms.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Simplify, Vector")
@Status(Status.DRAFT)
@Label(JGTConstants.VECTORPROCESSING)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VectorSimplifier extends JGTModel {

    @Description("The features to be simplified.")
    @In
    public SimpleFeatureCollection inFeatures;

    @Description("The simplification type: TopologyPreservingSimplifier = 0, Douglas Peucker = 1 (default = 0).")
    @In
    public int pType = 0;

    @Description("Distance tolerance for the simplification")
    @In
    public double pTolerance = 0.2;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The simplified features.")
    @Out
    public SimpleFeatureCollection outFeatures;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }
        FeatureIterator<SimpleFeature> inFeatureIterator = inFeatures.features();

        outFeatures = FeatureCollections.newCollection();

        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inFeatures.getSchema());

        int id = 0;
        int size = inFeatures.size();
        pm.beginTask("Simplifing features...", size);
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();

            if (fGS == null) {
            }

            Geometry geometry = (Geometry) feature.getDefaultGeometry();

            List<Geometry> geomList = new ArrayList<Geometry>();

            int numGeometries = geometry.getNumGeometries();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                switch( pType ) {
                case 0:
                    TopologyPreservingSimplifier tpSimplifier = new TopologyPreservingSimplifier(
                            geometryN);
                    tpSimplifier.setDistanceTolerance(pTolerance);
                    Geometry tpsGeometry = tpSimplifier.getResultGeometry();
                    geomList.add(tpsGeometry);
                    break;
                case 1:
                    DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(geometryN);
                    dpSimplifier.setDistanceTolerance(pTolerance);
                    Geometry dpsGeometry = dpSimplifier.getResultGeometry();
                    geomList.add(dpsGeometry);
                    break;
                default:
                }

            }

            Geometry newGeometry = null;
            if (geomList.size() == 1) {
                newGeometry = geomList.get(0);
            } else {
                Geometry[] geomArray = (Geometry[]) geomList.toArray(new Geometry[geomList.size()]);
                newGeometry = new GeometryCollection(geomArray, gF);
            }

            SimpleFeature newFeature = fGS.substituteGeometry(feature, newGeometry, id);
            id++;

            outFeatures.add(newFeature);
            pm.worked(1);
        }
        pm.done();

        inFeatureIterator.close();

    }

}
