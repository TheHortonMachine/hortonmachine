package org.hortonmachine.gears.libs.modules;

import java.util.ArrayList;
import java.util.List;

public class NetNumNode {

    public int num;

    public int row;
    public int col;

    public int nodeTca;

    public NetNumNode downStreamNode;

    public List<NetNumNode> upStreamNodes = new ArrayList<NetNumNode>();

    public NetNumNode( int num, int col, int row ) {
        this.num = num;
        this.col = col;
        this.row = row;
    }

    @Override
    public String toString() {
//        return "num=" + num + "\nrow=" + row + "\ncol=" + col + "\nhasdown=" + (downStreamNode != null) + "\nupcount="
//                + upStreamNodes.size();

//        int deltaTca = endTca-startTca;
        
        
        
        return "<b>basin" + num + "</b>\\n  <i>row=" + row + ", col=" + col + "</i>";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + col;
        result = prime * result + row;
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
        NetNumNode other = (NetNumNode) obj;
        if (col != other.col)
            return false;
        if (row != other.row)
            return false;
        return true;
    }

}
