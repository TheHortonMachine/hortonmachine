/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) Michael Michaud
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.v.vectorconverter;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_LABEL;
import static org.jgrasstools.gears.io.dxfdwg.libs.DxfUtils.LAYER;

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
import org.jgrasstools.gears.io.dxfdwg.libs.DxfUtils;
import org.jgrasstools.gears.io.dxfdwg.libs.dxf.DxfGroup;
import org.jgrasstools.gears.io.dxfdwg.libs.dxf.DxfTABLE_LAYER_ITEM;
import org.jgrasstools.gears.io.dxfdwg.libs.dxf.DxfTABLE_LTYPE_ITEM;
import org.jgrasstools.gears.io.dxfdwg.libs.dxf.DxfTABLE_STYLE_ITEM;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;

import com.vividsolutions.jts.geom.Envelope;

@Description("Shapefiles to dxf converter (Based on work of Michael Michaud)")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("dxf, vector, converter")
@Label(OMSDXFCONVERTER_LABEL)
@Name("shp2dxfconverter")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class OmsShp2DxfConverter extends JGTModel {

    @Description("The folder containing the shapefiles.")
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder = null;

    @Description("Optional field name for elevation value (in case of lines it will be applied to the whole feature).")
    @In
    public String fElev = null;

    @Description("The output dxf file path.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String inDxfpath = null;

    @Description("Do the suffix.")
    @In
    public boolean doSuffix = false;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, inDxfpath);

        File fodlerFile = new File(inFolder);
        if (!fodlerFile.exists()) {
            throw new ModelsIllegalargumentException("Folder doesn't exist: " + inFolder, this);
        }

        File[] shpFiles = fodlerFile.listFiles(new FilenameFilter(){
            public boolean accept( File arg0, String name ) {
                return name.endsWith(".shp");
            }
        });

        Envelope envelope = null;
        int count = 0;
        LinkedHashMap<String, List<FeatureMate>> layer2FeaturesMap = new LinkedHashMap<String, List<FeatureMate>>();
        for( File shpFile : shpFiles ) {
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
            fw.write(DxfGroup.toString(999, "TRANSLATION BY fr.michaelm.jump.drivers.dxf.DxfFile (version jgrasstools)"));
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

            for( String layerName : layer2FeaturesMap.keySet() ) {
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

            for( Entry<String, List<FeatureMate>> entries : layer2FeaturesMap.entrySet() ) {
                String layerName = entries.getKey();
                List<FeatureMate> featuresList = entries.getValue();
                for( FeatureMate feature : featuresList ) {
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
