package org.osgeo.grass.nviz;

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

@Description("nviz - Visualization and animation tool for GRASS data.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, vector, visualization")
@Label("Grass")
@Name("nviz")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class nviz {

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for Elevation (optional)")
	@In
	public String $$elevationPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for Color (optional)")
	@In
	public String $$colorPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of vector lines/areas overlay map(s) (optional)")
	@In
	public String $$vectorPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of vector points overlay file(s) (optional)")
	@In
	public String $$pointsPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of existing 3d raster map (optional)")
	@In
	public String $$volumePARAMETER;

	@Description("Set alternative panel path (optional)")
	@In
	public String $$pathPARAMETER;

	@Description("Execute script file at startup (optional)")
	@In
	public String $$scriptPARAMETER;

	@Description("Load previously saved state file (optional)")
	@In
	public String $$statePARAMETER;

	@Description("Quickstart - Do not load any data")
	@In
	public boolean $$qFLAG = false;

	@Description("Exit after completing script launched from the command line")
	@In
	public boolean $$kFLAG = false;

	@Description("Start in Demo mode (skip the \"please wait\" message)")
	@In
	public boolean $$xFLAG = false;

	@Description("Verbose module output")
	@In
	public boolean $$vFLAG = false;

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
