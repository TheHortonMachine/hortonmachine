# Pitfiller

Real terrain drains to the sea, but in a digital elevation model there are always cells lower than all their neighbours: errors of the data, effects of the resolution, or real closed depressions. From such cells the drainage directions can't be defined, so the depressions are filled first. The depitted elevation model is the input of the drainage directions modules, like [FlowDirections](../geomorphology/flowdirections.md).

```{include} ../generated/Pitfiller.md
```
