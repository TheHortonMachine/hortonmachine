# Profile

Extracts the profile of a raster along a line: its values at regular steps, every cell size, with their distance from the start of the line and their position. On an elevation model it gives the cross sections of the valleys and the long profiles of the rivers; on any other raster, how a value changes along a path.

The line is given as the coordinates of its vertexes, or as the lines of a vector. With a vector, each line gives a profile, written into a CSV file named after a field of the line. The values are the ones of the cells, not interpolated: on detailed elevation models this is precise enough, on coarse ones the profile shows the steps of the cells.

## Example

The example compares the section of a valley of the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, before and after the dam built by [BobTheBuilder](bobthebuilder.md), along its crest. It writes the two profiles into a table:

```{literalinclude} ../examples/hm/profile.groovy
:language: groovy
```

:::{figure} ../../images/modules/Profile_dam_section.png
:alt: The section of the valley along the crest of the dam: the terrain descends into the valley and climbs the other slope, the dam fills the valley at 1310 m between the slopes.

The section of the valley along the crest of the dam: the terrain descends into the valley and climbs the other slope, the dam fills the valley at 1310 m between the slopes.
:::

## Reference

```{include} ../generated/Profile.md
```
