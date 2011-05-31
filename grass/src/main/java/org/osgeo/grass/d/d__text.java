package org.osgeo.grass.d;

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

@Description("Draws text in the active display frame on the graphics monitor using the current font.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__text")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__text {

	@Description("Text to display (optional)")
	@In
	public String $$textPARAMETER;

	@Description("Height of letters in percentage of available frame height (optional)")
	@In
	public String $$sizePARAMETER = "5";

	@Description("Text color, either a standard GRASS color or R:G:B triplet (optional)")
	@In
	public String $$colorPARAMETER = "gray";

	@Description("Text background color, either a standard GRASS color or R:G:B triplet (optional)")
	@In
	public String $$bgcolorPARAMETER;

	@Description("The screen line number on which text will begin to be drawn (optional)")
	@In
	public String $$linePARAMETER;

	@Description("Screen position at which text will begin to be drawn (percentage, [0,0] is lower left) (optional)")
	@In
	public String $$atPARAMETER;

	@Description("Text alignment (optional)")
	@In
	public String $$alignPARAMETER = "ll";

	@Description("Rotation angle in degrees (counter-clockwise) (optional)")
	@In
	public String $$rotationPARAMETER = "0";

	@Description("Line spacing (optional)")
	@In
	public String $$linespacingPARAMETER = "1.25";

	@Description("Font name (optional)")
	@In
	public String $$fontPARAMETER;

	@Description("Path to font file (optional)")
	@In
	public String $$pathPARAMETER;

	@Description("Text encoding (only applicable to TrueType fonts) (optional)")
	@In
	public String $$charsetPARAMETER;

	@Description("Use mouse to interactively place text")
	@In
	public boolean $$mFLAG = false;

	@Description("Screen position in pixels ([0,0] is top left)")
	@In
	public boolean $$pFLAG = false;

	@Description("Screen position in geographic coordinates")
	@In
	public boolean $$gFLAG = false;

	@Description("Use bold text")
	@In
	public boolean $$bFLAG = false;

	@Description("Use radians instead of degrees for rotation")
	@In
	public boolean $$rFLAG = false;

	@Description("Font size is height in pixels")
	@In
	public boolean $$sFLAG = false;

	@Description("Ignored (compatibility with d.text.freetype)")
	@In
	public boolean $$cFLAG = false;

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
