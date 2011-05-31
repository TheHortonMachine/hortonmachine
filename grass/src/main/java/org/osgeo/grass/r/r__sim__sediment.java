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

@Description("Sediment transport and erosion/deposition simulation using path sampling method (SIMWE)")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, sediment flow, erosion, deposition")
@Label("Grass/Raster Modules")
@Name("r__sim__sediment")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__sim__sediment {

	@UI("infile,grassfile")
	@Description("Name of the elevation raster map [m]")
	@In
	public String $$elevinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the water depth raster map [m]")
	@In
	public String $$wdepthPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the x-derivatives raster map [m/m]")
	@In
	public String $$dxinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the y-derivatives raster map [m/m]")
	@In
	public String $$dyinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the detachment capacity coefficient raster map [s/m]")
	@In
	public String $$detinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the transport capacity coefficient raster map [s]")
	@In
	public String $$traninPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the critical shear stress raster map [Pa]")
	@In
	public String $$tauinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the Mannings n raster map (optional)")
	@In
	public String $$maninPARAMETER;

	@Description("Name of the Mannings n value (optional)")
	@In
	public String $$maninvalPARAMETER = "0.1";

	@UI("outfile,grassfile")
	@Description("Output transport capacity raster map [kg/ms] (optional)")
	@In
	public String $$tcPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output transp.limited erosion-deposition raster map [kg/m2s] (optional)")
	@In
	public String $$etPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output sediment concentration raster map [particle/m3] (optional)")
	@In
	public String $$concPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output sediment flux raster map [kg/ms] (optional)")
	@In
	public String $$fluxPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output erosion-deposition raster map [kg/m2s] (optional)")
	@In
	public String $$erdepPARAMETER;

	@Description("Number of walkers (optional)")
	@In
	public String $$nwalkPARAMETER;

	@Description("Time used for iterations [minutes] (optional)")
	@In
	public String $$niterPARAMETER = "10";

	@Description("Time interval for creating output maps [minutes] (optional)")
	@In
	public String $$outiterPARAMETER = "2";

	@Description("Water diffusion constant (optional)")
	@In
	public String $$diffcPARAMETER = "0.8";

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
