package org.jgrasstools.gears.io.geopaparazzi.forms.items;

public class ItemDouble implements Item {

    private String description;
    private boolean isMandatory;
    private String defaultValueStr;
    private boolean isLabel;

    public ItemDouble( String description, Double defaultValue, boolean isMandatory, boolean isLabel ) {
        this.isLabel = isLabel;
        if (defaultValue == null) {
            defaultValueStr = "";
        } else {
            this.defaultValueStr = defaultValue.toString();
        }
        this.description = description;
        this.isMandatory = isMandatory;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"value\": \"").append(defaultValueStr).append("\",\n");
        if (isLabel)
            sb.append("             \"islabel\": \"").append("true").append("\",\n");
        sb.append("             \"type\": \"").append("double").append("\",\n");
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
        defaultValueStr = value;
    }

    @Override
    public String getValue() {
        return defaultValueStr;
    }
}
