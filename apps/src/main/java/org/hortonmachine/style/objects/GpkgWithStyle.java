package org.hortonmachine.style.objects;

import java.io.File;

import org.geotools.api.style.Style;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.BasicStyle;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.style.StyleUtilities;

public class GpkgWithStyle implements IObjectWithStyle {

    private File dataFile;
    private SqlName tableName;

    private GeopackageCommonDb db;
    private FeatureEntry featureEntry;

    public void setDataFile( File dataFile, String tableName ) throws Exception {
        this.dataFile = dataFile;
        this.tableName = SqlName.m(tableName);

        db = new GeopackageDb();
        db.open(dataFile.getAbsolutePath());
        if (tableName != null) {
            featureEntry = db.feature(this.tableName);
        } else {
            featureEntry = db.features().get(0);
            this.tableName = SqlName.m(featureEntry.getTableName());
        }
    }

    public String getName() {
        return tableName.name;
    }

    public File getDataFile() {
        return dataFile;
    }

    @Override
    public String getNormalizedPath() {
        return dataFile.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName;
    }

    @Override
    public boolean isVector() {
        return featureEntry != null;
    }

    @Override
    public boolean isRaster() {
        return false;
    }

    @Override
    public String getSldString() throws Exception {
        String sldString = db.getSldString(tableName);
        return sldString;
    }

    @Override
    public void saveSld( String xml ) throws Exception {
        db.updateSldStyle(tableName, xml);
        Style style = SldUtilities.getStyleFromSldString(xml);
        BasicStyle basicStyle = StyleUtilities.getBasicStyle(style);
        db.updateSimplifiedStyle(tableName, basicStyle.toString());
    }

}
