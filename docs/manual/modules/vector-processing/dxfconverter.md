# DxfConverter

Reads a DXF drawing of a CAD, as a technical survey or a project, into three vectors: its points, its lines and its polygons, with the layer and the other properties of each entity.

## Details

```{include} ../generated/DxfConverter_details.md
```

## Parameters

```{include} ../generated/DxfConverter.md
```

## Example

The example reads `flanginec.dxf`, in `docs/manual/modules/maps/data`: a drawing with the outline of the basin of the sub-basins of [BasinShape](../basin/basinshape.md), as a closed polyline in the layer `basin`, and its river network, as polylines in the layer `network`:

```{literalinclude} ../examples/hm/dxfconverter.groovy
:language: groovy
```

The 99 polylines of the network go to the lines, in red, and the closed outline of the basin to the polygons, in blue. The drawing has no points, so no points vector is written:

```{image} ../../images/modules/DxfConverter_dxf_lines.png
:alt: The river network and the outline of the basin read from the DXF drawing
```
