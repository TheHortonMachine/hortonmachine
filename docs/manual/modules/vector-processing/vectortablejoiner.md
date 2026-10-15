# VectorTableJoiner

Joins the fields of a table, a dbf file or the attribute table of another vector, to the features of a vector, by a common field, as the population of a census table to the municipalities by their code.

## Details

```{include} ../generated/VectorTableJoiner_details.md
```

## Parameters

```{include} ../generated/VectorTableJoiner.md
```

## Example

The example makes a table with the mean slope of each sub-basin of [BasinShape](../basin/basinshape.md), with [RasterCatToFeatureAttribute](rastercattofeatureattribute.md) on the slope of [Slope](../geomorphology/slope.md), and joins it, by the number of the sub-basin, to their centroids of the [DelaunayTriangulation](delaunaytriangulation.md) example:

```{literalinclude} ../examples/hm/vectortablejoiner.groovy
:language: groovy
```

The first centroids, with the joined slope, in m/m:

| netnum | avgelev | slope_avg |
|---|---|---|
| 1 | 2009.0 | 0.526 |
| 2 | 2099.7 | 0.534 |
| 3 | 2088.0 | 0.592 |
