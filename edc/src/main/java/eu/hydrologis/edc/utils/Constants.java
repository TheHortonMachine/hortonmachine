package eu.hydrologis.edc.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Table;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.hydrologis.edc.annotatedclasses.BasinTypeTable;
import eu.hydrologis.edc.annotatedclasses.DataSourceAccountTable;
import eu.hydrologis.edc.annotatedclasses.DataSourceTable;
import eu.hydrologis.edc.annotatedclasses.DataTypeTable;
import eu.hydrologis.edc.annotatedclasses.DmvTable;
import eu.hydrologis.edc.annotatedclasses.DynamicMonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.GeologyCategoriesTable;
import eu.hydrologis.edc.annotatedclasses.GeologyMapTable;
import eu.hydrologis.edc.annotatedclasses.GeologyParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.GeologyParametersTable;
import eu.hydrologis.edc.annotatedclasses.HydrometersDischargeScalesTable;
import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.IntakesTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverCategoriesTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverMapTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverParametersTable;
import eu.hydrologis.edc.annotatedclasses.LandslideBasinRelationTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesClassificationsTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesTable;
import eu.hydrologis.edc.annotatedclasses.LevelsTable;
import eu.hydrologis.edc.annotatedclasses.MapTypeTable;
import eu.hydrologis.edc.annotatedclasses.MeasuresTable;
import eu.hydrologis.edc.annotatedclasses.MeteoMapTable;
import eu.hydrologis.edc.annotatedclasses.ModelsTable;
import eu.hydrologis.edc.annotatedclasses.MonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.MorphologyMapTable;
import eu.hydrologis.edc.annotatedclasses.OfftakesPermissionsTable;
import eu.hydrologis.edc.annotatedclasses.OfftakesTable;
import eu.hydrologis.edc.annotatedclasses.ParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.ParametersTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsControlLevelsTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsDischargeTable;
import eu.hydrologis.edc.annotatedclasses.PermissionsUsageTable;
import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.annotatedclasses.PointTypeTable;
import eu.hydrologis.edc.annotatedclasses.ProcessesTable;
import eu.hydrologis.edc.annotatedclasses.RamaddaTable;
import eu.hydrologis.edc.annotatedclasses.ReliabilityTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsDischargeScalesTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsPermissionsTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsVolumesScalesTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.annotatedclasses.ScaleTypeTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeCategoriesTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeMapTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeParametersSetTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeParametersTable;
import eu.hydrologis.edc.annotatedclasses.StatusTable;
import eu.hydrologis.edc.annotatedclasses.SurveysTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.annotatedclasses.UsersTable;
import eu.hydrologis.edc.annotatedclasses.ValueDescriptionTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesHydrometersTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesIntakesTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesOfftakesTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesReservoirsTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.historical.SeriesHistoricalHydrometersTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.historical.SeriesHistoricalPrecipitationTable;

/**
 * Constants used in the EDC.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings({"unchecked", "nls"})
public class Constants {

    public static int start = 1990;
    public static int end = 2015;

    public static final String CSV_COMMENT = "#";
    public static final String CSV_SEPARATOR = ";";

    public static final String H2 = "H2";
    public static final String H2_DRIVER = "org.h2.Driver";
    public static final String H2_DIALECT = "org.hibernate.dialect.H2Dialect";

    public static final String POSTGRESQL = "POSTGRESQL";
    public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String POSTGRESQL_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";

    public static final String TYPE = "TYPE";
    public static final String DATABASE = "DATABASE";
    public static final String HOST = "HOST";
    public static final String PORT = "PORT";
    public static final String USER = "USER";
    public static final String PASS = "PASS";
    public static final String SHOW_SQL = "SHOW_SQL";
    public static final String FORMAT_SQL = "FORMAT_SQL";

    public static final String RAMADDA_HOST = "RAMADDA_HOST";
    public static final String RAMADDA_PORT = "RAMADDA_PORT";
    public static final String RAMADDA_BASE = "RAMADDA_BASE";
    public static final String RAMADDA_USER = "RAMADDA_USER";
    public static final String RAMADDA_PASS = "RAMADDA_PASS";
    public static final String RAMADDA_DUMPTREE = "RAMADDA_DUMPTREE";
    public static final String RAMADDA_FINDBYID = "RAMADDA_FINDBYID";
    public static final String RAMADDA_FINDBYNAME = "RAMADDA_FINDBYNAME";
    public static final String RAMADDA_FILEUPLOAD = "RAMADDA_FILEUPLOAD";
    public static final String RAMADDA_FILEUPLOADPARENTID = "RAMADDA_FILEUPLOADPARENTID";
    public static final String RAMADDA_FILEDOWNLOADID = "RAMADDA_FILEDOWNLOADID";
    public static final String RAMADDA_FILEDOWNLOADFOLDER = "RAMADDA_FILEDOWNLOADFOLDER";
    public static final String RAMADDA_NEWGROUP = "RAMADDA_NEWGROUP";
    public static final String RAMADDA_NEWGROUPPARENTID = "RAMADDA_NEWGROUPPARENTID";
    public static final String RAMADDA_OPENDAPBYID = "RAMADDA_OPENDAPBYID";

    public static final String GENERATEDB = "GENERATEDB";
    public static final String DUMPTABLESDEFINITIONS = "DUMPTABLESDEFINITIONS";

    public static final String INSERT_TABLE = "INSERT_TABLE";
    public static final String INSERT_FILE = "INSERT_FILE";
    public static final String INSERT_FOLDER = "INSERT_FOLDER";

    public static final String QUERYSERIES_DB = "QUERYSERIES_DB";
    public static final String QUERYSERIES_TABLE = "QUERYSERIES_TABLE";
    public static final String QUERYSERIES_START = "QUERYSERIES_START";
    public static final String QUERYSERIES_END = "QUERYSERIES_END";
    public static final String QUERYSERIES_MONPOINTIDS = "QUERYSERIES_MONPOINTIDS";

    public static final String UPLOADGEOMETRYTYPE = "UPLOADGEOMETRYTYPE";
    public static final String UPLOADGEOMETRYFILE = "UPLOADGEOMETRYFILE";
    public static final String UPLOADGEOMETRYSCHEMA = "UPLOADGEOMETRYSCHEMA";
    public static final String UPLOADGEOMETRYTABLE = "UPLOADGEOMETRYTABLE";

    public static final String PRINTGEOMETRYSCHEMA = "PRINTGEOMETRYSCHEMA";
    public static final String PRINTGEOMETRYTABLE = "PRINTGEOMETRYTABLE";
    public static final String PRINTGEOMETRYID = "PRINTGEOMETRYID";
    public static final String PRINTGEOMETRYEPSG = "PRINTGEOMETRYEPSG";
    public static final String PRINTGEOMETRYHAS3D = "PRINTGEOMETRYHAS3D";

    public static String utcDateFormatterYYYYMMDDHHMMSS_string = "yyyy-MM-dd HH:mm:ss";
    public static DateTimeFormatter utcDateFormatterYYYYMMDDHHMMSS = DateTimeFormat.forPattern(
            utcDateFormatterYYYYMMDDHHMMSS_string).withZone(DateTimeZone.UTC);
    public static DateTimeFormatter dateFormatterYYYYMMDDHHMMSS = DateTimeFormat
            .forPattern(utcDateFormatterYYYYMMDDHHMMSS_string);
    public static String utcDateFormatterYYYYMMDDHHMM_string = "yyyy-MM-dd HH:mm";
    public static DateTimeFormatter utcDateFormatterYYYYMMDDHHMM = DateTimeFormat.forPattern(
            utcDateFormatterYYYYMMDDHHMM_string).withZone(DateTimeZone.UTC);
    public static DateTimeFormatter dateFormatterYYYYMMDDHHMM = DateTimeFormat
            .forPattern(utcDateFormatterYYYYMMDDHHMM_string);

    /*
     * TABLES AND SCHEMA NAMES (in alphabetic order)
     */
    public static final String EDC_SCHEMA = "edc";
    public static final String EDC_SCHEMA_UPPERCASE = EDC_SCHEMA.toUpperCase();
    public static final String EDCSERIES_SCHEMA = "edcseries";
    public static final String EDCSERIES_SCHEMA_UPPERCASE = EDCSERIES_SCHEMA.toUpperCase();
    public static final String EDCGEOMETRIES_SCHEMA = "edcgeometries";
    public static final String EDCGEOMETRIES_SCHEMA_UPPERCASE = EDCGEOMETRIES_SCHEMA.toUpperCase();
    public static final String[] SCHEMAS = {EDC_SCHEMA, EDCSERIES_SCHEMA, EDCGEOMETRIES_SCHEMA};
    public static final String ANNOTATEDCLASSES = "annotatedclasses";
    public static final String ANNOTATEDCLASSESDAOS = "annotatedclassesdaos";
    public static final String TABLE = "Table";
    public static final String DAO = "Dao";

    // edc tables
    public static final String BASINTYPE = "basintype";
    public static final String DATASOURCEACCOUNT = "datasourceaccount";
    public static final String DATASOURCE = "datasource";
    public static final String DATATYPE = "datatype";
    public static final String MORPHOLOGYMAP = "morphologymap";
    public static final String DYNAMICMONITORINGPOINTS = "dynamicmonitoringpoints";
    public static final String DMV = "dmv";
    public static final String GEOLOGYCATEGORIES = "geologycategories";
    public static final String GEOLOGYPARAMETERS = "geologyparameters";
    public static final String GEOLOGYPARAMETERSSET = "geologyparametersset";
    public static final String GEOLOGYMAP = "geologymap";
    public static final String HYDROMETERSDISCHARGESCALES = "hydrometersdischargescales";
    public static final String HYDROMETERS = "hydrometers";
    public static final String INTAKES = "intakes";
    public static final String LANDCOVERCATEGORIES = "landcovercategories";
    public static final String LANDCOVERPARAMETERS = "landcoverparameters";
    public static final String LANDCOVERPARAMETERSSET = "landcoverparametersset";
    public static final String LANDCOVERMAP = "landcovermap";
    public static final String LANDSLIDEBASINRELATION = "landslidebasinrelation";
    public static final String LANDSLIDES = "landslides";
    public static final String LANDSLIDESCLASSIFICATIONS = "landslidesclassifications";
    public static final String LANDSLIDESGEOMETRIES = "landslidesgeometries";
    public static final String LEVELS = "levels";
    public static final String MAPTYPE = "maptype";
    public static final String MEASURES = "measures";
    public static final String METEOMAP = "meteomap";
    public static final String MONITORINGPOINTS = "monitoringpoints";
    public static final String MODELS = "models";
    public static final String OBSTRUCTIONGEOMETRIES = "obstructiongeometries";
    public static final String OFFTAKES = "offtakes";
    public static final String OFFTAKES_PERMISSIONS = "offtakespermissions";
    public static final String PARAMETERS = "parameters";
    public static final String PARAMETERSSET = "parametersset";
    public static final String PERMISSIONSUSAGE = "permissionsusage";
    public static final String PERMISSIONSCONTROLLEVEL = "permissionscontrollevel";
    public static final String PERMISSIONSDISCHARGE = "permissionsdischarge";
    public static final String PROCESSES = "processes";
    public static final String POI = "poi";
    public static final String POIGEOMETRIES = "poigeometries";
    public static final String POINTTYPE = "pointtype";
    public static final String RAMADDA = "ramadda";
    public static final String RELIABILITY = "reliability";
    public static final String RESERVOIRS = "reservoirs";
    public static final String RESERVOIRS_PERMISSIONS = "reservoirspermissions";
    public static final String RESERVOIRSDISCHARGESCALES = "reservoirsdischargescales";
    public static final String RESERVOIRSVOLUMESSCALES = "reservoirsvolumesscales";
    public static final String RUNS = "runs";
    public static final String SCALETYPE = "scaletype";
    public static final String SOILTYPECATEGORIES = "soiltypecategories";
    public static final String SOILTYPEPARAMETERS = "soiltypeparameters";
    public static final String SOILTYPEPARAMETERSSET = "soiltypeparametersset";
    public static final String SOILTYPEMAP = "soiltypemap";
    public static final String STATUS = "status";
    public static final String SURVEYS = "surveys";
    public static final String UNITS = "units";
    public static final String USERS = "users";
    public static final String VALUEDESCRIPTION = "valuedescription";

    // edcseries tables
    public static final String SERIES_HISTORICALHYDROMETERS = "series_historicalhydrometers";
    public static final String SERIES_HISTORICALPRECIPITATION = "series_historicalprecipitation";
    public static final String SERIES_HYDROMETERS = "series_hydrometers";
    public static final String SERIES_OFFTAKES = "series_offtakes";
    public static final String SERIES_INTAKES = "series_intakes";
    public static final String SERIES_RESERVOIRS = "series_reservoirs";

    // edcseries tables split
    public static final String SERIES_PRECIPITATIONS = "series_precipitations_";
    public static final String SERIES_PRESSURE = "series_pressure_";
    public static final String SERIES_RELATIVEHUMIDITY = "series_relativehumidity_";
    public static final String SERIES_SNOWDEPTH = "series_snowdepth_";
    public static final String SERIES_TEMPERATURE = "series_temperature_";
    public static final String SERIES_WINDDIRECTION = "series_winddirection_";
    public static final String SERIES_WINDSPEED = "series_windspeed_";

    private static final Map<String, Class> edcTables2ClassMap = new HashMap<String, Class>();
    static {
        edcTables2ClassMap.put(BASINTYPE, BasinTypeTable.class);
        edcTables2ClassMap.put(DATASOURCEACCOUNT, DataSourceAccountTable.class);
        edcTables2ClassMap.put(DATASOURCE, DataSourceTable.class);
        edcTables2ClassMap.put(DATATYPE, DataTypeTable.class);
        edcTables2ClassMap.put(MORPHOLOGYMAP, MorphologyMapTable.class);
        edcTables2ClassMap.put(DYNAMICMONITORINGPOINTS, DynamicMonitoringPointsTable.class);
        edcTables2ClassMap.put(DMV, DmvTable.class);
        edcTables2ClassMap.put(GEOLOGYCATEGORIES, GeologyCategoriesTable.class);
        edcTables2ClassMap.put(GEOLOGYPARAMETERS, GeologyParametersTable.class);
        edcTables2ClassMap.put(GEOLOGYPARAMETERSSET, GeologyParametersSetTable.class);
        edcTables2ClassMap.put(GEOLOGYMAP, GeologyMapTable.class);
        edcTables2ClassMap.put(HYDROMETERSDISCHARGESCALES, HydrometersDischargeScalesTable.class);
        edcTables2ClassMap.put(HYDROMETERS, HydrometersTable.class);
        edcTables2ClassMap.put(INTAKES, IntakesTable.class);
        edcTables2ClassMap.put(LANDCOVERCATEGORIES, LandcoverCategoriesTable.class);
        edcTables2ClassMap.put(LANDCOVERPARAMETERS, LandcoverParametersTable.class);
        edcTables2ClassMap.put(LANDCOVERPARAMETERSSET, LandcoverParametersSetTable.class);
        edcTables2ClassMap.put(LANDCOVERMAP, LandcoverMapTable.class);
        edcTables2ClassMap.put(LANDSLIDEBASINRELATION, LandslideBasinRelationTable.class);
        edcTables2ClassMap.put(LANDSLIDES, LandslidesTable.class);
        edcTables2ClassMap.put(LANDSLIDESCLASSIFICATIONS, LandslidesClassificationsTable.class);
        edcTables2ClassMap.put(LEVELS, LevelsTable.class);
        edcTables2ClassMap.put(MAPTYPE, MapTypeTable.class);
        edcTables2ClassMap.put(MEASURES, MeasuresTable.class);
        edcTables2ClassMap.put(METEOMAP, MeteoMapTable.class);
        edcTables2ClassMap.put(MONITORINGPOINTS, MonitoringPointsTable.class);
        edcTables2ClassMap.put(MODELS, ModelsTable.class);
        edcTables2ClassMap.put(OFFTAKES, OfftakesTable.class);
        edcTables2ClassMap.put(PARAMETERS, ParametersTable.class);
        edcTables2ClassMap.put(PARAMETERSSET, ParametersSetTable.class);
        edcTables2ClassMap.put(PERMISSIONSCONTROLLEVEL, PermissionsControlLevelsTable.class);
        edcTables2ClassMap.put(PERMISSIONSDISCHARGE, PermissionsDischargeTable.class);
        edcTables2ClassMap.put(PROCESSES, ProcessesTable.class);
        edcTables2ClassMap.put(OFFTAKES_PERMISSIONS, OfftakesPermissionsTable.class);
        edcTables2ClassMap.put(RESERVOIRS_PERMISSIONS, ReservoirsPermissionsTable.class);
        edcTables2ClassMap.put(PERMISSIONSUSAGE, PermissionsUsageTable.class);
        edcTables2ClassMap.put(POI, PoiTable.class);
        edcTables2ClassMap.put(POINTTYPE, PointTypeTable.class);
        edcTables2ClassMap.put(RAMADDA, RamaddaTable.class);
        edcTables2ClassMap.put(RELIABILITY, ReliabilityTable.class);
        edcTables2ClassMap.put(RESERVOIRSDISCHARGESCALES, ReservoirsDischargeScalesTable.class);
        edcTables2ClassMap.put(RESERVOIRS, ReservoirsTable.class);
        edcTables2ClassMap.put(RESERVOIRSVOLUMESSCALES, ReservoirsVolumesScalesTable.class);
        edcTables2ClassMap.put(RUNS, RunsTable.class);
        edcTables2ClassMap.put(SCALETYPE, ScaleTypeTable.class);
        edcTables2ClassMap.put(SOILTYPECATEGORIES, SoilTypeCategoriesTable.class);
        edcTables2ClassMap.put(SOILTYPEPARAMETERS, SoilTypeParametersTable.class);
        edcTables2ClassMap.put(SOILTYPEPARAMETERSSET, SoilTypeParametersSetTable.class);
        edcTables2ClassMap.put(SOILTYPEMAP, SoilTypeMapTable.class);
        edcTables2ClassMap.put(STATUS, StatusTable.class);
        edcTables2ClassMap.put(SURVEYS, SurveysTable.class);
        edcTables2ClassMap.put(UNITS, UnitsTable.class);
        edcTables2ClassMap.put(USERS, UsersTable.class);
        edcTables2ClassMap.put(VALUEDESCRIPTION, ValueDescriptionTable.class);
        edcTables2ClassMap
                .put(SERIES_HISTORICALHYDROMETERS, SeriesHistoricalHydrometersTable.class);
        edcTables2ClassMap.put(SERIES_HISTORICALPRECIPITATION,
                SeriesHistoricalPrecipitationTable.class);
        edcTables2ClassMap.put(SERIES_HYDROMETERS, SeriesHydrometersTable.class);
        edcTables2ClassMap.put(SERIES_INTAKES, SeriesIntakesTable.class);
        edcTables2ClassMap.put(SERIES_OFFTAKES, SeriesOfftakesTable.class);
        edcTables2ClassMap.put(SERIES_RESERVOIRS, SeriesReservoirsTable.class);
    }

    /*
     * COLUMN NAMES
     */
    // ids
    public static final String ID = "ID";
    public static final String BASINTYPE_ID = "BASINTYPE_ID";
    public static final String DATASOURCE_ID = "DATASOURCE_ID";
    public static final String DISCHARGEUNIT_ID = "DISCHARGEUNIT_ID";
    public static final String DATASOURCEACCOUNT_ID = "DATASOURCEACCOUNT_ID";
    public static final String DATATYPE_ID = "DATATYPE_ID";
    public static final String MORPHOLOGYMAP_ID = "MORPHOLOGYMAP_ID";
    public static final String DYNAMICMONITORINGPOINTS_ID = "DYNAMICMONITORINGPOINTS_ID";
    public static final String DMV_ID = "DMV_ID";
    public static final String GEOLOGYMAP_ID = "GEOLOGYMAP_ID";
    public static final String GEOLOGYCATEGORIES_ID = "GEOLOGYCATEGORIES_ID";
    public static final String GEOLOGYPARAMETERS_ID = "GEOLOGYPARAMETERS_ID";
    public static final String GEOLOGYPARAMETERSSET_ID = "GEOLOGYPARAMETERSSET_ID";
    public static final String HYDROMETER_ID = "HYDROMETER_ID";
    public static final String INTAKES_ID = "INTAKES_ID";
    public static final String LANDCOVERMAP_ID = "LANDCOVERMAP_ID";
    public static final String LANDCOVERCATEGORIES_ID = "LANDCOVERCATEGORIES_ID";
    public static final String LANDCOVERPARAMETERS_ID = "LANDCOVERPARAMETERS_ID";
    public static final String LANDCOVERPARAMETERSSET_ID = "LANDCOVERPARAMETERSSET_ID";
    public static final String LANDSLIDEBASINRELATION_ID = "LANDSLIDEBASINRELATION_ID";
    public static final String LANDSLIDESCLASSIFICATIONS_ID = "LANDSLIDESCLASSIFICATIONS_ID";
    public static final String LANDSLIDES_ID = "LANDSLIDES_ID";
    public static final String LEVELUNIT_ID = "LEVELUNIT_ID";
    public static final String LEVELS_ID = "LEVELS_ID";
    public static final String MAPTYPE_ID = "MAPTYPE_ID";
    public static final String MEASURES_ID = "MEASURES_ID";
    public static final String METEOMAP_ID = "METEOMAP_ID";
    public static final String MONITORINGPOINTS_ID = "MONITORINGPOINTS_ID";
    public static final String MODELS_ID = "MODELS_ID";
    public static final String OBSTRUCTIONGEOMETRIES_ID = "OBSTRUCTIONGEOMETRIES_ID";
    public static final String OFFTAKES_ID = "OFFTAKES_ID";
    public static final String OID = "OID";
    public static final String ORDERING = "ORDERING";
    public static final String OUTLETDISCHARGEUNITS_ID = "OUTLETDISCHARGEUNITS_ID";
    public static final String OFFTAKES_PERMISSIONS_ID = "OFFTAKESPERMISSIONS_ID";
    public static final String PARAMETERS_ID = "PARAMETERS_ID";
    public static final String PARAMETERSSET_ID = "PARAMETERSSET_ID";
    public static final String PERMISSIONSCONTROLLEVEL_ID = "PERMISSIONSCONTROLLEVEL_ID";
    public static final String PERMISSIONSDISCHARGE_ID = "PERMISSIONSDISCHARGE_ID";
    public static final String PERMISSIONSUSAGE_ID = "PERMISSIONSUSAGE_ID";
    public static final String PROCESSES_ID = "PROCESSES_ID";
    public static final String POI_ID = "POI_ID";
    public static final String POINTTYPE_ID = "POINTTYPE_ID";
    public static final String RELIABILITY_ID = "RELIABILITY_ID";
    public static final String RESERVOIRS_ID = "RESERVOIRS_ID";
    public static final String RESERVOIRSPERMISSIONS_ID = "RESERVOIRSPERMISSIONS_ID";
    public static final String RUNS_ID = "RUNS_ID";
    public static final String PARENTRUNS_ID = "PARENTRUNS_ID";
    public static final String SCALETYPE_ID = "SCALETYPE_ID";
    public static final String SET_ID = "SET_ID";
    public static final String SOILTYPEMAP_ID = "SOILTYPEMAP_ID";
    public static final String SOILTYPECATEGORIES_ID = "SOILTYPECATEGORIES_ID";
    public static final String SOILTYPEPARAMETERS_ID = "SOILTYPEPARAMETER_ID";
    public static final String SOILTYPEPARAMETERSET_ID = "SOILTYPEPARAMETERSET_ID";
    public static final String STATUS_ID = "STATUS_ID";
    public static final String SURFACELEVELUNITS_ID = "SURFACELEVELUNITS_ID";
    public static final String SURVEYS_ID = "SURVEYS_ID";
    public static final String UNITS_ID = "UNITS_ID";
    public static final String USAGE_ID = "USAGE_ID";
    public static final String USERS_ID = "USERS_ID";
    public static final String VALUEDESCRIPTION_ID = "VALUEDESCRIPTION_ID";
    public static final String WATERLEVELUNITS_ID = "WATERLEVELUNITS_ID";
    public static final String WHIRLEDDISCHARGEUNITS_ID = "WHIRLEDDISCHARGEUNITS_ID";
    public static final String VOLUMEUNITS_ID = "VOLUMEUNITS_ID";

    // join tables
    public static final String RUNSTOPARAMETERS = "RUNSTOPARAMETERS";

    // time
    public static final String YEAR = "YEAR";
    public static final String MONTH = "MONTH";
    public static final String DAY = "DAY";
    public static final String TIMESTAMPUTC = "TIMESTAMPUTC";
    public static final String TIMESTEP = "TIMESTEP";
    public static final String CREATIONDATE = "CREATIONDATE";
    public static final String APPROVALDATE = "APPROVALDATE";
    public static final String SURVEYDATE = "SURVEYDATE";
    public static final String STARTDATE = "STARTDATE";
    public static final String ENDDATE = "ENDDATE";
    public static final String STARTMONTH = "STARTMONTH";
    public static final String STARTDAY = "STARTDAY";
    public static final String ENDMONTH = "ENDMONTH";
    public static final String ENDDAY = "ENDDAY";

    // others
    public static final String THE_GEOM = "the_geom";
    public static final String BASE = "BASE";
    public static final String PARENTID = "PARENTID";
    public static final String RESOLUTION = "RESOLUTION";
    public static final String DEFAULTVALUE = "DEFAULTVALUE";
    public static final String NORTH = "NORTH";
    public static final String SOUTH = "SOUTH";
    public static final String EAST = "EAST";
    public static final String WEST = "WEST";
    public static final String DENOMINATION = "DENOMINATION";
    public static final String FIELDNAME = "FIELDNAME";
    public static final String REPORTURL = "REPORTURL";
    public static final String ATTACHMENTURL = "ATTACHMENTURL";
    public static final String PROGRESSIVE = "PROGRESSIVE";
    public static final String DISTANCE = "DISTANCE";
    public static final String DEPTH = "DEPTH";
    public static final String PRIMARYMEASURE = "PRIMARYMEASURE";
    public static final String ACCESSORYMEASURE = "ACCESSORYMEASURE";
    public static final String EPSG = "EPSG";
    public static final String AREA = "AREA";
    public static final String CALIBRATION = "CALIBRATION";
    public static final String RATEDPOWER = "RATEDPOWER";
    public static final String ISBIGOFFTAKE = "ISBIGOFFTAKE";
    public static final String VALUE = "VALUE";
    public static final String WATERLEVEL = "WATERLEVEL";
    public static final String WHIRLEDDISCHARGE = "WHIRLEDDISCHARGE";
    public static final String OUTLETDISCHARGE = "OUTLETDISCHARGE";
    public static final String LEVEL = "LEVEL";
    public static final String DISCHARGE = "DISCHARGE";
    public static final String LOGIN = "LOGIN";
    public static final String PASSWORD = "PASSWORD";
    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String CONTACT = "CONTACT";
    public static final String TITLE = "TITLE";
    public static final String INPUTSURL = "INPUTSURL";
    public static final String RESULTSURL = "RESULTSURL";
    public static final String URL = "URL";
    public static final String AGENCY = "AGENCY";
    public static final String CODE = "CODE";
    public static final String MUNICIPALITY = "MUNICIPALITY";
    public static final String PROVINCE = "PROVINCE";
    public static final String DISTRICT = "DISTRICT";
    public static final String RESORT = "RESORT";
    public static final String ELEVATION = "ELEVATION";
    public static final String SKY = "SKY";
    public static final String POSITION = "POSITION";
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String SILLLEVEL = "SILLLEVEL";
    public static final String AMAX = "AMAX";
    public static final String ADISCR = "ADISCR";
    public static final String HASSCALE = "HASSCALE";
    public static final String H_HS_MIN = "H_HS_MIN";
    public static final String H_HS_MAX = "H_HS_MAX";
    public static final String TOPDAMLEVEL = "TOPDAMLEVEL";
    public static final String CAPACITYLEVEL = "CAPACITYLEVEL";
    public static final String AVERAGEAREA = "AVERAGEAREA";
    public static final String TOTALVOLUME = "TOTALVOLUME";
    public static final String SURFACELEVEL = "SURFACELEVEL";
    public static final String VOLUME = "VOLUME";
    public static final String TOPRINCIPAL = "TOPRINCIPAL";
    public static final String FIRSTNAME = "FIRSTNAME";
    public static final String LASTNAME = "LASTNAME";
    public static final String USERNAME = "USERNAME";
    public static final String EMAIL = "EMAIL";
    public static final String MAXRANGE = "MAXRANGE";
    public static final String MINRANGE = "MINRANGE";
    public static final String MAXERROR = "MAXERROR";
    public static final String MINERROR = "MINERROR";
    public static final String PFAFFSTETTER = "PFAFFSTETTER";
    public static final String MEASUREMENTHEIGHT = "MEASUREMENTHEIGHT";
    public static final String MOVEMENTTYPE = "MOVEMENTTYPE";
    public static final String MATERIALTYPE = "MATERIALTYPE";
    public static final String Z = "Z";

    /*
     * SERIES GENERATION
     */

    // precipitations
    public static final String PRECIPITATIONS = "precipitations";
    public static final String PRECIPITATIONS_SERIES_NAME = "SeriesPrecipitations";
    public static final String PRECIPITATIONS_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.precipitations";
    public static final String PRECIPITATIONS_SERIES_TABLE_PREFIX = "series_precipitations_";
    public static final String PRECIPITATIONS_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/precipitations";
    // pressure
    public static final String PRESSURE = "pressure";
    public static final String PRESSURE_SERIES_NAME = "SeriesPressure";
    public static final String PRESSURE_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.pressure";
    public static final String PRESSURE_SERIES_TABLE_PREFIX = "series_pressure_";
    public static final String PRESSURE_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/pressure";
    // relativehumidity
    public static final String RELATIVEHUMIDITY = "relativehumidity";
    public static final String RELATIVEHUMIDITY_SERIES_NAME = "SeriesRelativehumidity";
    public static final String RELATIVEHUMIDITY_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.relativehumidity";
    public static final String RELATIVEHUMIDITY_SERIES_TABLE_PREFIX = "series_relativehumidity_";
    public static final String RELATIVEHUMIDITY_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/relativehumidity";
    // snowdepth
    public static final String SNOWDEPTH = "snowdepth";
    public static final String SNOWDEPTH_SERIES_NAME = "SeriesSnowdepth";
    public static final String SNOWDEPTH_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.snowdepth";
    public static final String SNOWDEPTH_SERIES_TABLE_PREFIX = "series_snowdepth_";
    public static final String SNOWDEPTH_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/snowdepth";
    // temperature
    public static final String TEMPERATURE = "temperature";
    public static final String TEMPERATURE_SERIES_NAME = "SeriesTemperature";
    public static final String TEMPERATURE_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.temperature";
    public static final String TEMPERATURE_SERIES_TABLE_PREFIX = "series_temperature_";
    public static final String TEMPERATURE_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/temperature";
    // winddirection
    public static final String WINDDIRECTION = "winddirection";
    public static final String WINDDIRECTION_SERIES_NAME = "SeriesWinddirection";
    public static final String WINDDIRECTION_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.winddirection";
    public static final String WINDDIRECTION_SERIES_TABLE_PREFIX = "series_winddirection_";
    public static final String WINDDIRECTION_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/winddirection";
    // windspeed
    public static final String WINDSPEED = "windspeed";
    public static final String WINDSPEED_SERIES_NAME = "SeriesWindspeed";
    public static final String WINDSPEED_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.windspeed";
    public static final String WINDSPEED_SERIES_TABLE_PREFIX = "series_windspeed_";
    public static final String WINDSPEED_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/windspeed";
    // shortwave radiation global
    public static final String SHORTWAVERADIATIONGLOBAL = "shortwaveradiationglobal";
    public static final String SHORTWAVERADIATIONGLOBAL_SERIES_NAME = "SeriesShortwaveradiationglobal";
    public static final String SHORTWAVERADIATIONGLOBAL_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.shortwaveradiationglobal";
    public static final String SHORTWAVERADIATIONGLOBAL_SERIES_TABLE_PREFIX = "series_shortwaveradiationglobal_";
    public static final String SHORTWAVERADIATIONGLOBAL_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/shortwaveradiationglobal";
    // shortwave radiation direct
    public static final String SHORTWAVERADIATIONDIRECT = "shortwaveradiationdirect";
    public static final String SHORTWAVERADIATIONDIRECT_SERIES_NAME = "SeriesShortwaveradiationdirect";
    public static final String SHORTWAVERADIATIONDIRECT_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.shortwaveradiationdirect";
    public static final String SHORTWAVERADIATIONDIRECT_SERIES_TABLE_PREFIX = "series_shortwaveradiationdirect_";
    public static final String SHORTWAVERADIATIONDIRECT_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/shortwaveradiationdirect";
    // shortwave radiation diffuse
    public static final String SHORTWAVERADIATIONDIFFUSE = "shortwaveradiationdiffuse";
    public static final String SHORTWAVERADIATIONDIFFUSE_SERIES_NAME = "SeriesShortwaveradiationdiffuse";
    public static final String SHORTWAVERADIATIONDIFFUSE_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.shortwaveradiationdiffuse";
    public static final String SHORTWAVERADIATIONDIFFUSE_SERIES_TABLE_PREFIX = "series_shortwaveradiationdiffuse_";
    public static final String SHORTWAVERADIATIONDIFFUSE_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/shortwaveradiationdiffuse";
    // net shortwave radiation
    public static final String NETSHORTWAVERADIATION = "netshortwaveradiation";
    public static final String NETSHORTWAVERADIATION_SERIES_NAME = "SeriesNetshortwaveradiation";
    public static final String NETSHORTWAVERADIATION_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.netshortwaveradiation";
    public static final String NETSHORTWAVERADIATION_SERIES_TABLE_PREFIX = "series_netshortwaveradiation_";
    public static final String NETSHORTWAVERADIATION_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/netshortwaveradiation";
    // cloudiness transmissivity
    public static final String CLOUDINESSTRANSMISSIVITY = "cloudinesstransmissivity";
    public static final String CLOUDINESSTRANSMISSIVITY_SERIES_NAME = "SeriesCloudinesstransmissivity";
    public static final String CLOUDINESSTRANSMISSIVITY_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.cloudinesstransmissivity";
    public static final String CLOUDINESSTRANSMISSIVITY_SERIES_TABLE_PREFIX = "series_cloudinesstransmissivity_";
    public static final String CLOUDINESSTRANSMISSIVITY_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/cloudinesstransmissivity";
    // cloudiness
    public static final String CLOUDINESS = "cloudiness";
    public static final String CLOUDINESS_SERIES_NAME = "SeriesCloudiness";
    public static final String CLOUDINESS_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.cloudiness";
    public static final String CLOUDINESS_SERIES_TABLE_PREFIX = "series_cloudiness_";
    public static final String CLOUDINESS_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/cloudiness";
    // incoming longwave radiation
    public static final String INCOMINGLONGWAVERADIATION = "incominglongwaveradiation";
    public static final String INCOMINGLONGWAVERADIATION_SERIES_NAME = "SeriesIncominglongwaveradiation";
    public static final String INCOMINGLONGWAVERADIATION_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.incominglongwaveradiation";
    public static final String INCOMINGLONGWAVERADIATION_SERIES_TABLE_PREFIX = "series_incominglongwaveradiation_";
    public static final String INCOMINGLONGWAVERADIATION_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/incominglongwaveradiation";

    public static final List<String> seriesTypesNames = new ArrayList<String>();
    static {
        seriesTypesNames.add(PRECIPITATIONS);
        seriesTypesNames.add(PRESSURE);
        seriesTypesNames.add(RELATIVEHUMIDITY);
        seriesTypesNames.add(SNOWDEPTH);
        seriesTypesNames.add(TEMPERATURE);
        seriesTypesNames.add(WINDDIRECTION);
        seriesTypesNames.add(WINDSPEED);
        seriesTypesNames.add(SHORTWAVERADIATIONGLOBAL);
        seriesTypesNames.add(SHORTWAVERADIATIONDIRECT);
        seriesTypesNames.add(SHORTWAVERADIATIONDIFFUSE);
        seriesTypesNames.add(NETSHORTWAVERADIATION);
        seriesTypesNames.add(CLOUDINESSTRANSMISSIVITY);
        seriesTypesNames.add(CLOUDINESS);
        seriesTypesNames.add(INCOMINGLONGWAVERADIATION);
    }

    /**
     * Get the {@link Map} of all EDC tables mapped to their annotated classes.
     * 
     * @return the map of tables mapped to their classes.
     * @throws Exception
     */
    public static Map<String, Class> getAllEdcTables2ClassesMap() {
        HashMap<String, Class> table2ClassesMap = new HashMap<String, Class>();
        table2ClassesMap.putAll(edcTables2ClassMap);
        table2ClassesMap.putAll(getEdcSeriesTables2ClassesMap());
        return table2ClassesMap;
    }

    /**
     * Get the {@link Map} of the EDC tables (without series) mapped to their annotated classes.
     * 
     * @return the map of tables mapped to their classes (without series tables).
     * @throws Exception
     */
    public static Map<String, Class> getEdcTables2ClassesMap() {
        return edcTables2ClassMap;
    }

    private static HashMap<String, Class> edcSeriesTables2ClassesMap;

    /**
     * Get the {@link Map} of all series tables mapped to their annotated classes.
     * 
     * @return the map of tables mapped to their classes.
     * @throws Exception
     */
    public synchronized static Map<String, Class> getEdcSeriesTables2ClassesMap() {
        if (edcSeriesTables2ClassesMap != null) {
            return edcSeriesTables2ClassesMap;
        }
        edcSeriesTables2ClassesMap = new HashMap<String, Class>();
        Map<String, Class> tmptable2ClassesMap = getSeriesTables2ClassesMap(
                PRECIPITATIONS_SERIES_PACKAGE, PRECIPITATIONS_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(PRESSURE_SERIES_PACKAGE,
                PRESSURE_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(RELATIVEHUMIDITY_SERIES_PACKAGE,
                RELATIVEHUMIDITY_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(SNOWDEPTH_SERIES_PACKAGE,
                SNOWDEPTH_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(TEMPERATURE_SERIES_PACKAGE,
                TEMPERATURE_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(WINDDIRECTION_SERIES_PACKAGE,
                WINDDIRECTION_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(WINDSPEED_SERIES_PACKAGE,
                WINDSPEED_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(CLOUDINESS_SERIES_PACKAGE,
                CLOUDINESS_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(CLOUDINESSTRANSMISSIVITY_SERIES_PACKAGE,
                CLOUDINESSTRANSMISSIVITY_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(INCOMINGLONGWAVERADIATION_SERIES_PACKAGE,
                INCOMINGLONGWAVERADIATION_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(NETSHORTWAVERADIATION_SERIES_PACKAGE,
                NETSHORTWAVERADIATION_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(SHORTWAVERADIATIONDIFFUSE_SERIES_PACKAGE,
                SHORTWAVERADIATIONDIFFUSE_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(SHORTWAVERADIATIONDIRECT_SERIES_PACKAGE,
                SHORTWAVERADIATIONDIRECT_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);
        tmptable2ClassesMap = getSeriesTables2ClassesMap(SHORTWAVERADIATIONGLOBAL_SERIES_PACKAGE,
                SHORTWAVERADIATIONGLOBAL_SERIES_NAME);
        edcSeriesTables2ClassesMap.putAll(tmptable2ClassesMap);

        return edcSeriesTables2ClassesMap;
    }

    /**
     * Create a map of tables and their annotated classes for a particular series.
     * 
     * @param series_package
     * @param series_name
     * @return the map of tables and their annotated classes.
     * @throws Exception
     */
    private static Map<String, Class> getSeriesTables2ClassesMap( String series_package,
            String series_name ) {

        Map<String, Class> map = new HashMap<String, Class>();
        try {
            for( int i = start; i <= end; i++ ) {
                Class< ? > theClass = Class.forName(series_package + "." + series_name + i);
                String tableName = theClass.getAnnotation(Table.class).name();
                map.put(tableName, theClass);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return map;
    }

}
