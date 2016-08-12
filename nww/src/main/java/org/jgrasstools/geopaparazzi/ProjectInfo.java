/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package org.jgrasstools.geopaparazzi;

import java.io.File;
import java.util.List;

import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gears.utils.files.FileUtilities;

/**
 * Project information.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ProjectInfo {
    public File databaseFile;
    public String fileName;
    public String metadata;

    public Image[] images;
    public List<GpsLog> logs;
    
    @Override
    public String toString() {
        return FileUtilities.getNameWithoutExtention(databaseFile);
    }
    
}
