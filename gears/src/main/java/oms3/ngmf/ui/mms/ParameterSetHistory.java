/*
 * ParameterSetHistory.java
 *
 * Created on June 23, 2005, 4:53 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package oms3.ngmf.ui.mms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class ParameterSetHistory {
    private String date;
    private String who;
    private String comment;
    private String source;
    private String what;
    private final Throwable tracer = new Throwable();
    private DateFormat df =  DateFormat.getDateTimeInstance();

    public ParameterSetHistory(String hist_string) {
        StringTokenizer st = new StringTokenizer (hist_string);
        st.nextToken("\"");
        this.date = st.nextToken("\"");
        st.nextToken("\"");
        this.who = st.nextToken("\"");
        st.nextToken("\"");
        this.what = st.nextToken("\"");
        st.nextToken("\"");
        this.source = st.nextToken("\"");
        st.nextToken("\"");
        this.comment = st.nextToken("\"");
    }

    public ParameterSetHistory(String date, String who, String what, String source, String comment) {
        this.date = date;
        this.who = who;
        this.what = what;
        this.source = source;
        this.comment = comment;
    }

    public ParameterSetHistory(Object what, String comment) {
        this.comment = comment;
        this.date = df.format (new Date ());
        this.who = System.getProperty("user.name");
        this.source = findCaller (2);

        if (what.getClass() == MmsParameter.class) {
            MmsParameter param = (MmsParameter)what;
            this.what = "parameter " + param.getName() + " changed";

        } else if (what.getClass() == MmsDimension.class) {
            MmsDimension dim = (MmsDimension)what;
            this.what = "dimension " + dim.getName() + " changed";

        } else if (what.getClass() == File.class) {
            File file = (File)what;
            this.what = "file renamed from " + file.getPath();

        } else {
            System.out.println ("MmsParameterSetHistory unknown object type");
        }
    }

    private String findCaller(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException();
        }
        tracer.fillInStackTrace();
        return tracer.getStackTrace()[depth+1].toString();
    }

    /**
     * Getter for property date.
     * @return Value of property date.
     */
    public String getDate() {return this.date;}

    /**
     * Getter for property who.
     * @return Value of property who.
     */
    public String getWho() {return this.who;}

    /**
     * Getter for property comment.
     * @return Value of property comment.
     */
    public String getComment() {return this.comment;}

    /**
     * Setter for property comment.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Getter for property classMethod.
     * @return Value of property classMethod.
     */
    public String getSource() {return this.source;}

    /**
     * Getter for property what.
     * @return Value of property what.
     */
    public String getWhat() {
        return this.what;
    }

    public String toString () {
        return  "<history date=\"" + date + "\" who=\"" + who + "\" what=\"" + what + "\" source=\"" + source + "\" comment=\"" + comment + "\"\\>";
    }
}