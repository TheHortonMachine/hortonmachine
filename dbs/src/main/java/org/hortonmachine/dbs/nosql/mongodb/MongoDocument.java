package org.hortonmachine.dbs.nosql.mongodb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.hortonmachine.dbs.nosql.INosqlDocument;
import org.json.JSONObject;

public class MongoDocument implements INosqlDocument {

    private Document document;

    public MongoDocument( Document document ) {
        this.document = document;
    }

    public MongoDocument( String json ) {
        this.document = Document.parse(json);
    }

    public String toJson() {
        JsonWriterSettings settings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
        String json = document.toJson(settings);
        JSONObject obj = new JSONObject(json);
        String formatted = obj.toString(2);
        return formatted;
    }

    public List<String[]> getFirstLevelKeysAndTypes() {
        List<String[]> list = new ArrayList<>();

        for( Entry<String, Object> entry : document.entrySet() ) {
            Object value = entry.getValue();
            String simpleName = value.getClass().getSimpleName();
            if (simpleName.equals("ObjectId")) {
                simpleName = "OID";
            } else if (simpleName.equals("ArrayList")) {
                simpleName = "Object";
            }

            list.add(new String[]{entry.getKey(), simpleName});

        }

        return list;
    }

    public LinkedHashMap<String, Object> getSchema() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        handleObject(map, document);

        return map;
    }

    @SuppressWarnings("rawtypes")
    private void handleObject( LinkedHashMap<String, Object> map, Object object ) {
        if (object instanceof Document) {
            Document document = (Document) object;
            for( Entry<String, Object> entry : document.entrySet() ) {
                Object value = entry.getValue();
                String key = entry.getKey();
                if (value instanceof List) {
                    List list = (List) value;
                    if (!list.isEmpty()) {
                        Object firstObject = list.get(0);
                        if (firstObject instanceof Document) {
                            LinkedHashMap<String, Object> submap = new LinkedHashMap<>();
                            map.put(key, submap);
                            handleObject(submap, firstObject);
                        } else {
                            map.put(key, "List of " + firstObject.getClass().getSimpleName());
                        }
                    } else {
                        map.put(key, "List");
                    }
                } else if (value instanceof Document) {
                    LinkedHashMap<String, Object> submap = new LinkedHashMap<>();
                    map.put(key, submap);
                    handleObject(submap, new MongoDocument((Document) value));
                } else {
                    String simpleName = value.getClass().getSimpleName();
                    if (simpleName.equals("ObjectId")) {
                        simpleName = "OID";
                    }
                    map.put(key, simpleName);
                }

            }
        }
    }

    @Override
    public <T> T adapt( Class<T> adaptee ) {
        if (adaptee.isAssignableFrom(Document.class)) {
            return adaptee.cast(document);
        }
        return null;
    }

}
