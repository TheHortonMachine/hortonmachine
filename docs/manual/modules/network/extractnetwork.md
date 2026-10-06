# ExtractNetwork

Defines the river network as the cells draining a large enough area. The threshold sets how far upstream the channels begin: lower values give a denser network. It is best chosen comparing the result with the channels visible in maps or imagery.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/extractnetwork.groovy
:language: groovy
```

:::{figure} ../../images/modules/ExtractNetwork_net.png
:alt: The network of the sample area, extracted with a threshold of 100 cells: 1 ha, with cells of 10 m.

The network of the sample area, extracted with a threshold of 100 cells: 1 ha, with cells of 10 m.
:::

## Reference

```{include} ../generated/ExtractNetwork.md
```
