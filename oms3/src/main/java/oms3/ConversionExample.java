/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import java.awt.Color;
import java.util.Currency;

/**
 *
 * @author od
 */
public class ConversionExample implements ConversionProvider {

    @Override
    public Converter getConverter(Class from, Class to) {
        if (from == String.class && to == Currency.class) {

            return new Converter<String, Currency>() {

                @Override
                public Currency convert(String src, Object arg) {
                    return Currency.getInstance(src);
                }
            };
        } else {
            if (from == String.class && to == Color.class) {

                return new Converter<String, Color>() {

                    @Override
                    public Color convert(String src, Object arg) {

                        return new Color(Integer.parseInt(src, 16));
                    }
                };
            }
            return null;
        }
    }
}
