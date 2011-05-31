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

@Description("Converts vector map to 3D by sampling of elevation raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, geometry, sampling")
@Label("Grass Vector Modules")
@Name("v__drape")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__drape {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,centroid,line,boundary,face,kernel";

	@UI("infile,grassfile")
	@Description("Elevation raster map for height extraction (optional)")
	@In
	public String $$rastPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Sampling method (optional)")
	@In
	public String $$methodPARAMETER = "nearest";

	@Description("Scale sampled raster values (optional)")
	@In
	public String $$scalePARAMETER = "1.0";

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Layer is only used for WHERE SQL statement (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Will set Z to this value, if value from raster map can not be read (optional)")
	@In
	public String $$null_valuePARAMETER;

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
