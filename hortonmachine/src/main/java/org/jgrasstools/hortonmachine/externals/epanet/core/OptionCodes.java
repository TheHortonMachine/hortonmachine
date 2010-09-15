package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum OptionCodes {
    EN_TRIALS(0), //
    EN_ACCURACY(1), //
    EN_TOLERANCE(2), //
    EN_EMITEXPON(3), //
    EN_DEMANDMULT(4);

    private int code;
    OptionCodes( int code ) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static OptionCodes forCode( int i ) {
        OptionCodes[] values = values();
        for( OptionCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
