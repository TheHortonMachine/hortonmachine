# LasConverter

Converts the points of a LAS lidar file into a vector of points, with their elevation, intensity, classification and return number, or into a csv file, or into a new LAS file with a selection of the points. The points can be filtered by area, with a bounding box or polygons, and by classes, as 2 for the ground, returns, as 1 for the first ones, and intensity. It can also just print the header of the file and information about its points.

## Example

The example runs on the sample lidar survey of the manual, `uni_bz_plot777.las` in `docs/manual/modules/maps/data`, a forest plot of 60 x 60 m with its elevation model `dtm_777.tif`:

```{literalinclude} ../examples/hm/lasconverter.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasConverter_las_points_elev.png
:alt: The lidar points of the sample plot, colored by their elevation, over the hillshade of its elevation model: the crowns of the trees stand out over the ground, and the scanning lines of the survey cross the plot diagonally.

The lidar points of the sample plot, colored by their elevation, over the hillshade of its elevation model: the crowns of the trees stand out over the ground, and the scanning lines of the survey cross the plot diagonally.
:::

## Reference

```{include} ../generated/LasConverter.md
```
