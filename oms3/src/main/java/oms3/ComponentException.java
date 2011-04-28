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

/**
 * Component Exception.
 * 
 * @author od
 */
public class ComponentException extends RuntimeException {

    private static final long serialVersionUID = -8177666866536291028L;
    Object comp;

    public ComponentException(Throwable cause, Object comp) {
        super(cause);
        this.comp = comp;
    }

    public ComponentException(String msg) {
        super(msg);
    }
    
    /**
     * Get the component that caused the exception
     * 
     * @return the component that caused the exception.
     */
    public Object getSource() {
        return comp;
    }

}
