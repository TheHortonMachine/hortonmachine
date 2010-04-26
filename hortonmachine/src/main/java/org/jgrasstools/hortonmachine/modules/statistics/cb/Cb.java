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
package org.jgrasstools.hortonmachine.modules.statistics.cb;

import java.awt.image.RenderedImage;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.CoupledFieldsMoments;

@Description("Calculates the histogram of a set of data contained in a matrix "
        + "with respect to the set of data contained in another matrix")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Histogram, Geomorphology, Statistic")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Cb extends JGTModel {
    @Description("The first coverage to analyse.")
    @In
    public GridCoverage2D inMap1 = null;

    @Description("The second coverage to analyse.")
    @In
    public GridCoverage2D inMap2 = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Role(Role.PARAMETER)
    @Description("The number of bins into which divide the data range.")
    @In
    public int pBins;

    @Role(Role.PARAMETER)
    @Description("The first moment to consider.")
    @In
    public int pFirst;

    @Role(Role.PARAMETER)
    @Description("The last moment to consider.")
    @In
    public int pLast;

    @Description("A matrix containing " + "1) the mean value of the data in abscissa; "
            + "2) the number of elements in each interval; "
            + "3) the mean value of the data in ordinate; "
            + "n+2) the n-esimal moment of the data in ordinate.")
    @Out
    public double[][] outCb;

    private int binmode = 1;

    // private int bintype;
    // private float base;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outCb == null, doReset)) {
            return;
        }

        RenderedImage map1RI = inMap1.getRenderedImage();
        RenderedImage map2RI = null;
        if (inMap2 == null) {
            map2RI = map1RI;
        } else {
            map2RI = inMap2.getRenderedImage();
        }

        outCb = new CoupledFieldsMoments().process(map1RI, map2RI, pBins, pFirst, pLast, pm,
                binmode);

    }

}
