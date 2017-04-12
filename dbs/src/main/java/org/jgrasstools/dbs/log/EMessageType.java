package org.jgrasstools.dbs.log;

public enum EMessageType {
    ALL(0), INFO(1), WARNING(2), ERROR(3), ACCESS(4), DEBUG(5);

    private int code;

    private EMessageType( int code ) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EMessageType fromCode( int code ) {
        switch( code ) {
        case 0:
            return ALL;
        case 1:
            return INFO;
        case 2:
            return WARNING;
        case 3:
            return ERROR;
        case 4:
            return ACCESS;
        case 5:
            return DEBUG;
        default:
            return ALL;
        }
    }

}
