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

@Description("Performs an affine transformation (shift, scale and rotate, or GPCs) on vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, transformation")
@Label("Grass Vector Modules")
@Name("v__transform")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__transform {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("If not given, transformation parameters (xshift, yshift, zshift, xscale, yscale, zscale, zrot) are used instead (optional)")
	@In
	public String $$pointsfilePARAMETER;

	@Description("Shifting value for x coordinates (optional)")
	@In
	public String $$xshiftPARAMETER = "0.0";

	@Description("Shifting value for y coordinates (optional)")
	@In
	public String $$yshiftPARAMETER = "0.0";

	@Description("Shifting value for z coordinates (optional)")
	@In
	public String $$zshiftPARAMETER = "0.0";

	@Description("Scaling factor for x coordinates (optional)")
	@In
	public String $$xscalePARAMETER = "1.0";

	@Description("Scaling factor for y coordinates (optional)")
	@In
	public String $$yscalePARAMETER = "1.0";

	@Description("Scaling factor for z coordinates (optional)")
	@In
	public String $$zscalePARAMETER = "1.0";

	@Description("Rotation around z axis in degrees counterclockwise (optional)")
	@In
	public String $$zrotPARAMETER = "0.0";

	@Description("Name of table containing transformation parameters (optional)")
	@In
	public String $$tablePARAMETER;

	@Description("Format: parameter:column, e.g. xshift:xs,yshift:ys,zrot:zr (optional)")
	@In
	public String $$columnsPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Suppress display of residuals or other information")
	@In
	public boolean $$qFLAG = false;

	@Description("Shift all z values to bottom=0")
	@In
	public boolean $$tFLAG = false;

	@Description("Print the transformation matrix to stdout")
	@In
	public boolean $$mFLAG = false;

	@Description("Instead of points use transformation parameters (xshift, yshift, zshift, xscale, yscale, zscale, zrot)")
	@In
	public boolean $$sFLAG = false;

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
