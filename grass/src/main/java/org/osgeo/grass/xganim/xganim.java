package org.osgeo.grass.xganim;

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

@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Name("xganim")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class xganim {

	@UI("infile,grassfile")
	@Description("Raster file(s) for View1")
	@In
	public String $$view1PARAMETER;

	@UI("infile,grassfile")
	@Description("Raster file(s) for View2 (optional)")
	@In
	public String $$view2PARAMETER;

	@UI("infile,grassfile")
	@Description("Raster file(s) for View3 (optional)")
	@In
	public String $$view3PARAMETER;

	@UI("infile,grassfile")
	@Description("Raster file(s) for View4 (optional)")
	@In
	public String $$view4PARAMETER;

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
