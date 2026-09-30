# LAS Viewer

The LAS Viewer inspects lidar point clouds in LAS and LAZ format: it shows the header of the file, filters the points by class, return, intensity, height and region, draws them from above, and exports the selected points to a new file.

## Launching

Start the application with the `hm-lasviewer` launcher (see [Launching an application](../installation.md#launching-an-application)).

## The main window

:::{figure} ../images/apps/lasviewer/main.png
:alt: The LAS Viewer
:width: 100%
:align: center

An airborne lidar plot, drawn with the heights above ground calculated from a DTM.
:::

On the left:

Las/Laz input file
: the point cloud to inspect, chosen with the **...** button. Its **Header Information** (version, point format, number of points, bounds, scale and offset, projection) and the **First Point Information** are shown below.

Optional DTM for delta
: a DTM raster (`.asc` or `.tif`) of the same area. When set, the elevation of each point is replaced by its height above the ground, which turns the terrain flat and makes the vegetation and the buildings stand out.

Filters
: which points to read:
  - **sampling**: reads one point every *n*, 1000 by default, to get a quick look at large files. Set it to 1 to read all the points;
  - **classes**: the classification codes to keep, separated by commas (for example `2` for the ground, `3,4,5` for the vegetation);
  - **impulses**: the return numbers to keep (for example `1` for the first returns);
  - **min/max intensity**: the range of intensity to keep, as `min,max`;
  - **DTM lower thres** and **DTM upper thres**: the range of heights above the DTM to keep, when a DTM is set.

Bounds
: the region to read: **West**, **East**, **South**, **North** and the **Min Z** and **Max Z** elevations. They can be typed, taken from a shapefile with **Bounds from file**, or set with the mouse on the preview (see below).

**Load data** reads the points of the file that pass the filters.

## Drawing the points

**Draw loaded data** draws the loaded points from above in the **Preview**, according to the **Preview properties**:

Color by
: the value used to color the points: **elevation** (or the height above the DTM), **intensity**, **classification**, **impulse** (the return number) or **own color**, the RGB colors stored in the file.

point size
: the size of the points, in pixels.

show elevation higher than, show intensity higher than
: highlights the points above the given elevation or intensity.

:::{figure} ../images/apps/lasviewer/elevation.png
:alt: The points colored by elevation
:width: 90%
:align: center

The points colored by their elevation, without a DTM: the slope of the terrain dominates the colors.
:::

:::{figure} ../images/apps/lasviewer/classification.png
:alt: The points colored by classification
:width: 90%
:align: center

The same points colored by classification: ground, vegetation and more.
:::

Click on the preview to set the bounds: a first left click sets the lower left corner, a second one the upper right corner, and the preview is redrawn on the new region. A right click removes the bounds, a middle click enlarges them by 20%. Press **Load data** again to read only the points of the new region.

## Slicing and trunk circles

The **Slicing** tools are meant for dense point clouds of forests, like those of terrestrial or drone scanners, to find the tree trunks.

1. Check **enable slicing mode**, set the vertical **interval** between the slices and their **slice width**, both in meters, and press **Load Slice Data**. The points are cut into horizontal slices, at heights above the DTM when one is set.
2. Choose a slice in the drop-down to draw only its points.
3. **Extract Circles** looks for the circles formed by the points of the trunks in the slice, drawing them in red; **min cell count** is the minimum number of points needed to form a circle.
4. **Save Circles Shp** saves the circles found as a shapefile.

On airborne data like the example above, with a few points per square meter, the slices contain too few points to show the trunks.

## Exporting

Export to shp/las
: saves the points that pass the filters and the bounds to a new file: a LAS file, or a shapefile of points with their attributes. The format is chosen by the extension of the file name.

Create overview
: saves a shapefile with the rectangle covering the points that pass the filters, useful to map the coverage of many lidar files.
