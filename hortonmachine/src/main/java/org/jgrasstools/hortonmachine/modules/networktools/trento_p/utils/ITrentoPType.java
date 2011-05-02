package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Interface used to implement a class which is used to create  SimpleFeatureType.
 * 
 * 
 * @see {@link SimpleFeatureType}
 * @author daniele andreis
 * 
 */
public interface ITrentoPType {
    /**
     * Getter for the binding class.
     * 
     * @return the binding class.
     */
    public Class< ? > getClazz();

    /**
     * Getter for the attribute name.
     * 
     * @return the attribute name.
     */
    public String getAttributeName();

    /**
     * Getter for the name.
     * 
     * @return the name of the type.
     */
    public String getName();

}
