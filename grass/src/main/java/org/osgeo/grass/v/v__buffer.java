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

@Description("Creates a buffer around features of given type (areas must contain centroid).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, buffer")
@Label("Grass Vector Modules")
@Name("v__buffer")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__buffer {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Buffer distance along major axis in map units (optional)")
	@In
	public String $$distancePARAMETER;

	@Description("Buffer distance along minor axis in map units (optional)")
	@In
	public String $$minordistancePARAMETER;

	@Description("Angle of major axis in degrees (optional)")
	@In
	public String $$anglePARAMETER = "0";

	@Description("Name of column to use for buffer distances (optional)")
	@In
	public String $$bufcolumnPARAMETER;

	@Description("Scaling factor for attribute column values (optional)")
	@In
	public String $$scalePARAMETER = "1.0";

	@Description("Maximum distance between theoretical arc and polygon segments as multiple of buffer (optional)")
	@In
	public String $$tolerancePARAMETER = "0.01";

	@Description("This does nothing. It is retained for backwards compatibility (optional)")
	@In
	public String $$debugPARAMETER;

	@Description("Buffer distance in map units (optional)")
	@In
	public String $$bufferPARAMETER;

	@Description("Make outside corners straight")
	@In
	public boolean $$sFLAG = false;

	@Description("Don't make caps at the ends of polylines")
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
