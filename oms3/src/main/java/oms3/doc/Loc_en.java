/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.doc;

import java.util.ListResourceBundle;

/**
 * Localization for DB5 documents.
 * 
 * @author od
 */
public class Loc_en extends ListResourceBundle {

    static private final Object[][] contents = {
        {"date_format", "EEE, MMM d yyyy HH:mm:ss z"},
        {"subtitle", "Simulation, Model, and Parameter Documentation"},
        {"parameterset", "Parameter Set"},
        {"model", "Model Component"},
        {"sub", "Sub Component"},
        {"component", "Component"},
        {"keyword", "Keyword"},
        {"parameter", "Parameter"},
        {"variable", "Variable"},
        {"name", "Name"},
        {"author", "Author"},
        {"version", "Version"},
        {"source", "Source"},
        {"license", "License"},
        {"var_in", "Variables (In)"},
        {"var_out", "Variables (Out)"},
        {"bibliography", "Bibliography"},
        {"further", "Further Reading"},
    };

    @Override
    public Object[][] getContents() {
        return contents;
    }
}
