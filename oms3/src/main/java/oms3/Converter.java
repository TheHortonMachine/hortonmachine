package oms3;

import oms3.Conversions.Params;

/**
 * Converter Interface
 * @param <F>
 * @param <T>
 */
public interface Converter<F, T> {

     T convert(F src, Object arg);
}
