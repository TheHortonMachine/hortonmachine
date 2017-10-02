/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_IN_FOLDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSIMAGEMOSAICCREATOR_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSIMAGEMOSAICCREATOR_DESCRIPTION)
@Author(name = OMSIMAGEMOSAICCREATOR_AUTHORNAMES, contact = OMSIMAGEMOSAICCREATOR_AUTHORCONTACTS)
@Keywords(OMSIMAGEMOSAICCREATOR_KEYWORDS)
@Label(OMSIMAGEMOSAICCREATOR_LABEL)
@Name("_" + OMSIMAGEMOSAICCREATOR_NAME)
@Status(OMSIMAGEMOSAICCREATOR_STATUS)
@License(OMSIMAGEMOSAICCREATOR_LICENSE)
public class ImageMosaicCreator extends HMModel {

    @Description(OMSIMAGEMOSAICCREATOR_IN_FOLDER_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Execute
    public void process() throws Exception {
        OmsImageMosaicCreator imagemosaiccreator = new OmsImageMosaicCreator();
        imagemosaiccreator.inFolder = inFolder;
        imagemosaiccreator.pm = pm;
        imagemosaiccreator.doProcess = doProcess;
        imagemosaiccreator.doReset = doReset;
        imagemosaiccreator.process();
    }
}
