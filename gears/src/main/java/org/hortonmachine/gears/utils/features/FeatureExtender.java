/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gears.utils.features;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.factory.FactoryRegistryException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;

/**
 * Utility to add attributes to existing features.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureExtender {
    private SimpleFeatureType newFeatureType;

    /**
     * @param oldFeatureType the {@link FeatureType} of the existing features.
     * @param fieldArray the list of the names of new fields. 
     * @param classesArray the list of classes of the new fields.
     * @throws FactoryRegistryException 
     * @throws SchemaException
     */
    public FeatureExtender( SimpleFeatureType oldFeatureType, String[] fieldArray,
            Class<?>[] classesArray ) throws FactoryRegistryException, SchemaException {

        List<AttributeDescriptor> oldAttributeDescriptors = oldFeatureType
                .getAttributeDescriptors();
        List<AttributeDescriptor> addedAttributeDescriptors = new ArrayList<AttributeDescriptor>();
        for( int i = 0; i < fieldArray.length; i++ ) {
            AttributeTypeBuilder build = new AttributeTypeBuilder();
            build.setNillable(true);
            build.setBinding(classesArray[i]);
            AttributeDescriptor descriptor = build.buildDescriptor(fieldArray[i]);
            addedAttributeDescriptors.add(descriptor);
        }

        List<AttributeDescriptor> newAttributesTypesList = new ArrayList<AttributeDescriptor>();
        for( AttributeDescriptor attributeDescriptor : oldAttributeDescriptors ) {
            newAttributesTypesList.add(attributeDescriptor);
        }
        newAttributesTypesList.addAll(addedAttributeDescriptors);

        // create the feature type
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(oldFeatureType.getName());
        b.setCRS(oldFeatureType.getCoordinateReferenceSystem());
        b.addAll(newAttributesTypesList);
        newFeatureType = b.buildFeatureType();
    }
    
    public SimpleFeatureType getNewFeatureType() {
        return newFeatureType;
    }

    /**
     * @param oldFeature the feature from which to clone the existing attributes from.
     * @param additionalAttributes the array of attributes to add.
     * @return the new created feature, as merged from the old feature plus the new attributes.
     */
    public SimpleFeature extendFeature( SimpleFeature oldFeature, Object[] additionalAttributes ) {
        Object[] attributes = oldFeature.getAttributes().toArray();
        Object[] newAttributes = new Object[attributes.length + additionalAttributes.length];
        System.arraycopy(attributes, 0, newAttributes, 0, attributes.length);
        for( int i = 0; i < additionalAttributes.length; i++ ) {
            newAttributes[attributes.length + i] = additionalAttributes[i];
        }
        // create the feature
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(newFeatureType);
        builder.addAll(newAttributes);
        SimpleFeature f = builder.buildFeature(null);
        return f;
    }

}