# Web Services Browser

The Web Services Browser connects to OGC web services of three kinds: map services (WMS), coverage services (WCS) and feature services (WFS). It lets you browse their layers and metadata, request data for a region and look at the result on a map. Maps, coverages and features can then be saved to file, for example to get a background map, an elevation model or a vector layer of a study area.

Every request is written to the log with its full address, which makes it also a tool to check how well a service works with the HortonMachine, and why it doesn't, before using it from code or from the processing modules.

## Launching

Start the application with the `hm-webservices` launcher (see [Launching an application](../installation.md#launching-an-application)).

The launcher optionally takes the type of service (`WMS`, `WCS` or `WFS`) and the address of the service to connect to at startup:

```sh
./hm-webservices.sh WCS "https://geoservices9.civis.bz.it/geoserver/wcs?SERVICE=WCS&REQUEST=GetCapabilities"
```

## The main window

:::{figure} ../images/apps/webservices/wms_map_preview.png
:alt: The Web Services Browser main window
:width: 100%
:align: center

The main window, connected to the WMS of the Autonomous Province of Bolzano, with a hillshade of the area of Egna/Neumarkt drawn on the map.
:::

The window is organized in five areas:

- the **connection bar** at the top, with the type of service, its address and the protocol version;
- the **layers list** on the left;
- the **map** in the center, showing the extent of the selected layer, the region of the requests and the results;
- the **request panel** on the right, with the region, the options of the requests and the buttons to run them;
- the **tabs** at the bottom, with the details of the layer, the result of the last request, the service information and the log.

The region, the map and the log are shared by the three types of service, while the options, the requests and the **Result** tab change with the selected type.

The status bar at the bottom shows the last message, together with a progress bar for the running requests.

## Connecting to a service

1. Choose the type of service in the **Service** drop-down list: **WMS**, **WCS** or **WFS**.
2. Type or paste the address of the service in the **URL** field. It can be the full GetCapabilities address or just the address of the service: the missing parameters are added by the application.
3. Leave the **Version** on `auto` to let the application and the service agree on the protocol version, or choose one to force it. Forcing a version is useful when a service behaves badly in its latest one. WCS can be forced to `2.0.1`, `1.1.1`, `1.1.0` or `1.0.0`, WFS to `2.0.0`, `1.1.0` or `1.0.0`; for WMS the version is always negotiated.
4. Press **Connect** (or Enter).

The drop-down list of the **URL** offers a few public services of each type, and the services you connect to are remembered and added to the list for the next sessions.

Once connected, the layers of the service are listed on the left, with their name in bold and their title below. Type in the **Filter** field to show only the layers whose name or title contains the text. Selecting a layer shows its extent on the map and its details in the **Layer** tab.

The **Service** tab shows the information of the service, like its title, version, the formats and the operations it supports.

:::{figure} ../images/apps/webservices/wms_service.png
:alt: The Service tab
:width: 100%
:align: center

The Service tab of the WMS of the Autonomous Province of Bolzano.
:::

Each type of service keeps its own connection: switching to another type and back shows the last service of that type again, with its layers and selection.

## The region

The **Region** section at the top of the request panel defines the area of the requests, in WGS84 longitude and latitude. It is used only if **Restrict the requests to the region** is checked; otherwise the requests cover the whole layer.

The region can be:

- typed in the **West**, **East**, **South** and **North** fields;
- drawn on the map, pressing **Draw region** and dragging a rectangle (or dragging with Shift pressed at any time);
- taken from the visible map area with **Map view**;
- taken from the extent of the selected layer with **Layer**;
- taken from the bounds of a vector or raster file with **File...**, transformed to WGS84.

**Clear** removes it. The region is drawn on the map in orange; **Zoom to region** and **Zoom to layer** in the map toolbar move the map to it or to the extent of the layer.

:::{tip}
The region is kept when you select another layer or switch to another type of service. If a service answers that the request is empty or outside the data, check that the region still overlaps the layer, for example pressing **Layer**.
:::

Images received from the services are drawn on the map over the base map. The **Overlay** slider in the map toolbar sets their opacity and **Clear** removes them, together with the drawn features.

## Map services (WMS)

The **GetMap parameters** define the image requested:

Style
: the style of the layer, empty for its default style.

Format
: the image format, like `image/png` or `image/jpeg`.

CRS
: the projections supported by the layer. `EPSG:3857` (the web mercator of the map) and `EPSG:4326` are listed first when available.

Transparent background
: asks the service for a transparent background, where the format allows it.

The **Image size** section sets the **Width** in pixels of the images requested by **Get image** and **Export GeoTIFF...**. The **Height** follows the proportions of the region in the chosen CRS.

:::{figure} ../images/apps/webservices/wms_request_panel.png
:alt: The WMS request panel
:width: 40%
:align: center

The options of the WMS requests.
:::

Three requests are available:

Map view preview
: requests the visible map area at the size of the map and draws it on the map. The image is requested in `EPSG:3857` or, if the layer doesn't support it, in `EPSG:4326`, which is warped to fit the map.

Get image
: requests the region (or the whole layer) in the chosen CRS and size and shows it in the **Result** tab. If the CRS is `EPSG:3857` or `EPSG:4326` it is also drawn on the map, otherwise only its area is shown.

Export GeoTIFF...
: as **Get image**, then saves the image as a GeoTIFF in the chosen CRS, together with a world file (`.tfw`) and a projection file (`.prj`) with the same name.

:::{figure} ../images/apps/webservices/wms_result.png
:alt: The Result tab of a WMS request
:width: 100%
:align: center

The image of the region received with Get image, 1024 pixels wide in EPSG:3857.
:::

The **Request** field at the top of the **Result** tab shows the address of the last request; **Copy** copies it to the clipboard, handy to check the request in a browser. If the service answers with an error instead of an image, its message is shown in the **Result** tab and in a warning.

:::{tip}
Many services limit the size of the images they return, often to a few thousand pixels per side. If a large image fails, try a smaller width.
:::

## Coverage services (WCS)

Coverage services return the data of a raster, like the elevations of a terrain model, instead of a picture of it.

Selecting a coverage sends a DescribeCoverage request to the service, which tells the coverage bounds and native CRS, the names of its axes, its formats and the CRS it can be delivered in. The details are shown in the **Layer** tab, together with the full description and the summary from the capabilities document. **Describe** repeats the request.

:::{figure} ../images/apps/webservices/wcs_describe.png
:alt: The Layer tab of a coverage
:width: 100%
:align: center

The description of the 2.5 m terrain model of the Autonomous Province of Bolzano.
:::

The **GetCoverage parameters** define the request:

Format
: the format of the result. Only GeoTIFF results can be previewed.

Region CRS
: the CRS in which the region is sent to the service: the native CRS of the coverage or WGS84 (`EPSG:4326`). If the native CRS is unknown to the HortonMachine, WGS84 is used and the log says so.

Output CRS
: asks the service to reproject the coverage (WCS 2.0.1 only). Leave it empty to get the coverage in its native CRS.

Use axis urls (2.0.1)
: services name the axes used for scaling in different ways: some, like MapServer, want their short labels, others, like GeoServer, their full addresses. Check it if the service answers with a `ScaleAxisUndefined` error.

The **Size** section sets the size of the downloaded coverage: **As served** keeps the native resolution, **Columns** asks for a number of columns (the **Rows** follow the proportions of the region) and **Scale factor** scales the native resolution (0.5 halves the columns and rows, WCS 2.0.1 only). Scaling works only if the service supports it, otherwise the native resolution is returned.

:::{figure} ../images/apps/webservices/wcs_request_panel.png
:alt: The WCS request panel
:width: 40%
:align: center

The options of the WCS requests.
:::

Preview
: requests a GeoTIFF of the region 800 columns wide into a temporary file, reads it and shows it in the **Result** tab and on the map, reprojected to WGS84. Single band data are colored from their minimum (blue) to their maximum (red), with the novalues transparent; images with a palette or RGB bands keep their colors.

Download...
: requests the coverage with the chosen format and size and saves it to a file. GeoTIFF results are then read back and shown as for the preview.

:::{figure} ../images/apps/webservices/wcs_preview.png
:alt: The preview of a coverage
:width: 100%
:align: center

The preview of the terrain model of the region, on the map and in the Result tab, with the information of the raster received.
:::

The **Result** tab shows the raster received (columns and rows, bands, CRS, bounds, resolution, novalue and range of values) and the address of the request.

:::{warning}
With **As served** and no region, the whole coverage is requested at its native resolution, which for a regional terrain model can mean gigabytes of data. Use a region, or a number of columns, for a first look.
:::

## Feature services (WFS)

Selecting a feature type reads its schema and bounds. The **Layer** tab shows them, together with the CRS of the features and the order of its axes, and the attributes with their types.

:::{figure} ../images/apps/webservices/wfs_schema.png
:alt: The Layer tab of a feature type
:width: 100%
:align: center

The schema of the contour lines of the Autonomous Province of Bolzano.
:::

The options of the WFS requests are:

ArcGIS server compatibility
: adapts the communication to ArcGIS WFS servers. It is applied when connecting, so press **Connect** again after changing it.

Max features
: the maximum number of features requested, 0 for no limit (the service may still apply its own).

Swap the coordinates
: a last resort for services returning latitude and longitude where longitude and latitude are expected, and vice versa: the coordinates of the region and of the features are swapped.

:::{figure} ../images/apps/webservices/wfs_request_panel.png
:alt: The WFS request panel
:width: 40%
:align: center

The options of the WFS requests.
:::

**Load features** requests the features of the region (or all the features of the type, up to the maximum). The region is transformed to the CRS of the feature type and sent as bounding box filter. The features are drawn on the map and listed with their attributes in the **Result** tab: selecting a row highlights its feature on the map and clicking a feature on the map selects its row.

:::{figure} ../images/apps/webservices/wfs_features.png
:alt: Features loaded from a WFS
:width: 100%
:align: center

The contour lines of the region around Egna/Neumarkt, on the map and in the Result tab.
:::

**Export...** saves the loaded features as a GeoPackage (`.gpkg`, with a table named after the feature type) or as a shapefile (`.shp`), depending on the extension of the file chosen. Without extension a GeoPackage is created.

:::{note}
A few strict services require the name of the geometry in the bounding box filter to carry the namespace of the feature type, which the underlying GeoTools library doesn't write. These services reject the requests restricted to a region, while loading the features without region works.
:::

## The log

The **Log** tab lists every operation with its time: the connections, the address of each request, the size and time of the answers and the errors with their details. **Copy** copies the whole log to the clipboard, **Clear** empties it.

:::{figure} ../images/apps/webservices/log.png
:alt: The log tab
:width: 100%
:align: center

The log of a session on the WMS, WCS and WFS of the Autonomous Province of Bolzano.
:::

:::{tip}
When the internet is reached through a proxy, set it in the [Settings](settings.md) application: it is applied at startup.
:::
