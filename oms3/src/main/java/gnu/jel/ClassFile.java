/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: ClassFile.java 490 2006-10-01 16:08:04Z metlov $
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

import gnu.jel.debug.Debug;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

/**
 * This class represents a classfile image in JEL.
 */
public class ClassFile implements Cloneable {

  // constant pool handling
  private int poolEntries=1; // Number of entries in the constant pool
  // the class name is in the first element of the CP
  // it is not written by default to enable the dynamic class renaming.
  private ByteArrayOutputStream constPoolData;
  private DataOutputStream constPool;

  // Constant pool hashing :
  // In next hashtables keys are Objects, values their CP indices
  // UTFs require special handling since their keys can be the same as
  // ones of strings
  private HashMap<Object,Integer> Items=new HashMap<Object,Integer>();
  private HashMap<String,Integer>  UTFs=new HashMap<String,Integer>();
  
  // holds code of all methods
  private int nMethods=0;
  private int nMethodsPatch;

  // holds code of all methods
  private byte[] text;
  int tsize=0;
  
  // write a byte
  void write(int b) {
    try {
      text[tsize++]=(byte)b;
    } catch (ArrayIndexOutOfBoundsException exc) {
      byte[] new_text=new byte[text.length<<1];
      System.arraycopy(text, 0, new_text, 0, tsize-1);
      text=new_text;
      text[tsize-1]=(byte)b;
    };
  };

  // write short
  void writeShort(int v) {
    write((v >>> 8) & 0xFF);
    write( v        & 0xFF);
  };

  // write int
  void writeInt(int v) {
    write((v >>> 24) & 0xFF);
    write((v >>> 16) & 0xFF);
    write((v >>>  8) & 0xFF);
    write( v         & 0xFF);
  };
  

  private boolean isInterface;

  private IntegerStack[] cstk=new IntegerStack[6]; 
  // six stacks
  //   0) "false" jumps
  //   1) "true"  jumps
  //   2) unconditional jumps
  //   3) "false" blocks
  //   4) "true" blocks
  //   6) unconditional blocks

  /**
   * Starts creation of a new class file.
   * <P>Sizes of <TT>fAccess</TT>, <TT>fNames</TT> and <TT>fTypes</TT>
   * arrays must be the same.
   * @param modifiers sum of one or more of <TT>PUBLIC</TT>, <TT>FINAL</TT>,
   *                  <TT>INTERFACE</TT>, <TT>ABSTRACT</TT>
   *                  constants of java.lang.reflect.Modifier
   * @param name is the name of new class (must be in Java historical form,
   *             i.e. with dots replaced by slashes '/')
   * @param superClass is the superclass of this class
   * @param interfaces array of interfaces this class implements
   * @param fields fields this class will have
   */
  public ClassFile(int modifiers,String name,Class superClass,
                   Class[] interfaces,LocalField[] fields) {
    
    constPoolData=new ByteArrayOutputStream();
    constPool=new DataOutputStream(constPoolData);
    text=new byte[256];

    if (Debug.enabled)
      Debug.check(cstk.length==6);
    for (int i=0;i<6;i++)
      cstk[i]=new IntegerStack();

    try {
      if (Debug.enabled) // check if the name is in historical form
        Debug.check(name.indexOf('.')==-1);

      getUTFIndex(name); // must be the first entry
            

      if (Debug.enabled)
        Debug.check((modifiers & ~(java.lang.reflect.Modifier.PUBLIC+
                                    java.lang.reflect.Modifier.FINAL+
                                    java.lang.reflect.Modifier.INTERFACE+
                                    java.lang.reflect.Modifier.ABSTRACT))==0);
      isInterface=((modifiers & 0x0200)>0);
      
      // write modifiers
      writeShort(modifiers | 0x0020); // set ACC_SUPER flag
      
      // CONSTANT_CLASS for a new class, has to be written by hand
      // since no corresponding java.lang.Class object exists yet.
      if (Debug.enabled)
        Debug.check(poolEntries==2);
      
      poolEntries++;
      constPool.write(7);
      constPool.writeShort(1);  //name of this class is always the first entry.
      
      // write class cp index
      writeShort(2);
    
      // write superclass cp index
      writeShort(getIndex(superClass,9));
      
      // write out interfaces
      int nInterfaces;
      if (interfaces==null) nInterfaces=0; else nInterfaces=interfaces.length;
      
      writeShort(nInterfaces);
      for(int i=0;i<nInterfaces;i++) {
        if (Debug.enabled)
          Debug.check(interfaces[i].isInterface());
        writeShort(getIndex(interfaces[i],9));
      };
      
      // write out fields
      int nFields;
      if (fields==null) nFields=0; else nFields=fields.length;
      
      writeShort(nFields);
      for(int i=0;i<nFields;i++) {
        LocalField cLF=fields[i];
        
        if (Debug.enabled)
          Debug.check(!(cLF instanceof LocalMethod));
        
        writeShort(cLF.getModifiers());
        writeShort(getUTFIndex(cLF.getName()));
        writeShort(getUTFIndex(Library.getSignature(cLF.getType())));
        writeShort(0); // no attributes
      };
      nMethodsPatch=tsize;
      writeShort(0); // methods count placeholder
    } catch (IOException exc) {
      // can't be
    };
  };

  /**
   * Makes a clone of this object.
   * <P> This is used in JEL to avoid step-by step creation of service
   * methods of gnu.jel.CompiledExpression (<init>, getType, ...). They are
   * created only once, and then the resulting ClassFile is cloned
   * at the point, when it is ready to accept the code for evaluation method. 
   * @return a clone of this object
   */
  public ClassFile clone() {
    ClassFile res=null;
    try {
      res=(ClassFile)super.clone(); // clone all primitive members

      // clone containers
      res.Items=new HashMap<Object,Integer>(res.Items);
      res.UTFs=new HashMap<String,Integer>(res.UTFs);
      res.paramsVars=(int [])res.paramsVars.clone();
      res.branchStack=res.branchStack.copy();

      if (Debug.enabled)
        Debug.check(cstk.length==6);
      res.cstk=new IntegerStack[6];
      for(int i=0;i<6;i++)
        res.cstk[i]=cstk[i].copy();

      // clone streams
      res.constPoolData=new ByteArrayOutputStream();
      constPool.flush();
      constPoolData.writeTo(res.constPoolData);
      res.constPool=new DataOutputStream(res.constPoolData);

      res.text=(byte[]) res.text.clone();
      
    } catch (IOException exc) {
      if (Debug.enabled)
        Debug.reportThrowable(exc);
    } catch (CloneNotSupportedException exc) {
      if (Debug.enabled)
        Debug.reportThrowable(exc);
    };
    return res;
  };



  private int startCodeAttr=0;
  private int startCode=0;

  /**
   * Starts a new method of this class.
   * @param m method descriptor.
   * @param vars types of local variables by number.
   */
  public void newMethod(LocalMethod m, Class[] vars) {

    if (Debug.enabled) {
      Debug.check(cW==0);
      for(int i=0;i<6;i++)
        Debug.check(cstk[i].size()==0);
      Debug.check(currJump==0);
    };
    
    // first finish the last method
    finishMethod();
    
    nMethods++;
    
    // now prepare creation of a new one.
    int mdfrs=m.getModifiers();
    if (isInterface) mdfrs=mdfrs | 0x0400; // mark abstract for interfaces
    boolean isAbstract=((mdfrs & 0x0400) > 0);
    
    writeShort(mdfrs); // modifiers
    writeShort(getUTFIndex(m.getName())); // name index
    writeShort(getUTFIndex(Library.getSignature(m))); // signature index
    
    int temp=0;
    Class[] exceptions=m.getExceptionTypes();
    if (exceptions!=null) temp++; // exceptions are in separate attribute
    if (!isAbstract) temp++; // non-abstract methods have code
    writeShort(temp);
    
    // first we write exceptions attribute, if needed
    if (exceptions!=null) {
      temp=exceptions.length;
      writeShort(getUTFIndex("Exceptions"));
      writeInt((temp+1)*2); // attribute length
      writeShort(temp);
      for(int i=0;i<temp;i++)
        writeShort(getIndex(exceptions[i],9));
      // TODO: could have checked that exceptions[i] is actually subclass of
      // java.lang.Exception.
      // May be better place for this check is gnu.jel.reflect...
    };
      
    // now we start writing the code attribute
    if (!isAbstract) {
      startCodeAttr=tsize;
      writeShort(getUTFIndex("Code"));
      writeInt(0);       // attribute length, to be patched back
      
      writeShort(0);     // max stack, to be patched back
      
      // local variables
      Class[] params=m.getParameterTypes();
      int parlen=(params==null?0:params.length);
      int varlen=(vars==null?0:vars.length);
      int this_num=(mdfrs & 0x0008)==0?1:0; // if not static there is "this"
      int nLocalVars=parlen+varlen+this_num;
      paramsVars=new int[nLocalVars];
      
      if (Debug.enabled)
        Debug.check(cW==0);

      for (int i=0;i<paramsVars.length;i++) {
        int j=i-this_num;
        paramsVars[i]=cW;
        noteStk(-1,i<this_num?8:OP.typeID(j<parlen?params[j]:vars[j-parlen]));
      };
      
      writeShort(cW);  // number of local vars
      cW=0;
      
      writeInt(0);      // code length, to be patched back
      startCode=tsize;
    };
    
    // Reset Java stack statistics
    mW=0;
      
    if (!isAbstract) currMethod=m;
  };

  private void finishMethod() {
    if (currMethod!=null) { // finish the previous method
      int codeEnd=tsize;
      writeShort(0); // no exception table       // TODO: add exceptions
      writeShort(0); // no attributes

      int currPos=tsize;
      
      // Code attribute length
      tsize=startCodeAttr+2;
      writeInt(currPos-startCodeAttr-6);

      tsize=startCodeAttr+6;
      writeShort(mW);

      tsize=startCode-2;
      writeShort(codeEnd-startCode);
      
      tsize=currPos;

      currMethod=null;
    };
  };

  // adds one or more instructions to the code.
  //  public void asmOP(OP op);

  private static final byte[] prologue = 
  {(byte)0xCA,(byte)0xFE,(byte)0xBA,(byte)0xBE,0x00,0x03,0x00,0x2D};

  /**
   * Finishes class and returns the resulting bytecode.
   * @return array of bytes of the resulting *.class file.
   */
  public byte[] getImage() {
    ByteArrayOutputStream image=new ByteArrayOutputStream();

    try {
      finishMethod(); // finish the last method

      // patch methods count
      int otsize=tsize;
      tsize=nMethodsPatch;
      writeShort(nMethods);
      tsize=otsize;

      // assemble the method bytecode
      image.write(prologue);
      image.write((poolEntries >>> 8) & 0xFF);
      image.write((poolEntries >>> 0) & 0xFF);
      constPoolData.writeTo(image);
      image.write(text,0,tsize);
      image.write(0); // no class file attributes      
      image.write(0); // no class file attributes      
    } catch (IOException exc) {
      // can't be
    };
    return image.toByteArray();
  };
  
  //=========================================================
  //========== INSTRUCTIONS INTERFACE TO THE STATE===========
  //=========================================================
  private LocalMethod currMethod=null;
  int[] paramsVars=null; // correspondence between parameter number and lvars.

  // current, and maximum number of words in Java stack
  private int cW=0, mW=0;

  /** Notes removal of typeID s and subsequent addition of typeID a
   * to Java stack.
   * <P> If either is negative the corresponding operation
   * (addition/removal) is skipped. This method is needed to allow ClassFile
   * to compute the maximum stack occupation for the generated code. It is
   * responsibility of the user (of this class) to call noteStk() each
   * time the stack is changed from within the code.
   * @param s typeid to be put on stack (-1 if none).
   * @param a typeid to be taken off Java stack (-1 if none).
   */ 
  public void noteStk(int s,int a) {
    if (Debug.enabled)
      Debug.check(cW<=mW);

    if ((s>=0) && (s!=9)) {
      cW--;
      if ((s & (~2)) == 5) cW--; // Note additional word for J and D
      
      if (Debug.enabled)
        Debug.check(cW>=0);
    };

    if ((a>=0) && (a!=9)) {
      cW++;
      if ((a & (~2)) == 5) cW++; // Note additional word for J and D
      if (cW>mW) mW=cW;
    };
  };

  /**
   * classes frequently used in generated code
   */
  private static final Class[] specialClasses;

  static {
    if (Debug.enabled)
      Debug.check(OP.specialTypes.length==29,
                   "You changed special types in TypesStack please update "+
                   "specialClasses array in ClassFile.");

    specialClasses=(Class[])TableKeeper.getTable("specialClasses");

  };

  private static final Member[] specialMethods;

  static {
    char[][] specialMds=(char[][]) TableKeeper.getTable("specialMds");
    String[] specialMdsN=(String[]) TableKeeper.getTable("specialMdsN");

    specialMethods=new Member[specialMds.length];
    
    {
      Class definingClass=null;
      String name=null;
      Class[] params=null;
      int i=0;
      try {
        for (i=0;i<specialMds.length;i++) {
          int defClassID=specialMds[i][0] % 100;
          definingClass=specialClasses[defClassID];
          name=specialMdsN[specialMds[i][1]];
          params=new Class[specialMds[i].length-2];
          for(int j=0;j<params.length;j++) {
            params[j]=specialClasses[specialMds[i][2+j]];
          };
              
          switch (specialMds[i][0]/100) {
          case 0: // usual method
            specialMethods[i]=definingClass.getMethod(name,params);
            break;
          case 1:
            specialMethods[i]=definingClass.getConstructor(params);
            break;
          case 2: // field
            if (Debug.enabled)
              Debug.check(params.length==0);
            specialMethods[i]=definingClass.getField(name);
            break;
          default:
            if (Debug.enabled)
              throw new Exception("JEL: Wrong class ID modifier.");
          };
        };
        
      } catch (Exception exc) {
        if (Debug.enabled) {
          Debug.println("JEL: Problem with special method ["+i+"] "+
                        name+" in "+definingClass);
          for(int j=0;j<params.length;j++)
            Debug.println("parameter["+j+"]="+params[j]);
          Debug.reportThrowable(exc);
        };
      };
    };
  };

  // code up to 8 operations without extensions
  public final void codeB(long op) {
    while (op!=0) {
      write((byte)(op & 0xFFL));
      op=op >>> 8;
    };
  };

  // code method call or field reference
  public final void codeM(Member m) {
    int modifiers=m.getModifiers();
    if (Library.isField(m)) {
      if ((modifiers & 0x0008)>0)
        code(0xb2); //   |   getstatic
      else
        code(0xb4); //   |   getfield
      writeShort(getIndex(m,12));
    } else {
      // method or constructor
      boolean inInterface=false;
      int cfID=10;
      Class dClass;
      if (m instanceof Constructor) {
        code(0xb7);                        //         |  invokespecial
        cfID++;
      } else if ((modifiers & 0x0008)>0)   // static ?
        code(0xb8);                        //         |  invokestatic
      else if (inInterface=(((dClass=m.getDeclaringClass())!=null) 
                            && dClass.isInterface()))
        code(0xb9);                        //         |  invokeinterface
      else
        code(0xb6);                        //         |  invokevirtual
 
      writeShort(getIndex(m,cfID));             //         |  <CP index>
      
      if (inInterface) {    // declared in interface ?
        // based on the assumption that interfaces may not have constructors
        writeShort((1+((Method)m).getParameterTypes().length)<<8); //|  <nargs> 0
      };
    };
  };

  //////////////////////// BRANCHES HANDLING VARS \\\\\\\\\\\\\\\\\\\\\\\\

  // for storing the state of Java stack from branch to branch
  private IntegerStack branchStack=new IntegerStack();

  /** jump currently being generated(its code is here) */
  int currJump = 0;
  private boolean iNJ=false; // invert next jump

  // also cstk[i] is related to branches handling

  //\\\\\\\\\\\\\\\\\\\\\ END BRANCHES HANDLING VARS //////////////////////

  /**
   * code up to 8 operations.
   * <P>Additionally to Java bytecodes this method accepts (and interprets)
   * some more commands. See the implementation.
   * @param op operations to code (least significant bytes coded first).
   */
  public final void code(long op) {
    while (op!=0) {
      char opc=(char)(op & 0xFFL);
      if (Debug.enabled)
        Debug.check(opc !=0xFF);
      
      final int mc=228;
      switch (opc-mc) {
      case 0:  // opc=228  (0xE4)  -- start "true" branch
        code(0xF4); // logical param AND

        // store the number of words in Java stack before the first branch
        branchStack.push(cW);
        break;
      case 1:  // opc=229  (0xE5)  -- start "false" branch
        code(0xE7FA); 
        // FA -- ensure_value();
        // E7 -- unblock "false" jumps
        
        int beforeStk=branchStack.pop();
        
        if (Debug.enabled) {
          branchStack.push(cW);
          Debug.check(cW>=beforeStk);
        };
    
        // remove the result of the previous branch, the other branch must
        // put the same thing back into the types stack
        cW=beforeStk;
    
        code(0xEDECF2A7L);
        //| A7 --   goto
        //| F2 --   make down pointing unconditional label
        //| EC --   block unconditional labels
        //| ED --   land unblocked "false" labels
        break;
      case 2:  // opc=230  (0xE6)  -- finish "false" branch
        if (Debug.enabled) {
          code(0xFA); 
          // FA -- ensure_value();
          Debug.check((branchStack.pop()==cW),
                       "Stack mismatch when compiling conditional");
          code(0xEFE9);
          // E9 -- unblock unconditional jumps
          // EF -- land unblocked unconditional labels
        } else {
          code(0xEFE9FA);
          // FA -- ensure_value();
          // E9 -- unblock unconditional jumps
          // EF -- land unblocked unconditional labels
        };
        break;
      case 3:  // opc=231  (0xE7)  -- unblock "false" labels
      case 4:  // opc=232  (0xE8)  -- unblock "true" labels
      case 5:  // opc=233  (0xE9)  -- unblock unconditional labels
        cstk[opc-(mc+3-3)].pop();
        break;
      case 6:  // opc=234  (0xEA)  -- block "false" labels
      case 7:  // opc=235  (0xEB)  -- block "true" labels
      case 8:  // opc=236  (0xEC)  -- block unconditional labels
        cstk[opc-(mc+6-3)].push(cstk[opc-(mc+6)].size());
        break;
      case 9:  // opc=237  (0xED)  -- land unblocked "false" labels
      case 10: // opc=238  (0xEE)  -- land unblocked "true" labels
      case 11: // opc=239  (0xEF)  -- land unblocked unconditional labels
        {
          int blocked_at=0;
          final IntegerStack blk=cstk[opc-(mc+9-3)];
          final IntegerStack jmp=cstk[opc-(mc+9  )];
            
          if (blk.size()>0) blocked_at=blk.peek();
          while (jmp.size()>blocked_at) {
            int currpos=tsize;
            int addrpos=jmp.pop();
            
            // in the next command -1 because displacement is counted from the
            // jump opcode
            tsize=addrpos;
            writeShort(currpos-addrpos+1);
            tsize=currpos;
          };
        };
        break;
      case 12: // opc=240  (0xF0)  -- make down pointing "false" label (j0)
      case 13: // opc=241  (0xF1)  -- make down pointing "true" label  (j1)
      case 14: // opc=242  (0xF2)  -- make down pointing unconditional label
        cstk[opc-(mc+12  )].push(tsize);
        writeShort(0);              // placeholder for the backpatched address
        break;
      case 15: // opc=243  (0xF3)  -- set current open jump (code in next byte)
        currJump=(int)((op=op>>>8) & 0xFF);
        break;
      case 16: // opc=244  (0xF4)  -- logical param AND
      case 17: // opc=245  (0xF5)  -- logical param OR
        {
          // for AND s=0   for OR s=1
          int s=(opc-(mc+16));
          
          code(0xF9); // ensure jump
          
          if (iNJ ^ (s==0)) {
            if (Debug.enabled)
              Debug.check((currJump>=153) && (currJump<=166),
                           "Attempt to invert non jump bytecode ("+
                           currJump+")");
            currJump=(((currJump-1) ^ 0x0001)+1);
          };
          iNJ=false;

          code(0x00F300+currJump);
          // 00F3        -- set curr jump to 0
          code((0xEAEDF0) + (s<<16)+((s^1)<<8)+s);
          // code is
          // F0 - make label j0     modified by s
          // ED - land labels j1    modified by s^1
          // EA - block labels j0   modified by s
        };
        break;
      case 18: // opc=246  (0xF6)  -- logical end AND
      case 19: // opc=247  (0xF7)  -- logical end OR
        cstk[opc-(mc+18-3)].pop(); // just throw the corresponding block
        break;
      case 20: // opc 248  (0xF8)  --  unblock all labels
        code(0xE7E8E9);
        break;
      case 21: // opc=249  (0xF9)  -- ensure jump is in progress
        if (currJump==0) {
          // if no jump in progress yet
          noteStk(0,-1); // "boolean" is removed from stack
          currJump=157;  //|
        };
        break;
      case 22: // opc=250  (0xFA)  -- ensure value is in stack
        boolean noPendingJumps=false;

        if (currJump==0) { 
          // check if there are pending jumps
          int blocked0=0;
          if (cstk[3].size()>0) blocked0=cstk[3].peek();
      
          int blocked1=0;
          if (cstk[4].size()>0) blocked1=cstk[4].peek();
          
          noPendingJumps=(cstk[0].size()==blocked0) &&
            (cstk[1].size()==blocked1);
        };
    
        if (!noPendingJumps) {
          // if there are pending jumps or jump in progress
          code(0xE4); //         branch_true();
          codeLDC(Boolean.TRUE,0);
          code(0xE5); //         branch_false();
          codeLDC(Boolean.FALSE,0);
          code(0xE6); //         branch_end();
        };
        break;
      case 23: // opc=251  (0xFB)  -- block labels
        code(0xEAEBEC);
        break;
      case 24: // opc==252 (0xFC)  -- unblock labels with inversion
        code(0xF9); // ensure jump
        IntegerStack.swap(cstk[1],cstk[4].pop(),cstk[0],cstk[3].pop());
        cstk[5].pop();
        iNJ=!iNJ;
        break;
      case 25: // opc==253 (0xFD)  -- write special class CP ref
        op=op >>> 8;
        writeShort(getIndex(specialClasses[(int)(op  & 0xFFL)],9));
          break;
      case 26: // opc==254 (0xFE)  -- call special method
        op=op >>> 8;
        codeM(specialMethods[(int)(op & 0xFF)]);
        break;
      default:
        if (Debug.enabled)
          Debug.check(opc !=0xFF);
        write(opc);  
      };
      op=op >>> 8;
    };
  };
  
  // Shortcut load opcodes for int type
  // index is value+1; allowed values from -1 to 5.
  // other values should be loaded from CP.
  //  private final static int[] load_ints=
  //  {  0x02,  0x03,  0x04,  0x05,  0x06,  0x07,  0x08};
  //       -1     0      1      2      3      4      5
  //  private final static int[] load_long_ints=
  //  {0x8502,  0x09,  0x0A,0x8505,0x8506,0x8507,0x8508};
  //      -1     0      1      2      3      4      5

  /**
   * generates code for code loading constant of primitive type or string.
   * @param o reflection object or String containing the constant
   * @param primitiveID type ID to save lookups.
   */
  public final void codeLDC(Object o, int primitiveID) {
    if (Debug.enabled)  
      Debug.check(((primitiveID>=0) && (primitiveID<8)) || 
                   ((primitiveID==8) && (o==null)) || 
                   ((primitiveID==10) && (o instanceof StringBuffer)) ||
                   ((primitiveID==11) && (o instanceof String)));
    int short_opcodes=0;
    boolean tsb_store=false;
    
    int iv=-1;
    switch (primitiveID) {
    case 0:
      iv=(((Boolean)o).booleanValue()?1:0);
    case 2:
      if (iv<0) iv=(int)((Character)o).charValue();
    case 1:
    case 3:
    case 4:
      if (iv<0) iv=((Number)o).intValue();
      if ((iv>=-1) && (iv<=5))
        short_opcodes=iv+3; //|  iconst_<i>
      else if ((iv>=-128) && (iv<=127))
        short_opcodes=0x00000010 | ((iv & 0xFF)<<8); //| bipush, <value>
      break;
    case 5:
      long lv=((Long)o).longValue();
      if (((lv | 1)^1) == 0) { //  0 or 1
        short_opcodes=0x09+(int)lv;
      } else if ((lv>=-1) && (lv<=5)) {
        short_opcodes=0x8503+(int)lv;
      };
      break;
    case 6:
      float fv=((Float)o).floatValue();
      if (fv==0.0f) short_opcodes=0x0B;           //| fconst_0
      else if (fv==1.0f) short_opcodes=0x0C;      //| fconst_1
      else if (fv==2.0f) short_opcodes=0x0D;      //| fconst_2
      break;
    case 7:
      double dv=((Double)o).doubleValue();
      if (dv==0.0) short_opcodes=0x0E;      //| dconst_0
      else if (dv==1.0) short_opcodes=0x0F; //| dconst_1
      break;
    case 8:
      if (o==null) short_opcodes=0x01;      //| aconst_null
      break;
    case 10:
      tsb_store=true;
    case 11:
      short_opcodes=0;
      primitiveID=8;
      break;
    default:
      if (Debug.enabled) 
        Debug.check(false,"Loading of object constants is not supported by "+
                     "the Java class files.");
    };
    
    if (short_opcodes==0) {
      // longs and doubles occupy two elements of stack all others just one
      boolean dword_const=((primitiveID==5) || (primitiveID==7));

      int cpindex;
      if (tsb_store) {
        code(0x0001FE591DFDBBL); // STR => TSB
        //                      | new
        //                          <CP: java.lang.StringBuffer>
        //                      | dup
        //                      | invokespecial StringBuffer()
        noteStk(-1,10); // pushes ref to TSB
        cpindex=getIndex(o.toString(),primitiveID);
      } else {
        // makes use of the fact that primitiveID(Object)==typeID(string)
        cpindex=getIndex(o,primitiveID);
      };
      
      if (Debug.enabled)  
        Debug.check((cpindex>=0) && (cpindex<=65535));

      
      if ((!dword_const) && (cpindex<=255)) {
        write(0x12);          //|   ldc
        write(cpindex);
      } else {
        int opc=0x13;                        //|   ldc_w
        if (dword_const) opc++;              //|   ldc2_w
        write(opc);
        writeShort(cpindex);
      };

    } else {
      codeB(short_opcodes);
    }
    
    noteStk(-1,primitiveID); // add what was loaded
    if (tsb_store) {
      code(0x08FE);
      noteStk(11,-1);        // remove extra string used up by constructor
    };
  };

  //=========================================================
  //================= CONSTANTS POOL HANDLING ===============
  //=========================================================

  /**
   * Used to get the index of the given UTF8 string in the Constant Pool.
   * <P> If the specified string was not found in the pool -- it is added.
   * @param str the string to look for ( to add )
   * @return index of the string in the Constant Pool.
   */
  int getUTFIndex(String str) {
    // Check if it is in the pool already
    Integer index=UTFs.get(str);
    if (index==null) {    // add UTF to the pool
      index=new Integer(poolEntries++);
      try {
        constPool.write(1);            // CONSTANT_Utf8 = 1;
        constPool.writeUTF(str);
      } catch (java.io.IOException e) {
        if (Debug.enabled) Debug.reportThrowable(e);
      };
      UTFs.put(str,index);
    };
    return index.intValue();
  };

  // encodes types of relevant objects as integers
  // for classes corresponding to primitive types codes are the same as 
  // primitiveID's
  private int typeID(Object item) {
    int id=OP.typeIDObject(item);
    if (id<8) return id;
    if (item instanceof String) return 8;
    if (item instanceof Class) return 9;
    if (item instanceof Member) return 10;
    return -1;
  };

  /**
   * Used to determine an old CP index or to create a new one for an item.
   * @param item an item to create or get an index for
   * @return index for an item (negative if it has to be written)
   */
  private final int getIndex(Object item) {
    return getIndex(item,typeID(item));
  };

  /**
   * Used to determine an old CP index or to create a new one for an item.
   * @param item an item to create or get an index for
   * @param typeid identifies type of argument to avoid linear searches
   * @return index for an item (negative if it has to be written)
   */
  public int getIndex(Object item,int typeid) {

    Integer index=Items.get(item);
    if (index==null) {
      int newIndex=-1;

      try {
        int ival=-1;
        switch (typeid) {
        case 0:
          ival=((Boolean)item).booleanValue()?1:0;
        case 2:
          if (ival<0) ival=(int)((Character)item).charValue();
        case 1:
        case 3:
        case 4:
          if (ival<0) ival=((Number)item).intValue();
          newIndex=poolEntries++;
          constPool.write(3);                  // CONSTANT_Integer = 3;
          constPool.writeInt(ival);
          break;
        case 5:
          newIndex=poolEntries;
          constPool.write(5);                  //  CONSTANT_Long = 5;
          constPool.writeLong(((Long)item).longValue());
          poolEntries+=2; // Long occupies two entries in CP, weird !!!
          break;
        case 6:
          newIndex=poolEntries++;
          constPool.write(4);   //       CONSTANT_Float = 4;
          constPool.writeFloat(((Float)item).floatValue());
          break;
        case 7:
          newIndex=poolEntries;
          constPool.write(6);   //       CONSTANT_Double = 6;
          constPool.writeDouble(((Double)item).doubleValue());
          poolEntries+=2; // Double occupies two entries in CP, weird !!!
          break;
        case 8:
          {
            int UTFIndex=getUTFIndex((String)item); // write string , if needed
            newIndex=poolEntries++;
            constPool.write(8); //       CONSTANT_String = 8;
            constPool.writeShort(UTFIndex);
          }
          break;
        case 9:
          {
            String histNameStr=
              Library.toHistoricalForm(((Class)item).getName());
            int UTFIndex=getUTFIndex(histNameStr); // write FQCN , if needed
            newIndex=poolEntries++;
            constPool.write(7); //       CONSTANT_Class = 7;
            constPool.writeShort(UTFIndex);
          }
          break;
        case 10: // Method
        case 11: // Constructor
        case 12: // Field
          Member member = (Member) item;
          Class dClass=member.getDeclaringClass();
          int entryType;
          if (Library.isField(member))
            entryType=9;  //          CONSTANT_Fieldref = 9;
          else if ((dClass!=null) && (dClass.isInterface()))
            entryType=11; //          CONSTANT_InterfaceMethodref = 11;
          else
            entryType=10; //          CONSTANT_Methodref = 10;
          
          newIndex=writeMemberRef(member,entryType);
          break;
        default:
          if (Debug.enabled)
            Debug.println("Can't place an item of type \""+
                          item.getClass().getName()+
                          "\" to the constant pool.");
        };
      } catch (java.io.IOException e) {
        if (Debug.enabled) Debug.reportThrowable(e);
      };
      index=new Integer(newIndex);
      Items.put(item,index);
    };
    return index.intValue();
  };

  // writes out full reference to method, interface or field
  // this includes UTFs, Name&Type and XXX_ref entries
  private int writeMemberRef(Member member, int entry) 
    throws java.io.IOException {
    if (Debug.enabled)
      Debug.check((entry==10)||(entry==9)||(entry==11));
    //  CONSTANT_Fieldref = 9;  CONSTANT_Methodref = 10; 
    //  CONSTANT_InterfaceMethodref = 11;
    
    int name_ind=getUTFIndex((member instanceof Constructor)?"<init>":
                             member.getName());
    int sign_ind=getUTFIndex(Library.getSignature(member));

    Class dClass=member.getDeclaringClass();
    int cls_ind;
    if (dClass==null) cls_ind=2;  else  cls_ind=getIndex(dClass,9); 
    //  this class ---^^^^^^^^^                  9 means Class--^
    
    // Create Name and Type record
    int nat_ind=poolEntries++;
    constPool.write(12);                //   CONSTANT_NameAndType = 12;
    constPool.writeShort(name_ind);
    constPool.writeShort(sign_ind);

    // Create XXX_ref entry (where XXX is InterfaceMethod, Method or field)    
    int index=poolEntries++;
    constPool.write(entry);
    constPool.writeShort(cls_ind);
    constPool.writeShort(nat_ind);

    return index;
  };


  //================================================================
  //================== END OF CONSTANT POOL HANDLING ===============
  //================================================================

};









