<%@page import="java.awt.Color"%>
<%@page import="org.jgrasstools.gears.utils.style.SimpleStyle"%>
<%@page import="com.vividsolutions.jts.geom.Coordinate"%>
<%@page import="com.vividsolutions.jts.geom.Geometry"%>
<%@page import="org.jgrasstools.server.jetty.map.NwwDataProvider"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<%
    ServletContext servletContext = request.getServletContext();
			NwwDataProvider[] providers = (NwwDataProvider[]) servletContext.getAttribute(NwwDataProvider.PROVIDERS);
			if (providers == null) {
				providers = new NwwDataProvider[0];
			}
%>

<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<script src="http://worldwindserver.net/webworldwind/worldwindlib.js"
	type="text/javascript"></script>
<style type="text/css">
.clear {
	clear: both;
}

.main {
	width: 100%;
}

.top {
	background: white;
	width: 100%;
	height: 20%;
}

.left {
	float: left;
	width: 30%;
	background: white;
}

.right {
	float: right;
	width: 70%;
	height: 80%;
	background: white;
}
</style>
</head>
<body>

	<div class="main">
		<div class="top">
			<h1 style="text-align: center">World Wind Shapefiles Loading</h1>
		</div>
		<div class="left">

			<form action="">
				<%
				    for (NwwDataProvider provider : providers) {
				%>
				<input type="button" value="<%=provider.getName()%>"
					onclick="'loadData<%=provider.getName()%>()';" />
				<%
				    }
				%>
			</form>

		</div>
		<div class="right">
			<canvas id="canvasOne" style="width: 100%; height: auto">
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

			/**
			 * Added imagery layers.
			 */
			var layers = [ {
				layer : new WorldWind.BMNGLayer(),
				enabled : true
			}, {
				layer : new WorldWind.BMNGLandsatLayer(),
				enabled : false
			}, {
				layer : new WorldWind.BingAerialWithLabelsLayer(null),
				enabled : true
			}, {
				layer : new WorldWind.CompassLayer(),
				enabled : true
			} ];

			for (var l = 0; l < layers.length; l++) {
				layers[l].layer.enabled = layers[l].enabled;
				wwd.addLayer(layers[l].layer);
			}

			

		
		
			<%for (NwwDataProvider provider : providers) {%>
				<%if (provider.isLines()) {
					SimpleStyle style =  provider.getStyle();
					double width = style.strokeWidth;
					Color color = style.strokeColor;
					float r =  color.getRed()/255f;
					float g =  color.getGreen()/255f;
					float b =  color.getBlue()/255f;
					float a =  color.getAlpha()/255f;
				%>
					var pathsLayer = new WorldWind.RenderableLayer();
					pathsLayer.displayName = "<%=provider.getName()%>";
					
					var pathAttributes = new WorldWind.ShapeAttributes(null);
					pathAttributes.outlineColor = new WorldWind.Color(<%= r %>, <%= g %>, <%= b %>, <%= a %>);
					pathAttributes.outlineWidth = <%= width %>;
					var pathPositions = [];
					<%
				    int size = provider.size();
					for (int i=0; i < size;i++) {
						Geometry geometry = provider.getGeometryAt(i);
						if(i>1) break;
					%>
						pathPositions = [];
						
						<%Coordinate[] coordinates = geometry.getCoordinates();
						for (Coordinate c : coordinates) {
						%>
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
					SimpleStyle style =  provider.getStyle();
					double width = style.strokeWidth;
					Color color = style.strokeColor;
					float r =  color.getRed()/255f;
					float g =  color.getGreen()/255f;
					float b =  color.getBlue()/255f;
					float a =  color.getAlpha()/255f;
					%>
					
		            var textAttributes = new WorldWind.TextAttributes(null);
		            var textLayer = new WorldWind.RenderableLayer("<%=provider.getName()%>");

			        textAttributes.color = new WorldWind.Color(<%= r %>, <%= g %>, <%= b %>, <%= a %>);
			        // Set the depth test property such that the terrain does not obscure the text.
			        textAttributes.depthTest = false;
<%-- 						pathAttributes.outlineWidth = <%= width %>; --%>

					var peakPosition;
					var text;
					<%
				    int size = provider.size();
					for (int i=0; i < size;i++) {
						Geometry geometry = provider.getGeometryAt(i);
						String label = provider.getLabelAt(i);
						if(label.length()==0) label = "BAU";
						Coordinate[] coordinates = geometry.getCoordinates();
						for (Coordinate c : coordinates) {%>
							peakPosition = new WorldWind.Position(<%=c.y%>, <%=c.x%>);
				            text = new WorldWind.GeographicText(peakPosition, "<%= label %>");
				            text.attributes = textAttributes;
				            textLayer.addRenderable(text);
						<%}%>
					<%}%>
					
					wwd.addLayer(textLayer);
				<%}%>
			<%}%>
			
			wwd.redraw();
		}

		
		
	</script>
</body>
</html>