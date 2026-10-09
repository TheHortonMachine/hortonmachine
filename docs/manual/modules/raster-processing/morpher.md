# Morpher

Applies the operations of mathematical morphology to a raster, where the valid cells are the objects and the novalues the background:

- **dilate** grows the objects by one ring of cells, and **erode** shrinks them by one ring, removing the cells that touch the background;
- **open**, an erosion followed by a dilation, removes the small objects and the thin parts; **close**, a dilation followed by an erosion, fills the small holes and gaps between them;
- **skeletonize** thins the objects down to lines one cell wide, in four variants of the thinning, and **prune** removes the short branches of the lines;
- **lineendings** and **linejunctions** find the cells where the lines end and where they meet.

The first four keep the values of the objects, so they also work on rasters of values, as an elevation model; the others work on the shapes only, and give 1 on the result. The size and shape of the dilation and erosion is set by the kernel, by default the 3x3 square.

On the rasters of the HortonMachine, the morphology mostly cleans the masks made with a threshold, as with [Mapcalc](mapcalc.md) or [RangeLookup](rangelookup.md), from the small spots and holes that the noise of the data leaves. The line operations need lines that touch only where they really meet: on the D8 networks of [ExtractNetwork](../network/extractnetwork.md), the channels that run side by side touch on the corners, and give false junctions; there [NetNumbering](../network/netnumbering.md) finds the confluences properly.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. [Mapcalc](mapcalc.md) makes the mask of the slopes steeper than 45 degrees, from [Slope](../geomorphology/slope.md), and the opening cleans it:

```{literalinclude} ../examples/hm/morpher.groovy
:language: groovy
```

:::{figure} ../../images/modules/Morpher_steep.png
:alt: The mask of the slopes steeper than 45 degrees: the rock walls, and many isolated cells and thin strips on the edges of the terraces and of the channels.

The mask of the slopes steeper than 45 degrees: the rock walls, and many isolated cells and thin strips on the edges of the terraces and of the channels.
:::

:::{figure} ../../images/modules/Morpher_steep_open.png
:alt: The same mask after the opening: the isolated cells and the thin strips are gone, the compact steep areas, as the rock walls in the north east, remain.

The same mask after the opening: the isolated cells and the thin strips are gone, the compact steep areas, as the rock walls in the north east, remain.
:::

## Reference

```{include} ../generated/Morpher.md
```
