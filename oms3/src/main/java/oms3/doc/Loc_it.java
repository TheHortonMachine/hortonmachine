/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.doc;

import java.util.ListResourceBundle;

/**
 *
 * @author od, andrea antonello.
 */
public class Loc_it extends ListResourceBundle {

   static private final Object[][] contents = {
      {"date_format", "EEE, MMM d yyyy HH:mm:ss z"},
      {"subtitle", "Documentazione di Simulazioni, Modelli e Parametri"},
      {"parameterset", "Set dei Parametri"},
      {"model", "Modello"},
      {"sub", "Sottocomponente"},
      {"component", "Componente"},
      {"keyword", "Parola chiave"},
      {"parameter", "Parametro"},
      {"variable", "Variabile"},
      {"name", "Nome"},
      {"author", "Autore"},
      {"version", "Versione"},
      {"source", "Codice Sorgente"},
      {"license", "Licenza"},
      {"var_in", "Variabili (in Ingresso)"},
      {"var_out", "Variabili (in Uscita)"},
      {"bibliography", "Bibliografia"},
      {"further", "Letture Ulteriori"},
   };

   @Override
   public Object[][] getContents() {
       return contents;
   }

}
