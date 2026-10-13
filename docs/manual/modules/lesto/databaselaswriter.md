# DatabaseLasWriter

DatabaseLasWriter inserts the LAS files of a folder into a spatial database, spatialite or H2GIS: the points grouped in cells, with their summaries, and coarser levels of summaries over them. The lidar modules read the database in place of a LAS file, and the 3D viewer draws it quickly at any scale, from the levels when far and from the cells when near.

## Example

The four pieces of the sample survey written by [LasSplitter](lassplitter.md), in a spatialite database with cells of 1 m and two levels, of 5 and 10 m:

```{literalinclude} ../examples/hm/databaselaswriter.groovy
:language: groovy
```

The database `las_pieces.sqlite` has the four pieces in the table `lassources`, their points in the cells of `lascells` and the summaries in `laslevels1` and `laslevels2`.

## Reference

```{include} ../generated/DatabaseLasWriter.md
```
