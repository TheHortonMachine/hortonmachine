# Geomorphon

Geomorphons (Jasiewicz and Stepinski, 2013) classify the landforms by the way the terrain around a cell is seen from it. In each of the 8 main directions, up to the search radius, the terrain is higher, lower or level with the line of sight from the cell, within the angle threshold. The pattern of the 8 answers gives one of ten landforms:

| Code | Landform | Code | Landform |
|---|---|---|---|
| 1000 | flat | 1005 | slope |
| 1001 | peak | 1006 | hollow |
| 1002 | ridge | 1007 | footslope |
| 1003 | shoulder | 1008 | valley |
| 1004 | spur | 1009 | pit |

The search radius sets the scale of the landforms: a small radius finds the small channels and ridges, a large one the main valleys and mountain ridges. The angle threshold sets when the terrain is level: larger values give more flat areas.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/geomorphon.groovy
:language: groovy
```

:::{figure} ../../images/modules/Geomorphon_geomorphon.png
:alt: The landforms of the sample area, with a search radius of 300 m: the valleys follow the network, the ridges the divides, hollows and spurs are the concave and convex parts of the slopes.

The landforms of the sample area, with a search radius of 300 m: the valleys follow the network, the ridges the divides, hollows and spurs are the concave and convex parts of the slopes.
:::

## Reference

```{include} ../generated/Geomorphon.md
```
