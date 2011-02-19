/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: IntegerStack.java 490 2006-10-01 16:08:04Z metlov $
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
 * Specialized stack which works with integers.
 */
class IntegerStack {
  private int[] data;
  private int count=0;
  
  public IntegerStack(int initCapacity) {
    data=new int[initCapacity];
  };

  public IntegerStack() {
    this(30);
  };
  
  public IntegerStack copy() {
    IntegerStack res=new IntegerStack(data.length);
    res.count=count;
    for(int i=0;i<count;i++)
      res.data[i]=data[i];
    //    in most cases actually empty stacks are cloned in JEL
    //    System.arraycopy(data,0,res.data,0,count);
    return res;
  };

  public final void push(int what) {
    if (count>=data.length) incCap(count+1);
    data[count++]=what;
  };
  
  public final int peek() {
    return data[count-1];
  };

  public final int pop() {
    return data[--count];
  };

  public final int size() {
    return count;
  };

  // Swaps values above given limits in two stacks
  public static void swap(IntegerStack one,int oneLim,
                          IntegerStack other,int otherLim) {
    // this is used for swapping labels in logical expressions compilation
    // usually there are not so many labels to swap... and the element
    // by element copy should not incur significant peformance penalty
    // ordering of elements between limits is not important
    
    IntegerStack temp=null;
    if (one.size()>oneLim)
      temp=new IntegerStack();

    //    System.out.println("vgv  one.size()= "+one.size()+" ( "+oneLim+" )"+
    //                   "  other.size()= "+other.size()+" ( "+otherLim+" )");

    while (one.size()>oneLim)
      temp.push(one.pop());
    while (other.size()>otherLim)
      one.push(other.pop());
    while ((temp!=null) && (temp.size()>0))
      other.push(temp.pop());

// ----- faster version of the same
//      int copyFromOne=one.count-oneLim;
//      int copyFromOther=other.count-otherLim;
//      boolean cf_one=copyFromOne>0;
//      boolean cf_other=copyFromOther>0;
//      if ((cf_one) || (cf_other)) {
//        int nSizeOne=oneLim+copyFromOther;
//        int nSizeOther=otherLim+copyFromOne;
//        // ensure capacities
//        if (nSizeOne>one.data.length) one.incCap(nSizeOne);
//        if (nSizeOther>other.data.length) other.incCap(nSizeOther);
//        int[] temp=null;
//        if (cf_one) {
//          temp=new int[copyFromOne];
//          System.arraycopy(one.data,oneLim,temp,0,copyFromOne);
//        };
//        if (cf_other)
//         System.arraycopy(other.data,otherLim,one.data,oneLim,copyFromOther);
//        if (cf_one)
//          System.arraycopy(temp,0,other.data,otherLim,copyFromOne);
//        one.count=nSizeOne;
//        other.count=nSizeOther;
//      };
// ----- end of faster version of the same

  };

  private void incCap(int minCapacity) {
    int[] old_data=data;
    int oldSize=data.length;
    int newSize=oldSize*2;
    if (newSize<minCapacity) newSize=minCapacity;
    data=new int[newSize];
    System.arraycopy(old_data,0,data,0,count);
  };

};
