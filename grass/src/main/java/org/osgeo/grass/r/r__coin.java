package org.osgeo.grass.r;

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

@Description("Tabulates the mutual occurrence (coincidence) of categories for two raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__coin")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__coin {

	@UI("infile,grassfile")
	@Description("Name of first raster map")
	@In
	public String $$map1PARAMETER;

	@UI("infile,grassfile")
	@Description("Name of second raster map")
	@In
	public String $$map2PARAMETER;

	@Description("c(ells), p(ercent), x(percent of category [column]), y(percent of category [row]), a(cres), h(ectares), k(square kilometers), m(square miles)")
	@In
	public String $$unitsPARAMETER;

	@Description("Wide report, 132 columns (default: 80)")
	@In
	public boolean $$wFLAG = false;

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
