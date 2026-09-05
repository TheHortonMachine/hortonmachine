/*
 * $Id: BMI.java dba0d06e37a0 2015-04-14 odavid@colostate.edu $
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
package oms3.bmi;

/**
 *
 * @author od, architha
 */
public interface BMI {

    void initialize(String config_file) throws Exception;


    void update();

    //void UpdateUntil(double); what could be the purpose of this in the taken example
    // void UpdateFrac(double); what could be the purpose of this in the taken example

    void finalize();


    String getComponentName();


    int getInputVarNameCount();


    int getOutputVarNameCount();


    String[] getInputVarNames();


    String[] getOutputVarNames();


    String getVarType(String name);


    long getVarItemsize(String name); //if these parameters are passed to these methods then from where are they passed


    String getVarUnits(String name); //and also these parameters are used in a function at bmi_heat_cxx, what is the purpose 
    //public void GetVarRank(const char* name, int* rank);// and where would they be passed
    //public void GetVarSize(const char* name, int* size);
    //public void GetVarNbytes(const char* name, int* nbytes);


    void getCurrentTime(double time);


    void getStartTime(double time);


    void getEndTime(double end);


    void getTimeStep(double dt);


    void getTimeUnits(String units);


    Object getValue(String name) throws Exception;

    //public void GetValuePtr(const char *, char **); not sure of how i could apply these for current example
    //public void GetValueAtIndices(const char *, char *dest, int * inds, int len); not sure of how i could apply these for current example

    void setValue(String name, Object value) throws Exception; // how to assign the values @out using setValue
    //public void SetValuePtr(const char *, char **);
    //public void SetValueAtIndices(const char * name, int * inds, int len, char *src);


    String getGridType();
    //public void getGridType(const char *, char *); // what should be used at these fields when the grid type doesn't apply to the component
    //public void GetGridShape(const char *, int *);
    //public void GetGridSpacing(const char *, double *);
    //public void GetGridOrigin(const char *, double *);
    //public void GetGridX(const char *, double *);
    //public void GetGridY(const char *, double *);

}
