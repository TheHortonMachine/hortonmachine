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

@Description("Create Linear Reference System")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LRS, networking")
@Label("Grass Vector Modules")
@Name("v__lrs__create")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lrs__create {

	@UI("infile,grassfile")
	@Description("Input vector map containing lines")
	@In
	public String $$in_linesPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map where oriented lines are written")
	@In
	public String $$out_linesPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map of errors (optional)")
	@In
	public String $$errPARAMETER;

	@UI("infile,grassfile")
	@Description("Input vector map containing reference points")
	@In
	public String $$pointsPARAMETER;

	@Description("Line layer (optional)")
	@In
	public String $$llayerPARAMETER = "1";

	@Description("Point layer (optional)")
	@In
	public String $$playerPARAMETER = "1";

	@Description("Column containing line identifiers for lines")
	@In
	public String $$lidcolPARAMETER;

	@Description("Column containing line identifiers for points")
	@In
	public String $$pidcolPARAMETER;

	@Description("Column containing milepost position for the beginning of next segment (optional)")
	@In
	public String $$start_mpPARAMETER = "start_mp";

	@Description("Column containing offset from milepost for the beginning of next segment (optional)")
	@In
	public String $$start_offPARAMETER = "start_off";

	@Description("Column containing milepost position for the end of previous segment (optional)")
	@In
	public String $$end_mpPARAMETER = "end_mp";

	@Description("Column containing offset from milepost for the end of previous segment (optional)")
	@In
	public String $$end_offPARAMETER = "end_off";

	@Description("Driver name for reference system table (optional)")
	@In
	public String $$rsdriverPARAMETER = "dbf";

	@Description("Database name for reference system table (optional)")
	@In
	public String $$rsdatabasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("New table is created by this module")
	@In
	public String $$rstablePARAMETER;

	@Description("Maximum distance of point to line allowed (optional)")
	@In
	public String $$threshPARAMETER = "1";

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
