/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Naming support for components and fields
 *  (Ontology support)
 *
 * @see Label
 * @author Olaf David
 * @version $Id$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Name {

    String value();
}

