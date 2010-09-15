package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum NodeTypes {
    EN_JUNCTION (0, "Junction node"), //
    EN_RESERVOIR (1, "Reservoir node"), //
    EN_TANK (2, "Tank node");
    
    private int code;
    private String description;
    NodeTypes( int code, String description ) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NodeTypes forCode( int i ) {
        NodeTypes[] values = values();
        for( NodeTypes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
