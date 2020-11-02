package org.hortonmachine.hmachine.modules.networktools.trento_p.parameters;
/**
 * Implements this interface to store parameters value.
 * 
 * @author Daniele Andreis
 *
 */
public interface IParametersCode {
    public int getCode();
    /**
     * Get the key value.
     * 
     * @return a string which can used as a label in a gui.
     */
    public String getKey();
    /**
     * Get the unit.
     * 
     * @return the unit of measure.
     */
    public String getUnit();
    /**
     * Get the default value.
     * 
     * @return a default value of current parameter.
     */
    public Number getDefaultValue();
    /**
     * Get minimum value.
     * 
     * @return minimum value.
     */
    public Number getMinRange();
    /**
     * Get maximum value.
     * 
     * @return maximum value.
     */
    public Number getMaxRange();

    public boolean isInRange( Number value );
    /**
     * Get the name of this groups of parameters.
     * 
     * @return name that can be set as a WizardPage name..
     */
    public String getPageName();

}
