# Ab

The contributing area per unit contour length, a/b, is the area that drains through each metre of contour line crossing a cell: the specific contributing area. It measures how much water reaches a cell better than the total contributing area alone, because the same area spreads on divergent slopes and concentrates in convergent ones. It is used by the steady-state hydrological models, like the one of [Shalstab](../hydro-geomorphology/shalstab.md).

The contour length b is estimated from the planar curvature of the cell, from the [Curvatures](curvatures.md) module: it is longer on divergent, convex cells, and shorter on convergent, concave ones, between 0.1 and 1.9 times the cell size; on planar cells it is the cell size.

```{include} ../generated/Ab.md
```
