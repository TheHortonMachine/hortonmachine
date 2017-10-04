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
 */package org.hortonmachine.gears.libs.modules;

import java.io.File;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

/**
 * A custom file filter base.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMFileFilter extends FileFilter {

    private String description;
    private String[] allowedExt;

    public HMFileFilter( String description, String[] allowedExt ) {
        this.description = description;
        this.allowedExt = allowedExt;
    }

    public String[] getAllowedExtensions() {
        return allowedExt;
    }

    @Override
    public String getDescription() {
        return description + " " + Arrays.toString(allowedExt);
    }

    @Override
    public boolean accept( File f ) {
        if (f.isDirectory()) {
            return true;
        }
        String name = f.getName();
        for( String ext : allowedExt ) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

}
