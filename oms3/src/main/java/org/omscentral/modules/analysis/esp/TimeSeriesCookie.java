package org.omscentral.modules.analysis.esp;

public interface TimeSeriesCookie {
    public double[] getDates();
    public double[] getVals();
    public ModelDateTime getStart();
    public ModelDateTime getEnd();
    public String getName();
    public String getDescription();
    public String getUnits();
    public String getSource();
    public void setDates(double[] dates);
    public void setVals(double[] vals);
    public void setStart(ModelDateTime start);
    public void setEnd(ModelDateTime end);
    public void setName(String name);
    public void setDescription(String description);
    public void setUnits(String units);
    public void setSource(String source);
    public String getXmlBlock ();
    public void dump ();
    public void trim (ModelDateTime start, ModelDateTime end);
}

