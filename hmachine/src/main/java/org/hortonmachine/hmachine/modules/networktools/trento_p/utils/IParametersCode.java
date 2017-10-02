package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;
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
     * Get the description value.
     * 
     * @return a string which can used as a tip in a gui.
     */
    public String getDescription();
    /**
     * Get the default value.
     * 
     * @return a default value of current parameter.
     */
    public String getDefaultValue();
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
    /**
     * Get the name of this groups of parameters.
     * 
     * @return name that can be set as a WizardPage name..
     */
    public String getPageName();

}
