/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
/*
 * $Id: Parser.java 490 2006-10-01 16:08:04Z metlov $
 *
 * This file is part of the Java Expressions Library (JEL).
 *   For more information about JEL visit :
 *    http://kinetic.ac.donetsk.ua/JEL/
 *
 * (c) 1998 -- 2007 by Konstantin L. Metlov
 *
 * JEL is Distributed under the terms of GNU General Public License.
 *    This code comes with ABSOLUTELY NO WARRANTY.
 *  For license details see COPYING file in this directory.
 */

package gnu.jel;

import gnu.jel.debug.*;
import java.util.Stack;
import java.lang.reflect.Member;

public class Parser {

  //********************
  //*** PARSER variables
  Stack<OP> paramOPs;
  Stack<OP> xchgOP; // temporary paramops stack
  int err_col=-1;
  //  int err_line=-1; // not yet

  Library lib;
  
  StringBuffer accDV=null; // null means DVs are disabled
  StringBuffer typeAccum=new StringBuffer(); // accumulates class name for cast

  //*** PARSER variables
  //********************

  //***********************
  //*** TOKENIZER variables

  private String in;       // input string
  private int pos=0;

  protected int c;           // current character

  // the next four are used for counting the columns and lines
  private boolean prevCR = false; // true if the last read char was CR
  private boolean prevLF = false; // true if the last read char was LF
  private int column=0;
  private int line=1;

  /**
   * Column, where the current token started
   */
  public int ct_column;

  /**
   * Line, where the current token started
   */
  public int ct_line;
  
  /**
   * value of the current <LITERAL> token wrapped into a reflection object
   */
  public Object val;
  
  /**
   * type of the currect token
   */
  public int type;
  
  // -1   "<EOF>"
  //  0   "+"
  //  1   "-"
  //  2   "*"
  //  3   "/"
  //  4   "%"
  //  5   "&"
  //  6   "|"
  //  7   "^"
  //  8   "=="
  //  9   "!="
  //  10  "<"
  //  11  ">="
  //  12  ">"
  //  13  "<="
  //  14  "<<"
  //  15  ">>"
  //  16  ">>>"
  //  17  "&&"
  //  18  "||"
  //  19  "["
  //  20  "]"

  //  30  "~"
  //  31  "!"

  //  35  "?"
  //  36  ":"

  //  40  "."
  //  41  "("
  //  42  ")"
  //  43  ","

  //  50  "<ID>"
  //  60  "<LITERAL>

  /**
   * accumulator, used in processing of <LITERAL>s, <ID>s or keywords
   */
  private StringBuffer buf=new StringBuffer();

  //*** TOKENIZER variables
  //***********************

  /**
   * Initializes the parser.
   * @param in expression to parse
   * @param lib library to resolve functions in
   */
  public Parser(String in, Library lib) {
    // init TOKENIZER
    this.in = in;
    read();

    // init PARSER
    this.lib=lib;
    paramOPs=new Stack<OP>();
    xchgOP=new Stack<OP>();
    
    // initialize DV names accumulator
    if (lib.resolver!=null)
      accDV=new StringBuffer();
  };

  //***********************
  //*** TOKENIZER methods
  
  /**
   * Reads character from underlying Reader.
   * <P>Additionally it counts line and column numbers. The read character
   * is automatically set as a current char of this Tokenizer.
   *
   * @return next character from the selected input.
   */
  protected int read() {
    try {
      c=in.charAt(pos++);
    } catch (Exception e) {
      c=-1;
    };
    
    column++;
    
    if (prevLF)
      {
        prevLF = false;
        line += (column = 1);
      }
    else if (prevCR)
      {
        prevCR = false;
        if (c == '\n')
          {
            prevLF = true;
          }
        else
          line += (column = 1);
      }
    
    switch (c) {
    case '\r':
      prevCR = true;
      break;
    case '\n':
      prevLF = true;
      break;
    case '\t':
      column--;
      column += (8 - (column & 07));
      break;
    default:
      break;
    };
    return c;
  };

  // because EOF can never be expected, "consume(-1)" has a special
  // meaning, which is just to report the "current unexpected" char.
  protected void consume(int cc) throws CompilationException {
    if ((cc<0) || (c!=cc)) {
      CompilationException exc=
        new CompilationException(c==-1?1:3,new Character((char)c));
      exc.col=column;
      if (c==-1) exc.col--;
      throw exc;
    };
    read();
  };

  public void nextToken() throws CompilationException {
    // store the column and line of the current token
    ct_column=column;
    ct_line=line;
    int cc;

    while (true) {
      switch (c) {
      case -1: // EOF
        type=-1; return;
      case ' ':
      case '\r':
      case '\n':
        read(); // just ignored
        ct_column=column; // adjust start token
        ct_line=line;     // adjust start token
        break;
      case '+':
        read(); type=0; return;
      case '-':
        read(); type=1; return;
      case '*':
        read(); type=2; return;
      case '/':
        read(); type=3; return;
      case '%':
        read(); type=4; return;
      case '&':
        switch (read()) {
        case '&':
          read(); type=17; return;
        default:
          type=5; return;
        }
      case '|':
        switch (read()) {
        case '|':
          read(); type=18; return;
        default:
          type=6; return;
        }
      case '^':
        read(); type=7; return;
      case '=':
        read(); consume('='); type=8; return;
      case '!':
        switch(read()) {
        case '=':
          read(); type=9; return;
        default:
          type=31; return;
        }
      case '<':
        switch (read()) {
        case '=':
          read(); type=13; return;
        case '<':
          read(); type=14; return;
        default:
          type=10; return;
        }
      case '>':
        switch (read()) {
        case '=':
          read(); type=11; return;
        case '>':
          switch(read()) {
          case '>':
            read(); type=16; return;
          default:
            type=15; return;
          }
        default:
          type=12; return;
        }
      case '~':
        read(); type = 30; return;
      case '[':
        read(); type = 19; return;
      case ']':
        read(); type = 20; return;
      case '?':
        read(); type = 35; return;
      case ':':
        read(); type = 36; return;
      case '.':
        cc=read();
        if ((cc>='0') && (cc<='9')) {
          // case '0': case '1': case '2': case '3': case '4':
          // case '5': case '6': case '7': case '8': case '9':
          parseReal();
        } else {
          type=40;
        };
        return;  
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        parseNumber();
        return;
      case '(':
        read(); type=41; return;
      case ')':
        read(); type=42; return;
      case ',':
        read(); type=43; return;
      case '\"':
        read();
        parseString();
        return;
      case '\'':
        parseChar();
        return;
      default:
        if (Character.isJavaIdentifierStart((char)c)) {
          parseID();
          return;
        } else consume(-1);
      }
    }
  };

  // current char must be starting slash
  private int parseEscape() throws CompilationException {
    switch (read()) {
    case 'r':
      read(); return '\r';
    case 'n':
      read(); return '\n';
    case 'f':
      read(); return '\f';
    case 'b':
      read(); return '\b';
    case 't':
      read(); return '\t';
    case 'u': { // parsing unicode escapes
      int v=0;
      for(int i=0;i<4;i++) {
        read();
        v=v<<4;
        if ((c>='0')&&(c<='9')) {
          v+=c-'0';
        } else if ((c>='a') && (c<='f')) {
          v+=c-'a'+10;
        } else if ((c>='A') && (c<'F')) {
          v+=c-'A'+10;
        } else {
          consume(-1);
          return -1; // never reached
        };
      };
      read(); // position to the next in stream
      return v; // character parsed
    }
    case '\\': case '\"':  case '\'':
      // TRICK ! This should return an _old_ c before read, and then read().
      return c+(read()-c);
    default: { // parsing octal escapes
      if ((c>='0') && (c<='7')) {
        int v=c-'0';
        for (int i=0;i<2;i++) {
          read();
          if ((c>='0') && (c<='7')) {
            v = (v << 3) + c - '0';
          } else {
            if (v>0xFF) consume(-1);
            return v;
          };
        };
        read();
        if (v>0xFF) consume(-1);
        return v;
      } else {
        // invalid escape character
        consume(-1);
        return -1; // never reached
      }
    }
    } // case
  };

  private void parseChar() throws CompilationException {
    char ch=0;
    switch(read()) {
    case '\\':
      ch=(char)parseEscape();
      break;
    case -1: // EOF
    case '\n':
    case '\r':
    case '\'':
      // these can't be encountered inside char literal
      consume(-1);
      break;
    default:
      ch=(char)c;
      read();
    };
    consume('\'');
    
    type=60; // <LITERAL>
    val=new Character(ch);
    return;
  };

  private void parseString() throws CompilationException {
    if (Debug.enabled)
      Debug.check(buf.length()==0);

    while(true) {
      switch(c) {
      case -1: // EOF
      case '\n':
      case '\r':
      case '\'':
        // these can't be encountered inside String literal
        consume(-1);
        break;
      case '\\':
        buf.append((char)parseEscape());
        break;
      case '\"':
        read();
        type=60; // <LITERAL>
        val=buf.toString();
        buf.setLength(0);
        return;
      default:
        buf.append((char)c);
        read();
      };
    }
  };

  private void parseID() throws CompilationException {
    if (Debug.enabled)
      Debug.check(buf.length()==0);
    
    do {
      buf.append((char)c);
      read();
    } while((c>0) && Character.isJavaIdentifierPart((char)c));
    
    type=50; // assume <ID> for the start
    val=buf.toString();
    buf.setLength(0);
    
    // check against keywords (so far JEL has only two of them)
    if (val.equals("true")) {
      type=60; // <LITERAL>
      val=Boolean.TRUE;
    } else if (val.equals("false")) {
      type=60; // <LITERAL>
      val=Boolean.FALSE;
    };

    return;
  };

  // starts to parse integer number, falls through to parseReal if '.' 
  // is encountered
  private void parseNumber() throws CompilationException {
    if (Debug.enabled)
      Debug.check(buf.length()==0);
    
    boolean seenDigit=false;
    boolean seen89=false;
    int base=(c=='0'?8:10);
    long value=c-'0';
    
    buf.append((char)c); // save digit, which was already read
  outerLoop:
    while(true) {
      switch(read()) {
      case '9': case '8':
        // can't just throw an error, the number can be Real
        seen89=true;
      case '7': case '6': case '5': case '4':
      case '3': case '2': case '1': case '0':
        seenDigit=true;
        buf.append((char)c);
        if (base==10) {
          // check for overflow, e.g. when can't store any more decimal digits
          if ((value*10)/10 != value) 
            consume(-1); // overflow (make better overflow reporting)
          value=value*10+(c-'0');
        } else if (base==8) {
          // check for overflow, e.g. when can't store any more octal digits
          // e.g. highest three bits already occupied
          if ((value>>>61)>0)
            consume(-1); // overflow (make better overflow reporting)
          value=(value<<3)+(c-'0');
        } else { // hexadecimal number
          // check for overflow, e.g. when can't store any more hex digits
          // e.g. highest four bits already occupied
          if ((value>>>60)>0)
            consume(-1); // overflow (make better overflow reporting)
          value=(value<<4)+(c-'0');
        };
        break;
      case 'D': case 'd': case 'E': case 'e': case 'F': case 'f':
        // in non-hexadecimal mode switch to parsing real number
        if (base!=16) {
          parseReal();
          return;
        };
      case 'A': case 'a': case 'B': case 'b': case 'C': case 'c':
        if (base!=16)
          break outerLoop; // illegal character, error will appear later
        seenDigit=true;
        // check for overflow, e.g. when can't store any more hex digits
        // e.g. highest four bits already occupied
        if ((value>>>60)>0)
          consume(-1);  // overflow (make better overflow reporting)
        value=(value<<4)+(10+Character.toLowerCase((char)c)-'a');
        break;

      case '.':
        if (base==16)
          break outerLoop; // illegal character, error will appear later
        // switch to parsing real number
        parseReal();
        return;
      case 'L': case 'l':
        // this, in particular, means FINISH
        read();
        buf.setLength(0); // discard accumulated chars
        type=60; // <LITERAL>
        val=new Long(value);
        break outerLoop; // finished parsing 'long' <LITERAL>
      case 'X': case 'x':
        if ((buf.length()==1) && (base==8)) {
          // if there was only '0' before, switch to hexadecimal mode
          base=16;
          seenDigit=false;
          break;
        } else 
          break outerLoop; // illegal character, error will appear later
      default:
        // anything else (e.g. EOF, '/n', space, letter, ...) means
        // we finished parsing an integer number
        buf.setLength(0); // discard accumulated chars

        type=60; // <LITERAL>
        // select type based on range
        // note that the minus sign is handled as unary OP, not
        // as part of the number
        if (value<=127) val=new Byte((byte)value);
        else if (value<=32767) val=new Short((short)value);
        else if (value<=2147483647) val=new Integer((int)value);
        else consume(-1); // overflow (make better overflow reporting)
        break outerLoop;
      };
    };
    
    // let's check what have we parsed
    if ((c == '.') || Character.isJavaIdentifierPart((char)c)) {
      consume(-1);   // immediately followed by letter, too bad
    } else if ((base==8) && seen89) {
      consume(-1);   // no '8', '9' in hexadecimal numbers
    } else if ((base==16) && !seenDigit) {
      consume(-1);   // hexadecimal with no digits inside
    };
  };
  
  private void parseReal() throws CompilationException {
    boolean seenE = false;
    boolean makeFloat = false;
    
//      if (c!='.') {
//        // it is called from nextToken after '.' is already discarded
//        // here we make up for it
//        buf.append('.');
//      } 
    if (c=='.') read();
    buf.append('.');
    // now we have the '.' in the buffer and the first digit? as
    // current char
    
  outerLoop:
    for(;;read()) {
      switch(c) {
      case 'e': case 'E':
        if (seenE)
          break outerLoop; // second 'E', error will appear later
        seenE=true;
        // fall through
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        buf.append((char)c);
        break;
      case '+': case '-':
        // can be only after 'E'
        char lch=buf.charAt(buf.length()-1);
        if ((lch!='e') && (lch!='E'))
          break outerLoop; // number finished
        buf.append((char)c);
        break;
      case 'f': case 'F':
        read();
        makeFloat = true;
        break outerLoop;
      case 'd': case 'D':
        read();
        // fall through
      default:
        // anything unknown means end of number
        break outerLoop;
      };
    };

    // let's check what have we accumulated and make a token out of it
    if ((c == '.') || Character.isJavaIdentifierPart((char)c)) {
      consume(-1);   // immediately followed by letter, too bad
    } else {
      char lch=buf.charAt(buf.length()-1);
      // should not end by 'E','e','+','-'
      if ((lch=='E') || (lch=='e') || (lch=='+') || (lch=='-'))
        consume(-1); // illegal character
      else {
        // make <LITERAL> token
        type=60;
        if (makeFloat) {
          val=Float.valueOf(buf.toString());
          if (Float.isInfinite(((Float)val).floatValue()))
            consume(-1); // overflow (make better overflow reporting)
        } else {
          // makeDouble
          val=Double.valueOf(buf.toString());
          if (Double.isInfinite(((Double)val).doubleValue()))
            consume(-1); // overflow (make better overflow reporting)
        };
        buf.setLength(0); // if success -- reset buf
      };
    };
  };

  // performs "cast" lookahead
  // returns true if the next sequence of tokens mathes:
  // 1) '(' <ID> ('.' <ID>)* ')' (<ID>|<LITERAL>)
  // 2) '(' <ID> ('.' <ID>)* '[' ']'
  // after the lookahead the state of the tokenizer is restored
  // no objects are allocated on heap during this lookahead.
  public boolean isCast() {
    // must start with '(' which is already read and classified
    if (type!=41) return false;

    // save read state
    boolean t_CR=prevCR, t_LF=prevLF;
    int t_column=column, t_line=line, t_pos=pos;
    int t_c=c;

    boolean result=false;
    boolean arrCast=false;
    
    // look ahead (we can now freely use read, the state is saved)
    
  outerLoop:
    {
    parenLoop: 
      while (true) {
        
      ws2:
        while(true)
          switch(c) {
          case -1:  break outerLoop;
          case ' ': case '\n': case '\r': read(); break;
          default:  break ws2;
          };
        
        if (!Character.isJavaIdentifierStart((char)c))
          break outerLoop;

        // scan the rest of identifier
        read();
        for(;Character.isJavaIdentifierPart((char)c);read());
        // all identifier is consumed, consume dot
      ws3:
        while(true)
          switch(c) {
          case -1:  break outerLoop;
          case ' ': case '\n': case '\r': read(); break;
          default:
            break ws3;
          };

        switch(c) {
        case '.':
          read();
          continue parenLoop; // if dot we go to match the next <ID>
        case ')':
          // closing bracket finishes the '(' <ID> ('.' <ID>)* ')'
          // match, now we only to check -- what's after
          break parenLoop; 
        case '[':
          // array designator, we match it with ']'
          // this covers the case ( <ID> ('.' <ID>)* '[' ']'
          // after which without ')' match it is already clear
          // that we have cast
          arrCast=true;          
          break parenLoop; 
        default:
          break outerLoop; // anything else -- bail
        }
      }; // parenLoop

      read(); // skip ')' or '['
      // we now match beginning of '(', <ID>, '.', '\'', '\"', <NUMBER>
      while (true)
        switch (c) {
        case ' ': case '\n': case '\r': read(); break;
        case ']': 
          result=true;
        case '(': case '.': case '\'': case '\"':
          if (arrCast) break outerLoop;
          result=true;
          // fall through
        case -1:
          break outerLoop;
        default:
          result= (!arrCast) && (((c>='0') && (c<='9')) || 
                                 Character.isJavaIdentifierStart((char)c));
          break outerLoop;
        }

    }; // outerLoop

    // restore state
    prevCR=t_CR; prevLF=t_LF; c=t_c;
    column=t_column; line=t_line; pos=t_pos; 

    return result;
  };

  //*** TOKENIZER methods
  //***********************

  //***********************
  //*** PARSER methods

  /**
   * Parses expression.
   * @param resultType the type to convert the result into
   * @return top of the parse tree
   */
  public OP parse(Class resultType) throws CompilationException {
    try {
      expression();

      err_col=ct_column-1; // this should be now set to <EOF>

      // remove TSB at return if present
      if (((OP)paramOPs.peek()).resID==10)
        paramOPs.push(new OPunary(paramOPs,11,null,false));
      // set result type
      if (resultType!=null) 
        paramOPs.push(new OPunary(paramOPs,OP.typeID(resultType),
                                  resultType,false));
      // add the "return" instruction
      paramOPs.push(new OPunary(paramOPs,3));
    } catch (CompilationException exc) {
      // examine if it has set error position
      if (exc.col<0) {
        if (Debug.enabled)
          Debug.check(err_col>0);
        exc.col=err_col; // set if not
        //exc.line=err_line; // not yet
      };
      //      exc.printStackTrace();
      throw exc;
    };
   
    if (Debug.enabled)
      Debug.check(paramOPs.size()==1,
                   "There must be only a single item left in paramOPs stack.");
    
    return (OP)paramOPs.pop();
  };
  
  private void consumeT(int t) throws CompilationException {
    if (type!=t)
      consume(-1); // encountered unexpected (FIXME: report as token)
    nextToken();
  };

  private void expression() throws CompilationException {
    // expression ::= conditional <EOF>
    nextToken();
    conditional();
    consumeT(-1); // consume EOF
  };
  
  private void conditional() throws CompilationException {
    binOP(0);
    if (type==35) { // '?'
      int ecol=ct_column;
      nextToken();
      
      int stackSizeBeforeBranch=paramOPs.size();
      
      conditional();
      consumeT(36); // ':'

      int stackSizeAfterFirstBranch=paramOPs.size();
      conditional();
      
      err_col=ecol; // report errors against '?'
      
      if (Debug.enabled)
        Debug.check((paramOPs.size()==stackSizeAfterFirstBranch+1) &&
                     (stackSizeAfterFirstBranch==stackSizeBeforeBranch+1),
                     "Stack in conditional branches is not balanced.");
      paramOPs.push(new OPcondtnl(paramOPs));
    };
  };
  
  // this array defines binary operators precedence
  //    private final static byte[][] binR = {
  //      // min max
  //      {  18,  18},  // 0 LOR      ('||')
  //      {  17,  17},  // 1 LAND     ('&&')
  //      {   6,   6},  // 2 BOR      ('|')
  //      {   7,   7},  // 3 BXOR     ('^')
  //      {   5,   5},  // 4 BAND     ('&')
  //      {   8,   9},  // 5 EQUALITY ('==' | '!=')
  //      {  10,  13},  // 6 RELATION ('<' | '>=' | '>' | '<=')
  //      {  14,  16},  // 7 SHIFT    ('<<' | '>>' | '>>>')
  //      {   0,   1},  // 8 SUM      ('+' | '-')
  //      {   2,   4}}; // 9 TERM     ('*' | '/' | '%')

  private void binOP(int idx) throws CompilationException {
    if (idx==9)
      unary();
    else
      binOP(idx+1);

    int t;
    if (Debug.enabled)
      Debug.check((idx>=0)&&(idx<=9));
    
    int v=(int)(idx<5?(0xA539CC68C652L>>>(idx*10)):
                (0x820820E6A928L>>>((idx-5)*10)));
    // m1=
    //     5,    5,    7,    7,    6,    6,   17,   17,   18,   18
    // 00101,00101,00111,00111,00110,00110,10001,10001,10010,10010
    // 00 1010 0101 0011 1001 1100 1100 0110 1000 1100 0110 0101 0010
    //  0    A    5    3    9    C    C    6    8    C    6    5    2
    // 0x0A539CC68C652L

    // m2=
    //     4,    2,    1,    0,   16,   14,  13,   10,    9,    8
    // 00100,00010,00001,00000,10000,01110,01101,01010,01001,01000
    // 00 1000 0010 0000 1000 0010 0000 1110 0110 1010 1001 0010 1000
    //  0    8    2    0    8    2    0    E    6    A    9    2    8
    // 0x0820820E6A928L
    
    while ( ((t=type)>=(v & 0x1F)) && (t<=((v>>>5) & 0x1F)) ) {
      int ecol=ct_column;
      nextToken();
      
      if (idx==9)
        unary();
      else
        binOP(idx+1);

      err_col=ecol;
      // make use that token types and binary OPs coincide
      paramOPs.push(new OPbinary(paramOPs,t));

    };
  };

  private void unary() throws CompilationException {
    int ecol;
    int t;
    int accumStrt;

    if (((t=type)==1)||(t==30)||(t==31)) { // '-' | '~' | '!'
      ecol=ct_column;
      nextToken();
      unary();
      if (t>=30) t-=28;
      err_col=ecol;
      paramOPs.push(new OPunary(paramOPs,t-1));
    } else if (isCast()) { // lookahead for cast
      consumeT(41); // '('
      ecol=ct_column; // error will be reported against the first <ID>
      accumStrt=typeAccum.length();
      typeAccum.append(val); // value of identifier (if not, does not matter)
      consumeT(50); // '<ID>'
      while (type==40) { // '.'
        typeAccum.append('.');
        nextToken();
        typeAccum.append(val);
        consumeT(50); // '<ID>'
      };
      consumeT(42); // ')'

      element();

      err_col=ecol;
      
      // generate convert OP
      int typeID=0;
      Class clazz=null;
      while ((typeID<8) &&
             !typeAccum.substring(accumStrt).
             equals(OP.specialTypes[typeID].toString()))
        typeID++;
      
      if (Debug.enabled)
        Debug.check(typeID<=8);
   
      // this handles non-primitive types.
      if ((typeID==8) &&
          (
           (clazz=(lib.cnmap==null?null:
                   (Class)lib.cnmap.get(typeAccum.substring(accumStrt))
                   )
            )==null
           )
          )
        typeID=-1;
      
      if (typeID==8) {
        // identify the type properly
        typeID=OP.typeID(clazz);
      };

      typeAccum.setLength(accumStrt);

      if (typeID<0) // the type is unknown
        {if (true) throw new CompilationException(4,typeAccum.toString());}
      
      paramOPs.push(new OPunary(paramOPs,typeID,clazz,true));
      // end of convert OP generation
    } else {
      // <element> lookahead
      // here we can find only <LITERAL> | <ID> | '(' 
      t=type;
      if ((t==60)||(t==50)||(t==41))
        element();
      else
        consume(-1); // throw an error (FIXME: report as unexpected _token_)
    };
  };

  private void element() throws CompilationException {
    // deciding between <LITERAL>, '(' conditional ')', <ID> invocation
    // there must be at least one of these

    switch (type) {
    case 60: // <LITERAL>
      paramOPs.push(new OPload(val));
      nextToken();
      break;
    case 41:        // '('
      nextToken();
      conditional();
      consumeT(42); // ')'
      break;
    case 50: // <ID>
      invocation(false);
      break;
    default:
      consume(-1); // throw an error (FIXME: report as unexpected _token_)
    };
    
    while (type==40) { // '.'
      nextToken();
      invocation(true);
    };

    genDVCall(); // finish prepared DV call
    
  };

  private void invocation(boolean afterDot) throws CompilationException {
    int paramsStart=0;
    Class resolveIn=null;
    int ecol,ecol_id;
    boolean inDVmatch=false;

    ecol_id=ecol=ct_column; // error will be reported against the first <ID>
    Object idImage=val;
    consumeT(50); // <ID>

    if (accDV!=null) {
      int oldLen=accDV.length();
      if (afterDot) accDV.append('.');
      accDV.append(idImage);
      if (!(inDVmatch=isDV())) {
        accDV.setLength(oldLen); // back up
        err_col=ecol;
        genDVCall(); // finish prepared DV call
      };
    }; // end if accDV!=null

    if (!inDVmatch) {
      if (afterDot) resolveIn=((OP)paramOPs.peek()).resType;
      // start prepating a call to an object's method x.image
      paramsStart=paramOPs.size();
    };

    if (type==41) { // '('
      ecol=ct_column; // error will be reported against this '('

      
      if (inDVmatch) {
        // error: variable must have no parameters
        err_col=ecol;
        {if (true) throw new CompilationException(26,null);}

        // this is the place to hack in the abort of match and check
        // if the last matched name can be called as a method
      };

      nextToken();

      // now we need to determine if conditional() is present
      // it is prefixed by '-','~','!','(',<LITERAL>,<ID>
      int t=type;

      if ((t==1) || (t==30) || (t==31) || (t==41) || (t==60) || (t==50)) {
        // conditional is here
        conditional();
        while (type==43) { // ','
          nextToken();
          conditional();
        };
        
      };

      consumeT(42); // ')'
    };
    
    if (!inDVmatch) {
      // generate the method invocation
      err_col=ecol_id;
      functionCall(resolveIn,(String)idImage,paramsStart);
    };
    
    while (type==19) { // '['
      ecol=ct_column; // error will be reported against this '['
      nextToken();
      genDVCall(); // finish prepared DV call
      conditional();
      consumeT(20); // ']'

      err_col=ecol;      
      paramOPs.push(new OPbinary(paramOPs,19));      
      
    };
  };

  // service methods not directly related to parser, but to
  // complex method calling semantics of JEL

  private final boolean isDV() {
    return lib.resolver.getTypeName(accDV.toString())!=null;
  }

  private final void genDVCall()
    throws CompilationException {
    if ((accDV==null) || (accDV.length()==0)) return;
    String varName=accDV.toString();
    
    String typeName=lib.resolver.getTypeName(varName);

    int paramsStart=paramOPs.size();

    Object varObj;
    
    // This condition ensures binary and source compatibility
    // with old (pre 0.9.9) dynamic variables interface and 
    // is subject for removal in JEL 1.0
    if (lib.resolver instanceof DVMap)
      varObj=((DVMap)lib.resolver).translate(varName);
    else
      varObj=varName;

    paramOPs.push(new OPload(varObj));

    functionCall(null,"get"+typeName+"Property",paramsStart);
    accDV.setLength(0);
  }

  
  private final void functionCall(Class resolveInClass,String name,
                                  int paramsStart) throws CompilationException{
    // collect params
    int np=paramOPs.size()-paramsStart;
    Class[] params=new Class[np];

    for(int i=np-1;i>=0;i--) {
      OP cop=(OP)paramOPs.pop();
      xchgOP.push(cop);
      params[i]=cop.resType;
    }
    
    // find method
    Member m=lib.getMember(resolveInClass,name,params); 
    
    // put "this" pointer in place
    if (resolveInClass==null) {
      if ((m.getModifiers() & 0x0008)==0) {
        // insert loading of "this" pointer
        paramOPs.push(new OPcall(1,(new Object[0]).getClass()));
        int classID=lib.getDynamicMethodClassID(m);
        paramOPs.push(new OPload(new Integer(classID)));
        paramOPs.push(new OPbinary(paramOPs,19));
      }
    }
    
    // restore params & param ops
    for(int i=0;i<np;i++)
      paramOPs.push(xchgOP.pop());

    paramOPs.push(new OPcall(paramOPs,m,lib.isStateless(m)));
  }

  //*** PARSER methods
  //***********************

};
