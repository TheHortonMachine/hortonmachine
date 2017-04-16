/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;

public class RunBasicFileDumper implements IRunBehavior {
    
    String path = null;
    GridCoverage2D coverage = null;
    
    public RunBasicFileDumper(String path, GridCoverage2D coverage){
        
        this.path = path;
        this.coverage = coverage;
    }

    @Override
    public void runLaunch() {
        try {
            OmsRasterWriter.writeRaster(path, coverage);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
