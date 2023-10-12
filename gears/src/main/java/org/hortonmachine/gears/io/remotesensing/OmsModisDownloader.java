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
package org.hortonmachine.gears.io.remotesensing;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.netcdf.INetcdfUtils;
import org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.mosaic.OmsMosaic;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;

@Description(OmsModisDownloader.DESCRIPTION)
@Author(name = OmsModisDownloader.AUTHOR, contact = OmsModisDownloader.CONTACT)
@Keywords(OmsModisDownloader.KEYWORDS)
@Label(HMConstants.NETCDF)
@Name(OmsModisDownloader.NAME)
@Status(OmsModisDownloader.STATUS)
@License(OmsModisDownloader.LICENSE)
public class OmsModisDownloader extends HMModel implements INetcdfUtils {
    @Description(DESCR_pIncludePattern)
    @In
    public String pIncludePattern = null;

    @Description(DESCR_pExcludePattern)
    @In
    public String pExcludePattern = null;

    @Description(DESCR_pIntermediateDownloadFolder)
    @In
    public String pIntermediateDownloadFolder = null;

    @Description(DESCR_pDay)
    @In
    public String pDay = null;

    @Description(DESCR_pDownloadUrl)
    @In
    public String pDownloadUrl = "https://e4ftl01.cr.usgs.gov";

    @Description(DESCR_pUser)
    @In
    public String pUser = null;

    @Description(DESCR_pPassword)
    @In
    public String pPassword;

    @Description(DESCR_pProductPath)
    @UI("combo:MOLA,MOLT,MOTA")
    @In
    public String pProductPath = "MOTA";

    @Description(DESCR_pProduct)
    @In
    public String pProduct = null;

    @Description(DESCR_pVersion)
    @In
    public String pVersion = "006";

    @Description(DESCR_pRoiEnvelope)
    @In
    public org.locationtech.jts.geom.Envelope pRoi = null;

    @Description(DESCR_doGeotiffs)
    @In
    public boolean doGeotiffs = true;

    @Description(DESCR_doMosaicAndClip)
    @In
    public boolean doMosaicAndClip = true;

    @Description(DESCR_outRaster)
    @Out
    public GridCoverage2D outRaster;

    public static final int STATUS = 40;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "omsmodisdownloader";
    public static final String KEYWORDS = "modis";
    public static final String CONTACT = "http://www.hydrologis.com";
    public static final String AUTHOR = "Antonello Andrea";
    public static final String DESCRIPTION = "Download MODIS data and patch dem together to coverages.";
    public static final String DESCR_pDay = "The download day in format YYYY-MM-DD.";
    public static final String DESCR_pDownloadUrl = "The url to download the data from.";
    public static final String DESCR_pProductPath = "The url path defining the type of product.";
    public static final String DESCR_pProduct = "The url part defining the product.";
    public static final String DESCR_pVersion = "The data version.";
    public static final String DESCR_pRoiEnvelope = "The envelope to extract in EPSG:4326.";
    public static final String DESCR_pIncludePattern = "In case of no grid name, an inclusion pattern can be used.";
    public static final String DESCR_pExcludePattern = "In case of no grid name, an exclusion pattern can be used.";
    public static final String DESCR_outRaster = "The output raster, patched from the downoaded tiles.";
    public static final String DESCR_pUser = "The user registered to the modis website.";
    public static final String DESCR_pPassword = "The user registered to the modis website.";
    public static final String DESCR_pIntermediateDownloadFolder = "A folder in which to download the tiles to be patched.";
    public static final String DESCR_doMosaicAndClip = "Do the Mosaic and clip over the region of interest.";
    public static final String DESCR_doGeotiffs = "Convert the downloaded hdf to geotiff.";

    private DecimalFormat df = new DecimalFormat("00");

    @Initialize
    @Execute
    public void process() throws Exception {
        checkNull(pDownloadUrl, pProductPath, pProduct, pVersion, pRoi, pUser, pPassword);

        if (pIntermediateDownloadFolder == null) {
            File temporaryFolder = FileUtilities.createTemporaryFolder("hm-modis-downloader");
            pIntermediateDownloadFolder = temporaryFolder.getAbsolutePath();
        } else {
            File intermediateDownloadFolder = new File(pIntermediateDownloadFolder);
            if (!intermediateDownloadFolder.exists()) {
                intermediateDownloadFolder.mkdirs();
            }
        }

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        Authenticator.setDefault(new Authenticator(){
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(pUser, pPassword.toCharArray());
            }
        });

        String baseTilesDefUrl = "https://lpdaacsvc.cr.usgs.gov/services/tilemap?";
        String from = baseTilesDefUrl + "product=" + pProduct + "&longitude=" + pRoi.getMinX() + "&latitude=" + pRoi.getMinY();
        String to = baseTilesDefUrl + "product=" + pProduct + "&longitude=" + pRoi.getMaxX() + "&latitude=" + pRoi.getMaxY();

        pm.beginTask("Gathering tile ranges to download...", 2);
        int[] llHv = getTiles(from);
        pm.worked(1);
        int[] urHv = getTiles(to);
        pm.done();
        pm.message("\tHorizontal: " + llHv[0] + " -> " + urHv[0]);
        pm.message("\tVertical: " + urHv[1] + " -> " + llHv[1]);

        List<String> tilesList = new ArrayList<>();
        pm.message("Tiles selected for download:");
        for( int v = urHv[1]; v <= llHv[1]; v++ ) {
            for( int h = llHv[0]; h <= urHv[0]; h++ ) {
                String tile = "h" + df.format(h) + "v" + df.format(v);
                tilesList.add(tile);
                pm.message("\t" + tile);
            }
        }

        String product = pProductPath + "/" + pProduct + "." + pVersion;
        String daysListUrl = pDownloadUrl + "/" + product;
        String dayDataUrl = daysListUrl + "/" + pDay;
        String dayDataPage = getWebpageString(dayDataUrl);

        List<File> downloadedFiles = new ArrayList<>();
        String[] tmp = dayDataPage.split("img src");
        pm.beginTask("Extracting day " + pDay + "...", tmp.length);
        for( String line : tmp ) {
            if (line.contains(pProduct) && containsOneOf(line, tilesList) && !line.contains(".jpg")) {
                String fileNameToDownload = line.trim().split("href=\"")[1].split("\"")[0];
                String downloadUrlPath = dayDataUrl + "/" + fileNameToDownload;

                String dayDownloadFolder = pIntermediateDownloadFolder + File.separator + pProductPath + "_" + pProduct + "_"
                        + pVersion + File.separator + pDay;
                File dayDownloadFolderFile = new File(dayDownloadFolder);
                if (!dayDownloadFolderFile.exists()) {
                    dayDownloadFolderFile.mkdirs();
                }

                String downloadPath = dayDownloadFolder + File.separator + fileNameToDownload;
                File downloadFile = new File(downloadPath);
                if (!downloadFile.exists()) {
                    pm.message("\tDownloading: " + fileNameToDownload);
                    pm.message("\tfrom url: " + downloadUrlPath);

                    URL downloadUrl = new URL(downloadUrlPath);
                    try (BufferedInputStream inputStream = new BufferedInputStream(downloadUrl.openStream());
                            FileOutputStream fileOS = new FileOutputStream(downloadFile)) {
                        byte data[] = new byte[1024];
                        int byteContent;
                        while( (byteContent = inputStream.read(data, 0, 1024)) != -1 ) {
                            fileOS.write(data, 0, byteContent);
                        }
                        if (fileNameToDownload.endsWith(".hdf")) {
                            downloadedFiles.add(downloadFile);
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        if (downloadedFiles.size() == 0) {
            pm.errorMessage("Found no data to download for " + product + " in day " + pDay + ".");
            return;
        }

        // now convert files and patch them into a geotiff
        List<GridCoverage2D> coverages = new ArrayList<>();
        String gridName = null;
        if (doGeotiffs) {
            pm.beginTask("Converting hdf to geotiff...", downloadedFiles.size());
            for( File file : downloadedFiles ) {
                OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
                converter.pm = pm;
                converter.inPath = file.getAbsolutePath();
                converter.pIncludePattern = pIncludePattern;
                converter.pExcludePattern = pExcludePattern;
                CalendarDateFormatter f = new CalendarDateFormatter("yyyy.MM.dd");
                CalendarDate forceDate = f.parse(pDay);
                converter.forcedModisDate = forceDate;
                converter.initProcess();
                converter.process();
                coverages.add(converter.outRaster);

                if (gridName == null) {
                    gridName = converter.selectedGridName;
                }
                pm.worked(1);
            }
            pm.done();

            if (doMosaicAndClip) {
                if (coverages.size() > 1) {
                    OmsMosaic mosaic = new OmsMosaic();
                    mosaic.inCoverages = coverages;
                    mosaic.pm = pm;
                    mosaic.process();
                    outRaster = mosaic.outRaster;
                } else {
                    outRaster = coverages.get(0);
                }
                outRaster = CoverageUtilities.clipCoverage(HMRaster.fromGridCoverage(outRaster),
                        new ReferencedEnvelope(pRoi, outRaster.getCoordinateReferenceSystem()), gridName);
            }
        }

    }

    private boolean containsOneOf( String string, List<String> list ) {
        for( String item : list ) {
            if (string.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private int[] getTiles( String urlPath )
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException {
        URL url = new URL(urlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(url.openStream());
        Element documentElement = doc.getDocumentElement();
        NodeList childNodes = documentElement.getChildNodes();
        String hStr = null;
        String vStr = null;
        for( int i = 0; i < childNodes.getLength(); i++ ) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String tagName = elem.getTagName();
                if (tagName.equals("horizontal")) {
                    hStr = elem.getTextContent();
                } else if (tagName.equals("vertical")) {
                    vStr = elem.getTextContent();
                }
                if (hStr != null && vStr != null) {
                    break;
                }
            }
        }
        return new int[]{Integer.parseInt(hStr), Integer.parseInt(vStr)};
    }

    public static String getWebpageString( String urlString ) throws Exception {
        URL url = new URL(urlString);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while( (line = reader.readLine()) != null ) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

}
