/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import static org.hortonmachine.gears.libs.modules.Variables.CALIBRATION;
import static org.hortonmachine.gears.libs.modules.Variables.PROJECT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_STATUS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_outDatabase_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_pPassword_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator.OMSTRENTOPPROJECTFILESGENERATOR_pUser_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION)
@Documentation(OMSTRENTOPPROJECTFILESGENERATOR_DOCUMENTATION)
@Author(name = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES, contact = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS)
@Keywords(OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS)
@Label(OMSTRENTOPPROJECTFILESGENERATOR_LABEL)
@Name(OMSTRENTOPPROJECTFILESGENERATOR_NAME)
@Status(OMSTRENTOPPROJECTFILESGENERATOR_STATUS)
@License(OMSTRENTOPPROJECTFILESGENERATOR_LICENSE)
public class TrentoPProjectFilesGenerator extends HMModel {

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_outDatabase_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDatabase = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pUser_DESCRIPTION)
    @In
    public String pUser = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pPassword_DESCRIPTION)
    @In
    public String pPassword = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION)
    @UI("combo:" + PROJECT + "," + CALIBRATION)
    @In
    public String pMode = PROJECT;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Execute
    public void process() throws Exception {
        OmsTrentoPProjectFilesGenerator generator = new OmsTrentoPProjectFilesGenerator();
        generator.pm = pm;
        generator.outDatabase = outDatabase;
        generator.pUser = pUser;
        generator.pPassword = pPassword;
        generator.pMode = pMode;
        generator.pCode = pCode;
        generator.process();
    }

}
