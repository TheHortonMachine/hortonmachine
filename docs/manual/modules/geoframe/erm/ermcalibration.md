# ErmCalibration

Calibrates the parameters of the water budget model with Particle Swarm Optimisation: a swarm of `pParticlesNum` candidate parameter sets moves through the parameter space for `pPsoIterations` iterations, each candidate attracted by its own best position and by the best one of the swarm. Each candidate is evaluated by running the model over the calibration period and comparing the simulated discharge with the observed one at the stream gauges, with the Kling-Gupta efficiency (KGE): 1 is a perfect agreement.

The first `pSpinUpDays` of the period only warm up the storages of the model and are not evaluated. The calibration runs `pParticlesNum` × `pPsoIterations` simulations, in parallel on `pCalibrationThreadCount` threads: on long periods it can take hours.

At the end, the best parameters and their KGE are reported in the log. These are the 18 parameters, in the order they are reported and accepted by [ErmSimulation](ermsimulation.md), with the ranges explored by the calibration:

| # | Parameter | Unit | Range |
|---|---|---|---|
| 1 | adjustment coefficient for the rain measurements | - | 0.8 – 1.5 |
| 2 | adjustment coefficient for the snow measurements | - | 0.8 – 1.5 |
| 3 | melting temperature | °C | -1 – 3 |
| 4 | combined melting factor | mm/°C/day | 0.0001 – 2 |
| 5 | freezing factor | mm/°C/day | 0.0001 – 1 |
| 6 | coefficient for the maximum liquid water in the snowpack | - | 0.001 – 0.5 |
| 7 | canopy outflow coefficient | - | 0.1 – 0.9 |
| 8 | partitioning coefficient of the free throughfall | - | 0.5 – 0.98 |
| 9 | maximum root zone storage | mm | 40 – 250 |
| 10 | maximum percolation rate of the root zone | - | 0.000001 – 3 |
| 11 | exponent of the non-linear reservoir of the root zone | - | 0.8 – 1 |
| 12 | degree of spatial variability of the soil moisture capacity | - | 0.5 – 3 |
| 13 | maximum runoff storage | mm | 5 – 100 |
| 14 | coefficient of the non-linear runoff reservoir | - | 0.000001 – 5 |
| 15 | exponent of the non-linear runoff reservoir | - | 0.9 – 1 |
| 16 | maximum groundwater storage | mm | 100 – 1000 |
| 17 | coefficient of the non-linear groundwater reservoir | - | 0.0000005 – 2 |
| 18 | exponent of the non-linear groundwater reservoir | - | 0.95 – 1 |

```{literalinclude} ../../examples/erm/06_calibration.groovy
:language: groovy
```

```{include} ../../generated/ErmCalibration.md
```
