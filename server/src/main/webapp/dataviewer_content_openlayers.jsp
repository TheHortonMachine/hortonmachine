<%@page import="org.jgrasstools.gears.utils.colors.ColorUtilities"%>
<%@page
	import="org.jgrasstools.server.jetty.providers.tiles.BingProvider"%>
<%@page
	import="org.jgrasstools.server.jetty.providers.tiles.OsmProvider"%>
<%@page
	import="org.jgrasstools.server.jetty.providers.tiles.WmsProvider"%>
<%@page import="org.jgrasstools.server.jetty.providers.IProvider"%>
<%@page
	import="org.jgrasstools.server.jetty.providers.tiles.ITilesProvider"%>
<%@page import="org.joda.time.DateTime"%>
<%@page import="org.jgrasstools.gears.libs.modules.JGTConstants"%>
<%@page import="org.joda.time.format.DateTimeFormatter"%>
<%@page
	import="org.jgrasstools.server.jetty.providers.data.NwwDataProvider"%>
<%@page import="java.util.List"%>
<%@page import="com.vividsolutions.jts.geom.Envelope"%>
<%@page import="java.awt.Color"%>
<%@page import="org.jgrasstools.gears.utils.style.SimpleStyle"%>
<%@page import="com.vividsolutions.jts.geom.Coordinate"%>
<%@page import="com.vividsolutions.jts.geom.Geometry"%>
<%@ include file="ol.jsp"%>
<%
    String isAdminStr = request.getParameter("isAdmin");
			boolean isAdmin = Boolean.parseBoolean(isAdminStr);

			ServletContext servletContext = request.getServletContext();
			NwwDataProvider[] providers = (NwwDataProvider[]) servletContext.getAttribute(IProvider.DATAPROVIDERS);
			if (providers == null) {
				providers = new NwwDataProvider[0];
			}

			ITilesProvider[] tilesProviders = (ITilesProvider[]) servletContext.getAttribute(IProvider.TILESPROVIDERS);
			if (tilesProviders == null) {
				tilesProviders = new ITilesProvider[0];
			}

			DateTimeFormatter formatter = JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS;
			int featuresLimitPerLayer = 1000;
%>
<style>
#canvasOne {
	height: 600px;
}
</style>


<div class="mainMap">
	<div class="leftMap">
		<table>
			<tr>
				<td>
					<h2>Shapefile Layers</h2>
					<form action="">
						<%
						    for (NwwDataProvider provider : providers) {
										String pname = provider.getName();
						%>
						<input
							id="<%=pname%>"
							type="checkbox"
							onclick="toggleSelection('<%=provider.getName()%>')"
							checked><%=pname%><br>
						<%
						    }
						%>
					</form>
				</td>
			</tr>
			<tr>
				<td>
					<h2>Layers</h2>
					<form action="">
						<%
						    for (ITilesProvider tilesProvider : tilesProviders) {
						%>
						<input
							id="<%=tilesProvider.getName()%>"
							type="checkbox"
							onclick="toggleSelection('<%=tilesProvider.getName()%>')"
							<%=tilesProvider.isVisible() ? "checked" : ""%>><%=tilesProvider.getName()%><br>
						<%
						    }
						%>
					</form>
				</td>
			</tr>
			<tr>
				<td>
					<div id="featureCountTd">
						<h2>Visible features</h2>
						<p><%=new DateTime().toString(formatter)%></p>
						<%
						    // 							    StringBuilder sb = new StringBuilder();
									// 										int total = 0;
									// 										for (NwwDataProvider provider : providers) {
									// 											String name = provider.getName();
									// 											int count = 0;// provider.subCollection("").size();
									// 											total += count;
									// 											sb.append("<li>").append(name).append(": ").append(count).append("</li>");
									// 										}
									// 										if (total > 0) {
						%>
						<!-- 							<ul> -->
						<%-- 								<%=sb.toString()%> --%>
						<!-- 							</ul> -->
						<%
						    // 							    }
						%>
						<button
							onclick="jQuery('#featureCountTd').load(' #featureCountTd');">Reload</button>
					</div>
				</td>
			</tr>
		</table>
	</div>
	<div class="rightMap">
		<div class="mapBorder">
			<div
				id="map"
				class="map"
				style="height: 500px;"></div>
		</div>
	</div>
	<div class="clear"></div>
</div>

<script>

var layersVisibility = {
	<%for (NwwDataProvider provider : providers) {%>
		'<%=provider.getName()%>' : true,
	<%}
			for (ITilesProvider provider : tilesProviders) {%>
		'<%=provider.getName()%>' : <%=provider.isVisible()%>,
	<%}%>
};

var mymap;


// Register an event listener to be called when the page is loaded.
window.addEventListener("load", eventWindowLoaded, false);

// Define the event listener to initialize Web World Wind.
function eventWindowLoaded() {
    
    var layers = [];
    addBackgrounds(layers);
    
    mymap = new ol.Map({
        layers: layers,
        target: 'map',
        view: new ol.View({
          center: [-10997148, 4569099],
          zoom: 4
        })
      });
    
    var pointFeatures = [];
    var linesFeatures = [];
	<%Envelope bounds = new Envelope();
			for (NwwDataProvider provider : providers) {%>
		<%if (provider.isLines()) {
					SimpleStyle style = provider.getStyle();
					double width = style.strokeWidth;
					Color color = style.strokeColor;
					String hexColor = ColorUtilities.asHex(color);%>
			var ls;
			<%int size = provider.size();
					for (int i = 0; i < size; i++) {
						if (i > featuresLimitPerLayer)
							break;
						Geometry geometry = provider.getGeometryAt(i);
						String label = provider.getLabelAt(i);
						if (i > 10000)
							break;
						if (bounds == null) {
							bounds = geometry.getEnvelopeInternal();
						} else {
							bounds.expandToInclude(geometry.getEnvelopeInternal());
						}%>
				<%Coordinate[] coordinates = geometry.getCoordinates();%>
					ls = new ol.geom.LineString();
					<%for (Coordinate c : coordinates) {%>
			        	  ls.appendCoordinate(ol.proj.transform([<%=c.x%>, <%=c.y%>], 'EPSG:4326', 'EPSG:3857'));
					<%}%>
					linesFeatures.push( new ol.Feature({
			          'geometry': ls,
			          'label': '<%=label%>'
			        }));
			<%}%>
			 var linesVector = new ol.layer.Vector({
			        source: new ol.source.Vector({
			          features: linesFeatures,
			          wrapX: false
			        }),
			        style: new ol.style.Style({
			          stroke: new ol.style.Stroke({
			            color: '<%=hexColor%>',
			            width: <%=width%>
			          })
			        })
			      });
			 mymap.addLayer(linesVector);
			
		<%} else if (provider.isPoints()) {
					SimpleStyle style = provider.getStyle();
					double oddSize = style.shapeSize;
					double evenSize = oddSize * 2;
					Color color = style.fillColor;
					String hexColor = ColorUtilities.asHex(color);%>
			var pointsStyles = {
				'odd': new ol.style.Style({
			          image: new ol.style.Circle({
			            radius: <%=oddSize%>,
			            fill: new ol.style.Fill({color: '<%=hexColor%>'}),
			            stroke: new ol.style.Stroke({color: '<%=hexColor%>', width: 1})
			          })
			        }),
				'even': new ol.style.Style({
			          image: new ol.style.Circle({
			            radius: <%=evenSize%>,
			            fill: new ol.style.Fill({color: '<%=hexColor%>'}),
			            stroke: new ol.style.Stroke({color: '<%=hexColor%>', width: 1})
			          })
			        }),
			};
			<%int size = provider.size();
					for (int i = 0; i < size; i++) {
						if (i > featuresLimitPerLayer)
							break;
						Geometry geometry = provider.getGeometryAt(i);
						Coordinate[] coordinates = geometry.getCoordinates();
						for (Coordinate c : coordinates) {
							bounds.expandToInclude(c);%>
						pointFeatures.push( new ol.Feature({
						          'geometry': new ol.geom.Point(ol.proj.transform([<%=c.x%>, <%=c.y%>], 'EPSG:4326',     
						          'EPSG:3857')),
						          'type': <%=i % 2 == 0 ? "'odd'" : "'even'"%>
						        }));
					<%}%>
					    
			<%}%>
			var pointFeaturesSource = new ol.source.Vector({
			        features: pointFeatures,
			        wrapX: false
			      });
			    var pointFeaturesVector = new ol.layer.Vector({
			        source: pointFeaturesSource,
			        style: function(feature) {
			          return pointsStyles[feature.get('type')];
			        }
			    });
		   		mymap.addLayer(pointFeaturesVector);
			
			
		<%}%>
	<%}%>
					    
	centerOn(<%=bounds.centre().x%>, <%=bounds.centre().y%>, 8)
}

function centerOn(lon, lat, zoom) {
    mymap.getView().setCenter(ol.proj.transform([lon, lat], 'EPSG:4326', 'EPSG:3857'));
    mymap.getView().setZoom(zoom);
}
	
function toggleSelection(layerName) {
    var isVisible = layersVisibility[layerName];
	var layer = getLayerByName(layerName);
	layer.setVisible(!isVisible);
	layersVisibility[layerName] = !isVisible;
}

function getLayerByName(name) {
	var selLayer;
	mymap.getLayers().forEach(function(layer, i) {
	    if(layer.get('name') == name){
			selLayer = layer;
		}
	});
	return selLayer;
}

function addBackgrounds(layers) {
    <%for (ITilesProvider tilesProvider : tilesProviders) {
				if (tilesProvider instanceof WmsProvider) {%>
	    	layers.push(new ol.layer.Tile({
	    	  visible : <%=tilesProvider.isVisible()%>,
	    	  source: new ol.source.TileWMS({
	    	    url: '<%=tilesProvider.getUrl()%>',
	    	    params: <%=tilesProvider.getParams()%>,
	    	    serverType: '<%=tilesProvider.getServerType()%>'
	    	  }),
	    	  name: "<%=tilesProvider.getName()%>"
	    	}));
	    <%}
				if (tilesProvider instanceof BingProvider) {%>
    	layers
    	.push(new ol.layer.Tile(
    		{
    		    visible : <%=tilesProvider.isVisible()%>,
    		    preload : Infinity,
    		    source : new ol.source.BingMaps(
    			    {
    				key : '<%=tilesProvider.getKey()%>',
    				imagerySet : '<%=tilesProvider.getImagerySet()%>'
    			    }),
    			name : "<%=tilesProvider.getName()%>"
    		}));
    	<%}
				if (tilesProvider instanceof OsmProvider) {%>
    	layers.push(new ol.layer.Tile({
	    	source: <%=tilesProvider.getSource()%>,
	    	name: "<%=tilesProvider.getName()%>"
	}));
<%}%>
    
<%}%>
    }

    function refreshFeatureCount(event) {
	jQuery('#featureCountTd').load(' #featureCountTd');
    }
</script>