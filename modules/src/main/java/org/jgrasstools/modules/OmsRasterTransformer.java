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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_doFlipHorizontal_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_doFlipVertical_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_outBounds_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pAngle_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pInterpolation_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pScaleX_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pScaleY_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pTransX_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERTRANSFORMER_pTransY_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.BICUBIC;
import static org.jgrasstools.gears.libs.modules.Variables.BILINEAR;
import static org.jgrasstools.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.EAST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.WEST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.XRES;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.YRES;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.RotateDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import javax.media.jai.operator.TransposeDescriptor;

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
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

@Description(OMSRASTERTRANSFORMER_DESCRIPTION)
@Documentation(OMSRASTERTRANSFORMER_DOCUMENTATION)
@Author(name = OMSRASTERTRANSFORMER_AUTHORNAMES, contact = OMSRASTERTRANSFORMER_AUTHORCONTACTS)
@Keywords(OMSRASTERTRANSFORMER_KEYWORDS)
@Label(OMSRASTERTRANSFORMER_LABEL)
@Name(OMSRASTERTRANSFORMER_NAME)
@Status(OMSRASTERTRANSFORMER_STATUS)
@License(OMSRASTERTRANSFORMER_LICENSE)
public class OmsRasterTransformer extends JGTModel {
    @Description(OMSRASTERTRANSFORMER_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERTRANSFORMER_pInterpolation_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERTRANSFORMER_pTransX_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransX;

    @Description(OMSRASTERTRANSFORMER_pTransY_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransY;

    @Description(OMSRASTERTRANSFORMER_pScaleX_DESCRIPTION)
    @In
    public Double pScaleX;

    @Description(OMSRASTERTRANSFORMER_pScaleY_DESCRIPTION)
    @In
    public Double pScaleY;

    @Description(OMSRASTERTRANSFORMER_doFlipHorizontal_DESCRIPTION)
    @In
    public boolean doFlipHorizontal;

    @Description(OMSRASTERTRANSFORMER_doFlipVertical_DESCRIPTION)
    @In
    public boolean doFlipVertical;

    @Description(OMSRASTERTRANSFORMER_pNorth_DESCRIPTION)
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public Double pNorth;

    @Description(OMSRASTERTRANSFORMER_pEast_DESCRIPTION)
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public Double pEast;

    @Description(OMSRASTERTRANSFORMER_pAngle_DESCRIPTION)
    @Unit("degrees")
    @In
    public Double pAngle;

    @Description(OMSRASTERTRANSFORMER_outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    @Description(OMSRASTERTRANSFORMER_outBounds_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outBounds = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (pInterpolation.equals(BILINEAR)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation.equals(BICUBIC)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        }

        RenderedImage inRasterRI = inRaster.getRenderedImage();
        RegionMap sourceRegion = CoverageUtilities.gridGeometry2RegionParamsMap(inRaster.getGridGeometry());
        Envelope2D envelope2d = inRaster.getEnvelope2D();
        Envelope targetEnvelope = new Envelope(envelope2d.getMinX(), envelope2d.getMaxX(), envelope2d.getMinY(),
                envelope2d.getMaxY());
        Geometry targetGeometry = null;
        GeometryFactory gf = GeometryUtilities.gf();

        RenderedOp finalImg = null;
        if (pAngle != null) {
            pm.beginTask("Rotate raster by angle: " + pAngle, IJGTProgressMonitor.UNKNOWN);

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

            Envelope jtsEnv = new Envelope(targetEnvelope.getMinX(), targetEnvelope.getMaxX(), targetEnvelope.getMinY(),
                    targetEnvelope.getMaxY());
            targetEnvelope = JTS.transform(jtsEnv, rotationTransform);

            Geometry rotGeometry = gf.toGeometry(jtsEnv);
            targetGeometry = JTS.transform(rotGeometry, rotationTransform);

            pm.done();
        }

        if (doFlipHorizontal) {
            pm.beginTask("Flip horizontally...", IJGTProgressMonitor.UNKNOWN);

            if (finalImg != null) {
                finalImg = TransposeDescriptor.create(finalImg, TransposeDescriptor.FLIP_HORIZONTAL, null);
            } else {
                finalImg = TransposeDescriptor.create(inRasterRI, TransposeDescriptor.FLIP_HORIZONTAL, null);
            }

            Envelope jtsEnv = new Envelope(targetEnvelope.getMinX(), targetEnvelope.getMaxX(), targetEnvelope.getMinY(),
                    targetEnvelope.getMaxY());
            targetGeometry = gf.toGeometry(jtsEnv);
            pm.done();
        }
        if (doFlipVertical) {
            pm.beginTask("Flip vertically...", IJGTProgressMonitor.UNKNOWN);

            if (finalImg != null) {
                finalImg = TransposeDescriptor.create(finalImg, TransposeDescriptor.FLIP_VERTICAL, null);
            } else {
                finalImg = TransposeDescriptor.create(inRasterRI, TransposeDescriptor.FLIP_VERTICAL, null);
            }

            Envelope jtsEnv = new Envelope(targetEnvelope.getMinX(), targetEnvelope.getMaxX(), targetEnvelope.getMinY(),
                    targetEnvelope.getMaxY());
            targetGeometry = gf.toGeometry(jtsEnv);
            pm.done();
        }

        if (pScaleX != null || pScaleY != null) {
            float scaleX = 1f;
            float scaleY = 1f;
            if (pScaleX == null) {
                scaleX = 1f;
            } else {
                scaleX = pScaleX.floatValue();
            }
            if (pScaleY == null) {
                scaleY = 1f;
            } else {
                scaleY = pScaleY.floatValue();
            }
            pm.beginTask("Scale raster by: " + scaleX + " and " + scaleY, IJGTProgressMonitor.UNKNOWN);

            // float centerX = (float) envelope2d.getCenterX();
            // float centerY = (float) envelope2d.getCenterY();
            if (finalImg != null) {
                finalImg = ScaleDescriptor.create(finalImg, new Float(scaleX), new Float(scaleY), new Float(0.0f),
                        new Float(0.0f), interpolation, null);
            } else {
                finalImg = ScaleDescriptor.create(inRasterRI, new Float(scaleX), new Float(scaleY), new Float(0.0f), new Float(
                        0.0f), interpolation, null);
            }
            // also keep track of the transforming envelope
            AffineTransform scaleAT = new AffineTransform();
            // scaleAT.translate(centerX, centerY);
            scaleAT.scale(scaleX, scaleY);
            // scaleAT.translate(-centerX, -centerY);
            MathTransform scaleTransform = new AffineTransform2D(scaleAT);

            Envelope jtsEnv = new Envelope(targetEnvelope.getMinX(), targetEnvelope.getMaxX(), targetEnvelope.getMinY(),
                    targetEnvelope.getMaxY());
            targetEnvelope = JTS.transform(jtsEnv, scaleTransform);

            Geometry scaledGeometry = gf.toGeometry(jtsEnv);
            targetGeometry = JTS.transform(scaledGeometry, scaleTransform);
            pm.done();
        }

        if (pTransX != null || pTransY != null) {
            float transX = 1f;
            float transY = 1f;
            if (pTransX == null) {
                transX = 1f;
            } else {
                transX = pTransX.floatValue();
            }
            if (pTransY == null) {
                transY = 1f;
            } else {
                transY = pTransY.floatValue();
            }
            pm.beginTask("Translate raster by: " + transX + " and " + transY, IJGTProgressMonitor.UNKNOWN);

            if (finalImg != null) {
                finalImg = TranslateDescriptor.create(finalImg, transX, transY, interpolation, null);
            } else {
                finalImg = TranslateDescriptor.create(inRasterRI, transX, transY, interpolation, null);
            }

            // also keep track of the transforming envelope
            AffineTransform translationAT = new AffineTransform();
            translationAT.translate(transX, transY);
            MathTransform translateTransform = new AffineTransform2D(translationAT);

            if (targetGeometry == null) {
                targetGeometry = gf.toGeometry(targetEnvelope);
            }

            targetEnvelope = JTS.transform(targetEnvelope, translateTransform);
            targetGeometry = JTS.transform(targetGeometry, translateTransform);

            pm.done();
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
