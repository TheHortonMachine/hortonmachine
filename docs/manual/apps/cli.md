# Command line

The `hm-cli` command line lists, describes and runs the HortonMachine processing modules without any window. It is meant for scripts, servers and batch processing, and for other programs that want to use the modules, as it can describe them in a machine readable format.

It offers the same modules of the [Spatial Toolbox](spatialtoolbox.md), with the same parameters.

## Launching

Run the `hm-cli` launcher from a terminal, `hm-cli.sh` on Linux and macOS, `hm-cli.bat` on Windows (see [Launching an application](../installation.md#launching-an-application)). It works from any folder, so it can be called with its full path:

```sh
/path/to/hortonmachine/hm-cli.sh list
```

Run it without arguments to see the commands. In the examples below, `hm-cli` stands for the launcher of your platform.

The modules can use up to 2 GB of memory by default. To process large datasets, raise it with the `HM_MEM` environment variable, as for all the launchers (see [Memory](../installation.md#memory)):

```sh
HM_MEM=16g ./hm-cli.sh run Pitfiller ...
```

On Windows: `set HM_MEM=16g` before running `hm-cli.bat`.

## Listing the modules

`hm-cli list` lists the modules with their folder of the Spatial Toolbox. A text after the command shows only the modules whose name, folder or keywords contain it, ignoring the case:

```text
$ hm-cli list pit
DePitter         Dem Manipulation
MultiTca         Geomorphology
NetDiff          Network
Pitfiller        Dem Manipulation
PitfillerBarnes  Dem Manipulation
```

## Describing a module

`hm-cli help <Module>` describes a module and its parameters, grouped as in the Spatial Toolbox into inputs, parameters and outputs, with their units, ranges, allowed values and defaults:

```text
$ hm-cli help Gradient
Gradient (Geomorphology)

Calculates the gradient of the elevation, the steepest slope in each cell, in any direction, from
the 3x3 window around the cell.

Usage: hm-cli run Gradient [--parameter=value ...]

Inputs:
  --inElev=<raster>
      The map of the elevation. Unit: m.

Parameters:
  --pMode=<choice>
      The formula of the gradient: finite differences, from the four neighbours, or the methods of
      Horn and of Evans, from all the eight neighbours and less sensitive to the noise of the
      elevation. One of: Finite Differences, Horn, Evans. Default: Finite Differences.
  --doDegrees[=true|false]
      Write the gradient in degrees; if not set, as the tangent of the slope angle. Default: false.

Outputs:
  --outSlope=<raster>
      The map of the gradient. Unit: m/m or °.
```

The names of the modules are those of the Spatial Toolbox; a wrong case is accepted, and a wrong name gets suggestions of similar ones.

## Running a module

`hm-cli run <Module>` runs a module, with its parameters given as `--parameter=value`:

```sh
hm-cli run Gradient --inElev=/data/flanginec/pit_flanginec.tif --doDegrees --outSlope=/data/flanginec/slope_flanginec.tif
```

- Values with spaces must be quoted, as in `--pMode="Finite Differences"`.
- An option can be given alone to set it to true, as `--doDegrees` above, or as `--doDegrees=false`.
- Parameters not given keep the default value of the module.
- The formats of the files are those of the Spatial Toolbox (see [Setting the parameters](spatialtoolbox.md#setting-the-parameters)).

Before running, the values are checked: unknown parameters, values not allowed, numbers out of range, input files that don't exist and outputs in folders that don't exist stop the command with a message, before any data is read.

The parameters can also be written in a JSON file, given with `--params`; parameters given on the command line override those of the file:

```json
{
    "inElev": "/data/flanginec/pit_flanginec.tif",
    "pMode": "Horn",
    "doDegrees": true,
    "outSlope": "/data/flanginec/slope_flanginec.tif"
}
```

```sh
hm-cli run Gradient --params=gradient.json --pMode=Evans
```

When something goes wrong in the module, the error message is shown; run again with `--debug` to see the full error trace, for example to attach it to a bug report.

## Output and exit codes

While running, the command writes the progress of the module, then the outputs and the time taken:

```text
Task: Reading coverage: pit_flanginec.tif
Task: Processing gradient... (Finite Differences)
Progress: 0%
Progress: 5%
...
Progress: 100%
Task: Writing coverage: slope_flanginec.tif
Output: outSlope = /data/flanginec/slope_flanginec.tif
Done in 0.6 s.
```

The lines that start with `Task:`, `Progress:` and `Output:` have always this form, so that other programs can follow the run. The other lines are messages of the module. Errors are written to the error stream.

The exit code tells the result:

| Code | Meaning |
|---|---|
| `0` | the module ran successfully |
| `1` | the module failed |
| `2` | wrong usage: unknown command, module or parameter, or a value not valid |

## Using the command line from other programs

`hm-cli describe` writes the description of all the modules as JSON; `hm-cli describe <Module>` of a single one. For each parameter it gives the name, the kind (`input`, `parameter` or `output`), the type of data, the description and, if available, the unit, range, allowed values and default:

```json
{
  "format" : 1,
  "version" : "0.11.5",
  "modules" : [ {
    "name" : "Gradient",
    "class" : "org.hortonmachine.modules.Gradient",
    "folder" : "Geomorphology",
    "description" : "Calculates the gradient of the elevation, ...",
    "status" : "certified",
    "keywords" : [ "Geomorphology", "Slope", "Gradient" ],
    "parameters" : [ {
      "name" : "inElev",
      "kind" : "input",
      "type" : "raster",
      "computed" : false,
      "description" : "The map of the elevation.",
      "unit" : "m"
    }, {
      "name" : "pMode",
      "kind" : "parameter",
      "type" : "choice",
      "computed" : false,
      "description" : "The formula of the gradient: ...",
      "choices" : [ "Finite Differences", "Horn", "Evans" ],
      "default" : "Finite Differences"
    } ]
  } ]
}
```

The types of data are `raster`, `vector`, `las`, `csv`, `file` and `folder` for paths, `crs`, `choice`, `text`, `string`, `number`, `integer` and `boolean` for values. Parameters marked as `computed` are values calculated by the module, printed as `Output:` lines at the end of the run, which can't be set. The `format` number changes only if the structure of the description changes in a way that breaks the programs reading it.

Together with the fixed form of the progress lines and the exit codes, this lets other applications offer the modules in their own interface and run them through `hm-cli run`.
