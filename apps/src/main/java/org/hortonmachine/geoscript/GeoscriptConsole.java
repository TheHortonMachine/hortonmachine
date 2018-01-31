package org.hortonmachine.geoscript;

import groovy.ui.Console;

public class GeoscriptConsole {
    
    public GeoscriptConsole() {
        Console console = new Console();
        console.run();
    }
    
    
    public static void main( String[] args ) {
        new GeoscriptConsole();
    }

}
