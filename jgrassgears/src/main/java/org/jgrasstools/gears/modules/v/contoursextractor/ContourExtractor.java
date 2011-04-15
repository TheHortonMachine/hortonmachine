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
package org.jgrasstools.gears.modules.v.contoursextractor;

import jaitools.media.jai.contour.ContourDescriptor;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.util.AffineTransformation;

@Description("Module that extracts contour lines from a raster.")
@Documentation("ContourExtractor.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector")
@Status(Status.EXPERIMENTAL)
@Label(JGTConstants.VECTORPROCESSING)
@Name("contourextract")
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public class ContourExtractor extends JGTModel {

    @Description("The raster on which to calculate the contours.")
    @In
    public GridCoverage2D inCoverage;

    @Description("The minimum value for the contours.")
    @In
    public Double pMin;

    @Description("The maximum value for the contours.")
    @In
    public Double pMax;

    @Description("The contours interval.")
    @In
    public Double pInterval;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The generated contour lines vector.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    @SuppressWarnings("unchecked")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }
        checkNull(inCoverage, pMin, pMax, pInterval);

        if (pMin > pMax) {
            throw new ModelsIllegalargumentException("Min has to be bigger than Max.", this);
        }

        final AffineTransform mt2D = (AffineTransform) inCoverage.getGridGeometry().getGridToCRS2D(PixelOrientation.CENTER);

        List<Double> contourIntervals = new ArrayList<Double>();
        pm.message("Adding levels:");
        for( double level = pMin; level <= pMax; level += pInterval ) {
            contourIntervals.add(level);
            pm.message("-> " + level);
        }

        pm.beginTask("Extracting contours...", IJGTProgressMonitor.UNKNOWN);
        ParameterBlockJAI pb = new ParameterBlockJAI("Contour");
        pb.setSource("source0", inCoverage.getRenderedImage());
        pb.setParameter("levels", contourIntervals);
        RenderedOp dest = JAI.create("Contour", pb);
        Collection<LineString> contours = (Collection<LineString>) dest.getProperty(ContourDescriptor.CONTOUR_PROPERTY_NAME);
        pm.done();

        outGeodata = FeatureCollections.newCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("contours");
        b.setCRS(inCoverage.getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class);
        b.add("elevation", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        int id = 0;

        final AffineTransformation jtsTransformation = new AffineTransformation(mt2D.getScaleX(), mt2D.getShearX(),
                mt2D.getTranslateX(), mt2D.getShearY(), mt2D.getScaleY(), mt2D.getTranslateY());
        for( LineString lineString : contours ) {
            Object userData = lineString.getUserData();
            double elev = -1.0;
            if (userData instanceof Double) {
                elev = (Double) userData;
                lineString.setUserData(null);
            }
            lineString.apply(jtsTransformation);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{lineString, elev};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + id);
            id++;
            outGeodata.add(feature);
        }

    }
}
