package org.hortonmachine.geoscript;

import groovy.ui.Console;

public class GeoscriptConsole {
    
    public GeoscriptConsole() {
        Console console = new Console();
//        console.setVariable("var1", getValueOfVar1());
//        console.setVariable("var2", getValueOfVar2());
        console.run();
        
        
    }
    
    
    public static void main( String[] args ) {
        new GeoscriptConsole();
    }

}
