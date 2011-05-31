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

@Description("Brovey transform to merge multispectral and high-res panchromatic channels")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, fusion, Brovey")
@Label("Grass/Imagery Modules")
@Name("i__fusion__brovey")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__fusion__brovey {

	@UI("infile,grassfile")
	@Description("Name of input raster map (green: tm2 | qbird_green | spot1)")
	@In
	public String $$ms1PARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map (NIR: tm4 | qbird_nir | spot2)")
	@In
	public String $$ms2PARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map (MIR; tm5 | qbird_red | spot3)")
	@In
	public String $$ms3PARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map (etmpan | qbird_pan | spotpan)")
	@In
	public String $$panPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map prefix (e.g. 'brov')")
	@In
	public String $$outputprefixPARAMETER;

	@Description("LANDSAT sensor")
	@In
	public boolean $$lFLAG = false;

	@Description("QuickBird sensor")
	@In
	public boolean $$qFLAG = false;

	@Description("SPOT sensor")
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
