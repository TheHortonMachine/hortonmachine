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

@Description("Creates a vector map from ASCII points file or ASCII vector file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import")
@Label("Grass/Vector Modules")
@Name("v__in__ascii")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__ascii {

	@Description("ASCII file to be imported, if not given reads from standard input (optional)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Input file format (optional)")
	@In
	public String $$formatPARAMETER = "point";

	@Description("Field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Number of header lines to skip at top of input file (points mode) (optional)")
	@In
	public String $$skipPARAMETER = "0";

	@Description("For example: 'x double precision, y double precision, cat int, name varchar(10)' (optional)")
	@In
	public String $$columnsPARAMETER;

	@Description("Number of column used as x coordinate (first column is 1) for points mode (optional)")
	@In
	public String $$xPARAMETER = "1";

	@Description("Number of column used as y coordinate (first column is 1) for points mode (optional)")
	@In
	public String $$yPARAMETER = "2";

	@Description("If 0, z coordinate is not used (optional)")
	@In
	public String $$zPARAMETER = "0";

	@Description("If 0, unique category is assigned to each row and written to new column 'cat' (optional)")
	@In
	public String $$catPARAMETER = "0";

	@Description("Create 3D vector map")
	@In
	public boolean $$zFLAG = false;

	@Description("Create a new empty vector map and exit. Nothing is read from input")
	@In
	public boolean $$eFLAG = false;

	@Description("Don't expect a header when reading in standard format")
	@In
	public boolean $$nFLAG = false;

	@Description("Do not create table in points mode")
	@In
	public boolean $$tFLAG = false;

	@Description("Do not build topology in points mode")
	@In
	public boolean $$bFLAG = false;

	@Description("Only import points falling within current region (points mode)")
	@In
	public boolean $$rFLAG = false;

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
