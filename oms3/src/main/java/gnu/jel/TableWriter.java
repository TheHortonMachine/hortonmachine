/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: TableKeeper.java,v 1.13 2004/03/16 15:20:52 metlov Exp $
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

public class TableWriter {
  private static final Hashtable<String,Object> tables = 
    new Hashtable<String,Object>();
  private static final ResourceBundle msgs = null;

  public static void main(String[] args) {
    if (Debug.enabled) {
      try {      
        // Tables from gnu.jel.TypesStack
        String[] specialTypesStr={
          null,    // Boolean.TYPE,                     //  0 0x00
          null,    // Byte.TYPE,                        //  1 0x01
          null,    // Character.TYPE,                   //  2 0x02
          null,    // Short.TYPE,                       //  3 0x03
          null,    // Integer.TYPE,                     //  4 0x04
          null,    // Long.TYPE,                        //  5 0x05
          null,    // Float.TYPE,                       //  6 0x06
          null,    // Double.TYPE,                      //  7 0x07
          null,    // Generic reference                 //  8 0x08
          null,    // Void.TYPE,                        //  9 0x09
          "java.lang.String",                  //[TSB]  // 10 0x0A
          "java.lang.String",                           // 11 0x0B
          "gnu.jel.reflect.Boolean",                    // 12 0x0C
          "gnu.jel.reflect.Byte",                       // 13 0x0D
          "gnu.jel.reflect.Character",                  // 14 0x0E
          "gnu.jel.reflect.Short",                      // 15 0x0F
          "gnu.jel.reflect.Integer",                    // 16 0x10
          "gnu.jel.reflect.Long",                       // 17 0x11
          "gnu.jel.reflect.Float",                      // 18 0x12
          "gnu.jel.reflect.Double",                     // 19 0x13
          "java.lang.Boolean",                          // 20 0x14
          "java.lang.Byte",                             // 21 0x15
          "java.lang.Character",                        // 22 0x16
          "java.lang.Short",                            // 23 0x17
          "java.lang.Integer",                          // 24 0x18
          "java.lang.Long",                             // 25 0x19
          "java.lang.Float",                            // 26 0x1A
          "java.lang.Double",                           // 27 0x1B
          "gnu.jel.reflect.String"                      // 29 0x1C
        };
        tables.put("specialTypes",specialTypesStr);

        Class[] specialTypes=new Class[specialTypesStr.length];
        for (int i=10; i<specialTypesStr.length;i++)
          specialTypes[i]=Class.forName(specialTypesStr[i]);

        specialTypes[0]=Boolean.TYPE;                      //  0
        specialTypes[1]=Byte.TYPE;                         //  1
        specialTypes[2]=Character.TYPE;                    //  2
        specialTypes[3]=Short.TYPE;                        //  3
        specialTypes[4]=Integer.TYPE;                      //  4
        specialTypes[5]=Long.TYPE;                         //  5
        specialTypes[6]=Float.TYPE;                        //  6
        specialTypes[7]=Double.TYPE;                       //  7
        //      specialTypes[8]=null, // Generic reference         //  8
        specialTypes[9]=Void.TYPE;                         //  9

        byte[] unwrapType={
          0,1,2,3,4,5,6,7,8,9,10,11,
          0,1,2,3,4,5,6,7,
          0,1,2,3,4,5,6,7,11
        };
        tables.put("unwrapType",unwrapType);
        
        // Possible widening conversions
        //  ZBCS IJFD LVTS
        //Z 1000 0000 0000
        //B 0100 0000 0000
        //C 0110 0000 0000
        //S 0101 0000 0000
        //I 0111 1000 0000
        //J 0111 1100 0000
        //F 0101 1110 0000
        //D 0101 1111 0000
        //L 0000 0000 0000
        //V 0000 0000 0000
        //T 0000 0000 0000
        //S 0000 0000 0011
        int[] cvt_wide= {
          0x800,0x400,0x600,0x500,0x780,0x7C0,0x5E0,0x5F0,0x000,
          0x000,0x000,0x003
        };
        tables.put("cvt_wide",cvt_wide);

        // Tables for gnu.jel.ClassFile  (these are appended to specialTypes)
        String[] specialClassesAddStr={
          "java.lang.StringBuffer",          // 29 0x1D // former 0x1C
          "java.lang.Object",                // 30 0x1E // former 0x1D
          "gnu.jel.CompiledExpression",      // 31 0x1F 
        };
        tables.put("specialClasses",specialClassesAddStr);

        Class[] specialClasses=new Class[specialTypes.length+
                                        specialClassesAddStr.length];
        System.arraycopy(specialTypes,0,specialClasses,0,
                         specialTypes.length);
        for(int i=specialTypes.length;i<specialClasses.length;i++)
          specialClasses[i]=Class.forName(specialClassesAddStr[i-specialTypes.length]);

        //        System.arraycopy(specialClassesAdd,0,specialClasses,
        //                         specialTypes.length,
        //                         specialClassesAdd.length);

//          for(int j=0;j<specialClasses.length;j++)
//            Debug.println("specialClasses["+j+"]="+specialClasses[j]);


        // Each descriptor is an array
        // first element is a number of special class containing the method
        // with the following modifications
        // +100 - constructor
        // +200 - field
        // then goes the index of the method name,
        // other elements are numbers of special classes for parameters types
  
        // in no particular order
        char[][] specialMds={
          {30,0},              // Object.toString()                    |  0
          {29+100,0},          // StringBuffer()                       |  1
          {29,1,0},            // StringBuffer.append(boolean)         |  2
          {29,1,2},            // StringBuffer.append(char)            |  3
          {29,1,4},            // StringBuffer.append(int)             |  4
          {29,1,5},            // StringBuffer.append(long)            |  5
          {29,1,6},            // StringBuffer.append(float)           |  6
          {29,1,7},            // StringBuffer.append(double)          |  7
          {29,1,11},           // StringBuffer.append(String)          |  8
          {29,1,30},           // StringBuffer.append(Object)          |  9
          {29,0},              // StringBuffer.toString()              | 10
          {12,2},              // gnu.jel.reflect.Boolean.getValue()   | 11
          {13,2},              // gnu.jel.reflect.Byte.getValue()      | 12
          {14,2},              // gnu.jel.reflect.Character.getValue() | 13
          {15,2},              // gnu.jel.reflect.Short.getValue()     | 14
          {16,2},              // gnu.jel.reflect.Integer.getValue()   | 15
          {17,2},              // gnu.jel.reflect.Long.getValue()      | 16
          {18,2},              // gnu.jel.reflect.Float.getValue()     | 17
          {19,2},              // gnu.jel.reflect.Double.getValue()    | 18
          {20,3},              // java.lang.Boolean.booleanValue()     | 19
          {21,4},              // java.lang.Byte.byteValue()           | 20
          {22,5},              // java.lang.Character.characterValue() | 21
          {23,6},              // java.lang.Short.shortValue()         | 22
          {24,7},              // java.lang.Integer.intValue()         | 23
          {25,8},              // java.lang.Long.longValue()           | 24
          {26,9},              // java.lang.Float.floatValue()         | 25
          {27,10},             // java.lang.Double.doubleValue()       | 26
          {11,11,30},          // String.valueOf(Object)               | 27 // unwraps String-like
          {31,12,11,11},       // CompiledExpression.compare(String,String) | 28 // compares two strings
        };
        tables.put("specialMds",specialMds);

        String[] specialMdsN = {
          "toString",          //                               |  0 0x00
          "append",            //                               |  1 0x01
          "getValue",          //                               |  2 0x02
          "booleanValue",      //                               |  3 0x03
          "byteValue",         //                               |  4 0x04
          "charValue",         //                               |  5 0x05
          "shortValue",        //                               |  6 0x06
          "intValue",          //                               |  7 0x07
          "longValue",         //                               |  8 0x08
          "floatValue",        //                               |  9 0x09
          "doubleValue",       //                               | 10 0x0A
          "valueOf",           //                               | 11 0x0B
          "compare",           //                               | 12 0xC
        };
        tables.put("specialMdsN",specialMdsN);

        // OPunary tables
        int[][] una ={
          // Z      B      C      S      I        J      F      D     L   VOID     TSB    STR   OP/TO
          {  0xFF,  0x74,  0xFF,  0x74,  0x74,    0x75,  0x76,  0x77,  0xFF,0xFF,    0xFF,  0xFF},//NE  | 0
          {  0xFF,0x8202,0x8202,0x8202,0x8202,0x838502,  0xFF,  0xFF,  0xFF,0xFF,    0xFF,  0xFF},//NO  | 1
          {  0xFC,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,  0xFF,  0xFF,  0xFF,0xFF,    0xFF,  0xFF},//LNO | 2
          {0xACFA,  0xAC,  0xAC,  0xAC,  0xAC,    0xAD,  0xAE,  0xAF,  0xB0,0xB1,0xB00AFE,  0xB0},//RET | 3
          {     0,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,  0xFF,  0xFF,  0xFF,0xFF,    0xFF,  0xFF},// Z  | 4
          {  0xFF,     0,  0x91,  0x91,  0x91,  0x9188,0x918B,0x918E,  0XFF,0xFF,    0xFF,  0xFF},// B  | 5
          {  0xFF,  0x92,     0,  0x92,  0x92,  0x9288,0x928B,0x928E,  0xFF,0xFF,    0xFF,  0xFF},// C  | 6
          {  0xFF,  0x93,  0x93,     0,  0x93,  0x9388,0x938B,0x938E,  0xFF,0xFF,    0xFF,  0xFF},// S  | 7
          {  0xFF,  0x00,  0x00,  0x00,     0,    0x88,  0x8B,  0x8E,  0xFF,0xFF,    0xFF,  0xFF},// I  | 8
          {  0xFF,  0x85,  0x85,  0x85,  0x85,       0,  0x8C,  0x8F,  0xFF,0xFF,    0xFF,  0xFF},// J  | 9
          {  0xFF,  0x86,  0x86,  0x86,  0x86,    0x89,     0,  0x90,  0xFF,0xFF,    0xFF,  0xFF},// F  |10
          {  0xFF,  0x87,  0x87,  0x87,  0x87,    0x8A,  0x8D,     0,  0XFF,0xFF,    0xFF,  0xFF},// D  |11
          {  0xFF,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,  0xFF,  0xFF,  0xc0,0xFF,    0xFF,  0xc0},// L  |12
          {0x57FA,  0x57,  0x57,  0x57,  0x57,    0x58,  0x57,  0x58,  0x57,   0,    0x57,  0x57},//VOID|13
          {  0xFF,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,  0xFF,  0xFF,0x09FE,0xFF,       0,0x08FE},// TSB|14
          {  0xFF,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,  0xFF,  0xFF,0x1BFE,0xFF,  0x0AFE,     0},// STR|15
        };
        tables.put("una",una);

        String[] opNames={"negation","bitwise inversion",
                          "logical inversion","return",
                          "cast to boolean",
                          "cast to byte","cast to char",
                          "cast to short","cast to int",
                          "cast to long","cast to float",
                          "cast to double","cast to reference","pop",
                          "make TSB","TSB to string"};
        tables.put("opNames",opNames);

        //                      Z   B   C   S   I   J   F   D   L
        byte[] unary_prmtns=  {0 ,  4,  4,  4,  4,  5,  6,  7,  8};
        tables.put("unary_prmtns",unary_prmtns);

        // OPbinary tables
        byte[][] promotions={ // binary promotions of primitive types
          //Z  B  C  S  I  J  F  D REF VOID TSB STR
          { 0,-1,-1,-1,-1,-1,-1,-1, -1, -1, -1, -1}, // Z
          {-1, 4, 4, 4, 4, 5, 6, 7, -1, -1, -1, -1}, // B
          {-1, 4, 4, 4, 4, 5, 6, 7, -1, -1, -1, -1}, // C
          {-1, 4, 4, 4, 4, 5, 6, 7, -1, -1, -1, -1}, // S
          {-1, 4, 4, 4, 4, 5, 6, 7, -1, -1, -1, -1}, // I
          {-1, 5, 5, 5, 5, 5, 6, 7, -1, -1, -1, -1}, // J
          {-1, 6, 6, 6, 6, 6, 6, 7, -1, -1, -1, -1}, // F
          {-1, 7, 7, 7, 7, 7, 7, 7, -1, -1, -1, -1}, // D
          {-1,-1,-1,-1,-1,-1,-1,-1,  8, -1,  8,  8}, // REF
          {-1,-1,-1,-1,-1,-1,-1,-1, -1, -1, -1, -1}, // VOID
          {-1,-1,-1,-1,-1,-1,-1,-1,  8, -1, 11, 11}, // TSB
          {-1,-1,-1,-1,-1,-1,-1,-1,  8, -1, 11, 11}, // STR
        };
        tables.put("promotions",promotions);


        int[][] ops={ // binary opcodes by OP and the promoted argument type
          // First opcode (255 if op is not allowed)
          //    Z      B      C      S      I        J        F        D     REF  VOID TSB      STR
          {  0xFF,  0x60,  0xFF,  0x60,  0x60,    0x61,    0x62,    0x63,    0xFF,0xFF,0xFF,      0xFF},  // PL  | 0
          {  0xFF,  0x64,  0xFF,  0x64,  0x64,    0x65,    0x66,    0x67,    0xFF,0xFF,0xFF,      0xFF},  // MI  | 1
          {  0xFF,  0x68,  0xFF,  0x68,  0x68,    0x69,    0x6A,    0x6B,    0xFF,0xFF,0xFF,      0xFF},  // MU  | 2
          {  0xFF,  0x6C,  0xFF,  0x6C,  0x6C,    0x6D,    0x6E,    0x6F,    0xFF,0xFF,0xFF,      0xFF},  // DI  | 3
          {  0xFF,  0x70,  0xFF,  0x70,  0x70,    0x71,    0x72,    0x73,    0xFF,0xFF,0xFF,      0xFF},  // RE  | 4
          {  0x7E,  0x7E,  0xFF,  0x7E,  0x7E,    0x7F,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // AN  | 5
          {  0x80,  0x80,  0xFF,  0x80,  0x80,    0x81,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // OR  | 6
          {  0x82,  0x82,  0xFF,  0x82,  0x82,    0x83,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // XO  | 7
          {0x9FF3,0x9FF3,0x9FF3,0x9FF3,0x9FF3,0x99F394,0x99F396,0x99F398,  0xA5F3,0xFF,0xFF,0x99F31CFE},  // EQ  | 8
          {0xA0F3,0xA0F3,0xA0F3,0xA0F3,0xA0F3,0x9AF394,0x9AF396,0x9AF398,  0xA6F3,0xFF,0xFF,0x9AF31CFE},  // NE  | 9
          {  0xFF,0xA1F3,0xA1F3,0xA1F3,0xA1F3,0x9BF394,0x9BF396,0x9BF398,    0xFF,0xFF,0xFF,0x9BF31CFE},  // LT  |10
          {  0xFF,0xA2F3,0xA2F3,0xA2F3,0xA2F3,0x9CF394,0x9CF395,0x9CF397,    0xFF,0xFF,0xFF,0x9CF31CFE},  // GE  |11
          {  0xFF,0xA3F3,0xA3F3,0xA3F3,0xA3F3,0x9DF394,0x9DF395,0x9DF397,    0xFF,0xFF,0xFF,0x9DF31CFE},  // GT  |12
          {  0xFF,0xA4F3,0xA4F3,0xA4F3,0xA4F3,0x9EF394,0x9EF396,0x9EF398,    0xFF,0xFF,0xFF,0x9EF31CFE},  // LE  |13
          {  0xFF,  0x78,  0x78,  0x78,  0x78,    0x79,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // LS  |14
          {  0xFF,  0x7A,  0x7A,  0x7A,  0x7A,    0x7B,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // RSS |15
          {  0xFF,  0x7C,  0x7C,  0x7C,  0x7C,    0x7D,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // RUS |16
          {     0,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // LAN |17
          {     0,  0xFF,  0xFF,  0xFF,  0xFF,    0xFF,    0xFF,    0xFF,    0xFF,0xFF,0xFF,      0xFF},  // LOR |18
          {  0x33,  0x33,  0x34,  0x35,  0x2E,    0x2F,    0x30,    0x31,    0x32,0xFF,0xFF,      0xFF},  // ARR |19
          {0x02FE,0x04FE,0x03FE,0x04FE,0x04FE,  0x05FE,  0x06FE,  0x07FE,0x09FE,0xFF,0x08FE0AFE,0x08FE},  // SCAT|20
        };
        tables.put("ops",ops);

        String[] binOpNames={"add","substract","multiply",
                             "divide","remainder",
                             "bitwise and","bitwise or",
                             "bitwise xor","equal","not equal",
                             "less","greater or equal",
                             "greater","less or equal",
                             "left shift", "signed right shift",
                             "unsigned right shift",
                             "logical and","logical or",
                             "array element access","string concatenation"};
        tables.put("binOpNames",binOpNames);

        // operand promotion types by operation
        // 1 - binary promotion, 0 - binary promotion but the result is boolean,
        // 2 - unary promotion of first, 3 - array promotion, 4 - string cat promotion
        //                     PL MI MU DI RE AN OR XO EQ NE LT GE GT LE LS RSS RUS LAN LOR ARR SCAT
        byte[] promotionTypes={ 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2,  2,  2,  0,  0,  3,   4};
        tables.put("promotionTypes",promotionTypes);
        

        //  --------- WRITING CODE
        ObjectOutputStream oos=
          new ObjectOutputStream(new FileOutputStream("gnu/jel/tables.dat"));
        
        oos.writeObject(tables);
      } catch(Exception exc) {
		Debug.println("Exception when writing tables:");
		Debug.reportThrowable(exc);
      };
    };
  };
}

