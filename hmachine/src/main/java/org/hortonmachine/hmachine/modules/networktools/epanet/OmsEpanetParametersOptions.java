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
package org.hortonmachine.hmachine.modules.networktools.epanet;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_accuracy_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_demandMultiplier_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_diffusivity_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_emitterExponent_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_headloss_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_inFile_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_outProperties_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_pattern_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_quality_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_specificGravity_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_tolerance_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_trials_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_unbalanced_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_units_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSOPTIONS_viscosity_DESCRIPTION;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

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

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.OptionParameterCodes;

@Description(OMSEPANETPARAMETERSOPTIONS_DESCRIPTION)
@Author(name = OMSEPANETPARAMETERSOPTIONS_AUTHORNAMES, contact = OMSEPANETPARAMETERSOPTIONS_AUTHORCONTACTS)
@Keywords(OMSEPANETPARAMETERSOPTIONS_KEYWORDS)
@Label(OMSEPANETPARAMETERSOPTIONS_LABEL)
@Name(OMSEPANETPARAMETERSOPTIONS_NAME)
@Status(OMSEPANETPARAMETERSOPTIONS_STATUS)
@License(OMSEPANETPARAMETERSOPTIONS_LICENSE)
public class OmsEpanetParametersOptions extends HMModel {

    @Description(OMSEPANETPARAMETERSOPTIONS_units_DESCRIPTION)
    @In
    public String units = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_headloss_DESCRIPTION)
    @In
    public String headloss = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_quality_DESCRIPTION)
    @In
    public String quality = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_viscosity_DESCRIPTION)
    @In
    public Double viscosity = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_diffusivity_DESCRIPTION)
    @In
    public Double diffusivity = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_specificGravity_DESCRIPTION)
    @In
    public Double specificGravity = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_trials_DESCRIPTION)
    @In
    public Integer trials = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_accuracy_DESCRIPTION)
    @In
    public Double accuracy = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_unbalanced_DESCRIPTION)
    @In
    public String unbalanced = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_pattern_DESCRIPTION)
    @In
    public Integer pattern = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_demandMultiplier_DESCRIPTION)
    @In
    public Double demandMultiplier = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_emitterExponent_DESCRIPTION)
    @In
    public Double emitterExponent = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_tolerance_DESCRIPTION)
    @In
    public Double tolerance = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_inFile_DESCRIPTION)
    @In
    public String inFile = null;

    @Description(OMSEPANETPARAMETERSOPTIONS_outProperties_DESCRIPTION)
    @Out
    public Properties outProperties = new Properties();

    /**
     * The title of the options section in the inp file.
     */
    public static final String OPTIONSSECTION = "[OPTIONS]"; //$NON-NLS-1$

    @Execute
    public void process() throws Exception {
        if (inFile != null) {
            File file = new File(inFile);
            outProperties.load(new FileReader(file));
        } else {
            if (units == null) {
                units = OptionParameterCodes.UNITS.getDefault();
            }
            outProperties.put(OptionParameterCodes.UNITS.getKey(), units);

            if (headloss == null) {
                headloss = OptionParameterCodes.HEADLOSS.getDefault();
            }
            outProperties.put(OptionParameterCodes.HEADLOSS.getKey(), headloss);

            if (quality == null) {
                quality = OptionParameterCodes.QUALITY.getDefault();
            }
            outProperties.put(OptionParameterCodes.QUALITY.getKey(), quality);

            if (unbalanced == null) {
                unbalanced = OptionParameterCodes.UNBALANCED.getDefault();
            }
            outProperties.put(OptionParameterCodes.UNBALANCED.getKey(), unbalanced);

            String vStr = ""; //$NON-NLS-1$
            if (viscosity == null) {
                vStr = OptionParameterCodes.VISCOSITY.getDefault();
            } else {
                vStr = String.valueOf(viscosity);
            }
            outProperties.put(OptionParameterCodes.VISCOSITY.getKey(), vStr);

            String dStr = ""; //$NON-NLS-1$
            if (diffusivity == null) {
                dStr = OptionParameterCodes.DIFFUSIVITY.getDefault();
            } else {
                dStr = String.valueOf(diffusivity);
            }
            outProperties.put(OptionParameterCodes.DIFFUSIVITY.getKey(), dStr);

            String sgStr = ""; //$NON-NLS-1$
            if (specificGravity == null) {
                sgStr = OptionParameterCodes.SPECIFICGRAVITY.getDefault();
            } else {
                sgStr = String.valueOf(specificGravity);
            }
            outProperties.put(OptionParameterCodes.SPECIFICGRAVITY.getKey(), sgStr);

            String tStr = ""; //$NON-NLS-1$
            if (trials == null) {
                tStr = OptionParameterCodes.TRIALS.getDefault();
            } else {
                tStr = String.valueOf(trials);
            }
            outProperties.put(OptionParameterCodes.TRIALS.getKey(), tStr);

            String aStr = ""; //$NON-NLS-1$
            if (accuracy == null) {
                aStr = OptionParameterCodes.ACCURACY.getDefault();
            } else {
                aStr = String.valueOf(accuracy);
            }
            outProperties.put(OptionParameterCodes.ACCURACY.getKey(), aStr);

            String pStr = ""; //$NON-NLS-1$
            if (pattern == null) {
                pStr = OptionParameterCodes.PATTERN.getDefault();
            } else {
                pStr = String.valueOf(pattern);
            }
            outProperties.put(OptionParameterCodes.PATTERN.getKey(), pStr);

            String dmStr = ""; //$NON-NLS-1$
            if (demandMultiplier == null) {
                dmStr = OptionParameterCodes.DEMANDMULTIPLIER.getDefault();
            } else {
                dmStr = String.valueOf(demandMultiplier);
            }
            outProperties.put(OptionParameterCodes.DEMANDMULTIPLIER.getKey(), dmStr);

            String eeStr = ""; //$NON-NLS-1$
            if (emitterExponent == null) {
                eeStr = OptionParameterCodes.EMITEXPON.getDefault();
            } else {
                eeStr = String.valueOf(emitterExponent);
            }
            outProperties.put(OptionParameterCodes.EMITEXPON.getKey(), eeStr);

            String toStr = ""; //$NON-NLS-1$
            if (tolerance == null) {
                toStr = OptionParameterCodes.TOLERANCE.getDefault();
            } else {
                toStr = String.valueOf(tolerance);
            }
            outProperties.put(OptionParameterCodes.TOLERANCE.getKey(), toStr);
        }
    }

    /**
     * Create a {@link OmsEpanetParametersOptions} from a {@link HashMap} of values.
     * 
     * @param options the {@link HashMap} of values. The keys have to be from {@link OptionParameterCodes}.
     * @return the created {@link OmsEpanetParametersOptions}.
     * @throws Exception 
     */
    public static OmsEpanetParametersOptions createFromMap( HashMap<OptionParameterCodes, String> options ) throws Exception {
        OmsEpanetParametersOptions epOptions = new OmsEpanetParametersOptions();
        String units = options.get(OptionParameterCodes.UNITS);
        epOptions.units = units;
        String headloss = options.get(OptionParameterCodes.HEADLOSS);
        epOptions.headloss = headloss;
        String quality = options.get(OptionParameterCodes.QUALITY);
        epOptions.quality = quality;
        String viscosity = options.get(OptionParameterCodes.VISCOSITY);
        epOptions.viscosity = NumericsUtilities.isNumber(viscosity, Double.class);
        String diffusivity = options.get(OptionParameterCodes.DIFFUSIVITY);
        epOptions.diffusivity = NumericsUtilities.isNumber(diffusivity, Double.class);
        String specGravity = options.get(OptionParameterCodes.SPECIFICGRAVITY);
        epOptions.specificGravity = NumericsUtilities.isNumber(specGravity, Double.class);
        String trials = options.get(OptionParameterCodes.TRIALS);
        epOptions.trials = NumericsUtilities.isNumber(trials, Integer.class);
        String accuracy = options.get(OptionParameterCodes.ACCURACY);
        epOptions.accuracy = NumericsUtilities.isNumber(accuracy, Double.class);
        String unbalanced = options.get(OptionParameterCodes.UNBALANCED);
        epOptions.unbalanced = unbalanced;
        String pattern = options.get(OptionParameterCodes.PATTERN);
        epOptions.pattern = NumericsUtilities.isNumber(pattern, Integer.class);
        String demandMultiplier = options.get(OptionParameterCodes.DEMANDMULTIPLIER);
        epOptions.demandMultiplier = NumericsUtilities.isNumber(demandMultiplier, Double.class);
        String emitterExp = options.get(OptionParameterCodes.EMITEXPON);
        epOptions.emitterExponent = NumericsUtilities.isNumber(emitterExp, Double.class);
        String tolerance = options.get(OptionParameterCodes.TOLERANCE);
        epOptions.tolerance = NumericsUtilities.isNumber(tolerance, Double.class);
        epOptions.process();
        return epOptions;
    }

}
