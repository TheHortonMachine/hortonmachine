# SurfaceInterpolator

Interpolates the values of a vector of points, as elevations or measurements, onto the grid of a raster: each cell is computed from the points within the buffer distance around it, with one of two methods:

- **TPS**, Thin Plate Splines: a smooth surface through the points, good for sparse points;
- **IDW**, Inverse Distance Weighting: the average of the points, weighted by the inverse of their distance, more robust with dense points.

The buffer must contain enough points for each cell: where there are none, the cell stays empty. With dense and irregular points, as those of a lidar survey, TPS can become unstable and give absurd values: IDW is the safer choice.

## Example

The example runs on the sample lidar survey of the manual, `uni_bz_plot777.las` in `docs/manual/modules/maps/data`, a forest plot of 60 x 60 m with its elevation model `dtm_777.tif`:

```{literalinclude} ../examples/hm/surfaceinterpolator.groovy
:language: groovy
```

:::{figure} ../../images/modules/SurfaceInterpolator_dsm_777.png
:alt: The surface of the lidar points of the sample plot, interpolated with IDW on the 0.5 m grid of its elevation model: the top of the trees and the ground between them.

The surface of the lidar points of the sample plot, interpolated with IDW on the 0.5 m grid of its elevation model: the top of the trees and the ground between them.
:::

:::{figure} ../../images/modules/SurfaceInterpolator_dsm_777-dtm_777.png
:alt: The same surface minus the elevation model of the ground: the height of the trees, in meters, with the crowns up to about 20 m.

The same surface minus the elevation model of the ground: the height of the trees, in meters, with the crowns up to about 20 m.
:::

## Reference

```{include} ../generated/SurfaceInterpolator.md
```
