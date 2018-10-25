/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Michael Michaud
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
package org.hortonmachine.gears.modules.v.vectorconverter;

import static org.hortonmachine.gears.i18n.GearsMessages.*;
import static org.hortonmachine.gears.io.dxfdwg.libs.DxfUtils.LAYER;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.dxfdwg.libs.DxfUtils;
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfGroup;
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfTABLE_LAYER_ITEM;
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfTABLE_LTYPE_ITEM;
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfTABLE_STYLE_ITEM;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

import org.locationtech.jts.geom.Envelope;

@Description(OmsShp2DxfConverter.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsShp2DxfConverter.KEYWORDS)
@Label(OMSDXFCONVERTER_LABEL)
@Name("_" + OmsShp2DxfConverter.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsShp2DxfConverter extends HMModel {

    @Description(THE_FOLDER_CONTAINING_THE_SHAPEFILES)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder = null;

    @Description(FIELD_NAME_FOR_ELEVATION_VALUE)
    @In
    public String fElev = null;

    @Description(DO_THE_SUFFIX)
    @In
    public boolean doSuffix = false;

    @Description(THE_OUTPUT_DXF_FILE_PATH)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String inDxfpath = null;

    // START VARIABLE DOCS
    public static final String NAME = "shp2dxfconverter";
    public static final String KEYWORDS = "dxf, vector, converter";
    public static final String DESCRIPTION = "Shapefiles to dxf converter (Based on work of Michael Michaud)";
    public static final String THE_FOLDER_CONTAINING_THE_SHAPEFILES = "The folder containing the shapefiles.";
    public static final String FIELD_NAME_FOR_ELEVATION_VALUE = "Optional field name for elevation value (in case of lines it will be applied to the whole feature).";
    public static final String DO_THE_SUFFIX = "Do the suffix.";
    public static final String THE_OUTPUT_DXF_FILE_PATH = "The output dxf file path.";
    // END VARIABLE DOCS


    @Execute
    public void process() throws Exception {
        checkNull(inFolder, inDxfpath);

        File fodlerFile = new File(inFolder);
        if (!fodlerFile.exists()) {
            throw new ModelsIllegalargumentException("Folder doesn't exist: " + inFolder, this);
        }

        File[] shpFiles = fodlerFile.listFiles(new FilenameFilter() {
            public boolean accept(File arg0, String name) {
                return name.endsWith(".shp");
            }
        });

        Envelope envelope = null;
        int count = 0;
        LinkedHashMap<String, List<FeatureMate>> layer2FeaturesMap = new LinkedHashMap<String, List<FeatureMate>>();
        for (File shpFile : shpFiles) {
            String path = shpFile.getAbsolutePath();
            String layerName = FileUtilities.getNameWithoutExtention(shpFile);
            SimpleFeatureCollection vector = getVector(path);

            List<FeatureMate> featuresList = FeatureUtilities.featureCollectionToMatesList(vector);
            layer2FeaturesMap.put(layerName, featuresList);
            count = count + featuresList.size();
            if (envelope == null) {
                envelope = vector.getBounds();
            } else {
                envelope.expandToInclude(vector.getBounds());
            }
        }

        FileWriter fw = new FileWriter(inDxfpath);

        Date date = new Date(System.currentTimeMillis());
        try {
            // if (header) {
            // COMMENTAIRES DU TRADUCTEUR
            fw.write(DxfGroup.toString(999, Integer.toString(count) + " features"));
            fw.write(DxfGroup.toString(999, "TRANSLATION BY fr.michaelm.jump.drivers.dxf.DxfFile (version HortonMachine)"));
            fw.write(DxfGroup.toString(999, "DATE : " + date.toString()));

            // ECRITURE DU HEADER
            fw.write(DxfGroup.toString(0, "SECTION"));
            fw.write(DxfGroup.toString(2, "HEADER"));
            fw.write(DxfGroup.toString(9, "$ACADVER"));
            fw.write(DxfGroup.toString(1, "AC1009"));
            fw.write(DxfGroup.toString(9, "$CECOLOR"));
            fw.write(DxfGroup.toString(62, 256));
            fw.write(DxfGroup.toString(9, "$CELTYPE"));
            fw.write(DxfGroup.toString(6, "DUPLAN"));
            fw.write(DxfGroup.toString(9, "$CLAYER"));
            fw.write(DxfGroup.toString(8, "0")); // corrected by L. Becker on 2006-11-08
            fw.write(DxfGroup.toString(9, "$ELEVATION"));
            fw.write(DxfGroup.toString(40, 0.0, 3));
            fw.write(DxfGroup.toString(9, "$EXTMAX"));
            fw.write(DxfGroup.toString(10, envelope.getMaxX(), 6));
            fw.write(DxfGroup.toString(20, envelope.getMaxY(), 6));
            // fw.write(DxfGroup.toString(30, envelope.getMaxX(), 6));
            fw.write(DxfGroup.toString(9, "$EXTMIN"));
            fw.write(DxfGroup.toString(10, envelope.getMinX(), 6));
            fw.write(DxfGroup.toString(20, envelope.getMinY(), 6));
            // fw.write(DxfGroup.toString(30, envelope.getMaxX(), 6));
            fw.write(DxfGroup.toString(9, "$INSBASE"));
            fw.write(DxfGroup.toString(10, 0.0, 1));
            fw.write(DxfGroup.toString(20, 0.0, 1));
            fw.write(DxfGroup.toString(30, 0.0, 1));
            fw.write(DxfGroup.toString(9, "$LIMCHECK"));
            fw.write(DxfGroup.toString(70, 1));
            fw.write(DxfGroup.toString(9, "$LIMMAX"));
            fw.write(DxfGroup.toString(10, envelope.getMaxX(), 6));
            fw.write(DxfGroup.toString(20, envelope.getMaxY(), 6));
            fw.write(DxfGroup.toString(9, "$LIMMIN"));
            fw.write(DxfGroup.toString(10, envelope.getMinX(), 6));
            fw.write(DxfGroup.toString(20, envelope.getMinY(), 6));
            fw.write(DxfGroup.toString(9, "$LUNITS"));
            fw.write(DxfGroup.toString(70, 2));
            fw.write(DxfGroup.toString(9, "$LUPREC"));
            fw.write(DxfGroup.toString(70, 2));
            fw.write(DxfGroup.toString(0, "ENDSEC"));
            // }
            // ECRITURE DES TABLES
            fw.write(DxfGroup.toString(0, "SECTION"));
            fw.write(DxfGroup.toString(2, "TABLES"));
            fw.write(DxfGroup.toString(0, "TABLE"));
            fw.write(DxfGroup.toString(2, "STYLE"));
            fw.write(DxfGroup.toString(70, 1));
            fw.write(DxfGroup.toString(0, "STYLE")); // added by L. Becker on 2006-11-08
            DxfTABLE_STYLE_ITEM style = new DxfTABLE_STYLE_ITEM("STANDARD", 0, 0f, 1f, 0f, 0, 1.0f, "xxx.txt", "yyy.txt");
            fw.write(style.toString());
            fw.write(DxfGroup.toString(0, "ENDTAB"));
            fw.write(DxfGroup.toString(0, "TABLE"));
            fw.write(DxfGroup.toString(2, "LTYPE"));
            fw.write(DxfGroup.toString(70, 1));
            fw.write(DxfGroup.toString(0, "LTYPE")); // added by L. Becker on 2006-11-08
            DxfTABLE_LTYPE_ITEM ltype = new DxfTABLE_LTYPE_ITEM("CONTINUE", 0, "", 65, 0f, new float[0]);
            fw.write(ltype.toString());
            fw.write(DxfGroup.toString(0, "ENDTAB"));
            fw.write(DxfGroup.toString(0, "TABLE"));
            fw.write(DxfGroup.toString(2, LAYER));
            fw.write(DxfGroup.toString(70, 2));

            for (String layerName : layer2FeaturesMap.keySet()) {
                DxfTABLE_LAYER_ITEM layer = new DxfTABLE_LAYER_ITEM(layerName, 0, 131, "CONTINUE");
                fw.write(DxfGroup.toString(0, LAYER)); // added by L. Becker on 2006-11-08
                fw.write(layer.toString());
                if (doSuffix) {
                    layer = new DxfTABLE_LAYER_ITEM(layerName + "_", 0, 131, "CONTINUE");
                    fw.write(DxfGroup.toString(0, LAYER)); // added by L. Becker on 2006-11-08
                    fw.write(layer.toString());
                }
            }
            fw.write(DxfGroup.toString(0, "ENDTAB"));
            fw.write(DxfGroup.toString(0, "ENDSEC"));

            // ECRITURE DES FEATURES
            fw.write(DxfGroup.toString(0, "SECTION"));
            fw.write(DxfGroup.toString(2, "ENTITIES"));

            for (Entry<String, List<FeatureMate>> entries : layer2FeaturesMap.entrySet()) {
                String layerName = entries.getKey();
                List<FeatureMate> featuresList = entries.getValue();
                for (FeatureMate feature : featuresList) {
                    fw.write(DxfUtils.feature2Dxf(feature, layerName, fElev, doSuffix, false));
                }
            }
            fw.write(DxfGroup.toString(0, "ENDSEC"));

            // FIN DE FICHIER
            fw.write(DxfGroup.toString(0, "EOF"));
            fw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (fw != null)
                fw.close();
        }
        return;
    }

}
