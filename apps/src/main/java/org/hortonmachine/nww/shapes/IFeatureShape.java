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
package org.hortonmachine.nww.shapes;

import java.io.IOException;
import java.util.Collections;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * A NWW shape holding a {@link SimpleFeature} and if editable, also a store.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IFeatureShape {

    public SimpleFeature getFeature();

    public FeatureStoreInfo getFeatureStoreInfo();
    
   
    /**
     * Modify the attributes of this feature.
     * 
     * @param names the names of the attributes to modify.
     * @param values the values to set.
     * @return the new feature if the editing has been successful.
     */
    default public SimpleFeature modifyFeatureAttribute( String[] names, Object[] values ) {
        FeatureStoreInfo featureStoreInfo = getFeatureStoreInfo();
        SimpleFeatureStore featureStore = featureStoreInfo.getFeatureStore();
        if (featureStore != null) {
            Transaction transaction = new DefaultTransaction("modify");
            featureStore.setTransaction(transaction);

            FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Filter filter = ff.id(Collections.singleton(ff.featureId(getFeature().getID())));

            try {
                featureStore.modifyFeatures(names, values, filter);
                transaction.commit();

                SimpleFeature modifiedFeature = featureStore.getFeatures(filter).features().next();
                return modifiedFeature;
            } catch (Exception eek) {
                try {
                    transaction.rollback();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Add a feature to the store.
     * 
     * @param feature the feature to add.
     */
    default public void addFeature( SimpleFeature feature) {
        FeatureStoreInfo featureStoreInfo = getFeatureStoreInfo();
        SimpleFeatureStore featureStore = featureStoreInfo.getFeatureStore();
        if (featureStore != null) {
            Transaction transaction = new DefaultTransaction("add");
            featureStore.setTransaction(transaction);
            
            try {
                DefaultFeatureCollection fc = new DefaultFeatureCollection();
                fc.add(feature);
                featureStore.addFeatures(fc);
                transaction.commit();
            } catch (Exception eek) {
                try {
                    transaction.rollback();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setFeature( SimpleFeature feature );

}
