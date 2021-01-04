package org.hortonmachine.gears.libs.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A net channel helper class to order netnumbering in a hierarchy. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NetLink {

    public int num;

    /**
     * The row  of the last upstream node. 
     */
    public int upRow;
    /**
     * The col  of the last upstream node. 
     */
    public int upCol;
    /**
     * The row  of the last downstream node that is part of the link's basin. 
     */
    public int downRow;
    /**
     * The col  of the last downstream node that is part of the link's basin. 
     */
    public int downCol;
    /**
     * The row  of the first node of the next downstream link. 
     */
    public int downLinkRow;
    /**
     * The col  of the first node of the next downstream link. 
     */
    public int downLinkCol;

    /**
     * A possible parent in the chain of desired basin aggregations.
     * 
     * If this is not null, then this basin belongs to the one with this id.
     */
    public Integer desiredChainNetLink;

    /**
     * The tca of the basin closed at the lowest point of this link.
     */
    private int tca = 0;

    private NetLink downStreamLink;

    private List<NetLink> upStreamLinks = new CopyOnWriteArrayList<NetLink>();

    /**
     * Defines if the link can be merged with others or if it is required to be fixed.
     * 
     * If fixed is set to true, the node can't be removed as middle man.
     */
    private boolean isFixed;

    public NetLink( int num, int upCol, int upRow, int downCol, int downRow, int downLinkCol, int downLinkRow, boolean isFixed ) {
        this.num = num;
        this.upCol = upCol;
        this.upRow = upRow;
        this.downCol = downCol;
        this.downRow = downRow;
        this.downLinkCol = downLinkCol;
        this.downLinkRow = downLinkRow;
        this.isFixed = isFixed;
    }

    public boolean isFixed() {
        return isFixed;
    }

    public void setTca( int tca ) {
        this.tca = tca;
    }

    public int getTca() {
        return tca;
    }

    public NetLink getDownStreamLink() {
        return downStreamLink;
    }

    public void setDownStreamLink( NetLink downStreamLink ) {
        this.downStreamLink = downStreamLink;
    }

    public List<NetLink> getUpStreamLinks() {
        return upStreamLinks;
    }

    public void connect( NetLink other ) {
        // other is downstream link
        if (downLinkCol == other.upCol && downLinkRow == other.upRow && downStreamLink == null) {
            downStreamLink = other;
            if (!other.upStreamLinks.contains(this)) {
                other.upStreamLinks.add(this);
            }
        } else
        // this is downstream link
        if (other.downLinkCol == upCol && other.downLinkRow == upRow && other.downStreamLink == null) {
            other.downStreamLink = this;
            if (!upStreamLinks.contains(other)) {
                upStreamLinks.add(other);
            }
        }

    }

    @Override
    public String toString() {
        String ups = upStreamLinks.stream().map(nl -> nl.num + "").collect(Collectors.joining(", "));
        int upLinksTca = 0;
        for( NetLink netLink : upStreamLinks ) {
            upLinksTca += netLink.tca;
        }
        int cells = tca - upLinksTca;

        String s = "\n___________________\n";
        s += "| num=" + num + "\n";
        s += "| tca=" + cells + "\n";
        s += "| ups=[" + ups + "]\n";
        s += "|___________________|\n";
        s += "|        " + upCol + "/" + upRow + "\n";
        s += "|                || \n";
        s += "|                ||\n";
        s += "|                ||\n";
        s += "|                \\/\n";
        s += "|        " + downCol + "/" + downRow + "\n";
        s += "|___________________|\n";
        s += "|        " + downLinkCol + "/" + downLinkRow + "\n";
        if (downStreamLink != null) {
            s += "| down=[" + downStreamLink.num + "]\n";
        }
        s += "|___________________|\n";
        return s;
    }

    public String toMindMapString() {
        String down = "down=" + downCol + "/" + downRow;
        String up = "up=" + upCol + "/" + upRow;

        int upLinksTca = 0;
        for( NetLink netLink : upStreamLinks ) {
            upLinksTca += netLink.tca;
        }

        int cells = tca - upLinksTca;
        String cellsDeltaStr = "tca=" + cells;
        String cellsStr = "outlet tca=" + tca;
        String fixed = "";
        if (isFixed) {
            fixed = "\\n  <b>FIXED</b>";
        }
        return "<b>basin" + num + "</b>\\n  " + "\\n  " + down + "\\n  " + up + "\\n  " + cellsDeltaStr + "\\n  " + cellsStr
                + fixed;
    }

    public String toJsonString() {
        String str = "  {\n";
        str += "    \"num\":" + num + ",\n";
        str += "    \"downCol\":" + downCol + ",\n";
        str += "    \"downRow\":" + downRow + ",\n";
        str += "    \"upCol\":" + upCol + ",\n";
        str += "    \"upRow\":" + upRow + ",\n";
        str += "    \"downLinkCol\":" + downLinkCol + ",\n";
        str += "    \"downLinkRow\":" + downLinkRow + "\n";
        str += "  }";
        return str;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + downCol;
        result = prime * result + downRow;
        result = prime * result + upCol;
        result = prime * result + upRow;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NetLink other = (NetLink) obj;
        if (downCol != other.downCol)
            return false;
        if (downRow != other.downRow)
            return false;
        if (upCol != other.upCol)
            return false;
        if (upRow != other.upRow)
            return false;
        return true;
    }

}
