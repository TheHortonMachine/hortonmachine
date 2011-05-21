/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.grass.utils;

import org.jgrasstools.grass.dtd64.Task;

/**
 * OMS3 Code generation class. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Oms3CodeGenerator {

    private StringBuilder codeBuilder = new StringBuilder();

    public Oms3CodeGenerator( Task grassTask ) {

        String name = grassTask.getName().trim();
        String classSafeName = name.replaceAll("\\.", "_");
        String description = grassTask.getDescription();
        String keyWords = grassTask.getKeywords();
        String category = GrassUtils.name2GrassCategory(name);

        codeBuilder.append("import org.jgrasstools.gears.libs.modules.JGTModel;").append("\n");
        codeBuilder.append("").append("\n");
        codeBuilder.append("import oms3.annotations.Author;").append("\n");
        codeBuilder.append("import oms3.annotations.Documentation;").append("\n");
        codeBuilder.append("import oms3.annotations.Label;").append("\n");
        codeBuilder.append("import oms3.annotations.Description;").append("\n");
        codeBuilder.append("import oms3.annotations.Execute;").append("\n");
        codeBuilder.append("import oms3.annotations.In;").append("\n");
        codeBuilder.append("import oms3.annotations.Keywords;").append("\n");
        codeBuilder.append("import oms3.annotations.License;").append("\n");
        codeBuilder.append("import oms3.annotations.Name;").append("\n");
        codeBuilder.append("import oms3.annotations.Out;").append("\n");
        codeBuilder.append("import oms3.annotations.Status;").append("\n");
        codeBuilder.append("").append("\n");
        if (description != null)
            codeBuilder.append("@Description(\"" + description.trim() + "\")").append("\n");
        codeBuilder.append("@Author(name = \"Grass Developers Community\", contact = \"http://grass.osgeo.org\")").append("\n");
        if (keyWords != null)
            codeBuilder.append("@Keywords(\"" + keyWords.trim() + "\")").append("\n");
        if (category != null)
            codeBuilder.append("@Label(\"" + category + "\")").append("\n");
        codeBuilder.append("@Name(\"" + name + "\")").append("\n");
        codeBuilder.append("@Status(Status.CERTIFIED)").append("\n");
        codeBuilder.append("@License(\"General Public License Version >=2)\")").append("\n");
        codeBuilder.append("public class " + classSafeName + " extends JGTModel {").append("\n");
        codeBuilder.append("}").append("\n");

    }

    public String getOms3Class() {
        return codeBuilder.toString();
    }

}
