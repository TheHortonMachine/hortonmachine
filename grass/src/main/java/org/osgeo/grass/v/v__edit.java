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

@Description("Edits a vector map, allows adding, deleting and modifying selected vector features.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, editing, geometry")
@Label("Grass Vector Modules")
@Name("v__edit")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__edit {

	@UI("infile")
	@Description("Name of vector map to edit")
	@In
	public String $$mapPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid";

	@Description("Tool")
	@In
	public String $$toolPARAMETER;

	@Description("If not given (or \"-\") reads from standard input (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Difference in x,y direction for moving feature or vertex (optional)")
	@In
	public String $$movePARAMETER;

	@Description("'-1' for threshold based on the current resolution settings (optional)")
	@In
	public String $$threshPARAMETER = "-1,0,0";

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$idsPARAMETER;

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$catsPARAMETER;

	@Description("List of point coordinates (optional)")
	@In
	public String $$coordsPARAMETER;

	@Description("Bounding box for selecting features (optional)")
	@In
	public String $$bboxPARAMETER;

	@Description("Polygon for selecting features (optional)")
	@In
	public String $$polygonPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("For 'shorter' use negative threshold value, positive value for 'longer' (optional)")
	@In
	public String $$queryPARAMETER;

	@UI("infile")
	@Description("Name of background vector map(s) (optional)")
	@In
	public String $$bgmapPARAMETER;

	@Description("Snap added or modified features in the given threshold to the nearest existing feature (optional)")
	@In
	public String $$snapPARAMETER = "no";

	@Description("Pair: value,step (e.g. 1100,10) (optional)")
	@In
	public String $$zbulkPARAMETER;

	@Description("Reverse selection")
	@In
	public boolean $$rFLAG = false;

	@Description("Close added boundaries (using threshold distance)")
	@In
	public boolean $$cFLAG = false;

	@Description("Do not expect header of input data")
	@In
	public boolean $$nFLAG = false;

	@Description("Do not build topology")
	@In
	public boolean $$tFLAG = false;

	@Description("Modify only first found feature in bounding box")
	@In
	public boolean $$1FLAG = false;

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
