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
package org.hortonmachine.gears.utils.display;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.TableName;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.images.ImageUtilities;

/**
 * Renders {@link File}s and {@link Path}s of rasters, vectors and images by reading them
 * and rendering their content.
 *
 * <p>Geopackages with several vector tables show the first one, unless the table is
 * given after a <code>#</code> in the path.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FileDisplayRenderer implements IDisplayRenderer {

    private static final String[] RASTERS = {".tif", ".tiff", ".asc"};
    private static final String[] VECTORS = {".shp", ".geojson"};
    private static final String[] IMAGES = {".png", ".jpg", ".jpeg", ".gif"};
    private static final String GPKG = "." + HMConstants.GPKG;

    @Override
    public boolean canRender( Object value ) {
        File file = toFile(value);
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase();
        if (name.contains(GPKG + HMConstants.DB_TABLE_PATH_SEPARATOR)) {
            String dbPath = file.getPath().split(HMConstants.DB_TABLE_PATH_SEPARATOR)[0];
            return new File(dbPath).exists();
        }
        if (!file.isFile()) {
            return false;
        }
        return endsWith(name, RASTERS) || endsWith(name, VECTORS) || endsWith(name, IMAGES) || name.endsWith(GPKG);
    }

    @Override
    public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
        File file = toFile(value);
        String path = file.getPath();
        String name = file.getName().toLowerCase();

        List<DisplayPart> parts = new ArrayList<>();
        if (endsWith(name, RASTERS)) {
            parts.addAll(new RasterDisplayRenderer().render(OmsRasterReader.readRaster(path), options));
        } else if (endsWith(name, IMAGES)) {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return List.of(new DisplayText("Could not read the image: " + path));
            }
            int maxSize = Math.max(options.imageWidth(), options.imageHeight());
            if (image.getWidth() > maxSize || image.getHeight() > maxSize) {
                image = ImageUtilities.scaleImage(image, maxSize);
            }
            parts.add(new DisplayImage(image));
        } else if (name.endsWith(GPKG)) {
            List<String> tables = vectorTables(path);
            if (tables.isEmpty()) {
                return List.of(new DisplayText("No vector tables in: " + path));
            }
            if (tables.size() > 1) {
                parts.add(new DisplayText(
                        "Vector tables: " + String.join(", ", tables) + ". Showing " + tables.get(0) + "."));
            }
            String tablePath = path + HMConstants.DB_TABLE_PATH_SEPARATOR + tables.get(0);
            parts.addAll(new FeatureDisplayRenderer().render(OmsVectorReader.readVector(tablePath), options));
        } else {
            // vectors and geopackage tables
            parts.addAll(new FeatureDisplayRenderer().render(OmsVectorReader.readVector(path), options));
        }
        return parts;
    }

    private static List<String> vectorTables( String gpkgPath ) throws Exception {
        List<String> names = new ArrayList<>();
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgPath);
            db.initSpatialMetadata(null);
            for( TableName table : db.getTables() ) {
                if (db.getGeometryColumnsForTable(table.toSqlName()) != null) {
                    names.add(table.getName());
                }
            }
        }
        return names;
    }

    private static File toFile( Object value ) {
        if (value instanceof File) {
            return (File) value;
        }
        if (value instanceof Path) {
            return ((Path) value).toFile();
        }
        return null;
    }

    private static boolean endsWith( String name, String[] extensions ) {
        for( String extension : extensions ) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
