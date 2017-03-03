package org.jgrasstools.osm.utils;

import de.westnordost.osmapi.map.data.LatLon;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.user.UserInfo;

public class ToStringUtils {

    public static String toString( UserInfo userInfo ) {
        StringBuilder sb = new StringBuilder();

        sb.append(userInfo.id).append(") ").append(userInfo.displayName).append("\n");
        sb.append(userInfo.profileDescription).append("\n");
        sb.append("changesetsCount=").append(userInfo.changesetsCount).append("\n");
        sb.append("gpsTracesCount=").append(userInfo.gpsTracesCount).append("\n");
        return sb.toString();
    }

    public static String toString( Way way ) {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(way.getId()).append("\n");
        sb.append("type=").append(way.getType()).append("\n");
        sb.append("version=").append(way.getVersion()).append("\n");
        return sb.toString();
    }

    public static String toString( Node node ) {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(node.getId()).append("\n");
        LatLon p = node.getPosition();
        sb.append("position=").append(p.getLatitude()).append("/").append(p.getLatitude()).append("\n");
        sb.append("type=").append(node.getType()).append("\n");
        sb.append("version=").append(node.getVersion()).append("\n");
        return sb.toString();
    
    }

    public static String toString( Relation relation ) {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(relation.getId()).append("\n");
        sb.append("type=").append(relation.getType()).append("\n");
        sb.append("version=").append(relation.getVersion()).append("\n");
        return sb.toString();
    }

}
