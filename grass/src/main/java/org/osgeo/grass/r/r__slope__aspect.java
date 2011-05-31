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

@Description("Aspect is calculated counterclockwise from east.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, terrain")
@Label("Grass Raster Modules")
@Name("r__slope__aspect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__slope__aspect {

	@UI("infile,grassfile")
	@Description("Name of elevation raster map")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output slope raster map (optional)")
	@In
	public String $$slopePARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output aspect raster map (optional)")
	@In
	public String $$aspectPARAMETER;

	@Description("Format for reporting the slope (optional)")
	@In
	public String $$formatPARAMETER = "degrees";

	@Description("Type of output aspect and slope maps (optional)")
	@In
	public String $$precPARAMETER = "float";

	@UI("outfile,grassfile")
	@Description("Name for output profile curvature raster map (optional)")
	@In
	public String $$pcurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output tangential curvature raster map (optional)")
	@In
	public String $$tcurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output first order partial derivative dx (E-W slope) raster map (optional)")
	@In
	public String $$dxPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output first order partial derivative dy (N-S slope) raster map (optional)")
	@In
	public String $$dyPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output second order partial derivative dxx raster map (optional)")
	@In
	public String $$dxxPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output second order partial derivative dyy raster map (optional)")
	@In
	public String $$dyyPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output second order partial derivative dxy raster map (optional)")
	@In
	public String $$dxyPARAMETER;

	@Description("Multiplicative factor to convert elevation units to meters (optional)")
	@In
	public String $$zfactorPARAMETER = "1.0";

	@Description("Minimum slope val. (in percent) for which aspect is computed (optional)")
	@In
	public String $$min_slp_allowedPARAMETER = "0.0";

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

	@Description("Do not align the current region to the elevation layer")
	@In
	public boolean $$aFLAG = false;

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
