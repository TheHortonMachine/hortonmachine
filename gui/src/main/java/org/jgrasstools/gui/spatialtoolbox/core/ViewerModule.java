/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package org.jgrasstools.gui.spatialtoolbox.core;

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
