/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
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
package org.jgrasstools.gears.modules.r.tmsgenerator;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_LABEL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

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

import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.time.EggClock;

@Description("Feeds a jgrasstools mapurl enabled TMS folder into a new mbtiles database.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("tms, mbtiles")
@Label(OMSTMSGENERATOR_LABEL)
@Name("mapurl2mbtiles")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Mapurl2MbtilesConverter extends JGTModel {

    @Description("TMS folder mapurl file.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile = null;

    private MBTilesHelper mbtilesHelper;

    private String format;

    private volatile int imageIndex = 0;
    private volatile double n = Double.NEGATIVE_INFINITY;
    private volatile double e = Double.NEGATIVE_INFINITY;
    private volatile double s = Double.POSITIVE_INFINITY;
    private volatile double w = Double.POSITIVE_INFINITY;

    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        File mapurlFile = new File(inFile);

        HashMap<String, String> metadataMap = FileUtilities.readFileToHashMap(inFile, "=", false);
        String url = metadataMap.get("url");
        if (url == null) {
            throw new ModelsIllegalargumentException("The supplied file doesn't seem to be a valid jgrasstools mapurl file.",
                    this);
        }
        if (url.endsWith("jpg")) {
            format = "jpg";
        } else {
            format = "png";
        }

        File dbFile = FileUtilities.substituteExtention(mapurlFile, "mbtiles");
        String tilesetName = FileUtilities.getNameWithoutExtention(mapurlFile);
        File folderFile = new File(mapurlFile.getParentFile(), tilesetName);

        mbtilesHelper = new MBTilesHelper();
        mbtilesHelper.open(dbFile);
        mbtilesHelper.createTables(false);

        File[] zFolders = folderFile.listFiles();
        List<File> xFolder = new ArrayList<File>();
        for( File zFolder : zFolders ) {
            File[] xFiles = zFolder.listFiles();
            for( File xFile : xFiles ) {
                if (xFile.isDirectory()) {
                    xFolder.add(xFile);
                }
            }
        }

        final GlobalMercator mercator = new GlobalMercator();

        int minZ = 1000;
        int maxZ = -1000;

        EggClock clock = new EggClock("Time check: ", " sec");
        clock.startAndPrint(System.out);

        for( File xFile : xFolder ) {
            String zStr = xFile.getParentFile().getName();
            final int z = Integer.parseInt(zStr);
            minZ = min(minZ, z);
            maxZ = max(maxZ, z);

            String xStr = xFile.getName();
            final int x = Integer.parseInt(xStr);

            final File[] yFiles = xFile.listFiles(new FilenameFilter(){
                public boolean accept( File arg0, String name ) {
                    boolean endsWithPng = name.endsWith("png");
                    boolean endsWithJpg = name.endsWith("jpg");
                    if (endsWithPng || endsWithJpg) {
                        return true;
                    }
                    return false;
                }
            });

            for( File yFile : yFiles ) {
                String yStr = FileUtilities.getNameWithoutExtention(yFile);
                int y = Integer.parseInt(yStr);

                double[] wsen = mercator.TileLatLonBounds(x, y, z);
                n = max(n, wsen[3]);
                e = max(e, wsen[2]);
                s = max(s, wsen[1]);
                w = max(w, wsen[0]);

                try {
                    BufferedImage image = ImageIO.read(yFile);
                    mbtilesHelper.addTile(x, y, z, image, format);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                if (imageIndex % 1000 == 0) {
                    pm.message("Images inserted in db: " + imageIndex);
                    clock.printTimePassedInSeconds(System.out);
                }
                imageIndex++;
            }
        }

        mbtilesHelper.fillMetadata((float) n, (float) s, (float) w, (float) e, "tilesetName", format, minZ, maxZ);
        mbtilesHelper.createIndexes();
        mbtilesHelper.close();
    }

    public static void main( String[] args ) throws Exception {
        Mapurl2MbtilesConverter m = new Mapurl2MbtilesConverter();
        m.inFile = "/home/moovida/FIA_CASTELLO/geopaptiles/fia_castello.mapurl";
        m.process();
    }
}
