<%@page import="org.joda.time.format.DateTimeFormatter"%>
<%@page import="org.jgrasstools.gears.libs.modules.JGTConstants"%>
<%@page import="org.joda.time.DateTime"%>
<%@page import="java.util.Date"%>
<%@page import="org.jgrasstools.gears.utils.ColorUtilities"%>
<%@page import="com.vividsolutions.jts.geom.Envelope"%>
<%@page import="java.awt.Color"%>
<%@page import="org.jgrasstools.gears.utils.style.SimpleStyle"%>
<%@page import="com.vividsolutions.jts.geom.Coordinate"%>
<%@page import="com.vividsolutions.jts.geom.Geometry"%>
<%@page import="org.jgrasstools.server.jetty.map.NwwDataProvider"%>
<%@ page
	language="java"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<%
    ServletContext servletContext = request.getServletContext();
			NwwDataProvider[] providers = (NwwDataProvider[]) servletContext.getAttribute(NwwDataProvider.PROVIDERS);
			if (providers == null) {
				providers = new NwwDataProvider[0];
			}
			DateTimeFormatter formatter = JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS;
			int featuresLimitPerLayer = 1000;
%>

<head>
<meta
	name="viewport"
	content="width=device-width, initial-scale=1">
<script
	src="http://worldwindserver.net/webworldwind/worldwindlib.js"
	type="text/javascript"></script>
<script src="http://code.jquery.com/jquery-latest.js"></script>

<style type="text/css">
.clear {
	clear: both;
}

.main {
	width: 100%;
}

.left {
	float: left;
	width: 30%;
	height: 100vh;
	background: white;
}

.right {
	float: right;
	width: 70%;
	height: 100vh;
	background: white;
}

table {
	display: table;
	border-collapse: collapse;
	width: 95%;
	height: 98vh;
}

table, th, td {
	border: 1px solid black;
}

th, td {
	padding: 15px;
	text-align: left;
}

canvas {
	width: 100%;
	height: 98vh;
	border: 1px solid black;
}
</style>
</head>
<body>

	<div class="main">
		<div class="left">
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
						<h2>Nww Layers</h2>
						<form action="">
							<input
								id="Blue Marble & Landsat"
								type="checkbox"
								onclick="toggleSelection('Blue Marble & Landsat')">Blue
							Marble & Landsat<br> <input
								id="Blue Marble"
								type="checkbox"
								onclick="toggleSelection('Blue Marble')">Blue Marble<br>
							<input
								id="Bing Roads"
								type="checkbox"
								onclick="toggleSelection('Bing Roads')"
								checked>Bing Roads<br> <input
								id="View Controls"
								type="checkbox"
								onclick="toggleSelection('View Controls')">View Controls<br>
							<input
								id="Coordinates"
								type="checkbox"
								onclick="toggleSelection('Coordinates')">Coordinates<br>
							<input
								id="Compass"
								type="checkbox"
								onclick="toggleSelection('Compass')"
								checked>Compass<br>
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
		<div class="right">
			<canvas id="canvasOne">
	                Your browser does not support HTML5 Canvas.
	            </canvas>
		</div>
		<div class="clear"></div>
	</div>

	<script>
		var wwd;	
		// Register an event listener to be called when the page is loaded.
		window.addEventListener("load", eventWindowLoaded, false);

		// Define the event listener to initialize Web World Wind.
		function eventWindowLoaded() {
			// Tell World Wind to log only warnings and errors.
			WorldWind.Logger.setLoggingLevel(WorldWind.Logger.LEVEL_WARNING);

			// Create the World Window.
			 wwd = new WorldWind.WorldWindow("canvasOne");
			
			 //wwd.addEventListener("mousemove", refreshFeatureCount(  ));
			 //wwd.addEventListener("onwheel", refreshFeatureCount(  ));
			 wwd.addEventListener("mouseup", function (event) {
			     jQuery('#featureCountTd').load(' #featureCountTd');
			});
			 wwd.addEventListener("wheel", function (event) {
			     jQuery('#featureCountTd').load(' #featureCountTd');
			});

			/**
			 * Added imagery layers.
			 */
			var layers = [ {
				layer : new WorldWind.BMNGLayer(),
				enabled : false
			}, {
				layer : new WorldWind.BMNGLandsatLayer(),
				enabled : false
			}, {
				layer : new WorldWind.BingRoadsLayer(null),
				enabled : true
			}, {
				layer : new WorldWind.ViewControlsLayer(wwd),
				enabled : false
			}, {
				layer : new WorldWind.CoordinatesDisplayLayer(wwd),
				enabled : false
			}, {
				layer : new WorldWind.CompassLayer(),
				enabled : true
			} ];

			for (var l = 0; l < layers.length; l++) {
				layers[l].layer.enabled = layers[l].enabled;
				wwd.addLayer(layers[l].layer);
			}

			<%Envelope bounds = null;
			for (NwwDataProvider provider : providers) {
				if (bounds == null) {
					bounds = provider.getBounds();
				} else {
					bounds.expandToInclude(provider.getBounds());
				}%>
				<%if (provider.isLines()) {
					SimpleStyle style = provider.getStyle();
					double width = style.strokeWidth;
					Color color = style.strokeColor;
					float r = color.getRed() / 255f;
					float g = color.getGreen() / 255f;
					float b = color.getBlue() / 255f;
					float a = color.getAlpha() / 255f;%>
					var pathsLayer = new WorldWind.RenderableLayer();
					pathsLayer.displayName = "<%=provider.getName()%>";
					
					var pathAttributes = new WorldWind.ShapeAttributes(null);
					pathAttributes.outlineColor = new WorldWind.Color(<%=r%>, <%=g%>, <%=b%>, <%=a%>);
					pathAttributes.outlineWidth = <%=width%>;
					var pathPositions = [];
					<%int size = provider.size();
					for (int i = 0; i < size; i++) {
						Geometry geometry = provider.getGeometryAt(i);
						if (i > featuresLimitPerLayer)
							break;%>
						pathPositions = [];
						
						<%Coordinate[] coordinates = geometry.getCoordinates();
						for (Coordinate c : coordinates) {%>
							pathPositions.push(new WorldWind.Position(<%=c.y%>, <%=c.x%>));
						<%}%>
						
						var path = new WorldWind.Path(pathPositions, null);
						path.altitudeMode = WorldWind.CLAMP_TO_GROUND;
						path.followTerrain = true;
						path.attributes = pathAttributes;
						pathsLayer.addRenderable(path);
					<%}%>
					
					wwd.addLayer(pathsLayer);
				<%} else if (provider.isPoints()) {
					SimpleStyle style = provider.getStyle();
					double width = style.strokeWidth;
					Color color = style.strokeColor;

					float r = color.getRed() / 255f;
					float g = color.getGreen() / 255f;
					float b = color.getBlue() / 255f;
					//String hex = String.format("#%02x%02x%02x", r, g, b);
					String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);

					float a = color.getAlpha() / 255f;
					double shapeSize = style.shapeSize;%>
					
			        var canvas = document.createElement("canvas"),
		            ctx2d = canvas.getContext("2d"),
		            size = <%=shapeSize%>, c = size / 2  - 0.5;
			        canvas.width = size;
			        canvas.height = size;

			        ctx2d.beginPath();
			        ctx2d.arc(c, c, c, 0, 2 * Math.PI, false);
			        ctx2d.fillStyle = '<%=hex%>';
			        ctx2d.fill();
					// 			        ctx2d.lineWidth = 5;
					// 			        ctx2d.strokeStyle = '#003300';
					// 			        ctx2d.stroke();
	
					var placemarkAttributes = new WorldWind.PlacemarkAttributes(null)
		            var placemarkLayer = new WorldWind.RenderableLayer("<%=provider.getName()%>");
					var color = new WorldWind.Color(<%=r%>, <%=g%>, <%=b%>, <%=a%>);
 			        placemarkAttributes.imageColor = color;
 			       	placemarkAttributes.imageSource = new WorldWind.ImageSource(canvas);
			        placemarkAttributes.labelAttributes.offset = new WorldWind.Offset( WorldWind.OFFSET_FRACTION, 0.5, WorldWind.OFFSET_FRACTION, 1.0);
			        placemarkAttributes.labelAttributes.color = color; 
 			        placemarkAttributes.drawLeaderLine = true;
 			        placemarkAttributes.leaderLineAttributes.outlineColor = WorldWind.Color.RED;
					
					var peakPosition;
					var text;
					<%int size = provider.size();
					for (int i = 0; i < size; i++) {
					    if (i > featuresLimitPerLayer)
							break;
						Geometry geometry = provider.getGeometryAt(i);
						String label = provider.getLabelAt(i);
						if (label.length() == 0)
							label = "";
						Coordinate[] coordinates = geometry.getCoordinates();
						for (Coordinate c : coordinates) {%>
				            placemark = new WorldWind.Placemark(new WorldWind.Position(<%=c.y%>, <%=c.x%>, 1e2), true, null);
				            placemark.label = "<%=label%>";
				            placemark.altitudeMode = WorldWind.RELATIVE_TO_GROUND;
				            placemark.attributes = placemarkAttributes;
				            placemarkLayer.addRenderable(placemark);
						<%}%>
					<%}%>
					
					wwd.addLayer(placemarkLayer);
				<%}%>
			<%}

			Coordinate center = bounds.centre();%>
			
			wwd.navigator.lookAtLocation.latitude = <%=center.y%>;
			wwd.navigator.lookAtLocation.longitude = <%=center.x%>;
			wwd.navigator.range = 1000000;

<%-- 			wwd.goTo(new WorldWind.Location(<%= center.y%>, <%=center.x%>)); --%>
			
			wwd.redraw();
		}

		function toggleSelection(elementId){
			var layer = getLayerByName(elementId);
			if(document.getElementById(elementId).checked) {
				// enable layer
				layer.enabled = true;
			}else{
				// disable layer
				layer.enabled = false;
			}
			wwd.redraw();
		}
		
		function getLayerByName(name){
			var l = wwd.layers.length;
			for (var i = 0; i < l; i++) {
				var layer = wwd.layers[i];
				if(layer.displayName == name){
					return layer;
				}
			}
			return null;
		}
		
		function refreshFeatureCount (event) {
		    jQuery('#featureCountTd').load(' #featureCountTd');
		 }
		
// 		function getBounds(){
    // TODO if supported by NWW
// 		    console.log(p);
// 		    wwd.globe.computePositionFromPoint(0,0,0, p);
// 		    console.log(p);
// 		}
		
	</script>
</body>
</html>