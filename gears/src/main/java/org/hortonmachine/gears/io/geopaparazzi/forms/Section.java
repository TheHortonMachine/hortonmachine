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
package org.hortonmachine.gears.io.geopaparazzi.forms;

import java.util.ArrayList;
import java.util.List;

/**
 * A form section.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Section {
    private List<Form> formList = new ArrayList<Form>();

    private String name;
    private String description;
    public Section( String name ) {
        this.name = name;
        this.description = name;
    }
    public Section( String name, String description ) {
        this.name = name;
        this.description = description;
    }

    public void addForms( Form form ) {
        formList.add(form);
    }

    public List<Form> getFormList() {
        return formList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPre());
        StringBuilder tmp = new StringBuilder();
        for( Form form : formList ) {
            tmp.append(",\n").append(form.toString());
        }
        String sub = "";
        if (tmp.length() > 0)
            sub = tmp.substring(1);
        sb.append(sub);
        sb.append(getPost());
        return sb.toString();
    }

    public String getPre() {
        StringBuilder sb = new StringBuilder();
        sb.append("    {").append("\n");
        sb.append("        \"sectionname\": \"").append(name).append("\",\n");
        sb.append("        \"sectiondescription\": \"").append(description).append("\",\n");
        sb.append("        \"forms\": [").append("\n");
        return sb.toString();
    }

    public String getPost() {
        StringBuilder sb = new StringBuilder();
        sb.append("         ]\n");
        sb.append("    }\n");
        return sb.toString();
    }
}
