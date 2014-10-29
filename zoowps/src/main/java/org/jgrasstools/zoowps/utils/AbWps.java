// THIS FILE IS GENERATED, DO NOT EDIT, IT WILL BE OVERWRITTEN
package org.jgrasstools.zoowps.utils;

import java.util.Collection;
import java.util.HashMap;

import oms3.Access;
import oms3.ComponentAccess;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.Initialize;

import org.geotools.process.ProcessException;
import org.jgrasstools.modules.Ab;

/**
 * Template module used to copy the code generation.
 * 
 * <p>Here <b>Ab</b> would be substituted with the current module name. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AbWps {
    public static int AbBuilder( HashMap conf, HashMap inputs, HashMap outputs ) {
        try {
            Ab tmpModule = new Ab();

            // set the inputs to the model
            ComponentAccess.setInputData(inputs, tmpModule, null);

            // trigger execution of the module
            ComponentAccess.callAnnotated(tmpModule, Initialize.class, true);
            ComponentAccess.callAnnotated(tmpModule, Execute.class, false);
            ComponentAccess.callAnnotated(tmpModule, Finalize.class, true);

            // get the results
            ComponentAccess cA = new ComponentAccess(tmpModule);
            Collection<Access> outputsCollection = cA.outputs();

            // and put them into the output map
            HashMap<String, Object> outputMap = new HashMap<String, Object>();
            for( Access access : outputsCollection ) {
                try {
                    String fieldName = access.getField().getName();
                    Object fieldValue = access.getFieldValue();
                    outputMap.put(fieldName, fieldValue);
                } catch (Exception e) {
                    throw new ProcessException(e.getLocalizedMessage());
                }
            }

            outputs.put("Result", outputMap);
        } catch (Exception e) {
            e.printStackTrace();
            outputs.clear();
            outputs.put("Result", "ERROR: " + e.getLocalizedMessage());
            return 4;
        }
        return 3;
    }
}