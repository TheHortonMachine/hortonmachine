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
package org.hortonmachine.gears.utils.math.interpolation.splines;

/**
 * This is adapted from: http://www.cse.unsw.edu.au/~lambert/splines/CatmullRom.html
 * 
 * @author Tim Lambert (http://www.cse.unsw.edu.au/~lambert/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CatmullRom extends Bspline {

  // Catmull-Rom spline is just like a B spline, only with a different basis
  double b(int i, double t) {
    switch (i) {
    case -2:
      return ((-t+2)*t-1)*t/2;
    case -1:
      return (((3*t-5)*t)*t+2)/2;
    case 0:
      return ((-3*t+4)*t+1)*t/2;
    case 1:
      return ((t-1)*t*t)/2;
    }
    return 0; //we only get here if an invalid i is specified
  }
  
}
