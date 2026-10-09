import org.hortonmachine.dbs.compat.EDb
import org.hortonmachine.gears.utils.files.FileUtilities
import org.hortonmachine.modules.SaintGeo

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a flood wave entering at the head of the river: from 50 to 400 m3/s and back in 2 hours,
// then 2 hours at 50 m3/s, one value for each step of the model
var csv = new StringBuilder()
csv.append("@T,discharge\n")
csv.append("@H,timestamp,discharge\n")
csv.append("Type,Date,Double\n")
csv.append("Format,yyyy-MM-dd HH:mm,\n")
var steps = 2880
for (int i = 0; i < steps; i++) {
    var q = i < 1440 ? 50 + 350 * Math.pow(Math.sin(Math.PI * i / 1440), 2) : 50.0
    // the timestamps only give the order of the values: the model sets its own time step
    var time = String.format("2026-01-%02d %02d:%02d", 1 + (int) (i / 1440), (int) (i / 60) % 24, i % 60)
    csv.append(String.format(Locale.ROOT, ",%s,%.3f%n", time, q))
}
FileUtilities.writeFile(csv.toString(), new File(folder + "river_discharge.csv"))

var saint = new SaintGeo()
saint.inRiverPoints = folder + "river_points.shp"
saint.inSections = folder + "river_sections.shp"
saint.inSectionPoints = folder + "river_section_points.shp"
saint.inDischarge = folder + "river_discharge.csv"
saint.pKs = 30.0
saint.outGpkg = folder + "saintgeo.gpkg"
saint.outTableName = "flood"
saint.process()

// two tables of the results, for the charts
var db = EDb.GEOPACKAGE.getSpatialDb()
db.open(folder + "saintgeo.gpkg")
// along the river: the bottom, the level of the steady flow at the start and the highest level
db.runRawSqlToCsv("""
    select progressivedistance as distance, min(minsectionelevation) as bottom,
           min(case when timestep = 0 then waterlevel end) as start_level, max(waterlevel) as max_level
    from flood group by sectionid order by distance
    """, new File(folder + "flood_levels.csv"), true, ",")
// in time: the discharge at the head and at the end of the river
db.runRawSqlToCsv("""
    select timestep as step,
           max(case when progressivedistance = (select min(progressivedistance) from flood) then discharge end) as head,
           max(case when progressivedistance = (select max(progressivedistance) from flood) then discharge end) as outlet
    from flood group by timestep order by timestep
    """, new File(folder + "flood_discharge.csv"), true, ",")
db.close()
