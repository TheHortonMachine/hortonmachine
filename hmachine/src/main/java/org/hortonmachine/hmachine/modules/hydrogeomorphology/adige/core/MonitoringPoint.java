/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class MonitoringPoint
        implements
            Comparator<MonitoringPoint>,
            MonitoringPointsConstants {

    public static final int TYPE_HYDROMETER_DISCHARGE = 0;
    public static final int TYPE_DAM_SCARICO = 1;
    public static final int TYPE_DAM_TURBINATA = 2;

    /**
     * The identifier id of the data feed into the monitoring point. This identifier makes it
     * possible to understand how to calculate the needed output. It is very different from the
     * typeIdentifier parameter of the method
     * {@link MonitoringPoint#getDataValueAt(Date, Double, int)}.
     */
    protected int currentIdentifier = -1;

    protected Coordinate position = null;
    protected int ID = -1;

    protected PfafstetterNumber pfafstetterNumber;
    protected String name = " - "; //$NON-NLS-1$
    protected String description = " - "; //$NON-NLS-1$
    protected int type;
    protected int relatedID;
    protected Hashtable<String, MonitoringPoint> pfafRelatedMonitoringPointsTable = new Hashtable<String, MonitoringPoint>();
    protected boolean isActive = false;
    protected double dataTimeInterval = -1;
    protected String dataTimeIntervalUnit = ""; //$NON-NLS-1$
    protected String srsCode = ""; //$NON-NLS-1$
    protected String quantityName = ""; //$NON-NLS-1$
    protected String quantityUnit = ""; //$NON-NLS-1$

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PfafstetterNumber getPfatstetterNumber() {
        return pfafstetterNumber;
    }

    public int getID() {
        return ID;
    }

    public int getRelatedID() {
        return relatedID;
    }

    /**
     * Tries to connect another monitoringpoint if that one has a related id equal to that of this
     * object. That way it would be possible to add more than one point as related.
     * 
     * @param monitoringPoint the point to connect
     * @return true if the connection was successfull
     */
    public boolean connectIfPossible( MonitoringPoint monitoringPoint ) {
        // check if the other point has this as related id
        if (ID == monitoringPoint.getRelatedID()) {
            pfafRelatedMonitoringPointsTable.put(monitoringPoint.getPfatstetterNumber().toString(),
                    monitoringPoint);
            return true;
        }
        return false;

    }

    /**
     * Get the related monitoringpoint. If there are more than one, the pfafstetter number is used
     * to chose.
     * 
     * @param pfafStetter used to chose in the points table. Can be null, in which case the first
     *        found is taken (should be used only if there is only one point in the table)
     * @return the related point
     */
    public MonitoringPoint getRelatedMonitoringPoint( String pfafStetter ) {
        if (pfafStetter != null) {
            return pfafRelatedMonitoringPointsTable.get(pfafStetter);
        } else {
            Set<String> keySet = pfafRelatedMonitoringPointsTable.keySet();
            for( String key : keySet ) {
                return pfafRelatedMonitoringPointsTable.get(key);
            }
            return null;
        }
    }

    public int getType() {
        return type;
    }

    public Coordinate getPosition() {
        return position;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive( boolean isActive ) {
        this.isActive = isActive;
    }

    /**
     * Add a date/value data record to a particular series
     * 
     * @param date date of the record
     * @param value value of the record
     * @param identifier identifier of the recorded series
     */
    public abstract void addDateValueRecord( Date date, Double value, int identifier );

    /**
     * Add a value/value data record to a particular series (could be level/volume or
     * level/discharge)
     * 
     * @param xValue the domain value for the record
     * @param yValue the range value for the record
     * @param identifier and identifier for the series
     */
    public abstract void addValueValueRecord( Double xValue, Double yValue, int identifier );

    /**
     * Get a data record from a particular serie
     * 
     * @param date the date for which teh value is requested ({@link MonitoringPoint#geodataNovalue} is
     *        returned, if no value present)
     * @param value a value if a value is needed to calculate a result (for example in the case of a
     *        dam, if the output discharge is based on the input discharge)
     * @param tyepIdentifier identifier of the type of output requested. <b>IMPORTANT:</b> this is
     *        very different from the identifiers used to define the datattypes that are feed into
     *        the monitoringpoint, which use the {@link MonitoringPoint#currentIdentifier}. <br>
     *        The typeIdentifier can be of type:<br>
     *        <ul>
     *        <li> {@link MonitoringPoint#TYPE_HYDROMETER_DISCHARGE} </li>
     *        <li> {@link MonitoringPoint#TYPE_DAM_SCARICO} </li>
     *        <li> {@link MonitoringPoint#TYPE_DAM_TURBINATA} </li>
     *        </ul>
     * @return the value at the requested date
     */
    public abstract Double getDataValueAt( Date date, Double value, int tyepIdentifier );

    public int compare( MonitoringPoint o1, MonitoringPoint o2 ) {

        PfafstetterNumber p1 = o1.getPfatstetterNumber();
        PfafstetterNumber p2 = o2.getPfatstetterNumber();

        if (p1 != null && p2 != null) {

            List<Integer> p1OrdersList = o1.getPfatstetterNumber().getOrdersList();
            List<Integer> p2OrdersList = o2.getPfatstetterNumber().getOrdersList();

            int levels = p1OrdersList.size();
            if (p2OrdersList.size() < levels) {
                levels = p2OrdersList.size();
            }

            /*
             * check the numbers to the minor level of the two
             */
            for( int i = 0; i < levels; i++ ) {
                int thisone = p1OrdersList.get(i);
                int otherone = p2OrdersList.get(i);
                if (thisone > otherone) {
                    /*
                     * if this has major number of the other, then this has to be sorted as minor of
                     * the other, following the pfafstetter logic that has major numbers towards
                     * valley
                     */
                    return -1;
                } else if (thisone < otherone) {
                    return 1;
                } else {
                    // if they are equal, go on to the next level
                    continue;
                }
            }

            return 0;
        } else {
            if (p1 == null)
                System.out.println("Pstaett null  for " + o1.getName());
            if (p2 == null)
                System.out.println("Pstaett null  for " + o2.getName());
            return -2;
        }
    }

    /**
     * Create a featurecollection from a list of monitoringpoints. Based on the position of the
     * points and some of their attributes.
     * 
     * @param monitoringPointsList
     * @return the featurecollection
     */
    public SimpleFeatureCollection toFeatureCollection(
            List<MonitoringPoint> monitoringPointsList ) {

        // create the feature type
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        // set the name
        b.setName("monitoringpoints");
        // add a geometry property
        b.add("the_geom", Point.class);
        // add some properties
        b.add("id", Integer.class);
        b.add("relatedid", Integer.class);
        b.add("pfaf", String.class);
        // build the type
        SimpleFeatureType type = b.buildFeatureType();
        // create the feature
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        GeometryFactory gF = new GeometryFactory();
        /*
         * insert them in inverse order to get them out of the collection in the same order as the
         * list
         */
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        for( int i = 0; i < monitoringPointsList.size(); i++ ) {
            MonitoringPoint mp = monitoringPointsList.get(monitoringPointsList.size() - i - 1);

            Object[] values = new Object[]{gF.createPoint(mp.getPosition()), mp.getID(),
                    mp.getRelatedID(), mp.getPfatstetterNumber().toString()};
            // add the values
            builder.addAll(values);
            // build the feature with provided ID
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + i);
            newCollection.add(feature);
        }

        return newCollection;
    }
}
