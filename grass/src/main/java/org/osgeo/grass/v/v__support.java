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

@Description("Updates vector map metadata.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, metadata")
@Label("Grass/Vector Modules")
@Name("v__support")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__support {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Organization where vector map was created (optional)")
	@In
	public String $$organizationPARAMETER;

	@Description("Date of vector map digitization (e.g., \"15 Mar 2007\") (optional)")
	@In
	public String $$datePARAMETER;

	@Description("Person who created vector map (optional)")
	@In
	public String $$personPARAMETER;

	@Description("Vector map title (optional)")
	@In
	public String $$map_namePARAMETER;

	@Description("Date when the source map was originally produced (optional)")
	@In
	public String $$map_datePARAMETER;

	@Description("Vector map scale number (e.g., 24000) (optional)")
	@In
	public String $$scalePARAMETER;

	@Description("Vector map projection zone (optional)")
	@In
	public String $$zonePARAMETER;

	@Description("Vector map digitizing threshold number (e.g., 0.5) (optional)")
	@In
	public String $$threshPARAMETER;

	@Description("Text to append to the comment line of the map's metadata file (optional)")
	@In
	public String $$commentPARAMETER;

	@Description("Command line to store into vector map history file (used for vector scripts) (optional)")
	@In
	public String $$cmdhistPARAMETER;

	@Description("Replace comment instead of appending it")
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
