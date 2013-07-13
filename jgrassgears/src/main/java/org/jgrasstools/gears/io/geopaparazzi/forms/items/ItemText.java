package org.jgrasstools.gears.io.geopaparazzi.forms.items;

public class ItemText implements Item {

    private String description;
    private boolean isMandatory;
    private String defaultValue;
    private boolean isLabel;

    public ItemText( String description, String defaultValue, boolean isMandatory, boolean isLabel ) {
        this.isLabel = isLabel;
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
        sb.append("             \"value\": \"").append(defaultValue).append("\",\n");

        if (isLabel)
            sb.append("             \"islabel\": \"").append("true").append("\",\n");
        String type = "string";
        sb.append("             \"type\": \"").append(type).append("\",\n");
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
