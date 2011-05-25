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
package org.jgrasstools.gears.modules.utils.gcpoints2world;

import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

import org.geotools.geometry.DirectPosition2D;
import org.geotools.math.Statistics;
import org.geotools.referencing.operation.builder.AdvancedAffineBuilder;
import org.geotools.referencing.operation.builder.AffineToGeometric;
import org.geotools.referencing.operation.builder.MappedPosition;
import org.geotools.referencing.operation.builder.MathTransformBuilder;
import org.geotools.referencing.operation.builder.SimilarTransformBuilder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.geometry.DirectPosition;

@Description("A module to calculate world file coefficients from set of GCPs")
@Author(name = "Jan Jezek", contact = "http://code.google.com/p/oldmapsonline/")
@Keywords("gcp, wld")
@Label(JGTConstants.GENERICREADER)
@Status(Status.EXPERIMENTAL)
@Name("gcps2wld")
@License("General Public License Version 3 (GPLv3)")
public class GroundControlPoints2World extends JGTModel {

    @Description("The file containing the ground control points.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description("todo")
    @In
    public Double pSkew;

    @Description("todo")
    @In
    public Double pPhix;

    @Description("todo")
    @In
    public Double pPhiy;

    @Description("todo")
    @In
    public Double pTx;

    @Description("todo")
    @In
    public Double pTy;

    @Description("todo")
    @In
    public Double pSx;

    @Description("todo")
    @In
    public Double pSy;

    @Description("todo")
    @In
    public boolean doSimilar = false;

    @Description("todo")
    @Out
    public double outScaley;

    @Description("todo")
    @Out
    public double outScalex;

    @Description("todo")
    @Out
    public double outSheary;

    @Description("todo")
    @Out
    public double outShearx;

    @Description("todo")
    @Out
    public double outTranslatex;

    @Description("todo")
    @Out
    public double outTranslatey;

    @Description("todo")
    @Out
    public double outErrmean;

    @Description("todo")
    @Out
    public double outErrrms;

    @Description("todo")
    @Out
    public double outErrmax;

    @Description("todo")
    @Out
    public double outErrmin;

    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        List<MappedPosition> mps = new ArrayList<MappedPosition>();
        FileInputStream stream = new FileInputStream(inFile);

        BufferedReader cti = new BufferedReader(new InputStreamReader(stream));
        String s;
        while( (s = cti.readLine()) != null ) {
            String[] line = s.trim().split("\\s+"); //$NON-NLS-1$
            DirectPosition tp = new DirectPosition2D(null, (new Float(line[0])).floatValue(), (new Float(line[1])).floatValue());
            DirectPosition sp = new DirectPosition2D(null, (new Float(line[2])).floatValue(), (new Float(line[3])).floatValue());
            MappedPosition mp = new MappedPosition(sp, tp);
            mps.add(mp);
        }

        MathTransformBuilder ab;
        if (doSimilar) {
            ab = new SimilarTransformBuilder(mps);
        } else {
            ab = new AdvancedAffineBuilder(mps);
        }
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.SXY, pSkew);
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.PHIX, pPhix);
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.PHIY, pPhiy);
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.TX, pTx);
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.TY, pTy);
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.SX, pSx);
        ((AdvancedAffineBuilder) ab).setConstrain(AdvancedAffineBuilder.SY, pSy);

        outScalex = ((AffineTransform) ab.getMathTransform()).getScaleX();
        outScaley = ((AffineTransform) ab.getMathTransform()).getScaleY();
        outSheary = ((AffineTransform) ab.getMathTransform()).getShearY();
        outShearx = ((AffineTransform) ab.getMathTransform()).getShearX();
        outTranslatex = ((AffineTransform) ab.getMathTransform()).getTranslateX();
        outTranslatey = ((AffineTransform) ab.getMathTransform()).getTranslateY();

        // AffineToGeometric a2g = new AffineToGeometric((AffineTransform2D) ab.getMathTransform());
        // System.out.println("sx   = " + formatter.format(a2g.getXScale()));
        // System.out.println("sy   = " + formatter.format(a2g.getYScale()));
        // System.out.println("skew = " + formatter.format(a2g.getSkew()));
        // System.out.println("tx   = " + formatter.format(a2g.getXTranslate()));
        // System.out.println("ty   = " + formatter.format(a2g.getYTranslate()));
        // System.out.println("phix = " + formatter.format(a2g.getXRotation()));
        // System.out.println("phiy = " + formatter.format(a2g.getYRotation()));

        Statistics errorStatistics = ab.getErrorStatistics();
        outErrmean = errorStatistics.mean();
        outErrrms = errorStatistics.rms();
        outErrmax = errorStatistics.maximum();
        outErrmin = errorStatistics.minimum();
    }

}
