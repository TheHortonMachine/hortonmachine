# QGIS plugin

The HortonMachine plugin for [QGIS](https://qgis.org) adds the processing modules to the QGIS Processing Toolbox. They get the usual QGIS dialogs, with the map layers as inputs and the results loaded in the map, and they can be used in batch mode, in the Graphical Modeler and from Python, like any other Processing algorithm.

The plugin needs QGIS 3.36 or newer, QGIS 4 included. The modules run in Java through the HortonMachine [command line](cli.md), in a separate process, so a long run doesn't block QGIS and can be canceled.

## Installing the plugin

Until the plugin is published in the QGIS plugin repository, install it from its zip file: in QGIS open **Plugins › Manage and Install Plugins › Install from ZIP**, choose the `hortonmachine.zip` file and press **Install Plugin**. As the plugin is experimental, also check **Show also experimental plugins** in the **Settings** tab of the same dialog.

The plugin is developed in the [g-ant-eu/qgis-plugins](https://github.com/g-ant-eu/qgis-plugins) repository, in the `hortonmachine` folder; its `build_plugins.sh` script builds the zip file into `releases/`.

## Setting up the HortonMachine

The plugin needs a HortonMachine installation, which it can download. Open **Plugins › HortonMachine › HortonMachine setup** (QGIS also proposes it with a message the first time):

HortonMachine installation
: **Download the latest release** downloads the HortonMachine from the [GitHub releases page](https://github.com/TheHortonMachine/hortonmachine/releases) and installs it in the QGIS profile folder. **Choose...** uses a HortonMachine already extracted on this computer instead. **Reload modules** reads the list of the modules again, for example after updating the installation.

:::{figure} ../images/apps/qgisplugin/download_hm.png
:alt: Downloading the HortonMachine in the setup dialog
:width: 70%
:align: center

The latest release of the HortonMachine being downloaded.
:::

:::{figure} ../images/apps/qgisplugin/local_hm.png
:alt: A HortonMachine installation chosen on the computer
:width: 70%
:align: center

A HortonMachine already extracted on the computer, chosen with **Choose...**, after reloading its modules.
:::

Java runtime
: the modules need Java 17 or newer. On Windows the HortonMachine includes it. On Linux and macOS the plugin uses the `java` found on the system; if there is none, or it is too old, **Download a Java runtime** downloads one for the system into the HortonMachine installation (about 50 MB). After a release is downloaded, the plugin proposes it by itself when it is needed.

Maximum memory of the modules
: the memory the modules can use, 2 GB by default (or the value of the `HM_MEM` environment variable, see [Memory](../installation.md#memory)), as a number with the unit, as `512m` or `8g`. Raise it to process large datasets.

Once the installation is ready, the modules appear in the **HortonMachine** group of the Processing Toolbox, in the same folders of the [Spatial Toolbox](spatialtoolbox.md).

## Using the modules

The dialog of each module shows its parameters, with the description of the module and of its parameters on the right.

:::{figure} ../images/apps/qgisplugin/processing_toolbox_peakflow.png
:alt: The HortonMachine modules in the Processing Toolbox and the dialog of the Peakflow module
:width: 100%
:align: center

The HortonMachine group in the Processing Toolbox, on the right, and the dialog of the Peakflow module, with its help.
:::

Some notes:

- The modules don't declare which of their inputs are mandatory, so QGIS shows most of them as optional; a module run without a needed input stops with an error.
- Raster inputs are read directly when they are GeoTIFF or ESRI ASCII grid files; other layers are first converted to a temporary GeoTIFF. Vector inputs are passed as shapefiles, converted when needed.
- The outputs are written in the formats chosen in the dialog, GeoTIFF or ESRI ASCII grid for rasters and shapefile or GeoPackage for vectors, and loaded in the map at the end.
- Values the module computes, as statistics, are listed in the log and returned as outputs of the algorithm.
- A few modules have parameters that can only be set from code, like in-memory data: they are not shown in the dialog.

The log of the run shows the command that was executed, which can be copied to run the same module from the [command line](cli.md).

From the QGIS Python console the modules run as any Processing algorithm, by the lowercase name of the module:

```python
processing.run("hortonmachine:gradient", {
    "inElev": "/data/flanginec/pit_flanginec.tif",
    "doDegrees": True,
    "outSlope": "/data/flanginec/slope_flanginec.tif",
})
```
