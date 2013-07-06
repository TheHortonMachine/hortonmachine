package org.jgrasstools.gears.io.geopaparazzi.forms;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private List<Form> formList = new ArrayList<Form>();

    private String name;
    public Section( String name ) {
        this.name = name;
    }

    public void addForms( Form form ) {
        formList.add(form);
    }
    
    public List<Form> getFormList() {
        return formList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPre());
        StringBuilder tmp = new StringBuilder();
        for( Form form : formList ) {
            tmp.append(",\n").append(form.toString());
        }
        sb.append(tmp.substring(1));
        sb.append(getPost());
        return sb.toString();
    }

    public String getPre() {
        StringBuilder sb = new StringBuilder();
        sb.append("    {").append("\n");
        sb.append("        \"sectionname\": \"").append(name).append("\",\n");
        sb.append("        \"sectiondescription\": \"").append(name).append("\",\n");
        sb.append("        \"forms\": [").append("\n");
        return sb.toString();
    }

    public String getPost() {
        StringBuilder sb = new StringBuilder();
        sb.append("         ]\n");
        sb.append("    }\n");
        return sb.toString();
    }
}
