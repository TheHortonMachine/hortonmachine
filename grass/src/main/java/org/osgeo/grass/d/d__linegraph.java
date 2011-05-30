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

@Description("Generates and displays simple line graphs in the active graphics monitor display frame.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__linegraph")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__linegraph {

	@Description("Name of data file for X axis of graph")
	@In
	public String $$x_filePARAMETER;

	@Description("Name of data file(s) for Y axis of graph")
	@In
	public String $$y_filePARAMETER;

	@Description("Path to file location (optional)")
	@In
	public String $$directoryPARAMETER = ".";

	@Description("Color for Y data (optional)")
	@In
	public String $$y_colorPARAMETER;

	@Description("Color for axis, tics, numbers, and title (optional)")
	@In
	public String $$title_colorPARAMETER = "black";

	@Description("Title for X data (optional)")
	@In
	public String $$x_titlePARAMETER = "";

	@Description("Title for Y data (optional)")
	@In
	public String $$y_titlePARAMETER = "";

	@Description("Title for Graph (optional)")
	@In
	public String $$titlePARAMETER = "";

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
