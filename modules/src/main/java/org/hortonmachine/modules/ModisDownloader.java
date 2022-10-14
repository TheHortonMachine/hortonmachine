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
package org.hortonmachine.modules;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_outRaster;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pDay;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pDownloadUrl;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pExcludePattern;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pIncludePattern;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pIntermediateDownloadFolder;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pPassword;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pProduct;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pProductPath;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pRoiEnvelope;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pUser;
import static org.hortonmachine.gears.io.remotesensing.OmsModisDownloader.DESCR_pVersion;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.netcdf.INetcdfUtils;
import org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.remotesensing.OmsModisDownloader;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.mosaic.OmsMosaic;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.locationtech.jts.geom.Envelope;
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
public class ModisDownloader extends HMModel implements INetcdfUtils {
    @Description(DESCR_pIncludePattern)
    @In
    public String pIncludePattern = null;

    @Description(DESCR_pExcludePattern)
    @In
    public String pExcludePattern = null;

    @Description(DESCR_pIntermediateDownloadFolder)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String pIntermediateDownloadFolder = null;

    @Description(DESCR_pDay)
    @In
    public String pDay = null;

    @Description("The count of days to download before the selected pDay.")
    @In
    public int pDelta = 0;

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

    @Description("The region of interest as [w,e,s,n]")
    @In
    public double[] pRoi = null;

    @Description("The output folder in which to place the final result.")
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder;

    @Initialize
    @Execute
    public void process() throws Exception {
        checkNull(pDownloadUrl, pProductPath, pProduct, pVersion, pRoi, pUser, pPassword, outFolder);

        String daysListUrl = pDownloadUrl + "/" + pProductPath + "/" + pProduct + "." + pVersion;
        String daysListPage = OmsModisDownloader.getWebpageString(daysListUrl);
        String[] linesSplit = daysListPage.split("img src=");
        List<String> datesList = new ArrayList<>();
        for( String line : linesSplit ) {
            if (line.contains("\"/icons/folder.gif")) {
                String tmp = line.split("<a href=.{13}>")[1];
                String dateString = tmp.split("/</a>")[0];
                datesList.add(dateString);
            }
        }
        if (datesList.size() == 0) {
            throw new ModelsIOException("Could not retrieve the available dates at " + daysListUrl, this);

        }
        Collections.sort(datesList);

        pDay = pDay.replace("-", ".");
        int todayIndex = datesList.indexOf(pDay);
        if (todayIndex < 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("The asked date is not available: " + pDay + "\n");
            SimpleDateFormat f = new SimpleDateFormat("yyyy.MM.dd");
            Date pDayDate = f.parse(pDay);
            pDay = null;
            long dayDateMillis = pDayDate.getTime();
            for( int i = 1; i < datesList.size(); i++ ) {
                String pre = datesList.get(i - 1);
                String post = datesList.get(i);
                Date preDate = f.parse(pre);
                Date postDate = f.parse(post);
                if (dayDateMillis > preDate.getTime() && dayDateMillis < postDate.getTime()) {
                    int daysDiff = (int) Math.round((postDate.getTime() - pDayDate.getTime()) / 1000 / 60 / 60 / 24.0);
                    if (daysDiff < 7) {
                        // accept only data less than a week away
                        pDay = f.format(postDate);
                        todayIndex = i;
                        break;
                    }
                }
            }
            if (pDay == null) {
                sb.append("Unable to retrieve a valid nearby date.");
                pm.errorMessage(sb.toString());
                return;
            } else {
                sb.append("Using the nearest found date instead: " + pDay);
                pm.errorMessage(sb.toString());
            }
        }

        List<String> daysToDownload = new ArrayList<>();
        for( int i = todayIndex - pDelta; i <= todayIndex; i++ ) {
            daysToDownload.add(datesList.get(i));
        }

        pm.message("Downloading data of the following available days (" + daysToDownload.size() + "):");
        for( String day : daysToDownload ) {
            pm.message("\t" + day);
        }

        for( String day : daysToDownload ) {
            OmsModisDownloader md = new OmsModisDownloader();
            md.pm = pm;
            md.pRoi = new Envelope(pRoi[0], pRoi[1], pRoi[2], pRoi[3]);
            md.pProductPath = pProductPath;
            md.pProduct = pProduct;
            md.pVersion = pVersion;
            md.pDay = day;
            md.pIncludePattern = pIncludePattern;
            md.pExcludePattern = pExcludePattern;
            md.pUser = pUser;
            md.pPassword = pPassword;
            md.pIntermediateDownloadFolder = pIntermediateDownloadFolder;
            md.process();
            GridCoverage2D finalRaster = md.outRaster;
            String gridName = finalRaster.getName().toString();
            gridName = FileUtilities.getSafeFileName(gridName);
            File folder = new File(outFolder);
            File dayFolderFile = new File(folder, pProductPath + "_" + pProduct + "_" + pVersion + File.separator + day);
            if (!dayFolderFile.exists()) {
                dayFolderFile.mkdirs();
            }
            File file = new File(dayFolderFile, gridName + ".tif");
            dumpRaster(finalRaster, file.getAbsolutePath());
        }

    }

}
