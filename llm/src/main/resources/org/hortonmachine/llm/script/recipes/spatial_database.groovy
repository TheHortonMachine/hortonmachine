// Work with a spatial database (GeoPackage, SpatiaLite, PostGIS, H2GIS): info, tables, columns, counts, geometry info, geometries, records, sql
// keywords: database, db, geopackage, gpkg, spatialite, sqlite, postgis, postgres, postgresql, h2gis, connect, tables, list tables, layers, columns, spatial, geometry type, geometry column, srid, crs, count, records, rows, query, select, sql, update, geometries
import org.hortonmachine.* // the HM helper

// connect with the HM method of the database type: they all give the same api used below
def db = HM.connectGeopackage("/path/to/data.gpkg")
// def db = HM.connectSpatialite("/path/to/data.sqlite")
// def db = HM.connectH2GIS("/path/to/data", "user", "password")
// def db = HM.connectPostgis("localhost", 5432, "database", "user", "password")
try {
    println db.getDbInfo().join("\n")

    // the tables of the data, without the internal ones of the database
    def tables = db.getTables()
    tables.each { table ->
        def name = table.toSqlName()
        def count = db.getCount(name)
        if (db.isTableSpatial(name)) {
            def geometryColumn = db.getGeometryColumnsForTable(name)
            println "${table.name}: spatial, ${geometryColumn.geometryType} in column ${geometryColumn.geometryColumnName}, " +
                    "EPSG:${geometryColumn.srid}, ${count} records"
        } else {
            println "${table.name}: not spatial, ${count} records"
        }
        // the columns, as name, type and 1 if primary key
        db.getTableColumns(name).each { column ->
            println "    ${column[0]} ${column[1]}${column[2] == '1' ? ' (primary key)' : ''}"
        }
    }

    def spatialTable = tables.find { db.isTableSpatial(it.toSqlName()) }
    if (spatialTable) {
        def name = spatialTable.toSqlName()
        // the geometries of a table, as JTS geometries
        def geometries = db.getGeometries(name)
        println "${spatialTable.name}: ${geometries.size()} geometries, the first has ${geometries[0]?.numPoints} points"

        // records from sql, the first 5, without the geometry; fixedDoubleName is the quoted name for sql
        def result = db.getTableRecordsMapFromRawSql("SELECT * FROM " + name.fixedDoubleName, 5)
        result.data.each { row ->
            def values = []
            result.names.eachWithIndex { column, i ->
                if (i != result.geometryIndex) {
                    values << "${column}=${row[i]}"
                }
            }
            println values.join(", ")
        }
    }

    // insert, update and delete statements, returning the number of changed records
    // db.executeInsertUpdateDeleteSql("UPDATE mytable SET name = 'new' WHERE id = 1")
} finally {
    db.close()
}
