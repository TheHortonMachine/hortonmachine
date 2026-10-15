# LasFromFaroTlsXyz

LasFromFaroTlsXyz converts a scan of a FARO terrestrial laser scanner to a LAS file. The scan is the text export of the scan points of the SCENE software, with the coordinates of each point from the scanner and its color: the module places them in a projected reference system, from the position of the scanner, and keeps the points within a distance from it.

## Details

```{include} ../generated/LasFromFaroTlsXyz_details.md
```

## Parameters

```{include} ../generated/LasFromFaroTlsXyz.md
```

## Example

A scan with the scanner placed at 681270 E, 5127117 N in UTM zone 32N, keeping the points within 15 m of it. The manual has no sample scan, so this example is not run:

```{literalinclude} ../examples/hm/lasfromfarotlsxyz.groovy
:language: groovy
```

Each line of the scan has the row and column of the point in the scan, its x and y from the scanner, its elevation and its color:

```text
1205 3487 -7.4184 0.3945 1190.8795 254 254 254
```
