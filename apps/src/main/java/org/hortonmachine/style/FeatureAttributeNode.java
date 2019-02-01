package org.hortonmachine.style;

public class FeatureAttributeNode {
    private String name;
    private String type;

    public FeatureAttributeNode(String attributeName, String type) {
        this.name = attributeName;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return name + " ( " + type + " )";
    }

    public String getFieldName() {
        return name;
    }
}
