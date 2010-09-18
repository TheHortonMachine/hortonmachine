package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum Headloss {
    H_W("H-W", "The Hazen-Williams formula"), //
    D_W("D-W", "The Darcy-Weissbach formula"), //
    C_M("C-M", "The Chezy-Manning formula");

    private String name;
    private String description;
    Headloss( String name, String description ) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
