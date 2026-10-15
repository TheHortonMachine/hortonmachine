# VectorIntersector

Intersects the features of a vector, points, lines or polygons, with the polygons of another vector: each feature is cut into a piece for each polygon it crosses, with the attributes of both, as the reaches of a river with the municipality they flow through. To only cut the features to the area of the polygons, use [VectorClipper](vectorclipper.md).

## Details

```{include} ../generated/VectorIntersector_details.md
```

## Parameters

```{include} ../generated/VectorIntersector.md
```

## Example

The example intersects the network of [NetworkAttributesBuilder](../network/networkattributesbuilder.md) with the cells of [RasterCatToFeatureAttribute](rastercattofeatureattribute.md), so that each piece of the network gets the elevation of its cell:

```{literalinclude} ../examples/hm/vectorintersector.groovy
:language: groovy
```

The 99 reaches become 126 pieces, colored by the mean elevation of their cell, with the cells in blue:

```{image} ../../images/modules/VectorIntersector_net_cells_elev_avg.png
:alt: The network cut by the cells of the grid, colored by their mean elevation
```

The reach 13 of the Pfafstetter numbering, on the main stream (Hack order 1), crosses three cells: its pieces keep the fields of the network and get those of their cell:

| pfaf | hack | strahler | id | elev_avg | length (m) |
|---|---|---|---|---|---|
| 13 | 1 | 3 | 40 | 1889.2 | 162 |
| 13 | 1 | 3 | 32 | 1629.2 | 387 |
| 13 | 1 | 3 | 33 | 1759.2 | 12 |
