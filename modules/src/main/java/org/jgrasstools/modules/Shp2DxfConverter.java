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

/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) Michael Michaud
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
package org.jgrasstools.modules;

import oms3.annotations.*;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorconverter.OmsShp2DxfConverter;

import static org.jgrasstools.gears.i18n.GearsMessages.*;

@Description(OmsShp2DxfConverter.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsShp2DxfConverter.KEYWORDS)
@Label(OMSDXFCONVERTER_LABEL)
@Name(OmsShp2DxfConverter.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Shp2DxfConverter extends JGTModel {

    @Description(OmsShp2DxfConverter.THE_FOLDER_CONTAINING_THE_SHAPEFILES)
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder = null;

    @Description(OmsShp2DxfConverter.FIELD_NAME_FOR_ELEVATION_VALUE)
    @In
    public String fElev = null;

    @Description(OmsShp2DxfConverter.DO_THE_SUFFIX)
    @In
    public boolean doSuffix = false;

    @Description(OmsShp2DxfConverter.THE_OUTPUT_DXF_FILE_PATH)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String inDxfpath = null;


    @Execute
    public void process() throws Exception {
        OmsShp2DxfConverter omsShp2DxfConverter = new OmsShp2DxfConverter();
        omsShp2DxfConverter.inFolder = inFolder;
        omsShp2DxfConverter.fElev = fElev;
        omsShp2DxfConverter.doSuffix = doSuffix;
        omsShp2DxfConverter.inDxfpath = inDxfpath;
        omsShp2DxfConverter.process();
    }

}
