/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3;

/** Converter Service provider
 *
 * @author od
 */
public interface ConversionProvider {

     Converter<?,?> getConverter(Class from, Class to);

}
