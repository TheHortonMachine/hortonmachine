# MeltonNumber

The Melton number (Melton, 1965) is the relief of a basin divided by the square root of its area:

M = (H<sub>max</sub> - H<sub>min</sub>) / √A

It measures how rugged a basin is: small, steep basins have high values. It is used to tell the basins whose alluvial fans are built by debris flows, with high values, from those built by floods, with low values; the thresholds depend on the region, often around 0.5 to 0.6.

The polygons must be the basins draining into the fans, not the fans. The result is a table of the id of each polygon and its Melton number, also written in the log.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/meltonnumber.groovy
:language: groovy
```

For the basin of ExtractBasin, of 1.9 km² and with a relief of 774 m, the Melton number is 0.56.

## Reference

```{include} ../generated/MeltonNumber.md
```
