/*
 * $Id:$
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
package oms3;

import java.lang.reflect.Field;

/** Access Interface for Fields.
 *
 * @author od
 */
public interface Access {

    /**
     * Reading (in) access.
     * 
     * @throws Exception 
     */
    void in() throws Exception;

    /**
     * Writing (out) access
     * 
     * @throws Exception 
     */
    void out() throws Exception;

    Field getField();

    Object getFieldValue() throws Exception;

    void setFieldValue(Object o) throws Exception;

    Object getComponent();

    /** Check if field access is valid.
     *
     * @return true if valid, false otherwise.
     */
    boolean isValid();

    FieldContent getData();

    void setData(FieldContent data);
    
}
