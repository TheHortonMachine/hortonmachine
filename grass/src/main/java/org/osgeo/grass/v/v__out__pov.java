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

@Description("Converts to POV-Ray format, GRASS x,y,z -> POV-Ray x,z,y")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, export")
@Label("Grass/Vector Modules")
@Name("v__out__pov")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__out__pov {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,area,face";

	@Description("Output file")
	@In
	public String $$outputPARAMETER;

	@Description("Radius of sphere for points and tube for lines. May be also variable, e.g. grass_r. (optional)")
	@In
	public String $$sizePARAMETER = "10";

	@Description("Modifier for z coordinates, this string is appended to each z coordinate. 		Examples: '*10', '+1000', '*10+100', '*exaggeration' (optional)")
	@In
	public String $$zmodPARAMETER = "";

	@Description("Object modifier (OBJECT_MODIFIER in POV-Ray documentation). 		Example: \"pigment { color red 0 green 1 blue 0 }\" (optional)")
	@In
	public String $$objmodPARAMETER = "";

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
