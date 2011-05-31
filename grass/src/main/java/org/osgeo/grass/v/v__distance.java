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

@Description("Finds the nearest element in vector map 'to' for elements in vector map 'from'.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass/Vector Modules")
@Name("v__distance")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__distance {

	@UI("infile,grassfile")
	@Description("Name of existing vector map (from)")
	@In
	public String $$fromPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of existing vector map (to)")
	@In
	public String $$toPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$from_typePARAMETER = "point";

	@Description("Feature type (optional)")
	@In
	public String $$to_typePARAMETER = "point,line,area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$from_layerPARAMETER = "1";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$to_layerPARAMETER = "1";

	@UI("outfile,grassfile")
	@Description("Name for output vector map containing lines connecting nearest elements (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Maximum distance or -1 for no limit (optional)")
	@In
	public String $$dmaxPARAMETER = "-1";

	@Description("Minimum distance or -1 for no limit (optional)")
	@In
	public String $$dminPARAMETER = "-1";

	@Description("Values describing the relation between two nearest features")
	@In
	public String $$uploadPARAMETER;

	@Description("Column name(s) where values specified by 'upload' option will be uploaded")
	@In
	public String $$columnPARAMETER;

	@Description("Column name of nearest feature (used with upload=to_attr) (optional)")
	@In
	public String $$to_columnPARAMETER;

	@Description("Name of table created for output when the distance to all flag is used (optional)")
	@In
	public String $$tablePARAMETER;

	@Description("First column is always category of 'from' feature called from_cat")
	@In
	public boolean $$pFLAG = false;

	@Description("The output is written to stdout but may be uploaded to a new table created by this module. From categories are may be multiple.")
	@In
	public boolean $$aFLAG = false;

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
