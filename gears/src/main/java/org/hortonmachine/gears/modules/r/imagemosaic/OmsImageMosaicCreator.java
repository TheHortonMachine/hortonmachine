/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
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
package org.hortonmachine.gears.modules.r.imagemosaic;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_KEYWORDS;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_LABEL;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_LICENSE;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_NAME;
import static org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator.OMSIMAGEMOSAICCREATOR_STATUS;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vrt.VrtWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSIMAGEMOSAICCREATOR_DESCRIPTION)
@Documentation(OMSIMAGEMOSAICCREATOR_DOCUMENTATION)
@Author(name = OMSIMAGEMOSAICCREATOR_AUTHORNAMES, contact = OMSIMAGEMOSAICCREATOR_AUTHORCONTACTS)
@Keywords(OMSIMAGEMOSAICCREATOR_KEYWORDS)
@Label(OMSIMAGEMOSAICCREATOR_LABEL)
@Name(OMSIMAGEMOSAICCREATOR_NAME)
@Status(OMSIMAGEMOSAICCREATOR_STATUS)
@License(OMSIMAGEMOSAICCREATOR_LICENSE)
public class OmsImageMosaicCreator extends HMModel {

    @Description(OMSIMAGEMOSAICCREATOR_IN_FOLDER_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description(OMSIMAGEMOSAICCREATOR_OUT_VRT_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVrt;

    public static final String OMSIMAGEMOSAICCREATOR_DESCRIPTION = "Creates the index of a GeoTools image mosaic over the georeferenced images of a folder.";
    public static final String OMSIMAGEMOSAICCREATOR_DOCUMENTATION = """
            The index is a shapefile named as the folder, with the footprint of each image and its file \
            in the location field, next to a properties file with the configuration of the mosaic and a \
            sample_image.dat: the images stay as they are. They need to share the coordinate reference \
            system, the number of bands and the data type, while their resolutions can differ: an \
            image of another kind is left out of the index.

            The index is made only once: if the folder has one already, it is used as it is, and the \
            images added later are not in it. To index them, the shapefile, the properties file and \
            sample_image.dat need to be deleted first.

            Optionally the images of the index are also written as a GDAL virtual raster (VRT), \
            readable by GDAL, QGIS and HortonMachine, as an input for the modules that take a raster \
            file, for example to convert the whole mosaic to a Cloud Optimized GeoTIFF without \
            loading it in memory. The VRT takes the finest resolution of the images, and where \
            they overlap the images later in the index are drawn over the earlier ones. The images \
            in the folder of the VRT or below it are written relative to it.
            """;
    public static final String OMSIMAGEMOSAICCREATOR_KEYWORDS = "Imagemosaic, Mosaic, Tiles, Raster";
    public static final String OMSIMAGEMOSAICCREATOR_LABEL = RASTERPROCESSING;
    public static final String OMSIMAGEMOSAICCREATOR_NAME = "imagemosaic";
    public static final int OMSIMAGEMOSAICCREATOR_STATUS = 40;
    public static final String OMSIMAGEMOSAICCREATOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSIMAGEMOSAICCREATOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSIMAGEMOSAICCREATOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSIMAGEMOSAICCREATOR_IN_FOLDER_DESCRIPTION = "The folder containing the images to mosaic.";
    public static final String OMSIMAGEMOSAICCREATOR_OUT_VRT_DESCRIPTION = "An optional VRT file to write with the images of the mosaic.";

    @Execute
    public void process() throws Exception {
        checkNull(inFolder);
        File imageryFolder = new File(inFolder);
        if (!imageryFolder.isDirectory()) {
            throw new ModelsIllegalargumentException("The input folder doesn't exist: " + inFolder, this, pm);
        }

        pm.beginTask("Generating mosaic... this might take some time depending on the number of images...",
                IHMProgressMonitor.UNKNOWN);
        // the index is created by the reader the first time it opens the folder
        ImageMosaicReader reader = new ImageMosaicFormat().getReader(imageryFolder);
        pm.done();
        if (reader == null) {
            throw new ModelsIllegalargumentException("No mosaic could be created in the folder, are there georeferenced images in it? "
                    + inFolder, this, pm);
        }
        reader.dispose();

        if (outVrt != null && !outVrt.isBlank()) {
            List<File> images = getIndexedImages(imageryFolder);
            VrtWriter.writeMosaic(pm, images, new File(outVrt));
        }
    }

    /**
     * Get the images of the index of the mosaic, in the order of the index.
     *
     * @param imageryFolder the folder of the mosaic.
     * @return the image files.
     * @throws Exception if the index can't be read.
     */
    private List<File> getIndexedImages( File imageryFolder ) throws Exception {
        String name = imageryFolder.getName();
        File propertiesFile = new File(imageryFolder, name + ".properties");
        File shpFile = new File(imageryFolder, name + ".shp");
        if (!propertiesFile.exists() || !shpFile.exists()) {
            throw new ModelsIllegalargumentException("The index of the mosaic is missing in: " + imageryFolder, this, pm);
        }
        Properties properties = new Properties();
        properties.load(new StringReader(FileUtilities.readFile(propertiesFile)));
        String locationAttribute = properties.getProperty("LocationAttribute", "location");
        boolean absolute = Boolean.parseBoolean(properties.getProperty("AbsolutePath", "false"))
                || "ABSOLUTE".equalsIgnoreCase(properties.getProperty("PathType"));

        List<File> images = new ArrayList<>();
        SimpleFeatureCollection index = OmsVectorReader.readVector(shpFile.getAbsolutePath());
        try (SimpleFeatureIterator iterator = index.features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                String location = feature.getAttribute(locationAttribute).toString();
                File image = absolute ? new File(location) : new File(imageryFolder, location);
                images.add(image.getAbsoluteFile());
            }
        }
        if (images.isEmpty()) {
            throw new ModelsIllegalargumentException("The index of the mosaic has no images: " + shpFile, this, pm);
        }
        return images;
    }
}
