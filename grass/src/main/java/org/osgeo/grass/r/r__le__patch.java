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

@Description("Calculates attribute, patch size, core (interior) size, shape, fractal dimension, and perimeter measures for sets of patches in a landscape.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__le__patch")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__le__patch {

	@UI("infile,grassfile")
	@Description("Raster map to be analyzed")
	@In
	public String $$mapPARAMETER;

	@Description("Sampling method (choose only 1 method):  	w = whole map     u = units     m = moving window    r = regions (optional)")
	@In
	public String $$samPARAMETER = "w";

	@UI("infile,grassfile")
	@Description("Name of regions map, only when sam = r; omit otherwise (optional)")
	@In
	public String $$regPARAMETER;

	@Description("a1 = mn. pixel att. 		a2 = s.d. pixel att. 	a3 = mn. patch att. 		a4 = s.d. patch att. 	a5 = cover by gp 		a6 = density by gp 	a7 = total density 		a8 = eff. mesh number (optional)")
	@In
	public String $$attPARAMETER;

	@Description("s1 = mn. patch size		s2 = s.d. patch size 	s3 = mn. patch size by gp	s4 = s.d. patch size by gp  	s5 = no. by size class		s6 = no. by size class by gp 	s7 = eff. mesh size 		s8 = deg. landsc. division (optional)")
	@In
	public String $$sizPARAMETER;

	@Description("Depth-of-edge-influence in pixels (integer) for use with co2 (optional)")
	@In
	public String $$co1PARAMETER;

	@Description("Core size measures (required if co1 was specified): 	c1 = mn. core size		c2 = s.d. core size 	c3 = mn. edge size		c4 = s.d. edge size 	c5 = mn. core size by gp	c6 = s.d. core size by gp 	c7 = mn. edge size by gp	c8 = s.d. edge size by gp 	c9 = no. by size class		c10 = no. by size class by gp (optional)")
	@In
	public String $$co2PARAMETER;

	@Description("Shape index (choose only 1 index): 	m1 = per./area    m2 = corr. per./area    m3 = rel. circum. circle (optional)")
	@In
	public String $$sh1PARAMETER;

	@Description("Shape measures (required if sh1 was specified): 	h1 = mn. patch shape		h2 = s.d. patch shape 	h3 = mn. patch shape by gp	h4 = s.d. patch shape by gp 	h5 = no. by shape class 	h6 = no. by shape class by gp (optional)")
	@In
	public String $$sh2PARAMETER;

	@Description("n1 = mn. twist number           n2 = s.d. twist number 	n3 = mn. omega index            n4 = s.d. omega index (optional)")
	@In
	public String $$bndPARAMETER;

	@Description("p1 = sum of perims.		p4 = sum of perims. by gp 	p2 = mn. per.			p5 = mn. per. by gp 	p3 = s.d. per.			p6 = s.d. per. by gp (optional)")
	@In
	public String $$perPARAMETER;

	@Description("Name of output file for individual patch measures, when sam=w,u,r; 	if out=head, then column headings will be printed (optional)")
	@In
	public String $$outPARAMETER;

	@Description("Output map 'interior' with patch cores (specify co1 & co2)")
	@In
	public boolean $$cFLAG = false;

	@Description("Output map 'num' with patch numbers")
	@In
	public boolean $$nFLAG = false;

	@Description("Include sampling area boundary as perimeter")
	@In
	public boolean $$pFLAG = false;

	@Description("Use 4 neighbor instead of 8 neighbor tracing")
	@In
	public boolean $$tFLAG = false;

	@Description("Output maps 'units_x' with sampling units for each scale x")
	@In
	public boolean $$uFLAG = false;

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
