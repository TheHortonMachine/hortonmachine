package oms3;

/**
 * Converter Interface
 * @param <F>
 * @param <T>
 */
public interface Converter<F, T> {

     T convert(F src, Object... arg);
}
