// Import a shapefile into a table of a PostGIS database, only if the table is empty, then reproject its geometries with SQL
// keywords: postgis, postgres, postgresql, database, db, import, shapefile, table, load, upload, reproject, st_transform, sql
import org.hortonmachine.* // the HM helper
import org.hortonmachine.dbs.utils.SqlName
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils

// the connection: never write real passwords in shared scripts
def host = "localhost"
def port = 5432
def database = "database"
def user = "user"
def password = "password"
def shapefile = "/path/to/data.shp" as File
def table = SqlName.m("districts")

def db = HM.connectPostgis(host, port, database, user, password)
try {
    if (db.hasTable(table) && db.getCount(table) > 0) {
        println "The table " + table + " is not empty: nothing imported."
    } else {
        // -1 imports all the features
        SpatialDbsImportUtils.importShapefile(db, shapefile, table, -1, true, new LogProgressMonitor())
        println "Imported ${db.getCount(table)} records."
        db.executeInsertUpdateDeleteSql("ALTER TABLE " + table.fixedName
                + " ALTER COLUMN the_geom TYPE Geometry(MultiPolygon, 4326) USING ST_Transform(the_geom, 4326)")
        println "Reprojected to EPSG:4326."
    }
} finally {
    db.close()
}
