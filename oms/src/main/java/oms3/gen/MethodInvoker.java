/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.gen;


/**
 *
 * @author od
 */
public interface MethodInvoker {

    void invoke() throws Exception;

    void setTarget(Object target);
}
