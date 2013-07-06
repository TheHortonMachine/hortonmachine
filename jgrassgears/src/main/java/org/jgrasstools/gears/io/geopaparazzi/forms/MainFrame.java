package org.jgrasstools.gears.io.geopaparazzi.forms;

import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.gears.io.geopaparazzi.forms.items.Item;

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
