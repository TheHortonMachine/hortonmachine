package org.osgeo.grass.v;

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

@Description("Converts to one of the supported OGR vector formats.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, export")
@Label("Grass Vector Modules")
@Name("v__out__ogr")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__out__ogr {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Feature type. Combination of types is not supported by all formats. (optional)")
	@In
	public String $$typePARAMETER = "line,boundary";

	@Description("For example: ESRI Shapefile: filename or directory for storage")
	@In
	public String $$dsnPARAMETER;

	@Description("For example: ESRI Shapefile: shapefile name (optional)")
	@In
	public String $$olayerPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("OGR format (optional)")
	@In
	public String $$formatPARAMETER = "ESRI_Shapefile";

	@Description("OGR dataset creation option (format specific, NAME=VALUE) (optional)")
	@In
	public String $$dscoPARAMETER = "";

	@Description("OGR layer creation option (format specific, NAME=VALUE) (optional)")
	@In
	public String $$lcoPARAMETER = "";

	@Description("Export features with category (labeled) only. Otherwise all features are exported")
	@In
	public boolean $$cFLAG = false;

	@Description("Use ESRI-style .prj file format (applies to Shapefile output only)")
	@In
	public boolean $$eFLAG = false;

	@Description("Export lines as polygons")
	@In
	public boolean $$pFLAG = false;

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
