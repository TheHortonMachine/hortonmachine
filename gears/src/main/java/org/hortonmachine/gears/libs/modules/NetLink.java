package org.hortonmachine.gears.libs.modules;

import java.util.ArrayList;
import java.util.List;

public class NetLink {

    public int num;

    public int upRow;
    public int upCol;
    public int downRow;
    public int downCol;
    public int downLinkRow;
    public int downLinkCol;

    public int upTca;
    public int downTca;

    public NetLink downStreamLink;

    public List<NetLink> upStreamLinks = new ArrayList<NetLink>();

    public NetLink( int num, int upCol, int upRow, int downCol, int downRow, int downLinkCol, int downLinkRow ) {
        this.num = num;
        this.upCol = upCol;
        this.upRow = upRow;
        this.downCol = downCol;
        this.downRow = downRow;
        this.downLinkCol = downLinkCol;
        this.downLinkRow = downLinkRow;
    }

    public void connect( NetLink other ) {
        // other is downstream link
        if (downLinkCol == other.upCol && downLinkRow == other.upRow && downStreamLink == null) {
            downStreamLink = other;
            if (!other.upStreamLinks.contains(this)) {
                other.upStreamLinks.add(this);
            }
        } else
        // thisÏ is downstream link
        if (other.downLinkCol == upCol && other.downLinkRow == upRow && other.downStreamLink == null) {
            other.downStreamLink = this;
            if (!upStreamLinks.contains(other)) {
                upStreamLinks.add(other);
            }
        }

    }

    @Override
    public String toString() {
//        return "num=" + num + "\nrow=" + row + "\ncol=" + col + "\nhasdown=" + (downStreamNode != null) + "\nupcount="
//                + upStreamNodes.size();

        int deltaTca = downTca - upTca;

        String cells = "cells=" + deltaTca;
        String down = "down=" + downCol + "/" + downRow;
        String up = "up=" + upCol + "/" + upRow;
        return "<b>basin" + num + "</b>\\n  " + cells + "\\n  " + down + "\\n  " + up;
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
