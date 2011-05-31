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

@Description("Displays charts of vector data in the active frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, cartography")
@Label("Grass/Display Modules")
@Name("d__vect__chart")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__vect__chart {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Chart type (optional)")
	@In
	public String $$ctypePARAMETER = "pie";

	@Description("Attribute columns containing data")
	@In
	public String $$columnsPARAMETER;

	@Description("Column used for pie chart size (optional)")
	@In
	public String $$sizecolPARAMETER;

	@Description("Size of chart (diameter for pie, total width for bar) (optional)")
	@In
	public String $$sizePARAMETER = "40";

	@Description("Scale for size (to get size in pixels) (optional)")
	@In
	public String $$scalePARAMETER = "1";

	@Description("Outline color (optional)")
	@In
	public String $$ocolorPARAMETER = "black";

	@Description("Colors used to fill charts (optional)")
	@In
	public String $$colorsPARAMETER;

	@Description("Maximum value used for bar plot reference (optional)")
	@In
	public String $$max_refPARAMETER;

	@Description("Center the bar chart around a data point")
	@In
	public boolean $$cFLAG = false;

	@Description("Create legend information and send to stdout")
	@In
	public boolean $$lFLAG = false;

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
