# TrentoPCalibration

TrentoPCalibration verifies a storm sewer network whose pipes are known: for a rain, it gives the discharge and the fill degree of each pipe in time. It uses the same geomorphological model as [TrentoPProject](trentopproject.md), which designs a new network.

The rain is either a time series of measured intensities, or a rain generated from the rainfall curve. The slope of each pipe is the slope between the ground elevations of its two junctions.

:::{warning}
When a pipe fills up completely, the simulation stops, with a warning that names the pipe. The pipes downstream of it, which were not computed yet, keep a discharge of 0: check the warnings, and a fill degree that reaches its maximum, before reading the results.
:::

## Details

```{include} ../generated/TrentoPCalibration_details.md
```

## Preparing the data

The database holds the layers `network_calibration`, `areas` and `junctions` and the table `parameters`, prepared as described for [TrentoPProject](trentopproject.md#preparing-the-data), and created with [TrentoPProjectFilesGenerator](trentopprojectfilesgenerator.md) in Calibration mode. To verify a designed network, copy its `network_project_output` layer to `network_calibration`: it has all the fields needed. The pipes have the field `diameter`, the inner diameter of the existing pipes in cm, instead of `MinSlope` and `Section`.

All the pipes are taken as circular: other sections, as egg-shaped or box sections, can't be verified. Each pipe needs a drop between its two junctions, since its slope comes from their elevations: a flat pipe can't be computed. For the verification of a real network, the elevations of the junctions are best the ones of the pipe bottoms, when they are known, rather than the ones of the ground.

The verification is where `AveResT` and `Runoff` are calibrated: with a measured rain and the discharges measured in the network, see the advice on them in [TrentoPProject](trentopproject.md#the-pipes).

In the table `parameters`, the time step, the duration of the simulation and the maximum rain duration can be given instead of the arguments of the module, which win when both are set:

| Name | Unit | Content |
|---|---|---|
| `Hydraulic time step` | min | the time step of the generated rain and of the results, the argument `dt` |
| `Maximum simulation time step` | min | the duration of the simulation, the argument `tMax`, by default 120 |
| `Maximum Rain Time step` | min | the maximum rain duration, the argument `tpMaxCalibration` |
| `Minimum Rain Time step` | min | the shortest rain duration of the search, by default 5 |
| `Coefficient of the pluviometric curve` | mm/h^n | a of the rainfall curve, to generate the rain |
| `Exponent of the pluviometric curve` | - | n of the rainfall curve, to generate the rain |

The other parameters are the optional ones of TrentoPProject, from the `Celerity factor` to the exponents of the residence time.

### The rain

With the two parameters of the rainfall curve, the rain is generated, as in TrentoPProject. A rain lasting tp has the constant intensity a·tp^(n-1), so that it brings the depth a·tp^n of the curve: shorter rains are more intense, longer ones bring more water. With a maximum rain duration, the durations from the `Minimum Rain Time step` up to it, in steps of the time step, are tried, and the one that gives the highest discharge is kept; the module returns it as `outTpMax`. The time step is also the shortest rain that can be tried. Without a maximum rain duration, a single rain lasting the whole simulation is used.

Without the rainfall curve, the rain is read from `inRain`, an OMS time series of intensities in mm/min, at a constant time step, which is also the time step of the results:

```
@T, "rain data"
DateFormat,yyyy-MM-dd HH:mm

@H,   time,rain
,2020-06-01 12:00,0.5
,2020-06-01 12:05,0.5
,2020-06-01 12:10,0.4
```

## Parameters

```{include} ../generated/TrentoPCalibration.md
```

## Example

The example verifies an existing network of 25 pipes, from 12 to 40 cm, which drain 2 ha to the outlet pipe 31. Its data are in the GeoPackage `docs/manual/modules/maps/data/sewer_calibration.gpkg`, and all the times are in its parameters: a time step of 5 minutes, 120 minutes of simulation and rains from 5 to 60 minutes:

```{literalinclude} ../examples/hm/trentopcalibration.groovy
:language: groovy
```

The network is small and quick, so the shortest rain, 5 minutes, the most intense, gives the highest discharge. The output layer `network_calibration_output` has, besides the input fields, the maximum discharge `qmax`, in l/s, its time `t_qmax`, in minutes from the start, and the maximum fill degree `fillmax`, water depth over diameter. No pipe fills up: the fullest reach 0.55.

:::{figure} ../../images/modules/TrentoPCalibration_network_calibration_output_fillmax.png
:alt: The maximum fill degree of the pipes, for the critical rain of 5 minutes.

The maximum fill degree of the pipes, for the critical rain of 5 minutes: the loop of pipes at the top and the collector down to the outlet, at the bottom left, are the fullest.
:::

The discharge of each pipe in time is written to `outDischarge`, a table with the time, in minutes, and a column for each pipe, and the fill degree likewise to `outFillDegree`.

:::{figure} ../../images/modules/TrentoPCalibration_sewer_discharge.png
:alt: The discharge, in l/s, of the outlet pipe 31, of the collector pipe 27 and of the head pipe 1, for the critical rain of 5 minutes: the peak at the outlet is 227 l/s.

The discharge, in l/s, of the outlet pipe 31, of the collector pipe 27 and of the head pipe 1, for the critical rain of 5 minutes: the peak at the outlet, 227 l/s, comes right after the end of the rain, and the network empties in about half an hour.
:::
