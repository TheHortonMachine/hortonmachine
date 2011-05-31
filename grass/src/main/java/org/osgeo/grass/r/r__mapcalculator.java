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

@Description("Calculate new raster map from a r.mapcalc expression.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Label("Grass Raster Modules")
@Name("r__mapcalculator")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__mapcalculator {

	@UI("infile,grassfile")
	@Description("A (optional)")
	@In
	public String $$amapPARAMETER;

	@UI("infile,grassfile")
	@Description("B (optional)")
	@In
	public String $$bmapPARAMETER;

	@UI("infile,grassfile")
	@Description("C (optional)")
	@In
	public String $$cmapPARAMETER;

	@UI("infile,grassfile")
	@Description("D (optional)")
	@In
	public String $$dmapPARAMETER;

	@UI("infile,grassfile")
	@Description("E (optional)")
	@In
	public String $$emapPARAMETER;

	@UI("infile,grassfile")
	@Description("F (optional)")
	@In
	public String $$fmapPARAMETER;

	@Description("Formula (e.g. A-B or A*C+B)")
	@In
	public String $$formulaPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outfilePARAMETER;

	@Description("Show help (optional)")
	@In
	public String $$helpPARAMETER = "-";

	@Description("Expert mode (enter a set of r.mapcalc expressions)")
	@In
	public boolean $$eFLAG = false;

	@Description("Do not overwrite existing map")
	@In
	public boolean $$oFLAG = false;

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
