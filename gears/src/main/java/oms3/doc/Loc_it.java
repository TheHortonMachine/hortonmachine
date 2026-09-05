/*
 * $Id: Loc_it.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
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
