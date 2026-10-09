# LasTriangulation2Dsm

LasTriangulation2Dsm creates a surface model from the triangles of the lidar points, without the triangles that join points at very different elevations, as the edge of a roof and the ground under it. It suits the smooth surfaces of the cities, the roofs and the roads, with sharp edges and no slanted walls between them.

## Example

The triangles of all the points of the sample plot, without those with more than 3 m of difference of elevation:

```{literalinclude} ../examples/hm/lastriangulation2dsm.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasTriangulation2Dsm_dsm_triangles.png
:alt: The surface of the triangles: the ground between the trees and the top of their crowns, with holes along the edges of the crowns, where the triangles joined the crowns to the ground, and inside the roughest crowns.

The surface of the triangles: the ground between the trees and the top of their crowns, with holes along the edges of the crowns, where the triangles joined the crowns to the ground, and inside the roughest crowns.
:::

## Reference

```{include} ../generated/LasTriangulation2Dsm.md
```
