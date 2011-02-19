/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: OPload.java 490 2006-10-01 16:08:04Z metlov $
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

/**
 * A tree node, representing loading of a constant.
 */
public class OPload extends OP {

  /** Holds an object to be loaded  */
  public Object what;

  /**
   * Creates an OP, loading a constant.
   * @param what is a constant wrapped into a reflection object. E.g 
   *             <TT>java.lang.Integer(1)</TT> to load <TT>1</TT> of
   *             primitive type <TT>int</TT>.
   */
  public OPload(Object what) {
    this.resID=typeIDObject(what);
    
    if (Debug.enabled)
      Debug.check((resID!=8));
    
    this.resType=specialTypes[resID];
    
    this.what=what;
  };

  /**
   * Creates an OP, loading a constant to be put instead of another OP.
   * <P>For private JEL usage in constants folding.
   * @param instead an OP, which will be raplaced by this OPload.
   * @param what is a constant wrapped into a reflection object. E.g 
   *             <TT>java.lang.Integer(1)</TT> to load <TT>1</TT> of
   *             primitive type <TT>int</TT>.
   */
  public OPload(OP instead,Object what) {
    if (Debug.enabled) {
      if (!(
            (
             (typeIDObject(what)==instead.resID) && 
             (instead.resID!=8)
             ) || 
            (
             (instead.resID==10) && 
             (what instanceof StringBuffer)
             )
            )
          ) {
        Debug.println("typeIDObject(what)="+
                      typeIDObject(what));
        Debug.println("instead.resID="+instead.resID);
        Debug.println("what="+what);
        Debug.println("what.getClass()="+what.getClass());
      };

      Debug.check((
                    (typeIDObject(what)==instead.resID) && 
                    (instead.resID!=8)
                    ) || 
                   (
                    (instead.resID==10) && 
                    (what instanceof StringBuffer)
                    )
                   );
    };

    this.resType=instead.resType;
    this.resID=instead.resID;
    this.what=what;
  };

  public Object eval() throws Exception {
    return what;
  };  

  public void compile(ClassFile cf) {
    cf.codeLDC(what,resID);
  };

};
