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

@Description("Contains a set of measures for attributes, diversity, texture, juxtaposition, and edge.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__le__pixel")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__le__pixel {

	@UI("infile,grassfile")
	@Description("Raster map to be analyzed")
	@In
	public String $$mapPARAMETER;

	@Description("Sampling method (choose only 1 method): 	w = whole map      u = units        m = moving window   r = regions (optional)")
	@In
	public String $$samPARAMETER = "w";

	@UI("infile,grassfile")
	@Description("Name of regions map, only when sam = r; omit otherwise (optional)")
	@In
	public String $$regPARAMETER;

	@Description("b1 = mn. pixel att.                 b2 = s.d. pixel att. 	b3 = min. pixel att.                b4 = max. pixel att. (optional)")
	@In
	public String $$attPARAMETER;

	@Description("d1 = richness      d2 = Shannon     d3 = dominance     d4 = inv. Simpson (optional)")
	@In
	public String $$divPARAMETER;

	@Description("Texture method (choose only 1 method): 	m1 = 2N-H          m2 = 2N-45       m3 = 2N-V          m4 = 2N-135 	m5 = 4N-HV         m6 = 4N-DIAG     m7 = 8N (optional)")
	@In
	public String $$te1PARAMETER;

	@Description("Texture measures (required if te1 was specified): 	t1 = contagion           t2 = ang. sec. mom.     t3 = inv. diff. mom. 	t4 = entropy             t5 = contrast (optional)")
	@In
	public String $$te2PARAMETER;

	@Description("Juxtaposition measures (weight file in r.le.para needed): 	j1 = mn. juxtaposition              j2 = s.d. juxtaposition (optional)")
	@In
	public String $$juxPARAMETER;

	@Description("e1 = sum of edges  e2 = sum of edges by type (need edge file: r.le.para) (optional)")
	@In
	public String $$edgPARAMETER;

	@Description("Output map 'edge' of edges given a '1' in r.le.para/edge file")
	@In
	public boolean $$eFLAG = false;

	@Description("Output maps 'units_x' with sampling units for each scale x")
	@In
	public boolean $$uFLAG = false;

	@Description("Output map 'zscores' with standardized scores")
	@In
	public boolean $$zFLAG = false;

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
