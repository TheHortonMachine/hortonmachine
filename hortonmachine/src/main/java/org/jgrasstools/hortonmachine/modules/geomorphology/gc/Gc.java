package org.jgrasstools.hortonmachine.modules.geomorphology.gc;

import oms3.annotations.Description;
import oms3.annotations.In;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;

public class Gc extends JGTModel {
    @Description("The map of the slope")
    @In
    public GridCoverage2D inSlope = null;

    @Description("The map of with the network")
    @In
    public GridCoverage2D inNetwork = null;
    
    @Description("The map of with the Thopological classes cp9")
    @In
    public GridCoverage2D inCp9 = null;
    
    @Description("The gradient formula mode (0 = finite differences, 1 = horn, 2 = evans).")
    @In
    public int pTh = 0;
    
    

}
