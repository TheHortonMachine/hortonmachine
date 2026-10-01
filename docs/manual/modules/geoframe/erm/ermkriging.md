# ErmKriging

Interpolates the temperature and the precipitation of the stations to the centroid of each sub-basin, time step by time step, with ordinary kriging. At each time step the experimental variogram of the station values is computed and a theoretical variogram is fitted to it.

```{literalinclude} ../../examples/erm/03_kriging.groovy
:language: groovy
```

```{include} ../../generated/ErmKriging.md
```
