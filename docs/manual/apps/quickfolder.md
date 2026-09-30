# Quick Folder

Quick Folder is a quick look at the spatial data of a folder: it opens all the rasters and vectors it contains on a simple map, with their styles.

## Launching

Start the application with the `hm-quickfolder` launcher (see [Launching an application](../installation.md#launching-an-application)), followed by one or more folders or files to show:

```sh
./hm-quickfolder.sh /data/flanginec
```

Without arguments, it asks for the folder to show.

The rasters (GeoTIFF, ESRI ASCII grid, GeoPackage) and the vectors (shapefiles and GeoPackage) of the folder are loaded, using their style files (`.sld` with the same name) when present, or a default style otherwise. The map is then shown zoomed to the whole data.

More files can be added by dragging them from the file manager onto the window.

## The map

:::{figure} ../images/apps/quickfolder/main.png
:alt: Quick Folder
:width: 100%
:align: center

The rasters and the shapefile of a folder, with their styles.
:::

The **Layers** list on the left shows the loaded layers: the eye shows or hides a layer, the buttons at the bottom show, hide, select or unselect all of them.

The tools of the toolbar zoom in and out (click, or drag a box), pan, show the values of the layers at the clicked point, and reset the view to the whole data. The globe button adds an OpenStreetMap background. The status bar shows the coordinates under the mouse, the bounds of the view and the projection of the map.

## Raster cell values

The drop-down on the right of the toolbar chooses a raster to inspect. Zoom in on it far enough, and each cell shows its column (`c`), its row (`r`) and its value (`v`): a quick way to check the values of a raster, for example around a problem found by a processing module.

:::{figure} ../images/apps/quickfolder/raster_info.png
:alt: The cell values of a raster
:width: 100%
:align: center

The columns, rows and elevations of the cells of a DTM.
:::
