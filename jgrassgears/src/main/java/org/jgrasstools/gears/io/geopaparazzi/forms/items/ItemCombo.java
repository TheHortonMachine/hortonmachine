package org.jgrasstools.gears.io.geopaparazzi.forms.items;

public class ItemCombo implements Item{

    private String description;
    private boolean isMandatory;
    private String defaultValue;
    private String[] items;

    public ItemCombo( String description, String[] items, String defaultValue, boolean isMandatory ) {
        this.items = items;
        if (defaultValue == null) {
            defaultValue = "";
        }
        this.description = description;
        this.defaultValue = defaultValue;
        this.isMandatory = isMandatory;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"values\": {\n");
        sb.append("                 \"items\": [\n");
        sb.append("                     {\"item\": \"\"},\n");
        StringBuilder tmp = new StringBuilder();
        for( String itemString : items ) {
            tmp.append("                     {\"item\": \"" + itemString + "\"},\n");
        }
        String tmpStr = tmp.toString();
        int lastIndexOf = tmpStr.lastIndexOf(',');
        String substring = tmp.substring(0, lastIndexOf);
        sb.append(substring);
        sb.append("                 ]\n");
        sb.append("             },\n");
        sb.append("             \"value\": \"").append(defaultValue).append("\",\n");
        sb.append("             \"type\": \"").append("stringcombo").append("\",\n");
        sb.append("             \"mandatory\": \"").append(isMandatory ? "yes" : "no").append("\"\n");
        sb.append("        }\n");
        return sb.toString();
    }
    
    @Override
    public String getKey() {
        return description;
    }

    @Override
    public void setValue( String value ) {
        defaultValue = value;
    }
    
    @Override
    public String getValue() {
        return defaultValue;
    }
}
