/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.geoscript.console;

import java.util.LinkedHashMap;

/**
 * The imports and example scripts offered by the HM menu of the console.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ConsoleExamples {

    public static final String HM_IMPORTS = """
            import org.hortonmachine.* // hortonmachine support
            import org.hortonmachine.modules.* // hortonmachine processing modules
            """;

    public static final String GEOSCRIPT_IMPORTS = """
            import geoscript.geom.* // handles geometries objects
            import geoscript.proj.* // handles projections
            import geoscript.render.* // handles rendering and plotting
            import geoscript.layer.* // enables layer management
            import geoscript.style.* // enables tools to work with style
            import geoscript.viewer.* // handles various viewers
            import geoscript.filter.* // the package that works with filters
            import geoscript.workspace.* // enables workspace handling
            """;

    private ConsoleExamples() {
    }

    /**
     * @return the examples, by name.
     */
    public static LinkedHashMap<String, String> getExamples() {
        LinkedHashMap<String, String> examples = new LinkedHashMap<>();
        examples.put("Add prj to files in folder", """
                import org.hortonmachine.gears.modules.utils.fileiterator.*

                def folder = "..."
                def epsg = "EPSG:..."

                OmsFileIterator.addPrj(folder, epsg)
                """);
        examples.put("Create/render geometries", """
                // example geometries creation
                def g1 = [[[0, 0], [0, 5], [5, 5], [5, 0], [0, 0]]] as Polygon
                def g2 = [[[5, 0], [5, 2], [7, 2], [7, 0], [5, 0]]] as Polygon
                def g3 = [4, 1] as Point
                def g4 = [5, 4] as Point
                def g5 = [[1, 0], [1, 6]] as LineString
                def g6 = [[[3, 3], [3, 6], [6, 6], [6, 3], [3, 3]]] as Polygon

                def geomsList = [g1,g2,g3,g4,g5,g6]

                // view the geometries as an image
                HM.toImage(geomsList)
                """);
        examples.put("Work with a database", """
                def dbHost = "localhost"
                def user = "..."
                def dbName = "..."
                def pwd = "..."

                def db = HM.connectPostgis( dbHost, 5432, dbName, user, pwd )

                // db info
                println db.getDbInfo()

                // get list of tables
                def tables = db.getTables()
                println "List of tables:"
                tables.each{ t ->
                    println "\\t --> " + t.name
                }

                // table columns
                def columns = db.getTableColumns(tables[0].toSqlName())
                println "Columns of table " + tables[0].name + ":"
                for (c in columns){
                    println "\\t--> name: " + c[0] + ", type: " + c[1] + ", is pk: " + (c[2]=="1"?"yes":"no")
                }

                // records count
                def count = db.getCount(tables[0].toSqlName())
                println "Count for table: " + tables[0].name + " = " + count

                // is spatial?
                println "Is table: " +  tables[0].name + " spatial? -> " + db.isTableSpatial(tables[0].toSqlName())

                // get geometry info
                def gc = db.getGeometryColumnsForTable(tables[0].toSqlName())
                println "Geometry info for table: " + tables[0].name
                println gc

                // get the geometries in the table
                println "Get geometries table: " + tables[0].name
                def geoms = db.getGeometries(tables[0].toSqlName())
                println "Got " + geoms.size() + " geometries."
                println "First bit: " + geoms[0].toText().substring(0, 50) + "..."

                // execute update
                db.executeInsertUpdateDeleteSql("...")

                // read table
                def sql = "select * from " + tables[0].name
                def limit = 1
                def result = db.getTableRecordsMapFromRawSql(sql, limit)
                def colSize = result.names.size()-1
                println "Result of '" + sql + "' without geometry:"
                for (i in 0..colSize){
                    if ( result.names[i] != gc.geometryColumnName )
                        println result.names[i] + " = " + result.data[0][i]
                }

                db.close()
                """);
        examples.put("Print raster cell info", """
                // example values
                def dtm = "..."
                def raster = "..."
                def col = 100
                def row = 100
                def x = 1638625.0
                def y = 5112546.9
                int cellBuffer = 3

                // print the values of a cell with surrounding cells on a set of rasters
                println HM.getCellInfo(col, row, cellBuffer, raster, raster)

                // render the styled image of the dtm, with values and row/col
                HM.toImage(col, row, cellBuffer, raster, 600, 600 )

                // adding a dtm will also show the pits and steepest directions
                HM.toImage(col, row, cellBuffer, raster, dtm, 600, 600 )

                // this also works with world coords (larger buffer)
                HM.toImage(x, y, cellBuffer*10, raster, dtm, 2600, 2600 )
                """);
        examples.put("Extract stream network from dtm", """
                // check possible colortable names
                // println HM.printColorTables()

                def dtm = "..."
                def pit = "..."
                def flow = "..."
                def drain = "..."
                def tca = "..."
                def net = "..."
                def thres = 100.0

                def pitfiller = new Pitfiller()
                pitfiller.inElev = dtm
                pitfiller.outPit = pit
                pitfiller.process()

                def flowdirections = new FlowDirections()
                flowdirections.inPit = pit
                flowdirections.pMinElev = 0
                flowdirections.outFlow = flow
                flowdirections.process()

                def draindir = new DrainDir()
                draindir.inPit = pit
                draindir.inFlow = flow
                draindir.pLambda = 1.0
                draindir.doLad = true
                draindir.outFlow = drain
                draindir.outTca = tca
                draindir.process()

                def extractnetwork = new ExtractNetwork()
                extractnetwork.inTca = tca
                extractnetwork.inFlow = flow
                extractnetwork.pThres = thres
                extractnetwork.outNet = net
                extractnetwork.process()

                // create colortables for QGIS
                HM.makeQgisStyleForRaster("flow", flow, 0 )
                HM.makeQgisStyleForRaster("flow", drain, 0 )
                HM.makeQgisStyleForRaster("net", net, 0 )
                // tca is better seen in logarithmic scale
                HM.makeQgisStyleForRaster("logarithmic", tca, 0 )

                // but let's also see it in the output
                Map map = new Map( width: 1200, height: 1200)
                def raster = Format.getFormat(dtm).read()
                raster.style = HM.styleForColorTable('elev', 846, 2150, 1.0)
                map.addLayer(raster)
                raster = Format.getFormat(net).read()
                raster.style = HM.styleForColorTable('net', 0, 1, 1.0)
                map.addLayer(raster)
                map.display()
                """);
        return examples;
    }
}
