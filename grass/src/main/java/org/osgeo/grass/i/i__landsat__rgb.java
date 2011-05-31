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

@Description("Performs auto-balancing of colors for LANDSAT images.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, imagery, colors")
@Label("Grass Imagery Modules")
@Name("i__landsat__rgb")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__landsat__rgb {

	@UI("infile,grassfile")
	@Description("LANDSAT red channel")
	@In
	public String $$redPARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT green channel")
	@In
	public String $$greenPARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT blue channel")
	@In
	public String $$bluePARAMETER;

	@Description("Cropping intensity (upper brightness level) (optional)")
	@In
	public String $$strengthPARAMETER = "98";

	@Description("Extend colors to full range of data on each channel")
	@In
	public boolean $$fFLAG = false;

	@Description("Preserve relative colors, adjust brightness only")
	@In
	public boolean $$pFLAG = false;

	@Description("Reset to standard color range")
	@In
	public boolean $$rFLAG = false;

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
