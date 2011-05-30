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

@Description("Overland flow hydrologic simulation using path sampling method (SIMWE)")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, flow, hydrology")
@Label("Grass Raster Modules")
@Name("r__sim__water")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__sim__water {

	@UI("infile")
	@Description("Name of the elevation raster map [m]")
	@In
	public String $$elevinPARAMETER;

	@UI("infile")
	@Description("Name of the x-derivatives raster map [m/m]")
	@In
	public String $$dxinPARAMETER;

	@UI("infile")
	@Description("Name of the y-derivatives raster map [m/m]")
	@In
	public String $$dyinPARAMETER;

	@UI("infile")
	@Description("Name of the rainfall excess rate (rain-infilt) raster map [mm/hr] (optional)")
	@In
	public String $$rainPARAMETER;

	@Description("Rainfall excess rate unique value [mm/hr] (optional)")
	@In
	public String $$rain_valPARAMETER = "50";

	@UI("infile")
	@Description("Name of the runoff infiltration rate raster map [mm/hr] (optional)")
	@In
	public String $$infilPARAMETER;

	@Description("Runoff infiltration rate unique value [mm/hr] (optional)")
	@In
	public String $$infil_valPARAMETER = "0.0";

	@UI("infile")
	@Description("Name of the Mannings n raster map (optional)")
	@In
	public String $$maninPARAMETER;

	@Description("Mannings n unique value (optional)")
	@In
	public String $$manin_valPARAMETER = "0.1";

	@UI("infile")
	@Description("Name of the flow controls raster map (permeability ratio 0-1) (optional)")
	@In
	public String $$trapsPARAMETER;

	@UI("outfile")
	@Description("Output water depth raster map [m] (optional)")
	@In
	public String $$depthPARAMETER;

	@UI("outfile")
	@Description("Output water discharge raster map [m3/s] (optional)")
	@In
	public String $$dischPARAMETER;

	@UI("outfile")
	@Description("Output simulation error raster map [m] (optional)")
	@In
	public String $$errPARAMETER;

	@Description("Number of walkers, default is twice the no. of cells (optional)")
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

	@Description("Threshold water depth [m] (diffusion increases after this water depth is reached) (optional)")
	@In
	public String $$hmaxPARAMETER = "0.3";

	@Description("Diffusion increase constant (optional)")
	@In
	public String $$halphaPARAMETER = "4.0";

	@Description("Weighting factor for water flow velocity vector (optional)")
	@In
	public String $$hbetaPARAMETER = "0.5";

	@Description("Time-series output")
	@In
	public boolean $$tFLAG = false;

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
