package org.osgeo.grass.r;

import org.jgrasstools.grass.utils.ModuleSupporter;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.UI;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description("Downloads and imports data from WMS servers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("wms")
@Label("Grass Raster Modules")
@Name("r__in__wms")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__wms {

	@UI("outfile")
	@Description("Name for output raster map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Mapserver to request data from")
	@In
	public String $$mapserverPARAMETER;

	@Description("Layers to request from map server (optional)")
	@In
	public String $$layersPARAMETER;

	@Description("Styles to request from map server (optional)")
	@In
	public String $$stylesPARAMETER;

	@Description("Source projection to request from server (optional)")
	@In
	public String $$srsPARAMETER = "EPSG:4326";

	@Description("Image format requested from the server")
	@In
	public String $$formatPARAMETER = "geotiff";

	@Description("Addition query options for server (optional)")
	@In
	public String $$wmsqueryPARAMETER = "version=1.1.1";

	@Description("Maximum columns to request at a time")
	@In
	public String $$maxcolsPARAMETER = "1024";

	@Description("Maximum rows to request at a time")
	@In
	public String $$maxrowsPARAMETER = "1024";

	@Description("Additional options for r.tileset (optional)")
	@In
	public String $$tileoptionsPARAMETER;

	@Description("Named region to request data for. Current region used if omitted (optional)")
	@In
	public String $$regionPARAMETER;

	@Description("Folder to save downloaded data to (optional)")
	@In
	public String $$folderPARAMETER;

	@Description("Additional options for wget (optional)")
	@In
	public String $$wgetoptionsPARAMETER = "-c -t 5 -nv";

	@Description("Additional options for curl (optional)")
	@In
	public String $$curloptionsPARAMETER = "-C - --retry 5 -s -S";

	@Description("Reprojection method to use")
	@In
	public String $$methodPARAMETER = "nearest";

	@Description("Requires list available layers flag (optional)")
	@In
	public String $$cap_filePARAMETER;

	@Description("Verbosity level (optional)")
	@In
	public String $$vPARAMETER = "1";

	@Description("List available layers and exit")
	@In
	public boolean $$lFLAG = false;

	@Description("Skip to downloading (to resume downloads faster)")
	@In
	public boolean $$dFLAG = false;

	@Description("Don't request transparent data")
	@In
	public boolean $$oFLAG = false;

	@Description("Clean existing data out of download directory")
	@In
	public boolean $$cFLAG = false;

	@Description("Keep band numbers instead of using band color names")
	@In
	public boolean $$kFLAG = false;

	@Description("Don't reproject the data, just patch it")
	@In
	public boolean $$pFLAG = false;

	@Description("This may be needed to connect to servers which lack POST capability")
	@In
	public boolean $$gFLAG = false;

	@Description("Allow output files to overwrite existing files")
	@In
	public boolean $$overwriteFLAG = false;

	@Description("Verbose module output")
	@In
	public boolean $$verboseFLAG = false;

	@Description("Quiet module output")
	@In
	public boolean $$quietFLAG = false;


	@Execute
	public void process() throws Exception {
		ModuleSupporter.processModule(this);
	}

}
