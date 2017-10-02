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
package org.hortonmachine.gears.libs.exceptions;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * Extention of the {@link IllegalAccessException}
 * 
 * <p>This adds the caller class in front of the message.</p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class ModelsIllegalargumentException extends IllegalArgumentException {
    private static final long serialVersionUID = 4509285779491321905L;

    public ModelsIllegalargumentException( String message, Object owner, IHMProgressMonitor monitor ) {
        super(owner instanceof String ? (String) owner + ": " + message : owner.getClass().getSimpleName() + ": " + message);
        if (monitor != null)
            monitor.exceptionThrown(message);
    }
    
    public ModelsIllegalargumentException( String message, Object owner ) {
        this(message, owner, null);
    }
}
