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

@Description("Converts files in DXF format to GRASS vector map format.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import")
@Label("Grass Vector Modules")
@Name("v__in__dxf")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__dxf {

	@Description("Name of input DXF file")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("List of layers to import (optional)")
	@In
	public String $$layersPARAMETER;

	@Description("Ignore the map extent of DXF file")
	@In
	public boolean $$eFLAG = false;

	@Description("Do not create attribute tables")
	@In
	public boolean $$tFLAG = false;

	@Description("Do not build topology")
	@In
	public boolean $$bFLAG = false;

	@Description("Import polyface meshes as 3D wire frame")
	@In
	public boolean $$fFLAG = false;

	@Description("List available layers and exit")
	@In
	public boolean $$lFLAG = false;

	@Description("Invert selection by layers (don't import layers in list)")
	@In
	public boolean $$iFLAG = false;

	@Description("Import all objects into one layer")
	@In
	public boolean $$1FLAG = false;

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
