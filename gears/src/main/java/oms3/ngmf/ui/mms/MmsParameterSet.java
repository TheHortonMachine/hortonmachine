/*
 * MMSParameterSet.java
 *
 * Created on June 23, 2005, 4:51 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package oms3.ngmf.ui.mms;


import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;


public class MmsParameterSet implements ParameterSet {

    private HashMap dimensions = new HashMap();
    private HashMap parameters = new HashMap();
    private Vector history = new Vector();
    private String fileName;
    private String description, version;
    private Dimension[] dimensionArray;
    private Parameter[] parameterArray;
    private boolean writeHistory = false;


    public Vector getParamsFor2DDim(String dim_name) {
        Vector params = new Vector(20, 20);

        String dim1_name = dim_name.substring(0,dim_name.indexOf(','));
        String dim2_name = dim_name.substring(dim_name.indexOf(',') + 1,dim_name.length());

        Dimension dim1 = (Dimension)(dimensions.get(dim1_name));
        Dimension dim2 = (Dimension)(dimensions.get(dim2_name));

        for (Iterator i = parameters.values().iterator(); i.hasNext();) {
            Parameter p = (Parameter)(i.next());
            if ((p.isDimensionedBy(dim1)) && (p.isDimensionedBy(dim2))) {
                params.add(p);
            }
        }
        return (params);
    }

    public TreeSet get2DCombos() {
        TreeSet params = new TreeSet();

        for (Iterator i = parameters.keySet().iterator(); i.hasNext();) {
            Parameter p = (Parameter)(parameters.get(i.next()));
            if (p.getNumDim()== 2) {
                String foo = p.getDimension(0).getName() + "," + p.getDimension(1).getName();
                params.add(foo);
            }
        }
        return (params);
    }

    /*
     *  Implement ParameterSet interface
     */

    public Parameter getParameter(String name) {return (Parameter)(parameters.get(name));}
    public Dimension getDimension(String name) {return (Dimension)(dimensions.get(name));}
    public Map getDims() {return dimensions;}
    public Map getParams() {return parameters;}

    public Set getParamsForDim(Dimension dim) {
        TreeSet params = new TreeSet();

        for (Iterator i = parameters.values().iterator(); i.hasNext();) {
            Parameter p = (Parameter)(i.next());

            if (p.isDimensionedBy(dim)) {
                params.add(p);
            }
        }
        return (params);
    }

    public void addDimension(Dimension dim) {
        dimensions.put(dim.getName(), dim);

        Object[] o = dimensions.values().toArray();
        dimensionArray = new Dimension[dimensions.size()];
        for (int i = 0; i < dimensions.size(); i++) {
            dimensionArray[i] = (Dimension)(o[i]);
        }
    }

    public void setDimension(Dimension dim, int size) {
        if (dim.getSize() == size) return;

        dim.setSize(size);

        Set param_set = getParamsForDim(dim);
        for (Iterator i = param_set.iterator(); i.hasNext();) {
            Parameter param = (Parameter)(i.next());
            param.resize();
        }

        history.add (new ParameterSetHistory (dim, "changed size to " + size));
    }

    public void setParameterValues(Parameter param, Object vals) {
        param.setVals(vals);
        history.add (new ParameterSetHistory (param, "changed array values"));
    }

    public void setParameterValue(Parameter param, Object val, int index) {
        param.setValueAt(val, index);
        history.add (new ParameterSetHistory (param, "changed value at index " + index));
    }

    public void addParameter (Parameter parameter) {
        /*
         *  Check to make sure that all of the dimensions that this parameter uses
         *  are in the dimension map.  Add them if they are not.
         */
        int num_dim = parameter.getNumDim();
        for (int i = 0; i < num_dim; i++) {
            if (!dimensions.containsKey(parameter.getDimension(i).getName())) {
                addDimension (parameter.getDimension(i));
            }
        }

        /*
         *  See if this parameter is already in the parameter set.  Remove it
         *  if it is.
         */

        Parameter param = (Parameter)(getParameter (parameter.getName()));
        if (param != null) {
            parameters.remove(parameter.getName());
        }

        parameters.put(parameter.getName(), parameter);

        Object[] o = parameters.values().toArray();
        parameterArray = new Parameter[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            parameterArray[i] = (Parameter)(o[i]);
        }
    }

    public Object getValues(Dimension dim) {
        int[] i_a = new int[1];
        i_a[0] = dim.getSize();
        return i_a;
    }

    public Object getValues(Parameter param) {
        return param.getVals();
    }

    public Object getValues(String name) {
        Object ret = null;

        Parameter mp = (Parameter)(getParameter (name));
        if (mp != null) {
            ret = getValues(mp);

        } else {
            Dimension md = (Dimension)(getDimension (name));
            ret = getValues(md);
        }
        return ret;
    }

    public String getFileName () {
        return fileName;
    }

    public static void main(String arg[]) {
        try {
            MmsParamsReader mp = new MmsParamsReader(new java.io.FileReader(arg[0]));
            ParameterSet mps = mp.read();

            System.out.println ("Dimensions = " + mps.getDims());
            System.out.println ("Parameters = " + mps.getParams());

        } catch (java.io.FileNotFoundException e) {
            System.out.println(arg[0] + " not found");
        } catch (java.io.IOException e) {
            System.out.println(arg[0] + " io exception");
        }
    }

    /**
     * Getter for property description.
     * @return Value of property description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Setter for property description.
     * @param description New value of property description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for property version.
     * @return Value of property version.
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Setter for property version.
     * @param version New value of property version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public Set getDimensionNames() {
        return dimensions.keySet();
    }

    public Set getParameterNames() {
        return parameters.keySet();
    }

    public String toString() {
        if (fileName != null) {
            return fileName;
        } else {
            return "MMS Parameters";
        }
    }

    public Dimension[] getDimensionArray () {
        return dimensionArray;
    }

    public Parameter[] getParameterArray () {
        return parameterArray;
    }

    public String[] getEditableDimensionArray() {
        TreeSet dim_set = new TreeSet();

        for (Iterator i = parameters.values().iterator(); i.hasNext();) {
            Parameter p = (Parameter)(i.next());
            if (p.getSize() > 0) {
                String foo = "";
                for (int j = 0; j < p.getNumDim(); j++) {
                    if (j != 0) foo = foo + ",";
                    foo = foo + p.getDimension(j).getName();
                }
                dim_set.add(foo);
            }
        }
        Object[] objs = dim_set.toArray();
        String[] ret = new String[objs.length];
        for (int i = 0; i < objs.length; i++) {
            ret[i] = (String)objs[i];
        }
        return ret;
    }

    public void setFileName(String filename) {
        this.fileName = filename;
    }

    /**
     * Getter for property history.
     * @return Value of property history.
     */
    public Vector getHistory() {
        return this.history;
    }

    public void addHistory(String hist_line) {
        this.history.add (new ParameterSetHistory (hist_line));
    }

    public void addHistory(Object what, String comment) {
        this.history.add(new ParameterSetHistory(what, comment));
    }

    public void writeHistory(boolean write) {
        this.writeHistory = write;
    }

    public boolean isWriteHistory() {
        return this.writeHistory;
    }

    public void setWriteHistory(boolean writeHistory) {
        this.writeHistory = writeHistory;
    }

}
