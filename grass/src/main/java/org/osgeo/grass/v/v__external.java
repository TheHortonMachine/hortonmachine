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

@Description("Available drivers: ESRI Shapefile,MapInfo File,UK .NTF,SDTS,TIGER,S57,DGN,VRT,REC,Memory,BNA,CSV,GML,GPX,KML,GeoJSON,Interlis 1,Interlis 2,GMT,SQLite,ODBC,PGeo,OGDI,PostgreSQL,MySQL,XPlane,AVCBin,AVCE00,Geoconcept")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass/Vector Modules")
@Name("v__external")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__external {

	@Description("OGR datasource name. Examples: 		ESRI Shapefile: directory containing shapefiles 		MapInfo File: directory containing mapinfo files")
	@In
	public String $$dsnPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector. If not given, available layers are printed only. (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("OGR layer name. If not given, available layers are printed only. Examples: 		ESRI Shapefile: shapefile name 		MapInfo File: mapinfo file name (optional)")
	@In
	public String $$layerPARAMETER;

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
