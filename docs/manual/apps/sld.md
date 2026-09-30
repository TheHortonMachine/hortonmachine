# Style Editor

The Style Editor creates and edits the styles of vector and raster data, in the OGC SLD format used by most GIS software. It shows the style as a tree of groups, rules and symbolizers, with a live map preview of the data.

The editor works on shapefiles, GeoPackage tables, GeoTIFF and ESRI ASCII grid rasters. Styles of files are saved as `.sld` files next to the data; styles of GeoPackage tables are saved inside the GeoPackage.

## Launching

The Style Editor can be started in two ways.

**Standalone**, with the `hm-sld` launcher (see [Launching an application](../installation.md#launching-an-application)). The launcher optionally takes the path of the file to style:

```sh
./hm-sld.sh /data/flanginec/dtm_flanginec.asc
```

For a GeoPackage, the editor asks which of its tables to style. Without arguments, open a file with the **...** button at the top left.

**From the [Database Viewer](dbviewer.md)**: right-click on a spatial table of a GeoPackage and choose **Open in SLD editor**. The editor opens on that table, ready to edit and save its style inside the database.

:::{figure} ../images/apps/sld/dbviewer_menu.png
:alt: Opening the Style Editor from the Database Viewer
:width: 50%
:align: center

Open in SLD editor, from the menu of a table in the Database Viewer.
:::

## The main window

:::{figure} ../images/apps/sld/polygon_symbolizer.png
:alt: The Style Editor on a vector table
:width: 100%
:align: center

The Style Editor on the countries table of a GeoPackage, with the polygon symbolizer selected.
:::

On the left:

- at the top, the path of the data being styled, with the **...** button to open another file and the save button;
- the **Style Tree**, with the style and, below it, the **Datastore information**: projection, number of features or raster size, bounds and, for vectors, the attributes with their type;
- the **Templates** buttons (see [Style templates](#style-templates));
- the **Map Preview**, with tools to zoom, pan, get information on the features and reset the view.

On the right, the panel to edit the element selected in the Style Tree.

If the data already has a style, from an `.sld` file with the same name or stored in the GeoPackage, the editor loads it; otherwise it starts from a default style.

## Raster styles

Rasters are styled with a color table, stretched between the minimum and maximum value of the raster. Select the **Raster Symbolizer** in the Style Tree to edit it:

Opacity
: the opacity of the raster, from 0 (transparent) to 100.

Select Colortable
: one of the predefined color tables, like `elev` for elevation models, `flow` for drainage directions, `tca` and `logarithmic` for contributing areas, `net` for networks, `slope`, `aspect`, `rainbow` or `greyscale`. Press **Apply** to use it.

Define custom
: the rows of the selected color table, one per line, as a value (optional) followed by its red, green and blue components. Edit them and press the **Apply** below to use them as a custom color table, which is asked a name for.

Add shaded relief
: adds a hillshade effect, with the given **relief factor**, to give a three-dimensional feeling to elevation models.

:::{figure} ../images/apps/sld/dtm_relief.png
:alt: A DTM styled with the elev color table and shaded relief
:width: 100%
:align: center

A DTM styled with the `elev` color table and a shaded relief with factor 2.
:::

Color tables with values, like `flow`, assign each color to an exact value, which suits rasters of categories like the drainage directions:

:::{figure} ../images/apps/sld/flow.png
:alt: A drainage directions map styled with the flow color table
:width: 100%
:align: center

A map of drainage directions, styled with the `flow` color table.
:::

## Vector styles

A vector style is organized in three levels, shown in the Style Tree:

- **groups** (feature type styles), drawn one on top of the other;
- **rules** inside each group, each with an optional filter selecting the features it applies to, and an optional scale range;
- **symbolizers** inside each rule, defining how the features are drawn: as polygons, lines, points or labels.

Select an element of the tree to edit it on the right, and press **Apply** to see the result in the preview.

Polygon symbolizer
: the **Border** (width, opacity, color, dash pattern and offset) and the **Fill** (opacity and color).

Line symbolizer
: the width, opacity, color, dash pattern and dash offset of the lines.

Point symbolizer
: the mark used for the points, chosen among the well-known shapes (circle, square, triangle and more) or taken from an external graphic, with its size, rotation, offset, border and fill.

Text symbolizer
: the labels: the attribute used as **label**, the font and its color, a halo, the anchor point and displacement, and the placement options, like following the lines or wrapping the text.

Rule
: the **name**, the **min scale** and **max scale** of the rule, and its **Filter**, written in CQL (for example `MAPCOLOR7 = 1`).

:::{figure} ../images/apps/sld/rule.png
:alt: The parameters of a rule and its menu
:width: 100%
:align: center

A rule selected, with its parameters and filter on the right and its menu in the tree.
:::

Right-click on the elements of the Style Tree to change its structure:

- on the style: **Add new FeatureTypeStyle**;
- on a group: **Add new Rule**, **Remove all Rules**, **Move up** and **Remove selected FeatureTypeStyle**;
- on a rule: **Add Geometry Symbolizer** (if the rule has none), **Add Text Symbolizer** and **Remove selected Rule**;
- on a symbolizer: **Remove selected Symbolizer**.

### Rules from an attribute

The attributes listed under **Datastore information** have two more actions in their menu:

View field stats
: shows a table with statistics about the values of the attribute.

Create unique rules based on this attribute
: creates one rule for each distinct value of the attribute, each with its filter and a color taken from the rainbow color table.

:::{figure} ../images/apps/sld/attribute_menu.png
:alt: The menu of an attribute
:width: 50%
:align: center

The menu of the `MAPCOLOR7` attribute of the Natural Earth countries.
:::

:::{figure} ../images/apps/sld/unique_rules.png
:alt: Unique rules created from an attribute
:width: 100%
:align: center

The rules created from the `MAPCOLOR7` attribute, which Natural Earth provides to color the countries with 7 colors.
:::

The new rules are added after the existing ones. Remove the rules that are no longer needed, like the original rule applying to all the features, with **Remove selected Rule**.

## Saving

The save button at the top left saves the style:

- for files, as an `.sld` file with the same name as the data, in the same folder: `dtm_flanginec.asc` gets `dtm_flanginec.sld`. Any existing style file with that name is overwritten;
- for GeoPackage tables, inside the GeoPackage, where the other HortonMachine applications and the SMASH mobile app pick it up.

### Style templates

The **Templates** buttons keep a personal collection of styles, to reuse them on other data:

save
: stores the current style as a template, with a name.

load
: applies one of the stored templates to the current data.

from sld
: applies the style of an existing `.sld` file.

delete
: removes a stored template.

The templates are stored in the preferences of the user, so they are available in every session.
