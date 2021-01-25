/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.dbs.nosql.mongodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.nosql.INosqlCollection;
import org.hortonmachine.dbs.nosql.INosqlDb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

/**
 * A mongodb database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MongoDb implements INosqlDb {
    private String user;
    private String password;
    private ConnectionData connectionData;
    private String mDbPath;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private boolean useSsl = false;

    @Override
    public EDb getType() {
        return EDb.MONGODB;
    }

    @Override
    public String getDbEngineUrl() {
        return mDbPath.substring(0, mDbPath.lastIndexOf('/'));
    }

    @Override
    public String getDbName() {
        return db.getName();
    }

    @Override
    public void setCredentials( String user, String password ) {
        this.user = user;
        this.password = password;
    }

    @Override
    public ConnectionData getConnectionData() {
        return connectionData;
    }

    public void setUseSsl( boolean useSsl ) {
        this.useSsl = useSsl;
    }

    @Override
    public boolean open( String dbPath ) throws Exception {
        if (dbPath.startsWith("jdbc")) {
            dbPath = dbPath.replaceFirst("jdbc:", "");
        }
        this.mDbPath = dbPath;

        connectionData = new ConnectionData();
        connectionData.connectionLabel = dbPath;
        connectionData.connectionUrl = new String(dbPath);
        connectionData.user = user;
        connectionData.password = password;
        connectionData.dbType = getType().getCode();

        int lastSlash = dbPath.lastIndexOf('/');
        String clusterUrl = dbPath.substring(0, lastSlash);
        String dbName = dbPath.substring(lastSlash + 1);

        if (useSsl) {
            clusterUrl += "?ssl=true";
        }
        mongoClient = MongoClients.create(clusterUrl);

        db = mongoClient.getDatabase(dbName);

        return true;
    }

    @Override
    public void close() throws Exception {
        mongoClient.close();
    }

    @Override
    public String[] getDbInfo() {
        String descr = mongoClient.getClusterDescription().getShortDescription();
        StringBuilder sb = new StringBuilder("Databases:");
        String dbNames = getDatabasesNames().stream().collect(Collectors.joining(","));
        sb.append(dbNames);
        return new String[]{descr, dbNames};
    }

    @Override
    public List<String> getDatabasesNames() {
        List<String> list = new ArrayList<>();
        MongoIterable<String> names = mongoClient.listDatabaseNames();
        for( String name : names ) {
            list.add(name);
        }
        return list;
    }

    @Override
    public List<String> getCollections( boolean doOrder ) throws Exception {
        MongoIterable<String> names = db.listCollectionNames();
        List<String> list = new ArrayList<>();
        for( String name : names ) {
            list.add(name);
        }
        if (doOrder) {
            Collections.sort(list);
        }
        return list;
    }
    
    @Override
    public void drop() {
        db.drop();
    }

    @Override
    public boolean hasCollection( String name ) throws Exception {
        return getCollections(false).contains(name);
    }

    public INosqlCollection getCollection( String name ) {
        com.mongodb.client.MongoCollection<Document> collection = db.getCollection(name);
        if (collection == null) {
            return null;
        }
        MongoCollection coll = new MongoCollection(collection);
        return coll;
    }

    @Override
    public void createCollection( String newName ) {
        db.createCollection(newName);
    }

    public static void main( String[] args ) throws Exception {
        try (MongoDb db = new MongoDb()) {
            db.open("mongodb://localhost:27017/Loc8r");
            System.out.println("Databases");
            db.getDatabasesNames().forEach(n -> System.out.println(n));
            System.out.println("Collections in Loc8r");
            db.getCollections(true).forEach(n -> System.out.println(n));
        }
    }

}
