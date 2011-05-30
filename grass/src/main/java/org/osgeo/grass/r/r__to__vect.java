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

@Description("Converts a raster map into a vector map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__to__vect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__to__vect {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type")
	@In
	public String $$featurePARAMETER = "line";

	@Description("Smooth corners of area features")
	@In
	public boolean $$sFLAG = false;

	@Description("Use raster values as categories instead of unique sequence (CELL only)")
	@In
	public boolean $$vFLAG = false;

	@Description("Write raster values as z coordinate. Table is not created. Currently supported only for points.")
	@In
	public boolean $$zFLAG = false;

	@Description("Do not build vector topology (use with care for massive point export)")
	@In
	public boolean $$bFLAG = false;

	@Description("Quiet - Do not show progress")
	@In
	public boolean $$qFLAG = false;

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
