# TrentoPProject

TrentoPProject designs a storm sewer network. It takes the layout of the pipes, the areas they drain and the ground elevation at their junctions, and gives each pipe its design discharge, its commercial diameter, its slope and the depth of its excavation. [TrentoPCalibration](trentopcalibration.md) then verifies an existing network, or the designed one, for a given rain.

The model is the geomorphological model of TrentoP. Each drained area is a linear reservoir, and the water travels through the pipes with the delay of a kinematic wave. The pipes are designed one after the other, from the head pipes down to the outlet. Each pipe gets the peak discharge of the rain from the rainfall curve whose duration maximizes it, and the smallest commercial diameter that carries that discharge at the design fill degree while staying self-cleaning. The formulas are in the notes of the reference below.

## Preparing the data

Both modules read a spatial database, given in `inDatabase`: a GeoPackage (`.gpkg`) or H2GIS (`.mv.db`) file, or a PostGIS database, as `jdbc:postgresql://host:port/database`, with `pUser` and `pPassword`. It holds layers and tables with fixed names, in a projected reference system in meters:

| Layer or table | Content | Project | Calibration |
|---|---|---|---|
| `network_project` | the pipes, as lines | ✓ | |
| `network_calibration` | the pipes of an existing network, with their diameters | | ✓ |
| `areas` | the areas drained by the pipes, as polygons | ✓ | ✓ |
| `junctions` | the junctions at the ends of the pipes, as points | ✓ | ✓ |
| `parameters` | the parameters of the model | ✓ | ✓ |
| `diameters` | the commercial diameters | ✓ | |

The results are written into the same database: the designed pipes as `network_project_output`, the verified ones as `network_calibration_output`. Running a module again replaces them.

Start from [TrentoPProjectFilesGenerator](trentopprojectfilesgenerator.md): it creates the database with the empty layers, with all their fields, the parameters with their defaults and an example series of diameters. Then draw the pipes, areas and junctions in a GIS, fill their fields and the needed parameters. The project and the calibration of a network can share one database.

### The pipes

Draw a line for each pipe, between two junctions. The pipes are connected by their ends: the topology of the network is built from the geometries, starting from the outlet pipe given by `pOutPipe` and following the pipes whose ends are within 0.1 m of each other, so snap the ends while drawing. The direction of the lines does not matter, since the end at the higher junction is the upstream one. At most `Max number of junction` pipes can drain into the end of a pipe.

The fields of the pipes:

| Field | Type | Content |
|---|---|---|
| `ID` | integer | the unique number of the pipe |
| `Runoff` | real | the runoff coefficient of its area, from 0 to 1 |
| `AveResT` | real | the coefficient c of the residence time of its area, the mean residence time per unit area, in h/ha (see below) |
| `ks` | real | the roughness of Gauckler-Strickler, in m^(1/3)/s |
| `AveSlope` | real | the average slope of its area, in % |
| `MinSlope` | real | the minimum slope of the pipe, in % (project only) |
| `Section` | integer | the section: 1 circular, 2 rectangular, 3 trapezoidal (project only) |
| `diameter` | real | the inner diameter of the existing pipe, in cm (calibration only) |
| `dryArea` | real | optional: the part of the area that drains into the pipe, from 0 to 1 |

`dryArea` is not created by the generator: add it to the layer when only a part of some areas drains into the network, and then give it a value for every pipe. The `IDdrain` field, the pipe each pipe drains into, is not read: it is computed from the geometries and written in the output.

`AveResT` and `Runoff` are the two parameters that set how much water reaches the network, and how fast. The original manual of TrentoP gives this advice for them:

- `AveResT` is, in practice, the calibration parameter of the model, more than a physical quantity: set it from a measured event of the network when there is one, else from experience, and try a few values around the likely one. From the Italian test basins of TrentoP it is between 0.3 and 1 h/ha, around 0.38. It gets smaller as the slope of the area grows, and as the share of impervious surface grows.
- `Runoff` depends on the surfaces of the area, but also on the rain, and changes from event to event. With a measured event, set it so that the volumes match; without, take it from an empirical formula, and try a few values.

### The areas

Draw, for each pipe, the polygon of the area that drains into it, with the `ID` of the pipe: every pipe needs exactly one area. Its surface, in ha, is the area of the polygon. The inflow of the area is distributed along the pipe.

### The junctions

Put a point at each end of every pipe, within 0.1 m of it, and give it the ground elevation in the `elev` field, in m. Each end needs exactly one point, so two junctions can't be closer than 0.1 m. The elevations can be taken from an elevation model with [RasterCatToFeatureAttribute](../vector-processing/rastercattofeatureattribute.md), with `fNew = "elev"`. The other fields of the junctions are not used.

### The parameters

The table `parameters` has a row for each parameter, with its `name`, the one in the tables below, its `value`, and, for reference, its `unit`, `default_value`, `min_value` and `max_value`. The generator fills the value of the optional parameters with their default, and leaves the needed ones, the ones without a default, empty: give them a value. A parameter without value, or missing, takes its default.

The parameters of the design:

| Name | Unit | Default | Content |
|---|---|---|---|
| `Coefficient of the pluviometric curve` | mm/h^n | | a of the rainfall curve h = a·t^n, with t in hours |
| `Exponent of the pluviometric curve` | - | | n of the rainfall curve, from 0.05 to 0.95 |
| `Tangential bottom stress` | N/m² | | the shear stress at the bottom that keeps the pipes self-cleaning |
| `Fill degree` | - | | the design fill degree, water depth over diameter, up to 0.99 |
| `Align mode` | - | | at the junctions, 0 aligns the free surfaces by changing the depth of the pipes, 1 aligns them with steps in the bottom |
| `Time step for simulation` | min | 0.25 | the time step of the search of the peak discharge, from 0.015 to 0.5 |
| `Minimum Rain Time step` | min | 5 | the shortest rain duration of the search, at least 5 |
| `Maximum Rain Time step` | min | 60 | the longest rain duration of the search, at least 30 |
| `Minimum excavation depth` | m | 1.2 | the depth of the top of the pipes under the ground |

The other parameters, which usually keep their default:

| Name | Default | Content |
|---|---|---|
| `Max number of junction` | 4 | the most pipes that can drain into the end of a pipe, up to 6 |
| `Celerity factor` | 1 | the celerity of the wave over the mean velocity, from 1 to 1.6 |
| `Exponent of the basin extension` | 0.3 | α, the exponent of the area in the residence time |
| `Exponent of the influx coefficent` | 0.4 | b, the exponent of the runoff coefficient in the residence time |
| `Exponent of the average ponderal slope` | 0.2 | γ, the exponent of the slope in the residence time |
| `Minimum discharge in a pipe` | 1 | the first guess of the discharge, in l/s |
| `Minimum fill degree` | 0.01 | the lowest fill degree, up to 0.1 |
| `Maximum fill degree` | 6.28319 | the fill of a full pipe, as the angle of its wetted perimeter in radians: 2π is the whole pipe |
| `Base to height` | 1 | the base over the height of rectangular and trapezoidal sections |
| `Max bisection number`, `Precision`, `Tolerance`, `Accuracy (bisection mode)` | 40, 0.001, 0.0001, 0.005 | the iterations and accuracy of the numerical solutions |

### The diameters

The table `diameters` lists the commercial diameters of circular pipes, one per row, with their `diameter` and `thickness`, both in cm. The generator fills it with an example series of PVC pipes, from 14 to 100 cm: change it to the pipes that will be used. The diameter is the inner one, used for the hydraulics; the thickness adds to the excavation. Check the unit: a list in mm is read as cm, and gives pipes ten times too large.

## Example

The example designs a small network of 25 pipes, which drain 2 ha to the outlet pipe 31. Its data are in the GeoPackage `docs/manual/modules/maps/data/sewer_project.gpkg`, with the elevation model of the area, `dtm_sewer.tif`, at 1 m:

```{literalinclude} ../examples/hm/trentopproject.groovy
:language: groovy
```

:::{figure} ../../images/modules/TrentoPProject_network_project_output_diameter.png
:alt: The designed network, colored by the diameter of the pipes, in cm: 14 cm in the head pipes, up to 40 cm in the collector down to the outlet.

The designed network, colored by the diameter of the pipes, in cm: 14 cm in the head pipes, up to 40 cm in the collector down to the outlet, at the bottom left.
:::

The design discharge at the outlet is 285 l/s. The output layer `network_project_output` has, besides the input fields and `IDdrain`:

| Field | Content |
|---|---|
| `discharge` | the design discharge, in l/s |
| `cUdomet` | the discharge per unit area, in l/s/ha |
| `resTime` | the residence time of the area, in minutes |
| `tP` | the rain duration that gives the peak, in minutes |
| `t_Qmax` | the time of the peak, in minutes |
| `mean_speed` | the mean velocity, in m/s |
| `pipeslope` | the slope of the pipe, in % |
| `diameter` | the diameter, in cm |
| `empty_deg` | the fill degree, water depth over diameter |
| `DownDepth`, `upDepth` | the elevation of the bottom of the excavation, in m: despite the names, `DownDepth` at the upstream end and `upDepth` at the downstream end |
| `DoFreeSurf`, `UpFreeSurf` | the elevation of the free surface, in m, likewise at the upstream and downstream end |
| `totArea` | the area drained by the pipe and all the pipes upstream, in ha |
| `totlength`, `meanlength`, `varLength` | the total length of the network upstream, the mean length of its pipes, in m, and its variance |

## Reference

```{include} ../generated/TrentoPProject.md
```
