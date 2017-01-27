package org.jgrasstools.dbs.log;

public class Message {
    public long id;
    public long ts;
    /**
     * One of {@link EMessageType}.
     */
    public int type;
    public String tag;
    public String msg;
}
