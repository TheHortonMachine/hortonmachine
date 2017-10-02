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
package org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils;

import java.awt.geom.Point2D;
import java.util.Vector;

/**
 * This class allows to obtain arcs and circles given by the most usual parameters, in a
 * Gis geometry model. In this model, an arc or a circle is given by a set of points that
 * defines it shape
 * 
 * @author jmorell
 */
public class GisModelCurveCalculator {
    
    /**
     * This method calculates an array of Point2D that represents a circle. The distance
     * between it points is 1 angular unit 
     * 
     * @param c Point2D that represents the center of the circle
     * @param r double value that represents the radius of the circle
     * @return Point2D[] An array of Point2D that represents the shape of the circle
     */
	public static Point2D[] calculateGisModelCircle(Point2D c, double r) {
		Point2D[] pts = new Point2D[360];
		int angulo = 0;
		for (angulo=0; angulo<360; angulo++) {
			pts[angulo] = new Point2D.Double(c.getX(), c.getY());
			pts[angulo].setLocation(pts[angulo].getX() + r * Math.sin(angulo*Math.PI/(double)180.0), pts[angulo].getY() + r * Math.cos(angulo*Math.PI/(double)180.0));
		}
		return pts;
	}
	
    /**
     * This method calculates an array of Point2D that represents a ellipse. The distance
     * between it points is 1 angular unit 
     * 
     * @param center Point2D that represents the center of the ellipse
     * @param majorAxisVector Point2D that represents the vector for the major axis
     * @param axisRatio double value that represents the axis ratio
	 * @param initAngle double value that represents the start angle of the ellipse arc
	 * @param endAngle double value that represents the end angle of the ellipse arc
     * @return Point2D[] An array of Point2D that represents the shape of the ellipse
     */
	public static Point2D[] calculateGisModelEllipse(Point2D center, Point2D majorAxisVector, double axisRatio, double initAngle, double endAngle) {
		Point2D majorPoint = new Point2D.Double(center.getX()+majorAxisVector.getX(), center.getY()+majorAxisVector.getY());
	    double orientation  = Math.atan(majorAxisVector.getY()/majorAxisVector.getX());
	    double semiMajorAxisLength = center.distance(majorPoint);
		double semiMinorAxisLength = semiMajorAxisLength*axisRatio;
	    double eccentricity = Math.sqrt(1-((Math.pow(semiMinorAxisLength, 2))/(Math.pow(semiMajorAxisLength, 2))));
		int isa = (int)initAngle;
		int iea = (int)endAngle;
		double angulo;
		Point2D[] pts;
		if (initAngle <= endAngle) {
			pts = new Point2D[(iea-isa)+2];
			angulo = initAngle;
			double r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
		    double x = r*Math.cos(angulo*Math.PI/(double)180.0);
		    double y = r*Math.sin(angulo*Math.PI/(double)180.0);
		    double xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
		    double yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
			pts[0] = new Point2D.Double(center.getX() + xrot, center.getY() + yrot);
			for (int i=1; i<=(iea-isa)+1; i++) {
				angulo = (double)(isa+i);
				r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
			    x = r*Math.cos(angulo*Math.PI/(double)180.0);
			    y = r*Math.sin(angulo*Math.PI/(double)180.0);
			    xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
			    yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
			    pts[i] = new Point2D.Double(center.getX() + xrot, center.getY() + yrot);
			}
			angulo = endAngle;
			r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
		    x = r*Math.cos(angulo*Math.PI/(double)180.0);
		    y = r*Math.sin(angulo*Math.PI/(double)180.0);
		    xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
		    yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
		    pts[(iea-isa)+1] = new Point2D.Double(center.getX() + xrot, center.getY() + yrot);
		} else {
			pts = new Point2D[(360-isa)+iea+2];
			angulo = initAngle;
			double r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
		    double x = r*Math.cos(angulo*Math.PI/(double)180.0);
		    double y = r*Math.sin(angulo*Math.PI/(double)180.0);
		    double xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
		    double yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
		    pts[0] = new Point2D.Double(center.getX() + r*Math.cos(angulo*Math.PI/(double)180.0), center.getY() + r*Math.sin(angulo*Math.PI/(double)180.0));
			for (int i=1; i<=(360-isa); i++) {
				angulo = (double)(isa+i);
				r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
			    x = r*Math.cos(angulo*Math.PI/(double)180.0);
			    y = r*Math.sin(angulo*Math.PI/(double)180.0);
			    xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
			    yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
			    pts[i] = new Point2D.Double(center.getX() + xrot, center.getY() + yrot);
			}
			for (int i=(360-isa)+1; i<=(360-isa)+iea; i++) {
				angulo = (double)(i-(360-isa));
				r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
			    x = r*Math.cos(angulo*Math.PI/(double)180.0);
			    y = r*Math.sin(angulo*Math.PI/(double)180.0);
			    xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
			    yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
			    pts[i] = new Point2D.Double(center.getX() + xrot, center.getY() + yrot);
			}
			angulo = endAngle;
			r = semiMinorAxisLength/Math.sqrt(1-((Math.pow(eccentricity, 2))*(Math.pow(Math.cos(angulo*Math.PI/(double)180.0), 2))));
		    x = r*Math.cos(angulo*Math.PI/(double)180.0);
		    y = r*Math.sin(angulo*Math.PI/(double)180.0);
		    xrot = x*Math.cos(orientation) - y*Math.sin(orientation);
		    yrot = x*Math.sin(orientation) + y*Math.cos(orientation);
		    pts[(360-isa)+iea+1] = new Point2D.Double(center.getX() + xrot, center.getY() + yrot);
		}
		return pts;
	}
	
	/**
     * This method calculates an array of Point2D that represents an arc. The distance
     * between it points is 1 angular unit 
	 * 
     * @param c Point2D that represents the center of the arc
     * @param r double value that represents the radius of the arc
	 * @param sa double value that represents the start angle of the arc
	 * @param ea double value that represents the end angle of the arc
     * @return Point2D[] An array of Point2D that represents the shape of the arc
	 */
	public static Point2D[] calculateGisModelArc(Point2D c, double r, double sa, double ea) {
		int isa = (int)sa;
		int iea = (int)ea;
		double angulo;
		Point2D[] pts;
		if (sa <= ea) {
			pts = new Point2D[(iea-isa)+2];
			angulo = sa;
			pts[0] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
			for (int i=1; i<=(iea-isa)+1; i++) {
				angulo = (double)(isa+i);
				pts[i] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
			}
			angulo = ea;
			pts[(iea-isa)+1] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
		} else {
			pts = new Point2D[(360-isa)+iea+2];
			angulo = sa;
			pts[0] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
			for (int i=1; i<=(360-isa); i++) {
				angulo = (double)(isa+i);
				pts[i] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
			}
			for (int i=(360-isa)+1; i<=(360-isa)+iea; i++) {
				angulo = (double)(i-(360-isa));
				pts[i] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
			}
			angulo = ea;
			pts[(360-isa)+iea+1] = new Point2D.Double(c.getX() + r * Math.cos(angulo*Math.PI/(double)180.0), c.getY() + r * Math.sin(angulo*Math.PI/(double)180.0));
		}
		return pts;
	}
	
	/**
	 * This method applies an array of bulges to an array of Point2D that defines a
	 * polyline. The result is a polyline with the input points with the addition of the
	 * points that define the new arcs added to the polyline
	 * 
	 * @param newPts Base points of the polyline
	 * @param bulges Array of bulge parameters
	 * @return Polyline with a new set of arcs added and defined by the bulge parameters
	 */
	public static Point2D[] calculateGisModelBulge(Point2D[] newPts, double[] bulges) {
		Vector ptspol = new Vector();
		Point2D init = new Point2D.Double();
		Point2D end = new Point2D.Double();
		for (int j=0; j<newPts.length; j++) {
			init = newPts[j];
			if (j!=newPts.length-1) end = newPts[j+1];
			if (bulges[j]==0 || j==newPts.length-1 || (init.getX()==end.getX() && init.getY()==end.getY())) {
				ptspol.add(init);
			} else {
				ArcFromBulgeCalculator arcCalculator = new ArcFromBulgeCalculator(init, end, bulges[j]);
				Vector arc = arcCalculator.getPoints(1);
				if (bulges[j]<0) {
					for (int k=arc.size()-1; k>=0; k--) {
						ptspol.add(arc.get(k));
					}
					ptspol.remove(ptspol.size()-1);
				} else {
					for (int k=0;k<arc.size();k++) {
						ptspol.add(arc.get(k));
					}
					ptspol.remove(ptspol.size()-1);
				}
			}
		}
		Point2D[] points = new Point2D[ptspol.size()];
		for (int j=0;j<ptspol.size();j++) {
			points[j] = (Point2D)ptspol.get(j);
		}
		return points;
	}
}
