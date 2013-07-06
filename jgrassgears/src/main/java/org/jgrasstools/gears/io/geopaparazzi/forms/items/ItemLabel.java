package org.jgrasstools.gears.io.geopaparazzi.forms.items;

public class ItemLabel implements Item{

    private String description;
    private int size;
    private boolean doLine;

    public ItemLabel( String description, int size, boolean doLine ) {
        this.size = size;
        this.doLine = doLine;
        this.description = description;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        // sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"value\": \"").append(description).append("\",\n");
        sb.append("             \"size\": \"").append(size).append("\",\n");
        sb.append("             \"type\": \"").append(doLine ? "labelwithline" : "label").append("\"\n");
        sb.append("        }\n");
        return sb.toString();
    }
    
    @Override
    public String getKey() {
        return null;
    }

    @Override
    public void setValue( String value ) {
    }
    
    @Override
    public String getValue() {
        return null;
    }
}
