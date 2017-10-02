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
package org.hortonmachine.gears.io.dxfdwg.libs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgFile;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class DwgHandler {

    private final DwgFile dwgFile;
    private GeometryTranslator gTranslator;

    public DwgHandler( File dxfFile, CoordinateReferenceSystem crs )  {
        this.dwgFile = new DwgFile(dxfFile.getAbsolutePath());
        gTranslator = new GeometryTranslator(crs);
    }

    public DwgReader getDwgReader() throws Exception {
        return new DwgReader(dwgFile, gTranslator);
    }

    public List<String> getLayerTypes() throws Exception {
        dwgFile.read();
        dwgFile.initializeLayerTable();
        dwgFile.calculateGisModelDwgPolylines();
        dwgFile.applyExtrusions();
        dwgFile.testDwg3D();
        dwgFile.blockManagement();

        List<String> layerNames = dwgFile.getLayerNames();
        if (layerNames.isEmpty()) {
            throw new IOException("No layer found in the file.");
        }

        return layerNames;
    }

}
