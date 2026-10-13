# TrentoPProjectFilesGenerator

TrentoPProjectFilesGenerator creates the spatial database of a storm sewer network for [TrentoPProject](trentopproject.md) and [TrentoPCalibration](trentopcalibration.md): the empty layers of the pipes, areas and junctions, with all their fields, the table of the parameters and, for a project, the table of the commercial diameters.

The database can be a GeoPackage or H2GIS file, or a PostGIS database. In Project mode it gets the layers `network_project`, `areas` and `junctions`, the parameters of the design and an example series of PVC diameters, from 14 to 100 cm. In Calibration mode, the layer `network_calibration`, `areas`, `junctions` and the parameters of the verification. The optional parameters get their default as value, the needed ones are left empty.

An existing database is completed, not replaced: only the layers, tables and parameters it does not have yet are added. Running the module in both modes on the same database prepares the project and the calibration of a network together.

Once created, open the database in a GIS, as QGIS, draw the pipes, areas and junctions and fill their fields and the parameters, as described in [preparing the data](trentopproject.md#preparing-the-data).

## Example

The example creates a GeoPackage for both the project and the calibration of a network:

```{literalinclude} ../examples/hm/trentopprojectfilesgenerator.groovy
:language: groovy
```

The GeoPackage has the layers `network_project`, `network_calibration`, `areas` and `junctions`, and the tables `parameters` and `diameters`.

## Reference

```{include} ../generated/TrentoPProjectFilesGenerator.md
```
