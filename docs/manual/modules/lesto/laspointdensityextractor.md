# LasPointDensityExtractor

LasPointDensityExtractor counts the points of a LAS file in squares of a given size: their number for each return, their total and their density per square meter. It shows where a survey is dense enough for its use, and how much of the laser goes through the vegetation.

## Example

The density of the sample survey in squares of 5 m:

```{literalinclude} ../examples/hm/laspointdensityextractor.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasPointDensityExtractor_las_density_density.png
:alt: The squares of 5 m colored by their density, mostly between 6 and 12 points per square meter, denser in the north-east of the plot, up to 15; the thin last column and row on the east and north border, outside of most of the data, have almost no points.

The squares of 5 m colored by their density, mostly between 6 and 12 points per square meter, denser in the north-east of the plot, up to 15; the thin last column and row on the east and north border, outside of most of the data, have almost no points.
:::

## Reference

```{include} ../generated/LasPointDensityExtractor.md
```
