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

import org.hortonmachine.gears.io.geopaparazzi.forms.items.Item;

/**
 * The main frame.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MainFrame {

    private List<Section> sectionsList = new ArrayList<Section>();

    public void addSection( Section section ) {
        sectionsList.add(section);
    }

    public List<Section> getSectionsList() {
        return sectionsList;
    }

    public List<Item> getItemsList() {
        List<Item> itemsList = new ArrayList<Item>();
        List<Section> sectionsList = getSectionsList();
        for( Section section : sectionsList ) {
            List<Form> formList = section.getFormList();
            for( Form form : formList ) {
                List<Item> itemsList2 = form.getItemsList();
                itemsList.addAll(itemsList2);
            }
        }
        return itemsList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPre());

        StringBuilder tmp = new StringBuilder();
        for( Section section : sectionsList ) {
            tmp.append(",\n").append(section.toString());
        }
        sb.append(tmp.substring(1));
        sb.append(getPost());
        return sb.toString();
    }

    public String getPre() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        return sb.toString();
    }

    public String getPost() {
        StringBuilder sb = new StringBuilder();
        sb.append("]\n");
        return sb.toString();
    }
}
