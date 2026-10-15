# RiverSectionsExtractor

RiverSectionsExtractor takes the cross sections of a river from an elevation model, along the line of the river. They are the geometry of the river for the 1D hydraulic models: [SaintGeo](saintgeo.md), the model of the HortonMachine, and HEC-RAS, through [HecrasInputBuilder](hecrasinputbuilder.md).

The sections are perpendicular to the line, at a given interval along it, and reach a given distance from it on each side. Their points are taken from the elevation model, one for each cell crossed. The module writes three vectors: the section lines, the points of the sections with their elevation, and the points of the river between the sections. The hydraulic modules need all three.

The line has to be drawn from upstream to downstream, along the bottom of the channel. A network extracted from the elevation model, as the one of [NetworkAttributesBuilder](../network/networkattributesbuilder.md), works, but it follows the cells in steps of 45 degrees, and the sections, perpendicular to its local direction, would turn back and forth. Smooth it first, and simplify it, so that the sections follow the direction of the valley. The sections must not cross each other: in the bends they can reach at most as far as the radius of the bend.

## Details

```{include} ../generated/RiverSectionsExtractor_details.md
```

## Parameters

```{include} ../generated/RiverSectionsExtractor.md
```

## Example

The example takes the main channel of the network of the sample area, from the reaches of Hack order 1 of the example of [NetworkAttributesBuilder](../network/networkattributesbuilder.md), smooths it and extracts a section every 50 m:

```{literalinclude} ../examples/hm/riversectionsextractor.groovy
:language: groovy
```

:::{figure} ../../images/modules/RiverSectionsExtractor_sections.png
:alt: The sections, in red, across the main valley of the sample area, perpendicular to the smoothed channel, in blue.

The sections, in red, across the main valley of the sample area, perpendicular to the smoothed channel, in blue. In a few bends two sections touch at their ends: there they reach farther than the radius of the bend.
:::

The valley is a steep mountain stream: the sections are fine for HEC-RAS, but not for [SaintGeo](saintgeo.md), which needs a lowland river. Its page builds the sections of a synthetic river, with the same steps.
