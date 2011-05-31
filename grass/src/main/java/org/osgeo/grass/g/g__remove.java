package org.osgeo.grass.g;

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

@Description("Removes data base element files from the user's current mapset.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, map management")
@Label("Grass General Modules")
@Name("g__remove")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__remove {

	@UI("infile,grassfile")
	@Description("rast file(s) to be removed (optional)")
	@In
	public String $$rastPARAMETER;

	@UI("infile,grassfile")
	@Description("rast3d file(s) to be removed (optional)")
	@In
	public String $$rast3dPARAMETER;

	@UI("infile,grassfile")
	@Description("vect file(s) to be removed (optional)")
	@In
	public String $$vectPARAMETER;

	@UI("infile,grassfile")
	@Description("oldvect file(s) to be removed (optional)")
	@In
	public String $$oldvectPARAMETER;

	@UI("infile,grassfile")
	@Description("asciivect file(s) to be removed (optional)")
	@In
	public String $$asciivectPARAMETER;

	@UI("infile,grassfile")
	@Description("icon file(s) to be removed (optional)")
	@In
	public String $$iconPARAMETER;

	@UI("infile,grassfile")
	@Description("labels file(s) to be removed (optional)")
	@In
	public String $$labelsPARAMETER;

	@UI("infile,grassfile")
	@Description("sites file(s) to be removed (optional)")
	@In
	public String $$sitesPARAMETER;

	@UI("infile,grassfile")
	@Description("region file(s) to be removed (optional)")
	@In
	public String $$regionPARAMETER;

	@UI("infile,grassfile")
	@Description("region3d file(s) to be removed (optional)")
	@In
	public String $$region3dPARAMETER;

	@UI("infile,grassfile")
	@Description("group file(s) to be removed (optional)")
	@In
	public String $$groupPARAMETER;

	@UI("infile,grassfile")
	@Description("3dview file(s) to be removed (optional)")
	@In
	public String $$3dviewPARAMETER;

	@Description("Force remove")
	@In
	public boolean $$fFLAG = false;

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
