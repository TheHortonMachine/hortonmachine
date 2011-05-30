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

@Description("Converts a GRASS binary vector map to VTK ASCII output.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass Vector Modules")
@Name("v__out__vtk")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__out__vtk {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Path to resulting VTK file (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,kernel,centroid,line,boundary,area,face";

	@Description("Number of significant digits (floating point only) (optional)")
	@In
	public String $$dpPARAMETER;

	@Description("Scale factor for elevation (optional)")
	@In
	public String $$scalePARAMETER = "1.0";

	@Description("Layer number (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Correct the coordinates to fit the VTK-OpenGL precision")
	@In
	public boolean $$cFLAG = false;

	@Description("Export numeric attribute table fields as VTK scalar variables")
	@In
	public boolean $$nFLAG = false;

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
