# Simple NWW Viewer

The Simple NWW Viewer shows spatial data on a 3D globe, based on NASA WorldWind Java. It can show the terrain in 3D, with the elevations of the Copernicus DEM, and drape over it maps and your own rasters and vectors.

## Launching

Start the application with the `hm-simplenww-viewer` launcher (see [Launching an application](../installation.md#launching-an-application)).

The globe is drawn with OpenGL, so the viewer needs a graphics card supporting it.

## The main window

:::{figure} ../images/apps/nwwviewer/main.png
:alt: The Simple NWW Viewer
:width: 100%
:align: center

The viewer with a DTM loaded over the OpenStreetMap background.
:::

The window has three parts: the **Layers** on the left, the globe in the middle, and the **Tools** on the right.

Navigate the globe with the mouse:

- drag with the left button to move;
- use the mouse wheel to zoom;
- drag with the right button to rotate and tilt the view, which is how the 3D terrain is best seen.

The view controls in the lower left corner zoom and rotate too, and the compass in the upper right corner shows the north. The status bar at the bottom shows the altitude of the view, and the coordinates and elevation of the point under the mouse.

## Layers

The **Layers** panel lists the layers of the globe. The check box shows or hides a layer, the trash button removes it. Layers loaded from files also have a magnifier button, which zooms the globe to the whole layer.

The viewer starts with a few background maps, of which one or more can be shown at the same time:

- **OpenStreetMap**, shown by default;
- **OpenTopoMap**, a topographic map with contour lines and hillshade;
- **Esri-Satellite**, satellite and aerial imagery;

and with the **View Controls**, the **Compass** and the **Scale bar**.

## 3D terrain

Choose **Earth with 3D terrain** in the **Globe Mode** of the tools to see the terrain in 3D. The maps and the layers are draped over it.

:::{figure} ../images/apps/nwwviewer/terrain.png
:alt: The 3D terrain with OpenTopoMap
:width: 100%
:align: center

The valleys around the Flanginec DTM, in 3D with the OpenTopoMap background.
:::

The elevations come from the Copernicus DEM GLO-30, a global elevation model with a resolution of about 30 meters, published as open data on Amazon Web Services. The viewer reads only the parts of the dataset needed for the area in view, at the resolution needed for the zoom level, so the terrain appears progressively as the data arrive: the status bar shows **Downloading** meanwhile.

The downloaded data are kept in the cache of the viewer (see **Open Cache Manager** below), so an area already seen doesn't need to be downloaded again, also in the next sessions.

The terrain is shown only when the view covers up to a few hundred kilometers: in wider views the globe is flat, to not download the data of whole continents.

:::{note}
The 3D terrain needs an internet connection the first time an area is viewed. The other globe modes, **Earth** and the two flat ones, don't download any elevation data.
:::

The chosen globe mode is remembered for the next sessions.

## Tools

Load supported file
: loads a file as a new layer: rasters in ESRI ASCII grid (`.asc`) or GeoTIFF (`.tiff`) format, shapefiles, GeoPackages (vector and tile tables), MBTiles, Mapsforge maps (`.map`), RasterLite2 and SpatiaLite databases. Choosing a folder loads all the shapefiles it contains. The rasters are drawn with their style file (`.sld`) if present, or with a default color table.

Load shapefile to fly
: moves a GPS marker along the points of a shapefile, simulating a GPS track.

Open Cache Manager
: shows the data cached by the viewer, like the tiles of the maps and of the terrain, and lets you delete the data older than a given age.

Globe Mode
: the shape of the globe: **Earth**, **Earth with 3D terrain**, or a flat map in Mercator or lat/long projection.

Prefer rasterized vectors
: draws the vector layers as images instead of as geometries, which is much faster for large datasets.

White Background, Opaque Background
: a white background under the maps, and the background of the globe window.

Info/Editing Tool
: when active, clicking on a feature of a vector layer shows its attributes, which can also be edited: the changes are saved to the data source, when it supports writing.

Select by box
: when active, drag on the globe to count the features inside a box.

Zoom By Box
: when active, drag on the globe to zoom to a box.

Add Annotation
: adds a text annotation to the view, written in HTML.

Paste WGS84 WKT
: adds to the globe the geometry copied to the clipboard as WKT text, in WGS84 longitude and latitude.
