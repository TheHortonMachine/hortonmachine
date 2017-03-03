package org.jgrasstools.osm.examples;

import org.jgrasstools.osm.utils.ToStringUtils;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.MapDataDao;
import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import de.westnordost.osmapi.user.UserDao;
import de.westnordost.osmapi.user.UserInfo;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

public class Examples {

    private OsmConnection osm;

    public Examples() {
        OAuthConsumer auth = new DefaultOAuthConsumer("", null);
        OsmConnectionHandler ch = new OsmConnectionHandler(null,null, auth);
        osm = ch.getOsmConnection();

        double y = 46.63165;
        double x = 11.14438;
        double yD = 0.00065;
        double xD = 0.001;
        BoundingBox bbox = new BoundingBox(y - yD, xD, y + yD, x + xD);

        MapDataHandler mapDataHandler = new MapDataHandler(){

            @Override
            public void handle( Relation relation ) {
                System.out.println("*****************************************");
                ToStringUtils.toString(relation);
                long userId = relation.getChangeset().user.id;
                printUserInfo(userId);
                System.out.println("*****************************************");
            }

            @Override
            public void handle( Way way ) {
                System.out.println("*****************************************");
                ToStringUtils.toString(way);
                long userId = way.getChangeset().user.id;
                printUserInfo(userId);
                System.out.println("*****************************************");
            }

            @Override
            public void handle( Node node ) {
                System.out.println("*****************************************");
                ToStringUtils.toString(node);
                long userId = node.getChangeset().user.id;
                printUserInfo(userId);
                System.out.println("*****************************************");
            }

            @Override
            public void handle( BoundingBox bounds ) {
            }
        };

        getMapData(bbox, mapDataHandler);

    }

    public void printUserInfo( long id ) {
        UserInfo user = new UserDao(osm).get(id);
        ToStringUtils.toString(user);

    }

    public void getMapData( BoundingBox boundingBox, MapDataHandler myMapDataHandler ) {
        MapDataDao mapDao = new MapDataDao(osm);
        mapDao.getMap(boundingBox, myMapDataHandler);
    }

    public static void main( String[] args ) {
        new Examples();
    }
}
