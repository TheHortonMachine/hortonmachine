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

@Description("Import a binary raster file into a GRASS raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, import")
@Label("Grass/Raster Modules")
@Name("r__in__bin")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__bin {

	@Description("Binary raster file to be imported")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Title for resultant raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Number of bytes per cell (1, 2, 4) (optional)")
	@In
	public String $$bytesPARAMETER = "1";

	@Description("Northern limit of geographic region (outer edge) (optional)")
	@In
	public String $$northPARAMETER;

	@Description("Southern limit of geographic region (outer edge) (optional)")
	@In
	public String $$southPARAMETER;

	@Description("Eastern limit of geographic region (outer edge) (optional)")
	@In
	public String $$eastPARAMETER;

	@Description("Western limit of geographic region (outer edge) (optional)")
	@In
	public String $$westPARAMETER;

	@Description("Number of rows (optional)")
	@In
	public String $$rowsPARAMETER;

	@Description("Number of columns (optional)")
	@In
	public String $$colsPARAMETER;

	@Description("Set Value to NULL (optional)")
	@In
	public String $$anullPARAMETER;

	@Description("Import as Floating Point Data (default: Integer)")
	@In
	public boolean $$fFLAG = false;

	@Description("Import as Double Precision Data (default: Integer)")
	@In
	public boolean $$dFLAG = false;

	@Description("Signed data (high bit means negative value)")
	@In
	public boolean $$sFLAG = false;

	@Description("Byte Swap the Data During Import")
	@In
	public boolean $$bFLAG = false;

	@Description("Get region info from GMT style header")
	@In
	public boolean $$hFLAG = false;

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
