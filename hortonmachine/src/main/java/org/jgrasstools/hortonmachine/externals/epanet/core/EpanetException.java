package org.jgrasstools.hortonmachine.externals.epanet.core;

import java.io.IOException;

public class EpanetException extends IOException{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public EpanetException(String msg) {
        super(msg);
    }

    public EpanetException(Throwable th) {
        super(th);
    }
}