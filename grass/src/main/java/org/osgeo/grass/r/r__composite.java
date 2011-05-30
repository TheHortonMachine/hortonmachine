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

@Description("Combines red, green and blue raster maps into a single composite raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, composite")
@Label("Grass Raster Modules")
@Name("r__composite")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__composite {

	@UI("infile")
	@Description("Name of raster map to be used for <red>")
	@In
	public String $$redPARAMETER;

	@UI("infile")
	@Description("Name of raster map to be used for <green>")
	@In
	public String $$greenPARAMETER;

	@UI("infile")
	@Description("Name of raster map to be used for <blue>")
	@In
	public String $$bluePARAMETER;

	@Description("Number of levels to be used for each component (optional)")
	@In
	public String $$levelsPARAMETER = "32";

	@Description("Number of levels to be used for <red> (optional)")
	@In
	public String $$lev_redPARAMETER;

	@Description("Number of levels to be used for <green> (optional)")
	@In
	public String $$lev_greenPARAMETER;

	@Description("Number of levels to be used for <blue> (optional)")
	@In
	public String $$lev_bluePARAMETER;

	@UI("outfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Dither")
	@In
	public boolean $$dFLAG = false;

	@Description("Use closest color")
	@In
	public boolean $$cFLAG = false;

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
