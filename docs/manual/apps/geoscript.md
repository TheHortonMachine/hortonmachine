# Geoscript Console

The Geoscript Console is a scripting environment with the whole HortonMachine available. It is the place to chain processing modules together, to run them in batch over many files, and to quickly check data and services: read a raster cell, query a database, inspect a WFS, WCS or STAC service.

Scripts are written in [Groovy](https://groovy-lang.org/), a language that runs on Java and reads much like a simplified Java. The console also includes [GeoScript](https://geoscript.net/), a Groovy library for geometries, projections, layers and rendering.

## Launching

Start the application with the `hm-geoscript` launcher (see [Launching an application](../installation.md#launching-an-application)).

## The console

:::{figure} ../images/apps/geoscript/network_run.png
:alt: The Geoscript Console after running a script
:width: 100%
:align: center

A script extracting the stream network from a DTM, and its output.
:::

The console is based on the standard Groovy Console:

- the **editor** on the left, with syntax highlighting and line numbers;
- the **output** on the right, where the script prints its messages and the modules their progress;
- the **status bar** at the bottom, which tells whether the last execution completed or ended with an error.

Run the script with **Script > Run** (or Ctrl+R) or the run button of the toolbar. The **Script** menu also has an entry to interrupt a running script, and the broom button of the toolbar clears the output. Scripts are saved and opened with the **File** menu, as `.groovy` files.

## The HM menu

The **HM** menu adds a few HortonMachine specific helpers to the standard console.

:::{figure} ../images/apps/geoscript/hm_menu.png
:alt: The HM menu
:width: 80%
:align: center

The HM menu with its examples.
:::

Add HM main imports
: inserts at the beginning of the script the imports of the HortonMachine support class `HM` and of the processing modules.

Add Geoscript main imports
: inserts the imports of the main GeoScript packages: geometries, projections, rendering, layers, styles, viewers, filters and workspaces.

Show HM class helper methods
: prints in the output the list of the helper methods of the `HM` class, grouped by topic.

Examples
: inserts at the cursor position a ready-made example script, to be completed with your own paths and values:
  - *Add prj to files in folder*: adds a projection file to all the files of a folder;
  - *Create/render geometries*: creates some geometries and shows them as an image;
  - *Work with a database*: connects to a PostGIS database and explores its tables, columns and geometries;
  - *Print raster cell info*: prints the values around a raster cell and renders them as an image;
  - *Extract stream network from dtm*: the example used below.

:::{note}
The console doesn't add any import automatically: start your scripts with **Add HM main imports** (and **Add Geoscript main imports** when using GeoScript), or with your own imports.
:::

## Running modules from a script

Every module of the [Spatial Toolbox](spatialtoolbox.md) can be used from a script: create it, set its parameters, and call `process()`. The parameter names are the same shown by the scripts that the Spatial Toolbox saves.

This example, adapted from **HM > Examples > Extract stream network from dtm**, chains four modules to extract the stream network from a DTM:

```groovy
import org.hortonmachine.*
import org.hortonmachine.modules.*

def folder = "/home/hydrologis/data/flanginec/"
def dtm = folder + "dtm_flanginec.asc"
def pit = folder + "pit_flanginec.tif"
def flow = folder + "flow_flanginec.tif"
def drain = folder + "drain_flanginec.tif"
def tca = folder + "tca_flanginec.tif"
def net = folder + "net_flanginec.tif"
def thres = 100.0

// fill the depressions of the dtm
def pitfiller = new Pitfiller()
pitfiller.inElev = dtm
pitfiller.outPit = pit
pitfiller.process()

// calculate the drainage directions
def flowdirections = new FlowDirections()
flowdirections.inPit = pit
flowdirections.pMinElev = 0
flowdirections.outFlow = flow
flowdirections.process()

// correct the drainage directions and calculate the total contributing area
def draindir = new DrainDir()
draindir.inPit = pit
draindir.inFlow = flow
draindir.pLambda = 1.0
draindir.doLad = true
draindir.outFlow = drain
draindir.outTca = tca
draindir.process()

// extract the network where the contributing area is over the threshold
def extractnetwork = new ExtractNetwork()
extractnetwork.inTca = tca
extractnetwork.inFlow = flow
extractnetwork.pThres = thres
extractnetwork.outNet = net
extractnetwork.process()
```

The modules read and write the files themselves, so a script only has to pass the paths. Plain Groovy can then add loops, conditions and file handling around them, for example to process all the DTMs of a folder.

## The HM helper class

The `HM` class collects shortcuts for everyday tasks. **HM > Show HM class helper methods** prints them all, each with its description and signature.

:::{figure} ../images/apps/geoscript/hm_methods.png
:alt: The HM helper methods
:width: 70%
:align: center

The first part of the list of the HM helper methods.
:::

The methods are grouped by topic:

Generic tools
: loading other script files, converting timestamps, adding projection files to a folder, color interpolators.

Chart tools
: charts of matrices, histograms, time series and scatter plots.

Databases tools
: connecting to PostGIS, SpatiaLite, GeoPackage, SQLite and H2GIS databases, and running queries and updates on them.

Rendering tools
: raster color tables, QGIS and SLD style files for rasters, images of raster cells, geometries and WKT strings, and a simple spatial viewer on a folder of data.

Spatial tools
: distances between WGS84 coordinates, spatial indexes, reading and writing rasters and vectors, and opening WCS services.

Dialogs
: simple dialogs to ask the user for input, a yes/no answer or a choice, and to show messages.

For example, `HM.makeQgisStyleForRaster("net", net, 0)` creates the QGIS style file for the network raster of the example above.

## Running scripts in batch

Passing a script file to the launcher runs it without opening the console, printing its output in the terminal:

```sh
./hm-geoscript.sh /path/to/extract_network.groovy
```

This is the way to run long processing chains unattended, or from a scheduled task. Only the first argument is used: scripts that need parameters can read them from a file, or be copied and adapted.
