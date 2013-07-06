package org.jgrasstools.gears.io.geopaparazzi.forms;

import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.gears.io.geopaparazzi.forms.items.Item;

public class Form {

    private List<Item> itemsList = new ArrayList<Item>();

    private String name;
    public Form( String name ) {
        this.name = name;
    }

    public void addItem( Item item ) {
        itemsList.add(item);
    }
    
    public List<Item> getItemsList() {
        return itemsList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPre());
        StringBuilder tmp = new StringBuilder();
        for( Item item : itemsList ) {
            tmp.append(",\n").append(item.toString());
        }
        sb.append(tmp.substring(1));
        sb.append(getPost());
        return sb.toString();
    }

    public String getPre() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {").append("\n");
        sb.append("            \"formname\": \"").append(name).append("\",\n");
        sb.append("            \"formitems\": [").append("\n");
        return sb.toString();
    }

    public String getPost() {
        StringBuilder sb = new StringBuilder();
        sb.append("             ]\n");
        sb.append("        }\n");
        return sb.toString();
    }
}
