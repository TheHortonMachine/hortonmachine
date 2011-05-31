package org.osgeo.grass.r3;

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

@Description("Numerical calculation program for transient, confined groundwater flow in three dimensions")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass Raster 3D Modules")
@Name("r3__gwflow")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__gwflow {

	@UI("infile,grassfile")
	@Description("The initial piezometric head in [m]")
	@In
	public String $$pheadPARAMETER;

	@UI("infile,grassfile")
	@Description("The status for each cell, = 0 - inactive, 1 - active, 2 - dirichlet")
	@In
	public String $$statusPARAMETER;

	@UI("infile,grassfile")
	@Description("The x-part of the hydraulic conductivity tensor in [m/s]")
	@In
	public String $$hc_xPARAMETER;

	@UI("infile,grassfile")
	@Description("The y-part of the hydraulic conductivity tensor in [m/s]")
	@In
	public String $$hc_yPARAMETER;

	@UI("infile,grassfile")
	@Description("The z-part of the hydraulic conductivity tensor in [m/s]")
	@In
	public String $$hc_zPARAMETER;

	@UI("infile,grassfile")
	@Description("Sources and sinks in [m^3/s] (optional)")
	@In
	public String $$qPARAMETER;

	@UI("infile,grassfile")
	@Description("Specific yield in 1/m")
	@In
	public String $$sPARAMETER;

	@UI("infile,grassfile")
	@Description("Recharge raster map in m^3/s (optional)")
	@In
	public String $$rPARAMETER;

	@UI("outfile,grassfile")
	@Description("The piezometric head result of the numerical calculation will be written to this map")
	@In
	public String $$outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Calculate the groundwater distance velocity vector field and write the x, y, and z components to maps named name_[xyz]. Name is basename for the new raster3d maps (optional)")
	@In
	public String $$velocityPARAMETER;

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

	@Description("Use G3D mask (if exists)")
	@In
	public boolean $$mFLAG = false;

	@Description("Use a sparse linear equation system, only available with iterative solvers")
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
