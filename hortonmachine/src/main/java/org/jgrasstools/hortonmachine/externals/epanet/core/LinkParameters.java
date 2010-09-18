package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum LinkParameters {
    EN_DIAMETER(0, "Diameter "), //
    EN_LENGTH(1, "Length "), //
    EN_ROUGHNESS(2, "Roughness coeff. "), //
    EN_MINORLOSS(3, "Minor loss coeff. "), //
    EN_INITSTATUS(4, "Initial link status (0 = closed, 1 = open) "), //
    EN_INITSETTING(5, "Roughness for pipes, initial speed for pumps, initial setting for valves "), //
    EN_KBULK(6, "Bulk reaction coeff. "), //
    EN_KWALL(7, "Wall reaction coeff. "), //
    EN_FLOW(8, "Flow rate "), //
    EN_VELOCITY(9, "Flow velocity "), //
    EN_HEADLOSS(10, "Head loss "), //
    EN_STATUS(11, "Actual link status (0 = closed, 1 = open) "), //
    EN_SETTING(12, "Roughness for pipes, actual speed for pumps, actual setting for valves "), //
    EN_ENERGY(13, "Energy expended in kwatts  ");

    private int code;
    private String description;
    LinkParameters( int code, String description ) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LinkParameters forCode( int i ) {
        LinkParameters[] values = values();
        for( LinkParameters type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
