/*
 * ParameterSet.java
 *
 * Created on June 23, 2005, 4:47 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package oms3.ngmf.ui.mms;

import java.util.Map;
import java.util.Set;
import java.util.Vector;

public interface ParameterSet {

        public Map getDims();
        public Map getParams();
        public Set getParamsForDim(Dimension dim);
        public Parameter getParameter(String param_name);
        public Dimension getDimension(String dim_name);
        public void addDimension(Dimension dim);
        public void setDimension(Dimension dim, int size);
        public void addParameter(Parameter parameter);
        public Object getValues(String name);
        public Object getValues(Dimension dim);
        public Object getValues(Parameter param);
        public String getFileName();
        public void setFileName(String filename);
        public String getDescription();
        public void setDescription(String description);
        public String getVersion();
        public void setVersion(String version);
        public Set getDimensionNames();
        public Set getParameterNames();
        public Dimension[] getDimensionArray();
        public Parameter[] getParameterArray();
        public String[] getEditableDimensionArray();
        public Vector getHistory();
        public void addHistory(String hist_line);
        public void addHistory(Object what, String comment);
        public void setParameterValues(Parameter param, Object vals);
        public void setParameterValue(Parameter param, Object val, int index);
        public boolean isWriteHistory();
        public void setWriteHistory(boolean writeHistory);
        public Vector getParamsFor2DDim(String dim_name);
    }
