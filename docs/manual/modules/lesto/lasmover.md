# LasMover

LasMover moves all the points of a LAS file by the given distances along the three axes, as to correct a shift of a survey or a different datum of its elevations. Everything else of the points is kept.

## Example

The sample survey moved 100 m to the east and 0.35 m down:

```{literalinclude} ../examples/hm/lasmover.groovy
:language: groovy
```

The moved file has the bounds of the input, from 725817.98 to 725878.47 m of easting and from 1896.31 to 1948.46 m of elevation.

## Reference

```{include} ../generated/LasMover.md
```
