// Import CSV time series into a SQLite database: create the tables, insert with prepared statements in batches, one transaction per file
// keywords: sqlite, database, db, import, csv, timeseries, time series, table, create table, insert, batch, prepared statement, transaction, commit, measurements
import org.hortonmachine.* // the HM helper

// a folder of csv files with lines as: station_id,2020-01-01 00:00:00,12.5
def csvFolder = "/path/to/csv/"
def dbPath = "/path/to/data.sqlite"

def db = HM.connectSqlite(dbPath)
try {
    db.executeInsertUpdateDeleteSql("""
        CREATE TABLE IF NOT EXISTS measurement (
            station_id INTEGER NOT NULL,
            ts INTEGER NOT NULL,
            value REAL NOT NULL,
            PRIMARY KEY (station_id, ts)
        )""")

    def insert = "INSERT INTO measurement (station_id, ts, value) VALUES (?, ?, ?)"
    def connection = db.getConnectionInternal()
    connection.enableAutocommit(false)
    (csvFolder as File).eachFile { file ->
        if (!file.name.endsWith(".csv")) {
            return
        }
        def rows = []
        file.eachLine { line ->
            def values = line.trim().split(",")
            if (values.length == 3 && values[0].isInteger()) {
                // timestamps as epoch milliseconds
                rows << ([values[0] as int, HM.str2ts(values[1]), values[2] as double] as Object[])
            }
        }
        if (rows) {
            def inserted = db.executeBatchPreparedSql(insert, rows)
            connection.commit()
            println "Inserted ${inserted.length} rows from " + file.name
        }
    }
    connection.enableAutocommit(true)
    println "Rows in the table: " + db.getCount("measurement")
} finally {
    db.close()
}
