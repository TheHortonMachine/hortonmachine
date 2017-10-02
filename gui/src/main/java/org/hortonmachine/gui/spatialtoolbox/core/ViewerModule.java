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
package org.hortonmachine.gui.spatialtoolbox.core;

/**
 * A modules wrapper.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ViewerModule {
    private final ModuleDescription moduleDescription;

    private ViewerFolder parentFolder;

    public ViewerModule( ModuleDescription moduleDescription ) {
        this.moduleDescription = moduleDescription;
    }

    @Override
    public String toString() {
        String name = moduleDescription.getName();
        if (name.startsWith("Oms")) {
            name = name.substring(3);
        }
        return name;
    }

    public ModuleDescription getModuleDescription() {
        return moduleDescription;
    }

    public void setParentFolder( ViewerFolder parentFolder ) {
        this.parentFolder = parentFolder;
    }

    public ViewerFolder getParentFolder() {
        return parentFolder;
    }
}
