package org.osgeo.grass.d;

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

@Description("Displays a thematic vector area map in the active frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__thematic__area")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__thematic__area {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Data to be classified: column name or expression")
	@In
	public String $$columnPARAMETER;

	@Description("Class breaks, without minimum and maximum (optional)")
	@In
	public String $$breaksPARAMETER;

	@Description("Algorithm to use for classification (optional)")
	@In
	public String $$algorithmPARAMETER;

	@Description("Number of classes to define (optional)")
	@In
	public String $$nbclassesPARAMETER;

	@Description("Colors (one per class).")
	@In
	public String $$colorsPARAMETER;

	@Description("Layer number. If -1, all layers are displayed. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Boundary width (optional)")
	@In
	public String $$bwidthPARAMETER = "0";

	@Description("Boundary color (optional)")
	@In
	public String $$bcolorPARAMETER = "black";

	@Description("Rendering method for filled polygons (optional)")
	@In
	public String $$renderPARAMETER = "l";

	@Description("File in which to save d.graph instructions for legend display (optional)")
	@In
	public String $$legendfilePARAMETER;

	@Description("Create legend information and send to stdout")
	@In
	public boolean $$lFLAG = false;

	@Description("When printing legend info , include extended statistical info from classification algorithm")
	@In
	public boolean $$eFLAG = false;

	@Description("Do not draw map, only output the legend")
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
