package org.osgeo.grass.i;

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

@Description("Tasseled Cap (Kauth Thomas) transformation for LANDSAT-TM data")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, imagery")
@Label("Grass Imagery Modules")
@Name("i__tasscap")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__tasscap {

	@UI("infile,grassfile")
	@Description("raster input map (LANDSAT channel 1)")
	@In
	public String $$band1PARAMETER;

	@UI("infile,grassfile")
	@Description("raster input map (LANDSAT channel 2)")
	@In
	public String $$band2PARAMETER;

	@UI("infile,grassfile")
	@Description("raster input map (LANDSAT channel 3)")
	@In
	public String $$band3PARAMETER;

	@UI("infile,grassfile")
	@Description("raster input map (LANDSAT channel 4)")
	@In
	public String $$band4PARAMETER;

	@UI("infile,grassfile")
	@Description("raster input map (LANDSAT channel 5)")
	@In
	public String $$band5PARAMETER;

	@UI("infile,grassfile")
	@Description("raster input map (LANDSAT channel 7)")
	@In
	public String $$band7PARAMETER;

	@UI("outfile,grassfile")
	@Description("raster output TC maps prefix")
	@In
	public String $$outprefixPARAMETER;

	@Description("use transformation rules for LANDSAT-4")
	@In
	public boolean $$4FLAG = false;

	@Description("use transformation rules for LANDSAT-5")
	@In
	public boolean $$5FLAG = false;

	@Description("use transformation rules for LANDSAT-7")
	@In
	public boolean $$7FLAG = false;

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
