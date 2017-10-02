/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.hortonmachine.nww.layers.objects;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.nasa.worldwind.util.Logging;

/**
 * @author tag
 * @version $Id: BasicMarker.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicMarkerWithInfo extends AVListImpl implements Marker {

    protected Position position; // may be null
    protected Angle heading; // may be null
    protected Angle pitch; // may be null
    protected Angle roll; // may be null

    // To avoid the memory overhead of creating an attributes object for every new marker, attributes are
    // required to be specified at construction.
    protected MarkerAttributes attributes;
    private String info;

    public BasicMarkerWithInfo(Position position, MarkerAttributes attrs,
        String info) {
        this.info = info;
        if (attrs == null) {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.position = position;
        this.attributes = attrs;
    }

    public BasicMarkerWithInfo(Position position, MarkerAttributes attrs,
        Angle heading, String info) {
        this.info = info;
        if (attrs == null) {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.position = position;
        this.heading = heading;
        this.attributes = attrs;
    }

    public String getInfo() {
        return info;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    /** {@inheritDoc} */
    public Angle getHeading() {
        return this.heading;
    }

    /** {@inheritDoc} */
    public void setHeading(Angle heading) {
        this.heading = heading;
    }

    /** {@inheritDoc} */
    public Angle getRoll() {
        return this.roll;
    }

    /** {@inheritDoc} */
    public void setRoll(Angle roll) {
        this.roll = roll;
    }

    /** {@inheritDoc} */
    public Angle getPitch() {
        return this.pitch;
    }

    /** {@inheritDoc} */
    public void setPitch(Angle pitch) {
        this.pitch = pitch;
    }

    public MarkerAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(MarkerAttributes attributes) {
        this.attributes = attributes;
    }

    public void render(DrawContext dc, Vec4 point, double radius,
        boolean isRelative) {
        this.attributes.getShape(dc).render(dc, this, point, radius,
            isRelative);
    }

    public void render(DrawContext dc, Vec4 point, double radius) {
        this.attributes.getShape(dc).render(dc, this, point, radius, false);
    }
}
