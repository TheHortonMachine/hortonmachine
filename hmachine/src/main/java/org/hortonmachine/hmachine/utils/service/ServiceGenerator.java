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
package org.hortonmachine.hmachine.utils.service;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.HortonMachine;

/**
 * Class that generates the service file.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ServiceGenerator {

    public static void main( String[] args ) throws IOException {
        File serviceFile = new File("./src/main/resources/META-INF/services/org.hortonmachine.gears.libs.modules.HMModel");
        if (!serviceFile.exists()) {
            throw new IOException();
        }

        HortonMachine hm = HortonMachine.getInstance();
        Set<Entry<String, Class< ? >>> cls = hm.moduleName2Class.entrySet();
        List<String > names = new ArrayList<String>();
        for( Entry<String, Class< ? >> cl : cls ) {
            String canonicalName = cl.getValue().getCanonicalName();
            names.add(canonicalName);
        }

        Collections.sort(names);
        FileUtilities.writeFile(names, serviceFile);

    }
}
