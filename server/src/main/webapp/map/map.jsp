<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>A map example</title>

<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script type="text/javascript"
	src="https://cdnjs.cloudflare.com/ajax/libs/d3/4.2.6/d3.min.js"></script>
<script type="text/javascript"
	src="http://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.js"></script>
<link rel="stylesheet"
	href="http://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.css" />
<script type='text/javascript'
	src='https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js'></script>
<style>
.map {
	border-style: solid;
	border-width: 1px;
	border-color: #ccc;
}
</style>
<body>
	<div id="map"></div>
	<script>
		var width = Math.max(960, window.innerWidth) - 30, height = Math.max(
				500, window.innerHeight) - 30;
		var map = void 0;
		var mapData = void 0;
		var leafletMap;
		//            loadScript();
		function loadScript() {

			var body = d3.select("body");
			body.select("#map").style("width", width + "px").style("height",
					height + "px");
			leafletMap = L.map('map');
			var mapnik = L
					.tileLayer(
							'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
							{
								attribution : '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
								maxZoom : 25,
								maxNativeZoom : 18
							});
			var esri_world = L
					.tileLayer(
							'http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
							{
								attribution : 'Tiles &copy; Esri',
								maxZoom : 25,
								maxNativeZoom : 18
							});
			var baseMaps = {
				"mapnik" : mapnik,
				"esri_world" : esri_world,
			};
			L.control.layers(baseMaps).addTo(leafletMap);
			leafletMap.setView([ 0.0, 0.0 ], 2);
			leafletMap._initPathRoot();
			//                d3.json("geoms.json", function (data) {
			var dataStr = getJsonData();
			var data = JSON.parse(dataStr);

			var gjson = L.geoJson(data);
			L.geoJson(
					data,
					{
						pointToLayer : function(feature, latlng) {
							var p = feature.properties;
							var mOpt = {
								radius : 8,
								color : p.stroke ? p.stroke : "steelblue",
								fillColor : p.fill ? p.fill : "steelblue",
								fillOpacity : p.fillopacity ? p.fillopacity
										: "0.3",
								opacity : p.strokeopacity ? p.strokeopacity
										: "1",
								weight : p.strokewidth ? p.strokewidth : "1"
							};
							return L.circleMarker(latlng, mOpt);
						},
						style : function(feature) {
							var p = feature.properties;
							return {
								color : p.stroke ? p.stroke : "steelblue",
								fillColor : p.fill ? p.fill : "steelblue",
								fillOpacity : p.fillopacity ? p.fillopacity
										: "0.3",
								opacity : p.strokeopacity ? p.strokeopacity
										: "1",
								weight : p.strokewidth ? p.strokewidth : "1"
							};
						},
						onEachFeature : function(feature, layer) {
							var keys = Object.keys(feature.properties);
							var desc = "<table><tr><td><b>Feature type: </b>"
									+ feature.geometry.type + "</td></tr>";
							keys.forEach(function(key) {
								var value = feature.properties[key];
								if (value && value.trim().length > 0)
									if (key !== "fill" && key !== "stroke"
											&& key !== "fillopacity"
											&& key !== "strokewidth") {
										var msg = "<tr><td><b>" + key
												+ ":</b> " + value
												+ "</td></tr>";
										desc += msg;
									}
							});
							desc += "</table>";
							layer.bindPopup(desc);
						}
					}).addTo(leafletMap);
			mapnik.addTo(leafletMap);

			fitBounds = gjson.getBounds();
			leafletMap.fitBounds(fitBounds);
			//                });
		}

		function project(x, y) {
			var point = leafletMap.latLngToLayerPoint(new L.LatLng(y, x));
			return point;
		}
	</script>
</body>
</html>