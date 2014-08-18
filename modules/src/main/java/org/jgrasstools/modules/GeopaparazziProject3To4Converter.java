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
import org.jgrasstools.gears.io.geopaparazzi.OmsGeopaparazzi4Converter;
import org.jgrasstools.gears.io.geopaparazzi.OmsGeopaparazziProject3To4Converter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

import java.io.IOException;

import static org.jgrasstools.gears.i18n.GearsMessages.*;

@Description(OmsGeopaparazziProject3To4Converter.CONVERT_A_GEOPAPARAZZI_3_FOLDER_PROJECT_INTO_A_GEOPAPARAZZI_4_DATABASE)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(JGTConstants.MOBILE)
@Name("_" + OmsGeopaparazziProject3To4Converter.GEOPAP3TO4)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class GeopaparazziProject3To4Converter extends JGTModel {

    @Description(OmsGeopaparazziProject3To4Converter.GEOPAPARAZZI_3_INPUT_FOLDER_TO_CONVERT)
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    @Execute
    public void process() throws IOException {
        OmsGeopaparazziProject3To4Converter geopaparazziconverter = new OmsGeopaparazziProject3To4Converter();
        geopaparazziconverter.inGeopaparazzi = inGeopaparazzi;
        geopaparazziconverter.pm = pm;
        geopaparazziconverter.process();
    }

}
