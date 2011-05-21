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

        codeBuilder.append("import oms3.annotations.Author;");
        codeBuilder.append("import oms3.annotations.Documentation;");
        codeBuilder.append("import oms3.annotations.Label;");
        codeBuilder.append("import oms3.annotations.Description;");
        codeBuilder.append("import oms3.annotations.Execute;");
        codeBuilder.append("import oms3.annotations.In;");
        codeBuilder.append("import oms3.annotations.Keywords;");
        codeBuilder.append("import oms3.annotations.License;");
        codeBuilder.append("import oms3.annotations.Name;");
        codeBuilder.append("import oms3.annotations.Out;");
        codeBuilder.append("import oms3.annotations.Status;");
        codeBuilder.append("");
        codeBuilder.append("@Description(\"\")");
        codeBuilder.append("@Author(name = \"Grass Developers Community\", contact = \"http://grass.osgeo.org\")");
        codeBuilder.append("@Keywords(\"\")");
        codeBuilder.append("@Label(\"\")");
        codeBuilder.append("@Name(\"\")");
        codeBuilder.append("@Status(Status.CERTIFIED)");
        codeBuilder.append("@License(\"General Public License Version >=2)\")");
        codeBuilder.append("public class Pitfiller extends JGTModel {");
        codeBuilder.append("}");

    }

}
