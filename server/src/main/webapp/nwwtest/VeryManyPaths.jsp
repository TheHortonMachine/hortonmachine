<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!DOCTYPE html>
<!--@version $Id: VeryManyPaths.html 3062 2015-05-05 18:31:25Z tgaskins $-->
<html lang="en">
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
			<h1 style="text-align: center">World Wind Paths Performance</h1>
		</div>
		<div class="left">LEFT</div>
		<div class="right">
			<canvas id="canvasOne" style="width: 100%; height: auto">
                Your browser does not support HTML5 Canvas.
            </canvas>
		</div>
		<div class="clear"></div>
	</div>

	<script>
		// Register an event listener to be called when the page is loaded.
		window.addEventListener("load", eventWindowLoaded, false);

		// Define the event listener to initialize Web World Wind.
		function eventWindowLoaded() {
			// Tell World Wind to log only warnings and errors.
			WorldWind.Logger.setLoggingLevel(WorldWind.Logger.LEVEL_WARNING);
			
			
			// Create the World Window.
			var wwd = new WorldWind.WorldWindow("canvasOne");

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

			var makePath = function(startPosition, heading, length,
					numPositions) {
				var dLength = length / (numPositions - 1), positions = [], ll, path, attributes;

				for (var i = 0; i < numPositions; i++) {
					ll = WorldWind.Location.greatCircleLocation(startPosition,
							heading, i * dLength, new WorldWind.Location(0, 0));
					positions.push(new WorldWind.Position(ll.latitude,
							ll.longitude, startPosition.altitude))
				}

				path = new WorldWind.Path(positions, null);
				path.altitudeMode = WorldWind.RELATIVE_TO_GROUND;
				path.extrude = true;

				attributes = new WorldWind.ShapeAttributes(null);
				attributes.outlineColor = WorldWind.Color.BLUE;
				attributes.interiorColor = WorldWind.Color.CYAN;
				attributes.drawOutline = true;
				attributes.drawInterior = true;
				attributes.drawVerticals = true;
				path.attributes = attributes;

				attributes = new WorldWind.ShapeAttributes(attributes);
				attributes.outlineColor = WorldWind.Color.RED;
				attributes.interiorColor = WorldWind.Color.WHITE;
				path.highlightAttributes = attributes;

				return path;
			};

			var makePaths = function(layer, origin, numPaths, length,
					numPositions) {
				var angleDelta = 360.0 / numPaths;

				for (var i = 0; i < numPaths; i++) {
					layer.addRenderable(makePath(origin, i * angleDelta,
							length, numPositions));
				}
			};

			var pathsLayer = new WorldWind.RenderableLayer(), startPosition = new WorldWind.Position(
					45, -120, 100e3), numPaths = 2000, numPositions = 30, pathLength = 10 * WorldWind.Angle.DEGREES_TO_RADIANS;

			makePaths(pathsLayer, startPosition, numPaths, pathLength,
					numPositions);
			pathsLayer.displayName = "Paths";
			wwd.addLayer(pathsLayer);

			// Draw the World Window for the first time.
			wwd.redraw();

			// Create a layer manager for controlling layer visibility.
			var layerManger = new LayerManager(wwd);

			// Now set up to handle picking.

			var highlightedItems = [];

			// The pick-handling callback function.
			var handlePick = function(o) {
				// The input argument is either an Event or a TapRecognizer. Both
				// have the same properties for determining
				// the mouse or tap location.
				var x = o.clientX, y = o.clientY;

				var redrawRequired = highlightedItems.length > 0; // must redraw
				// if we
				// de-highlight
				// previously
				// picked items

				// De-highlight any previously highlighted placemarks.
				for (var h = 0; h < highlightedItems.length; h++) {
					highlightedItems[h].highlighted = false;
				}
				highlightedItems = [];

				// Perform the pick. Must first convert from window coordinates to
				// canvas coordinates, which are
				// relative to the upper left corner of the canvas rather than the
				// upper left corner of the page.
				var pickList = wwd.pick(wwd.canvasCoordinates(x, y));
				// console.log(wwd.frameStatistics.frameTime);
				if (pickList.objects.length > 0) {
					redrawRequired = true;
				}

				// Highlight the items picked by simply setting their highlight flag
				// to true.
				if (pickList.objects.length > 0) {
					for (var p = 0; p < pickList.objects.length; p++) {
						pickList.objects[p].userObject.highlighted = true;

						// Keep track of highlighted items in order to de-highlight
						// them later.
						highlightedItems.push(pickList.objects[p].userObject);
					}
				}

				// Update the window if we changed anything.
				if (redrawRequired) {
					wwd.redraw(); // redraw to make the highlighting changes take
					// effect on the screen
				}
			};

			// Listen for mouse moves and highlight the placemarks that the cursor
			// rolls over.
			wwd.addEventListener("mousemove", handlePick);

			// Listen for taps on mobile devices and highlight the placemarks that
			// the user taps.
			var tapRecognizer = new WorldWind.TapRecognizer(wwd, handlePick);
		}
	</script>
</body>
</html>