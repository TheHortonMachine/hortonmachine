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
public class Loc_de extends ListResourceBundle {

    static private final Object[][] contents = {
        {"date_format", "EEE, d.MM.yyyy HH:mm:ss z"},
        {"subtitle", "Simulation, Modell, and Parameter Dokumentation"},
        {"parameterset", "Model Parameter"},
        {"model", "Modell Komponente"},
        {"sub", "Subkomponenten"},
        {"component", "Komponente"},
        {"keyword", "Keyword"},
        {"parameter", "Parameter"},
        {"variable", "Variable"},
        {"name", "Name"},
        {"author", "Autor"},
        {"version", "Version"},
        {"source", "Quellcode"},
        {"license", "Lizenz"},
        {"var_in", "Variablen (In)"},
        {"var_out", "Variablen (Out)"},
        {"bibliography", "Bibliographie"},
        {"further", "Weiterfuehrende Dokumentation"},
    };

    @Override
    public Object[][] getContents() {
        return contents;
    }
}
