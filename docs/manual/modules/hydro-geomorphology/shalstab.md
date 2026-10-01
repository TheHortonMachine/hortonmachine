# Shalstab

SHALSTAB (SHAllow Landslide STABility) maps where the slopes are prone to shallow landslides. It couples two simple models:

- the **infinite slope** stability model, fit for shallow landslides: the soil involved is thin, the sliding surface is almost planar, and the water in the soil flows about parallel to it;
- a **steady-state hydrological** model, which gives the height of the water in the soil from the effective precipitation, the contributing area of the cell and the transmissivity of the soil.

A cell is unstable when the water in the soil rises enough to cancel the friction and the cohesion that hold it:

```{math}
\frac{a}{b} > \frac{T}{q} \sin\theta \, \frac{\rho_s}{\rho_w} \left[ 1 - \frac{\tan\theta}{\tan\phi} + \frac{C \, (1 + \tan^2\theta)}{\rho_s \, g \, z \tan\phi} \right]
```

with a/b the contributing area per unit contour length, T the transmissivity, q the effective precipitation, θ the slope angle, φ the internal friction angle, {math}`\rho_s/\rho_w` the ratio between the soil and the water densities, C the cohesion and z the soil depth.

The cells fall in four classes, plus the rock:

| Class | Value | Meaning |
|---|---|---|
| unconditionally unstable | 1 | unstable even with a dry soil |
| unconditionally stable | 2 | stable even with a saturated soil |
| stable | 3 | stable with the given effective precipitation |
| unstable | 4 | unstable with the given effective precipitation |
| rock | 8888 | soil depth up to 0.01 m, or slope above the rock threshold |

The second map gives, in classes, the critical effective precipitation that makes each cell unstable: the lower it is, the less rain is needed to trigger a landslide. It shows the stability for any rainfall, not only the given one.

The slope comes from the [Gradient](../geomorphology/gradient.md) or [Slope](../geomorphology/slope.md) modules, as the tangent of the slope angle, and the contributing area per unit contour length from the [Ab](../geomorphology/ab.md) module. The properties of the soil can be maps, with the soil depth for example from a geological survey, or constants, as in the Spatial Toolbox.

```{include} ../generated/Shalstab.md
```
