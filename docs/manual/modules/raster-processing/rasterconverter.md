# RasterConverter

Converts a raster to another data type: byte, short, integer, float or double. The smaller types save space, as the elevation in meters stored as short integers in a quarter of the size of the double raster, or the classes of a map stored as bytes. The novalues of the input become the novalue of the new type, and the values are rounded for the integer types.

Without a type the module only reads the raster and writes it again, which converts its format, as an ASCII grid to a GeoTIFF; the optional bounds, resolution or rows and columns read only a part of the raster or resample it.

## Details

```{include} ../generated/RasterConverter_details.md
```

## Parameters

```{include} ../generated/RasterConverter.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It stores the elevation as short integers and the elevation belts of [RangeLookup](rangelookup.md) as bytes:

```{literalinclude} ../examples/hm/rasterconverter.groovy
:language: groovy
```
