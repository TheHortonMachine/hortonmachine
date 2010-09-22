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
package org.jgrasstools.hortonmachine.externals.epanet;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.core.OptionParameterCodes;

@Description("The options parameters of the epanet inp file")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class EpanetParametersOptions extends JGTModel {

    @Description("Units.")
    @In
    public String units = null;

    @Description("Headloss.")
    @In
    public String headloss = null;

    @Description("Hydraulics.")
    @In
    public String hydraulics = null;

    @Description("Quality.")
    @In
    public String quality = null;

    @Description("Viscosity.")
    @In
    public Double viscosity = null;

    @Description("Diffusivity.")
    @In
    public Double diffusivity = null;

    @Description("Specific gravity.")
    @In
    public Double specificGravity = null;

    @Description("Trials.")
    @In
    public Integer trials = null;

    @Description("Accuracy.")
    @In
    public Double accuracy = null;

    @Description("Unbalanced.")
    @In
    public String unbalanced = null;

    @Description("Pattern.")
    @In
    public Integer pattern = null;

    @Description("Demand multiplier.")
    @In
    public Double demandMultiplier = null;

    @Description("Emitter exponent.")
    @In
    public Double emitterExponent = null;

    @Description("Tolerance.")
    @In
    public Double tolerance = null;

    @Description("Properties file containing the options.")
    @In
    public String inFile = null;

    @Description("The Properties needed for epanet.")
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

            if (hydraulics == null) {
                hydraulics = OptionParameterCodes.HYDRAULICS.getDefault();
            }
            outProperties.put(OptionParameterCodes.HYDRAULICS.getKey(), hydraulics);

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
     * Create a {@link EpanetParametersOptions} from a {@link HashMap} of values.
     * 
     * @param options the {@link HashMap} of values. The keys have to be from {@link OptionParameterCodes}.
     * @return the created {@link EpanetParametersOptions}.
     */
    public EpanetParametersOptions createFromMap( HashMap<OptionParameterCodes, String> options ) {
        EpanetParametersOptions epOptions = new EpanetParametersOptions();
        String units = options.get(OptionParameterCodes.UNITS);
        epOptions.units = units;
        String headloss = options.get(OptionParameterCodes.HEADLOSS);
        epOptions.headloss = headloss;
        String hydraulics = options.get(OptionParameterCodes.HYDRAULICS);
        epOptions.hydraulics = hydraulics;
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
        epOptions.trials = NumericsUtilities.isNumber(pattern, Integer.class);
        String demandMultiplier = options.get(OptionParameterCodes.DEMANDMULTIPLIER);
        epOptions.demandMultiplier = NumericsUtilities.isNumber(demandMultiplier, Double.class);
        String emitterExp = options.get(OptionParameterCodes.EMITEXPON);
        epOptions.emitterExponent = NumericsUtilities.isNumber(emitterExp, Double.class);
        String tolerance = options.get(OptionParameterCodes.TOLERANCE);
        epOptions.tolerance = NumericsUtilities.isNumber(tolerance, Double.class);
        return epOptions;
    }

}
