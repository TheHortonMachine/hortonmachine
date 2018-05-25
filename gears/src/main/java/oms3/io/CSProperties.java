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
package oms3.io;

import java.util.Map;

/** Comma separated properties
 *
 * @author Olaf David
 */
public interface CSProperties extends Map<String, Object> {

    /** Get the name of the propertyset
     * 
     * @return the name
     */
    String getName();

    /**
     * Set the name
     * @param name
     */
    void setName(String name);

    /** Get the annotations for the propertyset.
     * 
     * @return the info for the propertyset.
     */
    Map<String, String> getInfo();

    /** Get the info for a property.
     * 
     * @param propertyName
     * @return the annotations for this property.
     */
    Map<String, String> getInfo(String propertyName);

    void setInfo(String propertyname, Map<String, String> info);

    public void putAll(CSProperties p);
}
