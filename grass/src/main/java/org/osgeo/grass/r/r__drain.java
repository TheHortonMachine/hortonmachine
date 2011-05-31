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

@Description("Traces a flow through an elevation model on a raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__drain")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__drain {

	@UI("infile,grassfile")
	@Description("Name of elevation raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Map coordinates of starting point(s) (E,N) (optional)")
	@In
	public String $$coordinatePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of vector map(s) containing starting point(s) (optional)")
	@In
	public String $$vector_pointsPARAMETER;

	@Description("Copy input cell values on output")
	@In
	public boolean $$cFLAG = false;

	@Description("Accumulate input values along the path")
	@In
	public boolean $$aFLAG = false;

	@Description("Count cell numbers along the path")
	@In
	public boolean $$nFLAG = false;

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
