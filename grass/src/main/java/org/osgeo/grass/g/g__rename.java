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

@Description("Renames data base element files in the user's current mapset.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, map management")
@Label("Grass General Modules")
@Name("g__rename")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__rename {

	@UI("infile")
	@Description("rast file(s) to be renamed (optional)")
	@In
	public String $$rastPARAMETER;

	@UI("infile")
	@Description("rast3d file(s) to be renamed (optional)")
	@In
	public String $$rast3dPARAMETER;

	@UI("infile")
	@Description("vect file(s) to be renamed (optional)")
	@In
	public String $$vectPARAMETER;

	@UI("infile")
	@Description("oldvect file(s) to be renamed (optional)")
	@In
	public String $$oldvectPARAMETER;

	@UI("infile")
	@Description("asciivect file(s) to be renamed (optional)")
	@In
	public String $$asciivectPARAMETER;

	@UI("infile")
	@Description("icon file(s) to be renamed (optional)")
	@In
	public String $$iconPARAMETER;

	@UI("infile")
	@Description("labels file(s) to be renamed (optional)")
	@In
	public String $$labelsPARAMETER;

	@UI("infile")
	@Description("sites file(s) to be renamed (optional)")
	@In
	public String $$sitesPARAMETER;

	@UI("infile")
	@Description("region file(s) to be renamed (optional)")
	@In
	public String $$regionPARAMETER;

	@UI("infile")
	@Description("region3d file(s) to be renamed (optional)")
	@In
	public String $$region3dPARAMETER;

	@UI("infile")
	@Description("group file(s) to be renamed (optional)")
	@In
	public String $$groupPARAMETER;

	@UI("infile")
	@Description("3dview file(s) to be renamed (optional)")
	@In
	public String $$3dviewPARAMETER;

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
