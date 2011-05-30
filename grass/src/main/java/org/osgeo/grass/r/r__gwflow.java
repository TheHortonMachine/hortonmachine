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

@Description("Numerical calculation program for transient, confined and unconfined groundwater flow in two dimensions.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__gwflow")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__gwflow {

	@UI("infile")
	@Description("The initial piezometric head in [m]")
	@In
	public String $$pheadPARAMETER;

	@UI("infile")
	@Description("Boundary condition status, 0-inactive, 1-active, 2-dirichlet")
	@In
	public String $$statusPARAMETER;

	@UI("infile")
	@Description("X-part of the hydraulic conductivity tensor in [m/s]")
	@In
	public String $$hc_xPARAMETER;

	@UI("infile")
	@Description("Y-part of the hydraulic conductivity tensor in [m/s]")
	@In
	public String $$hc_yPARAMETER;

	@UI("infile")
	@Description("Water sources and sinks in [m^3/s] (optional)")
	@In
	public String $$qPARAMETER;

	@UI("infile")
	@Description("Specific yield in [1/m]")
	@In
	public String $$sPARAMETER;

	@UI("infile")
	@Description("Recharge map e.g: 6*10^-9 per cell in [m^3/s*m^2] (optional)")
	@In
	public String $$rPARAMETER;

	@UI("infile")
	@Description("Top surface of the aquifer in [m]")
	@In
	public String $$topPARAMETER;

	@UI("infile")
	@Description("Bottom surface of the aquifer in [m]")
	@In
	public String $$bottomPARAMETER;

	@UI("outfile")
	@Description("The map storing the numerical result [m]")
	@In
	public String $$outputPARAMETER;

	@UI("outfile")
	@Description("Calculate the groundwater filter velocity vector field [m/s] and write the x, and y components to maps named name_[xy] (optional)")
	@In
	public String $$velocityPARAMETER;

	@Description("The type of groundwater flow (optional)")
	@In
	public String $$typePARAMETER = "confined";

	@UI("infile")
	@Description("The height of the river bed in [m] (optional)")
	@In
	public String $$river_bedPARAMETER;

	@UI("infile")
	@Description("Water level (head) of the river with leakage connection in [m] (optional)")
	@In
	public String $$river_headPARAMETER;

	@UI("infile")
	@Description("The leakage coefficient of the river bed in [1/s]. (optional)")
	@In
	public String $$river_leakPARAMETER;

	@UI("infile")
	@Description("The height of the drainage bed in [m] (optional)")
	@In
	public String $$drain_bedPARAMETER;

	@UI("infile")
	@Description("The leakage coefficient of the drainage bed in [1/s] (optional)")
	@In
	public String $$drain_leakPARAMETER;

	@Description("The calculation time in seconds")
	@In
	public String $$dtPARAMETER = "86400";

	@Description("Maximum number of iteration used to solver the linear equation system (optional)")
	@In
	public String $$maxitPARAMETER = "100000";

	@Description("Error break criteria for iterative solvers (jacobi, sor, cg or bicgstab) (optional)")
	@In
	public String $$errorPARAMETER = "0.0000000001";

	@Description("The type of solver which should solve the symmetric linear equation system (optional)")
	@In
	public String $$solverPARAMETER = "cg";

	@Description("The relaxation parameter used by the jacobi and sor solver for speedup or stabilizing (optional)")
	@In
	public String $$relaxPARAMETER = "1";

	@Description("Use a sparse matrix, only available with iterative solvers")
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
