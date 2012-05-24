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
package org.jgrasstools.gears.modules.r.transformer;

import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.RotateDescriptor;
import javax.media.jai.operator.TranslateDescriptor;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operation2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.*;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

@Description("Module for raster tranforms.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Transform, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.DRAFT)
@Name("rtrans")
@License("General Public License Version 3 (GPLv3)")
public class RasterTransformer extends JGTModel {

    @Description("The raster that has to be transformed.")
    @In
    public GridCoverage2D inRaster;

    @Description("The interpolation type to use: nearest neightbour (0), bilinear (1), bicubic (2)")
    @In
    public int pInterpolation = 0;

    @Description("The translation along the X axis.")
    @In
    public Double pTransX;

    @Description("The translation along the Y axis.")
    @In
    public Double pTransY;

    @Description("The northern coordinate of the rotation point.")
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public Double pNorth;

    @Description("The eastern coordinate of the rotation point.")
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public Double pEast;

    @Description("The rotation angle in degree (rotation is performed before translation).")
    @In
    public Double pAngle;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The transformed raster.")
    @Out
    public GridCoverage2D outRaster = null;

    @Description("The new raster geometry.")
    @Out
    public SimpleFeatureCollection outBounds = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        switch( pInterpolation ) {
        case Interpolation.INTERP_BILINEAR:
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            break;
        case Interpolation.INTERP_BICUBIC:
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            break;
        default:
            break;
        }

        pm.beginTask("Transforming raster...", IJGTProgressMonitor.UNKNOWN);

        RenderedImage inRasterRI = inRaster.getRenderedImage();
        RegionMap sourceRegion = CoverageUtilities.gridGeometry2RegionParamsMap(inRaster.getGridGeometry());
        Envelope2D envelope2d = inRaster.getEnvelope2D();
        Envelope targetEnvelope = null;
        Geometry targetGeometry = null;
        GeometryFactory gf = GeometryUtilities.gf();

        RenderedOp finalImg = null;
        if (pAngle != null) {
            float centerX = 0f;
            float centerY = 0f;
            if (pEast == null) {
                centerX = (float) envelope2d.getCenterX();
            } else {
                centerX = pEast.floatValue();
            }
            if (pNorth == null) {
                centerY = (float) envelope2d.getCenterY();
            } else {
                centerY = pNorth.floatValue();
            }
            finalImg = RotateDescriptor.create(inRasterRI, centerX, centerY, (float) Math.toRadians(pAngle), interpolation, null,
                    null);

            // also keep track of the transforming envelope
            AffineTransform rotationAT = new AffineTransform();
            rotationAT.translate(centerX, centerY);
            rotationAT.rotate(Math.toRadians(-pAngle));
            rotationAT.translate(-centerX, -centerY);
            MathTransform rotationTransform = new AffineTransform2D(rotationAT);

            Envelope jtsEnv = new Envelope(envelope2d.getMinX(), envelope2d.getMaxX(), envelope2d.getMinY(), envelope2d.getMaxY());
            targetEnvelope = JTS.transform(jtsEnv, rotationTransform);

            Geometry rotGeometry = gf.toGeometry(jtsEnv);
            targetGeometry = JTS.transform(rotGeometry, rotationTransform);
        }

        if (pTransX != null && pTransY != null) {
            if (finalImg != null) {
                finalImg = TranslateDescriptor.create(finalImg, pTransX.floatValue(), pTransY.floatValue(), interpolation, null);
            } else {
                finalImg = TranslateDescriptor
                        .create(inRasterRI, pTransX.floatValue(), pTransY.floatValue(), interpolation, null);
            }

            // also keep track of the transforming envelope
            AffineTransform translationAT = new AffineTransform();
            translationAT.translate(pTransX.floatValue(), pTransY.floatValue());
            MathTransform translateTransform = new AffineTransform2D(translationAT);

            if (targetEnvelope == null) {
                targetEnvelope = new Envelope(envelope2d.getMinX(), envelope2d.getMaxX(), envelope2d.getMinY(),
                        envelope2d.getMaxY());
            }
            if (targetGeometry == null) {
                targetGeometry = gf.toGeometry(targetEnvelope);
            }

            targetEnvelope = JTS.transform(targetEnvelope, translateTransform);
            targetGeometry = JTS.transform(targetGeometry, translateTransform);
        }

        if (finalImg != null) {
            RegionMap targetRegion = new RegionMap();
            targetRegion.put(NORTH, targetEnvelope.getMaxY());
            targetRegion.put(SOUTH, targetEnvelope.getMinY());
            targetRegion.put(WEST, targetEnvelope.getMinX());
            targetRegion.put(EAST, targetEnvelope.getMaxX());
            targetRegion.put(XRES, sourceRegion.getXres());
            targetRegion.put(YRES, sourceRegion.getYres());
            // targetRegion.put(ROWS, (double) height);
            // targetRegion.put(COLS, (double) width);

            CoordinateReferenceSystem crs = inRaster.getCoordinateReferenceSystem();
            outRaster = CoverageUtilities.buildCoverage("out", finalImg, targetRegion, crs);

            outBounds = FeatureUtilities.featureCollectionFromGeometry(crs, targetGeometry);
        }

        pm.done();
    }
}
