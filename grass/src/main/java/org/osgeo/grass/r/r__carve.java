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

@Description("Takes vector stream data, transforms it to raster and subtracts depth from the output DEM.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__carve")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__carve {

	@UI("infile,grassfile")
	@Description("Name of input raster elevation map")
	@In
	public String $$rastPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of vector input map containing stream(s)")
	@In
	public String $$vectPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map for adjusted stream points (optional)")
	@In
	public String $$pointsPARAMETER;

	@Description("Stream width (in meters). Default is raster cell width (optional)")
	@In
	public String $$widthPARAMETER;

	@Description("Additional stream depth (in meters) (optional)")
	@In
	public String $$depthPARAMETER;

	@Description("No flat areas allowed in flow direction")
	@In
	public boolean $$nFLAG = false;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

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
