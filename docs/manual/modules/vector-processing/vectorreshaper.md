# VectorReshaper

Changes the attributes and the geometries of a vector with expressions: it adds fields computed from the other fields or from the geometry, replaces existing ones, and removes the fields not needed. The geometries can be transformed as well, into buffers, centroids or convex hulls.

## Example

The example runs on the polygons of the sub-basins of [BasinShape](../basin/basinshape.md), extracted from the sample elevation model of the manual:

```{literalinclude} ../examples/hm/vectorreshaper.groovy
:language: groovy
```

The first sub-basins, before:

| netnum | area | perimeter | maxelev | minelev | avgelev | height |
|---|---|---|---|---|---|---|
| 1 | 38100 | 2360 | 2143.2 | 1731.3 | 2009.0 | 2013.9 |
| 2 | 10500 | 680 | 2150.3 | 2023.9 | 2099.7 | 2098.9 |
| 3 | 10000 | 640 | 2148.4 | 2023.1 | 2088.0 | 2085.4 |

and after:

| netnum | area | perimeter | maxelev | minelev | avgelev | relief | area_km2 |
|---|---|---|---|---|---|---|---|
| 1 | 38100 | 2360 | 2143.2 | 1731.3 | 2009.0 | 411.9 | 0.0381 |
| 2 | 10500 | 680 | 2150.3 | 2023.9 | 2099.7 | 126.4 | 0.0105 |
| 3 | 10000 | 640 | 2148.4 | 2023.1 | 2088.0 | 125.3 | 0.0100 |

## Reference

```{include} ../generated/VectorReshaper.md
```
