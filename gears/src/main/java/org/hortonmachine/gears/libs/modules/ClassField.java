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
package org.hortonmachine.gears.libs.modules;

/**
 * Helper class for fields and annotations. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ClassField implements Comparable<ClassField> {
    public boolean isIn = false;
    public boolean isOut = false;
    public String fieldName = null;
    public String fieldDescription = null;
    public Class< ? > parentClass = null;
    public Class< ? > fieldClass = null;
    public String parentClassStatus = null;

    public int compareTo( ClassField o ) {
        return fieldName.compareTo(o.fieldName);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof ClassField) {
            ClassField oField = (ClassField) obj;
            return fieldName.equals(oField.fieldName);
        }
        return false;
    }
    
    

    @Override
    public String toString() {
        return "ClassField [fieldClass=" + fieldClass + ", fieldDescription=" + fieldDescription + ", fieldName=" + fieldName
                + ", isIn=" + isIn + ", isOut=" + isOut + ", parentClass=" + parentClass + "]";
    }
}