package org.osgeo.grass.r3;

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

@Description("Calculates new grid3D volume from r3.mapcalc expression.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Label("Grass Raster 3D Modules")
@Name("r3__mapcalculator")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__mapcalculator {

	@Description("A (grid3D file) (optional)")
	@In
	public String $$agridPARAMETER;

	@Description("B (grid3D file) (optional)")
	@In
	public String $$bgridPARAMETER;

	@Description("C (grid3D file) (optional)")
	@In
	public String $$cgridPARAMETER;

	@Description("D (grid3D file) (optional)")
	@In
	public String $$dgridPARAMETER;

	@Description("E (grid3D file) (optional)")
	@In
	public String $$egridPARAMETER;

	@Description("F (grid3D file) (optional)")
	@In
	public String $$fgridPARAMETER;

	@Description("Formula (e.g. A-B or A*C+B)")
	@In
	public String $$formulaPARAMETER;

	@Description("Name for output grid3D volume")
	@In
	public String $$outfilePARAMETER;

	@Description("Show help (optional)")
	@In
	public String $$helpPARAMETER = "-";

	@Description("Expert mode (enter a set of r3.mapcalc expressions)")
	@In
	public boolean $$eFLAG = false;

	@Description("Do not overwrite existing map")
	@In
	public boolean $$oFLAG = false;

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
