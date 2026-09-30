# WMS to GeoTIFF

WMS to GeoTIFF (the Web Maps Downloader) saves a map from a WMS service as a georeferenced GeoTIFF, covering the area of a vector file. It is a quick way to get a background map, like OpenStreetMap or an orthophoto, for a study area, to use offline or as input of other tools.

## Launching

Start the application with the `hm-wms2geotiff` launcher (see [Launching an application](../installation.md#launching-an-application)).

## Connecting to a service

Paste the GetCapabilities address of the WMS service in the field at the top left and press **load**, for example the OpenStreetMap WMS of [terrestris](https://ows.terrestris.de/):

```text
https://ows.terrestris.de/osm/service?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetCapabilities
```

If the address doesn't contain the usual `service=wms` and `request=getcapabilities` parameters, the application asks whether to proceed anyway.

The **Name** and **Title** of the service appear below the address, and the other fields are filled with what the service offers:

Layers & Filter
: the layers of the service. Type in the field on the right to show only the layers whose name contains the text.

Styles
: the styles available for the selected layer; leave it empty for the default style.

Formats
: the image formats of the service, like `image/png` or `image/jpeg`.

CRS
: the projections in which the layer declares its bounds. The **Bounds** of the layer in that projection are shown below.

## Previewing

**Load Preview** requests a small image of 256 × 256 pixels, of the whole layer or, once a bounds file is loaded, of its area. The address of the request is shown under **Call done** and copied to the clipboard, which is handy to check a request in a browser.

:::{figure} ../images/apps/wms2geotiff/world_preview.png
:alt: The preview of the whole layer
:width: 100%
:align: center

The terrestris OSM-WMS layer, with the preview of the whole world in EPSG:4326.
:::

## Downloading

1. Press the **...** button next to **Bounds from file** and choose a vector file (shapefile or GeoPackage) covering the area to download. Its bounds are shown under **File/Export Bounds**, transformed into the selected CRS. **reset** clears them.
2. Insert the **Width (Pixels)** of the image to create. The **Height** is calculated from the proportions of the area.
3. Choose the **Output geotiff file**, typing its path or with the **...** button.
4. Press **Convert WMS to Geotiff**.

:::{figure} ../images/apps/wms2geotiff/main.png
:alt: The application ready to download
:width: 100%
:align: center

Ready to download the OpenStreetMap map of the area of the Flanginec DTM, in EPSG:3857, 1000 pixels wide.
:::

The image is saved as a GeoTIFF in the selected CRS, together with a world file (`.tfw`) and a projection file (`.prj`) with the same name. The resolution of the result depends on the width chosen: for example, an area 5 km wide downloaded 1000 pixels wide gives cells of about 5 meters.

:::{figure} ../images/apps/wms2geotiff/result.png
:alt: The downloaded GeoTIFF
:width: 60%
:align: center

The downloaded GeoTIFF. The free terrestris service adds its promotional watermarks to the maps.
:::

:::{tip}
Many services limit the size of the images they return, often to a few thousand pixels per side. If the download fails for a large width, try a smaller one; if the service returns an error message, it is shown in a warning.
:::
