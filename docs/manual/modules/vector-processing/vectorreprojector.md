# VectorReprojector

Reprojects a vector, or all the vectors of a folder, to another coordinate reference system, as the data of a national grid to UTM or to lat/long for a web map. A vector without crs, or with a wrong one, can be given its crs first.

## Details

```{include} ../generated/VectorReprojector_details.md
```

## Parameters

```{include} ../generated/VectorReprojector.md
```

## Example

The example reprojects the sub-basins of [BasinShape](../basin/basinshape.md), in the crs of the sample elevation model, Monte Mario / Italy zone 1 (EPSG:3003), to UTM 32N on WGS84 (EPSG:32632):

```{literalinclude} ../examples/hm/vectorreprojector.groovy
:language: groovy
```

The bounds of the sub-basins, before and after:

| crs | west | east | south | north |
|---|---|---|---|---|
| EPSG:3003 | 1638650.0 | 1640330.0 | 5112520.0 | 5114210.0 |
| EPSG:32632 | 638619.3 | 640299.3 | 5112499.0 | 5114189.0 |

The two grids are both transverse Mercator projections with the same central meridian, on different datums: the coordinates differ by the 1000 km of the different false easting and by the datum shift, here 31 m west and 21 m south, the same, to the centimeter, that PROJ computes.
