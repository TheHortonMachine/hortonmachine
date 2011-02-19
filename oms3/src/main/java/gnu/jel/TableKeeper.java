/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: TableKeeper.java 490 2006-10-01 16:08:04Z metlov $
 *
 * This file is part of the Java Expressions Library (JEL).
 *   For more information about JEL visit :
 *    http://kinetic.ac.donetsk.ua/JEL/
 *
 * (c) 1998 -- 2007 by Konstantin Metlov(metlov@kinetic.ac.donetsk.ua);
 *
 * JEL is Distributed under the terms of GNU General Public License.
 *    This code comes with ABSOLUTELY NO WARRANTY.
 *  For license details see COPYING file in this directory.
 */

package gnu.jel;

import java.util.*;
import java.io.*;
import gnu.jel.debug.Debug;

public class TableKeeper {
  private static final Hashtable<String,Object> tables;
  private static final ResourceBundle msgs;

  static {
    Hashtable<String,Object> temp=new Hashtable<String,Object>();
    PropertyResourceBundle resB=null;
    
	try {
      Class c=Class.forName("gnu.jel.DVMap");
      // Read messages
      resB=
        new PropertyResourceBundle(c.getResourceAsStream("JEL.properties"));

      // Read tables
      ObjectInputStream ios=
        new ObjectInputStream(c.getResourceAsStream("tables.dat"));
      temp=(Hashtable<String,Object>)ios.readObject();

      // work around the serialization bug that classes representing the
      // primitive types can be serialized but can not be deserealized
      String[] specialTypesStr=(String[]) temp.get("specialTypes");
      String[] specialClassesAddStr=(String[]) temp.get("specialClasses");
      
      Class[] specialTypes=new Class[specialTypesStr.length];
      Class[] specialClasses=new Class[specialTypesStr.length+
                                               specialClassesAddStr.length];
      for(int i=10; i<specialTypesStr.length;i++)
        specialClasses[i]=specialTypes[i]=Class.forName(specialTypesStr[i]);

      for(int i=0;i<8;i++)
        specialClasses[i]=specialTypes[i]=(Class) 
          Class.forName(specialTypesStr[i+20]).getField("TYPE").get(null);

      //      specialTypes[8]=null, // Generic reference         //  8
      specialClasses[9]=specialTypes[9]=Void.TYPE;               //  9
      temp.put("specialTypes",specialTypes);
      
      Class[] specialClassesAdd=new Class[specialClassesAddStr.length];
      for (int i=0; i<specialClassesAddStr.length;i++)
        specialClasses[specialTypesStr.length+i]=
          Class.forName(specialClassesAddStr[i]);
      temp.put("specialClasses",specialClasses);

	} catch (Exception exc) {
      if (Debug.enabled) {
		Debug.println("Exception when reading tables:");
		Debug.reportThrowable(exc);
      };
    };
    tables=temp;
    msgs=resB;
  };

  /**
   * Used to get a reference to the named int[][] table.
   * @return reference to the table
   */
  public static Object getTable(String name) {
    return tables.get(name);
  };

  public static String getMsg(int code,Object[] params) {
    return java.text.MessageFormat.format(msgs.getString("e"+code),params);
  };

}

