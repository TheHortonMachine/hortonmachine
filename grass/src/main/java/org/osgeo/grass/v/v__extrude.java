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

@Description("Extrudes flat vector object to 3D with defined height.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, geometry, 3D")
@Label("Grass Vector Modules")
@Name("v__extrude")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__extrude {

	@UI("infile")
	@Description("Name of input 2D vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name of resulting 3D vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Shifting value for z coordinates (optional)")
	@In
	public String $$zshiftPARAMETER = "0";

	@UI("infile")
	@Description("Elevation raster for height extraction (optional)")
	@In
	public String $$elevationPARAMETER;

	@Description("Fixed height for 3D vector objects (optional)")
	@In
	public String $$heightPARAMETER;

	@Description("Name of attribute column with object heights (optional)")
	@In
	public String $$hcolumnPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Trace elevation")
	@In
	public boolean $$tFLAG = false;

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
