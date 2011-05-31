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

@Description("It optionally produces raster maps to contain backlink UTM coordinates for tracing spread paths.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__spread")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__spread {

	@UI("infile,grassfile")
	@Description("Name of raster map containing MAX rate of spread (ROS) (cm/min)")
	@In
	public String $$maxPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing DIRections of max ROS (degree)")
	@In
	public String $$dirPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing BASE ROS (cm/min)")
	@In
	public String $$basePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing STARTing sources")
	@In
	public String $$startPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing max SPOTting DISTance (m) (required w/ -s) (optional)")
	@In
	public String $$spot_distPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing midflame Wind SPEED (ft/min) (required w/ -s) (optional)")
	@In
	public String $$w_speedPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing fine Fuel MOISture of the cell receiving a spotting firebrand (%) (required w/ -s) (optional)")
	@In
	public String $$f_moisPARAMETER;

	@Description("Basic sampling window SIZE needed to meet certain accuracy (3) (optional)")
	@In
	public String $$least_sizePARAMETER;

	@Description("Sampling DENSity for additional COMPutin (range: 0.0 - 1.0 (0.5)) (optional)")
	@In
	public String $$comp_densPARAMETER;

	@Description("INITial TIME for current simulation (0) (min) (optional)")
	@In
	public String $$init_timePARAMETER;

	@Description("Simulating time duration LAG (fill the region) (min) (optional)")
	@In
	public String $$lagPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map as a display backdrop (optional)")
	@In
	public String $$backdropPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name of raster map to contain OUTPUT spread time (min)")
	@In
	public String $$outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name of raster map to contain X_BACK coordinates (optional)")
	@In
	public String $$x_outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name of raster map to contain Y_BACK coordinates (optional)")
	@In
	public String $$y_outputPARAMETER;

	@Description("Run VERBOSELY")
	@In
	public boolean $$vFLAG = false;

	@Description("DISPLAY 'live' spread process on screen")
	@In
	public boolean $$dFLAG = false;

	@Description("For wildfires: consider SPOTTING effect")
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
