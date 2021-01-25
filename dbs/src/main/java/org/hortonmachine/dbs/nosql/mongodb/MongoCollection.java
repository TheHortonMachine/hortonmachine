package org.hortonmachine.dbs.nosql.mongodb;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.hortonmachine.dbs.nosql.INosqlCollection;
import org.hortonmachine.dbs.nosql.INosqlDocument;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

public class MongoCollection implements INosqlCollection {

    private com.mongodb.client.MongoCollection<Document> mongoCollection;

    public MongoCollection( com.mongodb.client.MongoCollection<Document> mongoCollection ) {
        this.mongoCollection = mongoCollection;
    }

    @Override
    public List<INosqlDocument> find( String query, int limit ) {
        if (query != null && query.trim().length() == 0) {
            query = null;
        }
        FindIterable<Document> find = null;
        if (query == null) {
            find = mongoCollection.find();
        } else {
            Bson filter = null;
            if (query.contains("=")) {
                String[] split = query.split("=");
                filter = eq(split[0].trim(), split[1].trim());
            } else if (query.contains(">")) {
                String[] split = query.split(">");
                filter = lt(split[0].trim(), split[1].trim());
            } else if (query.contains(">=")) {
                String[] split = query.split(">=");
                filter = lte(split[0].trim(), split[1].trim());
            } else if (query.contains("<")) {
                String[] split = query.split("<");
                filter = gt(split[0].trim(), split[1].trim());
            } else if (query.contains("<=")) {
                String[] split = query.split("<=");
                filter = gte(split[0].trim(), split[1].trim());
            }
            find = mongoCollection.find(filter);
        }
        List<INosqlDocument> list = new ArrayList<>();
        int count = 0;
        for( Document document : find ) {
            MongoDocument doc = new MongoDocument(document);
            list.add(doc);
            if (count++ >= limit) {
                break;
            }
        }
        return list;
    }

    @Override
    public INosqlDocument getFirst() {
        Document first = mongoCollection.find().first();
        if (first != null) {
            return new MongoDocument(first);
        } else {
            return null;
        }
    }

    @Override
    public long getCount() {
        long countDocuments = mongoCollection.countDocuments();
        return countDocuments;
    }

    @Override
    public void drop() {
        mongoCollection.drop();
    }

    @Override
    public void insert( INosqlDocument document ) {
        Document mongoDocument = document.adapt(Document.class);
        mongoCollection.insertOne(mongoDocument);
    }

    @Override
    public void insert( String documentJson ) {
        Document mongoDocument = Document.parse(documentJson);
        mongoCollection.insertOne(mongoDocument);
    }

    public void deleteByOid( String oid ) {
        mongoCollection.deleteOne(Filters.eq("_id", new ObjectId(oid)));
    }

    public void updateByOid( String oid, String documentJson ) {
        mongoCollection.replaceOne(Filters.eq("_id", new ObjectId(oid)), Document.parse(documentJson));
    }

}
