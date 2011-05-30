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

@Description("Filters and generates a depressionless elevation map and a flow direction map from a given elevation layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__fill__dir")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__fill__dir {

	@UI("infile")
	@Description("Name of existing raster map containing elevation surface")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Output elevation raster map after filling")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile")
	@Description("Output direction raster map")
	@In
	public String $$directionPARAMETER;

	@UI("outfile")
	@Description("Output raster map of problem areas (optional)")
	@In
	public String $$areasPARAMETER;

	@Description("Output aspect direction format (agnps, answers, or grass) (optional)")
	@In
	public String $$typePARAMETER = "grass";

	@Description("Find unresolved areas only")
	@In
	public boolean $$fFLAG = false;

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
