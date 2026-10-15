/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import org.geotools.api.feature.simple.SimpleFeature;

import gov.nasa.worldwind.render.SurfacePolygon;

/**
 * A feature polygon draped on the terrain.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FeatureSurfacePolygon extends SurfacePolygon implements IFeatureShape {
    private SimpleFeature feature;
    private FeatureStoreInfo featureStoreInfo;

    public FeatureSurfacePolygon( FeatureStoreInfo featureStoreInfo ) {
        this.featureStoreInfo = featureStoreInfo;
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public void setFeature( SimpleFeature feature ) {
        this.feature = feature;
    }

    @Override
    public FeatureStoreInfo getFeatureStoreInfo() {
        return featureStoreInfo;
    }
}
