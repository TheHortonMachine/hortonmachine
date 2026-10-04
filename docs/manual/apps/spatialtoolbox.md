# Spatial Toolbox

The Spatial Toolbox is the graphical launcher of the HortonMachine processing modules. It lists all the available modules, builds a form for the parameters of the selected one, and runs it, showing its progress in a log window.

This chapter covers the application itself. The single modules and their parameters are described in their own section of the manual.

## Launching

Start the application with the `hm-spatialtoolbox` launcher (see [Launching an application](../installation.md#launching-an-application)).

The launcher passes the `libs` folder of the installation to the application, which uses it to run the modules. For this reason the Spatial Toolbox has to be started from its launcher, inside the extracted distribution.

## The main window

:::{figure} ../images/apps/spatialtoolbox/main.png
:alt: The Spatial Toolbox main window
:width: 100%
:align: center

The Spatial Toolbox at startup.
:::

The window has two parts:

- on the left, the **Modules** tree with the available modules grouped by category, and below it the execution options;
- on the right, the **Parameters** of the selected module, with the **Run script** button at the top right. Until a module is selected, this part only invites to select one.

The modules are grouped into categories, such as *HortonMachine* (hydrology and geomorphology), *Lesto* (lidar), *Raster Processing*, *Vector Processing*, *NetCDF* and *GeoFrame*. Categories can contain sub-categories; for example *HortonMachine* groups its modules into *Dem Manipulation*, *Geomorphology*, *Hydro-Geomorphology* and more.

## Finding a module

:::{figure} ../images/apps/spatialtoolbox/filter.png
:alt: Filtering the modules
:width: 50%
:align: center

The modules whose name contains "pit".
:::

Type in the field above the tree to show only the modules whose name contains the text; the tree is expanded to show all the matches. The filter ignores the case. The trash button next to the field clears the filter.

**Load Experimental**, below the tree, also shows the modules marked as experimental. They have a slightly different icon from the stable ones. Uncheck it to hide them.

## Setting the parameters

Select a module in the tree to show it on the right. At the top are its name, its folder in the tree (with *experimental* for the experimental modules) and what it does; hover over the description to read it in full, if it is longer than one sentence.

:::{figure} ../images/apps/spatialtoolbox/gradient.png
:alt: The parameters of the Gradient module
:width: 80%
:align: center

The parameters of the Gradient module: an input raster, a choice of method, an option and the output raster.
:::

The parameters are grouped in three sections:

Inputs
: the data the module reads, files and folders.

Parameters
: the values that tune the module.

Outputs
: the files and folders the module writes.

Click the title of a section to hide or show it; a module without parameters of a kind has no section for it. Each parameter has its description above its input field, and its unit and range of values, if any, on the right of the field. Hover over a parameter to see its name, the one used in [scripts](#scripts).

The input field depends on the type of the parameter:

Files and folders
: a text field for the path, with a **...** button that opens a file or folder chooser. A file can also be dragged from the file manager and dropped on the field.

Choices
: a drop-down list with the allowed values, like the formula mode of the Gradient module above.

Options
: a check box, for parameters that can only be true or false. Clicking its description also toggles it.

Numbers and text
: a text field.

For a file output, insert the path of the file to create; its extension decides the format, for example `.tif` for a GeoTIFF. Rasters are supported as GeoTIFF (`.tif`, `.tiff`), ESRI ASCII grid (`.asc`) and GeoPackage (`.gpkg`); vectors as shapefiles (`.shp`) and GeoPackage (`.gpkg`).

When the first input file is set, the empty outputs of the same kind of data, raster or vector, get a suggested name, in the same folder and with the same extension as the input. Names in the form *kind_area*, as `pit_flanginec.tif`, get the kind replaced by the name of the output, for example `slope_flanginec.tif` for the Gradient above; other names get it as prefix. A suggestion never overwrites the input, nor a name typed by the user.

Parameters left empty keep the module's default value.

## Help of a module

:::{figure} ../images/apps/spatialtoolbox/help.png
:alt: The help of the Gradient module
:width: 80%
:align: center

The help of the Gradient module.
:::

The **Help** tab, next to **Parameters**, describes the module: its description, folder, status and keywords, the table of its inputs and outputs, with types, units and default values, and its references. For the modules described in the [modules section](../modules/index.md) of this manual, it is the same reference included in their pages, as both are generated from the module itself.

## Running a module

:::{figure} ../images/apps/spatialtoolbox/buttons.png
:alt: The buttons of the module
:width: 30%
:align: center

The buttons below the parameters.
:::

Press the **Run** button, below the parameters, to run the selected module with the parameters of the form.

:::{figure} ../images/apps/spatialtoolbox/pitfiller.png
:alt: The Pitfiller module ready to run
:width: 100%
:align: center

The Pitfiller module, set to fill the depressions of a DTM.
:::

The module runs in a separate Java process, so the Spatial Toolbox stays usable, and more modules can run at the same time. Each run opens a log window showing the progress messages of the module.

:::{figure} ../images/apps/spatialtoolbox/log.png
:alt: The log of a module run
:width: 70%
:align: center

The log of the Pitfiller run.
:::

The log window has three buttons:

Clear
: empties the log.

Copy
: copies the log to the clipboard, for example to attach it to a bug report.

Stop
: stops the running module.

The start and end times of the run are shown at the beginning and at the end of the log.

## Execution options

The options below the modules tree apply to every run. They are remembered for the next sessions.

Heap [MB]
: the maximum memory, in megabytes, of the process that runs the module. Raise it when processing large rasters; keep it below the physical memory of the machine. This is independent of the memory of the Spatial Toolbox itself (see [Memory](../installation.md#memory)).

Debug
: shows more information in the log: the full command used to launch the module, which can also be run from a terminal, and the complete error traces when something fails.

## Scripts

Behind the scenes, each run is a small [Groovy](https://groovy-lang.org/) script that creates the module, sets its parameters and runs it. The script can be saved and run again later, or edited to process many files in a loop.

**Save as script...**, next to **Run**, saves the script of the current module and parameters to a file. For the Gradient module of the example above it looks like this:

```groovy
org.hortonmachine.modules.Gradient _gradient = new org.hortonmachine.modules.Gradient();
_gradient.inElev = """/data/flanginec/pit_flanginec.tif""";
_gradient.pMode = """Finite Differences""";
_gradient.doDegrees = true;
_gradient.outSlope = """/data/flanginec/slope_flanginec.tif""";
_gradient.process();
```

The saved script also ends with a few lines that print the outputs of the module.

**Run script**, the first button at the top right, asks for a script file and runs it, with its log in a window just like a module run. The heap and debug options apply to scripts too.


The saved scripts are plain Geoscript scripts, so they can also be:

- opened, edited and run in the [Geoscript Console](geoscript.md), for example to chain more modules or to process many files in a loop;
- run in batch, without any window, by passing them to the Geoscript launcher (`hm-geoscript.sh` on Linux and macOS, `hm-geoscript.bat` on Windows), as described in [Running scripts in batch](geoscript.md#running-scripts-in-batch):

```sh
./hm-geoscript.sh /path/to/gradient.groovy
```
