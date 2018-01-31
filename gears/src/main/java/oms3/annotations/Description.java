/*
 * $Id$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Description information for fields and components.
 *
 * @see In
 * @author Olaf David 
 * @version $Id$ 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Inherited
public @interface Description {

    /** The default value of a description
     *
     * @return the description string
     */
    String value() default "";

    // optional Localized descriptions below.
    String ar() default "";

    String be() default "";

    String bg() default "";

    String ca() default "";

    String cs() default "";

    String da() default "";

    String de() default "";

    String el() default "";

    String en() default "";

    String es() default "";

    String et() default "";

    String fi() default "";

    String fr() default "";

    String ga() default "";

    String hi() default "";

    String hr() default "";

    String hu() default "";

    String in() default "";

    String is() default "";

    String it() default "";

    String iw() default "";

    String ja() default "";

    String ko() default "";

    String lt() default "";

    String lv() default "";

    String mk() default "";

    String ms() default "";

    String mt() default "";

    String nl() default "";

    String no() default "";

    String pl() default "";

    String pt() default "";

    String ro() default "";

    String ru() default "";

    String sk() default "";

    String sl() default "";

    String sq() default "";

    String sr() default "";

    String sv() default "";

    String th() default "";

    String tr() default "";

    String uk() default "";

    String vi() default "";

    String zh() default "";
}
