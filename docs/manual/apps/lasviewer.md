# LAS Viewer

The LAS Viewer inspects lidar point clouds in LAS and LAZ format: it shows the header of the file, filters the points by class, return, intensity, height and region, and draws them in a 3D view over a base map and the terrain. A range slider shows only the points with values in a given range, for example a horizontal slice of a forest at the height of the trunks.

## Launching

Start the application with the `hm-lasviewer` launcher (see [Launching an application](../installation.md#launching-an-application)). A LAS or LAZ file can be given as first argument, to open it directly.

## The main window

:::{figure} ../images/apps/lasviewer/main.png
:alt: The LAS Viewer
:width: 100%
:align: center

A forest plot of a terrestrial scanner, with a CRS, drawn over the satellite map and the Copernicus terrain.
:::

On the left, the file, its header and what to load; on the right, the 3D view with its toolbar on top, the range slider at the side and the choice of the coloring below.

Las/Laz input file
: the point cloud to inspect, chosen with the **...** button. Its **Header** (version, point format, number of points, scale and offset, ranges of the coordinates) and its **First point** are shown in the two tabs below. When the file is opened, its extent is drawn in white in the view, which zooms on it.

Optional DTM for delta
: a DTM raster (`.asc` or `.tif`) of the same area. When set, the height above the ground of each point is calculated at loading, to color, slice and draw the points by it (see [Heights over a DTM](#heights-over-a-dtm)).

### Files without CRS

When the file has no CRS, the viewer asks for the EPSG code of its coordinates, for example `EPSG:32632`, and can save it as a `.prj` file next to the las file, so that it is found the next time. Without a code, the points are drawn on a local plane, with no base map and no terrain: the status in the toolbar says so. This is usually the case of terrestrial and mobile scanners, which record the points in the coordinates of the instrument.

## Loading the points

Files can hold hundreds of millions of points, so the points are not loaded when the file is opened. The **Filters** and the **Bounds** decide which points are read by **Load data**:

sampling
: reads one point every *n*, 1000 by default, to get a quick look at large files. Set it to 1 to read all the points.

classes
: the classification codes to keep, separated by commas (for example `2` for the ground, `3,4,5` for the vegetation).

impulses
: the return numbers to keep (for example `1` for the first returns).

min/max intensity
: the range of intensity to keep, as `min,max`.

DTM lower thres, DTM upper thres
: the range of heights above the DTM to keep, when a DTM is set.

Bounds
: the region to read: **West**, **East**, **South**, **North** in the coordinates of the file, and the **Min Z** and **Max Z** elevations. They can be typed, taken from the extent of a shapefile with **From file** (transformed to the CRS of the las file if needed), or drawn with **Draw on map**. **Clear** removes them.

:::{figure} ../images/apps/lasviewer/overview.png
:alt: A first look with sampling 1000
:width: 100%
:align: center

A file of almost 90 million points, without CRS and so on a local plane, loaded with the default sampling of 1000: the points of the scan are in the middle of a much larger extent, stretched by a few far points.
:::

A good way to work with large files is to load them first with the default sampling, look for the area of interest, and then load it all:

1. press **Draw on map** and drag a rectangle on the view: the bounds are filled while dragging;
2. press **Draw on map** again to stop: the bounds stay, drawn in red;
3. set the **sampling** to 1 and press **Load data**.

:::{figure} ../images/apps/lasviewer/bounds.png
:alt: Drawing the bounds on the map
:width: 100%
:align: center

The bounds drawn on the core of the scan.
:::

**Refilter loaded** applies the filters again to the points already loaded, without reading the file: it is quick, but it can only remove points. To get points back, or more points with a lower sampling, use **Load data**.

## The 3D view

:::{figure} ../images/apps/lasviewer/elevation.png
:alt: All the points of the bounds, colored by elevation
:width: 100%
:align: center

All the 90 million points of the bounds, colored by elevation.
:::

Drag with the left mouse button to move, with the right one to rotate and tilt, and use the wheel to zoom. When more than 5 million points are loaded, a regular subset of 5 million is drawn while the view moves, and all the points again when it stops; the status in the toolbar tells how many points are shown, and its tooltip shows the statistics of the loaded points (ranges of the values and extent).

The toolbar on top of the view:

zoom to the data
: the first button zooms to the loaded points, from a tilted point of view, or to the extent of the file if no point is loaded.

base map
: **Satellite**, **OpenStreetMap** or **No base map**. The base map is shown only for files with a CRS.

view
: - **2D map**: the points from the top, as on a map;
  - **3D, flat ground**: the base map on a flat ground just under the lowest point;
  - **3D, Copernicus terrain**: the base map on the terrain of the Copernicus DEM, for files with a CRS;
  - **3D, terrain from the DTM**: the base map on the terrain of the DTM set, with the Copernicus terrain around it.

normalize with DTM
: colors and slices the points by their height over the DTM, see [Heights over a DTM](#heights-over-a-dtm).

points over terrain
: with a terrain, draws the points even where the terrain is above them, which happens when their elevations do not match those of the terrain, as with the coarse Copernicus terrain under a forest.

point size
: the size of the points, in pixels.

fit colors, auto colors
: the range of the colors, see [Slicing with the range slider](#slicing-with-the-range-slider).

### Coloring

**Color and slice by**, below the view, chooses the value used to color the points, which is also the value sliced by the range slider:

elevation
: the elevation of the points, or their height above the DTM when normalized.

intensity
: the intensity of the return, which helps to tell apart materials, for example bark and leaves, or the road and its markings.

classification
: the classification code of the points: ground, vegetation, buildings and so on.

impulse
: the return number, which separates the first returns, on the top of the canopy, from the last ones, nearer to the ground.

own color
: the RGB colors stored in the file, sliced by elevation.

Files without intensity, classification or colors, as many terrestrial scans, are drawn in a single color with those choices.

For elevation and intensity the colors leave out the extreme 0.5% of the values at each end, so that a few outliers, like birds or noise far above the canopy, do not leave all the other points in the same color.

## Slicing with the range slider

The range slider at the right of the view shows only the points whose value is between its two knobs. The fields above and below it show the exact values, and new ones can be typed and applied with Enter.

To move a slice of fixed width through the points:

1. type the width of the slice in the field under **lock** and press Enter, or press **lock** to keep the current width;
2. drag the slice, click on the track or use the mouse wheel over the slider, which moves it by half of its width.

:::{figure} ../images/apps/lasviewer/slice.png
:alt: A horizontal slice of the forest
:width: 100%
:align: center

A locked slice 0.70 m thick, between 5.00 and 5.70 m: the crowns of the trees cut at that height. In such a thin slice all the points have nearly the same color.
:::

**fit colors** stretches the colors over the range of the slider, which then spans that range only: the details of a thin slice, or of the values between outliers, become visible. **auto colors** goes back to the colors over all the values.

:::{figure} ../images/apps/lasviewer/fitcolors.png
:alt: The colors fitted to the slice
:width: 100%
:align: center

The same slice after fit colors, seen from closer: the full color ramp now spans the 70 cm of the slice.
:::

## Heights over a DTM

When a DTM is set, the height of each point above the ground is calculated when the points are loaded. With **normalize with DTM** checked, the elevation colors and the slider use these heights, which turns a sloping terrain flat: a slice at 1.30 m then cuts all the trunks at breast height, wherever they stand on the slope.

How the points are placed depends on the view:

- with **3D, flat ground** they are drawn at their height above the ground, on the flat ground;
- with a terrain they stay at their real elevation, on the terrain; **3D, terrain from the DTM** places them on the same ground they were normalized with.

When the DTM is set after the points were loaded, press **Refilter loaded** or **Load data** to calculate the heights.
