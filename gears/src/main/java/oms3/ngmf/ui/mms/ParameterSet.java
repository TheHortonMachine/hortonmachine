/*
 * $Id: ParameterSet.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
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
