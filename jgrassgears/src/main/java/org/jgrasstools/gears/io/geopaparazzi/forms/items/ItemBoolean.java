package org.jgrasstools.gears.io.geopaparazzi.forms.items;

public class ItemBoolean implements Item {

    private String description;
    private String defaultValue;
    private boolean isMandatory;

    /**
     * @param description
     * @param defaultValue a default value: <b>false</b> or <b>true</b>.
     * @param isMandatory
     */
    public ItemBoolean( String description, String defaultValue, boolean isMandatory ) {
        this.isMandatory = isMandatory;
        if (defaultValue == null) {
            defaultValue = "false";
        }
        this.description = description;
        this.defaultValue = defaultValue;
        checkBoolean(this.defaultValue);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"value\": \"").append(defaultValue).append("\",\n");
        sb.append("             \"type\": \"").append("boolean").append("\",\n");
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
        checkBoolean(value);
        defaultValue = value;
    }

    private void checkBoolean( String value ) {
        if (!value.equals("false") && !value.equals("true")) {
            throw new IllegalArgumentException("Value has to be false or true.");
        }
    }

    @Override
    public String getValue() {
        return defaultValue;
    }
}
