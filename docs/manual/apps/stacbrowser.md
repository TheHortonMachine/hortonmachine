# STAC Browser

The STAC Browser connects to a [STAC](https://stacspec.org/) catalog, lets you browse its collections, search the items of a collection by region, time and attributes, and then download the assets of the items, either as whole files or as GeoTIFFs clipped to the search region.

Along the way it shows which assets the HortonMachine is able to read, which makes it also a tool to check how well a catalog is supported before using it from code or from the processing modules.

## Launching

Start the application with the `hm-stacbrowser` launcher (see [Launching an application](../installation.md#launching-an-application)).

The launcher optionally takes the address of the catalog to connect to at startup, followed, for catalogs on Amazon S3, by the AWS profile and region:

```sh
./hm-stacbrowser.sh https://earth-search.aws.element84.com/v1
./hm-stacbrowser.sh s3://my-bucket/catalog.json my-profile us-west-2
```

## The main window

:::{figure} ../images/apps/stacbrowser/collection.png
:alt: The STAC Browser main window
:width: 100%
:align: center

The main window, connected to Earth Search with the Sentinel-2 Level-2A collection selected.
:::

The window is organized in five areas:

- the **connection bar** at the top, with the catalog address;
- the **collections list** on the left;
- the **map** in the center, showing the collection extent, the search region and the footprints of the items found;
- the **query panel** on the right, with the search filters;
- the **tabs** at the bottom, with the details of the collection, the items found, the download preview, the service information and the log.

The status bar at the bottom shows the last message, together with a progress bar and a **Cancel** button for long running operations.

## Connecting to a catalog

Type or paste the address of a catalog in the **STAC catalog** field and press **Connect** (or Enter). The drop-down list offers a few well-known public catalogs:

- `https://earth-search.aws.element84.com/v1` (Element 84 Earth Search on AWS)
- `https://planetarycomputer.microsoft.com/api/stac/v1` (Microsoft Planetary Computer)
- `https://stac.dataspace.copernicus.eu/v1` (Copernicus Data Space Ecosystem)
- `https://stac.terrascope.be` (Terrascope)

The catalogs you connect to are remembered and added to the list for the next sessions.

Both STAC API services and static catalogs (a `catalog.json` file with its linked collections and items) are supported. When a catalog doesn't offer item search, the application warns in the log and collects the items by following the catalog links, applying the filters locally. This works, but can be slow on large catalogs.

:::{figure} ../images/apps/stacbrowser/connected.png
:alt: The STAC Browser after connecting
:width: 100%
:align: center

After connecting, the collections of the catalog are listed on the left.
:::

### Catalogs on Amazon S3

Catalogs stored in a private S3 bucket can be opened with their `s3://bucket/path/catalog.json` address, or with the https address of the bucket. For these addresses two more fields appear in the connection bar:

AWS profile
: the profile of your `~/.aws/credentials` file to use. Leave it empty to use the default profile.

Region
: the AWS region of the bucket, for example `us-west-2`. It is needed when neither the address nor the profile configuration contain it.

The credentials are used only for the bucket of the catalog. The profile and region are remembered for the next sessions.

### Microsoft Planetary Computer

The assets of the Planetary Computer are stored in Azure blob storage and need a signed address to be read. The application signs them automatically, both for the downloads and for the thumbnails; no account is needed.

## Browsing the collections

The list on the left shows the collections of the catalog, each with its id in bold and its title below. Type in the **Filter** field above the list to show only the collections whose id or title contain the text. The number of collections shown is displayed below the list.

Selecting a collection:

- shows its description in the **Collection** tab: title, id, license, spatial and temporal extent, keywords, providers and the item assets it declares, each with the HortonMachine handler able to read it (in green) or a note that it is not supported;
- shows its full metadata as a tree on the right side of the same tab. **Copy as JSON** copies the whole metadata to the clipboard;
- draws its spatial extent on the map (the dashed rectangle) and zooms to it;
- sets it as the target of the search, as shown by the title of the **Search in** section of the query panel.

## The map

The map shows OpenStreetMap tiles, with the collection extent, the search region and the footprints of the items found drawn on top of them.

- drag to pan, use the mouse wheel to zoom;
- the coordinates under the mouse (longitude, latitude and zoom level) are shown in the map toolbar;
- click on a footprint to select the corresponding item in the **Items** tab.

The toolbar above the map offers:

Draw bbox
: once pressed, drag on the map to draw the search region. The same can be done at any time by dragging with the Shift key pressed.

Zoom to collection
: zooms to the extent of the selected collection.

Zoom to results
: zooms to the footprints of the items found.

Zoom to bbox
: zooms to the search region.

## Searching items

:::{figure} ../images/apps/stacbrowser/query_panel.png
:alt: The query panel
:width: 40%
:align: center

The query panel with a region, a time range and an attribute filter.
:::

The query panel collects the filters of the search. Each filter is used only when its check box is ticked; the check boxes are ticked automatically when you fill in the fields.

Region (WGS84 lon/lat)
: the bounding box of the search, as West, East, South and North in decimal degrees. It can be typed in, drawn on the map, or set with the buttons below the fields: **Map view** uses the current map view, **Collection** the extent of the collection, **Clear** removes it.

Time range (UTC)
: the start and end of the search period, as `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`. A date without time in the **To** field includes the whole day. **Collection extent** uses the temporal extent of the collection, **Last 30 days** the last 30 days.

Filter (CQL)
: an attribute filter in CQL, for example `"eo:cloud_cover" < 20` to keep only the images with less than 20% cloud cover. Property names containing a colon must be enclosed in double quotes.

Max items
: the search stops once this many items are fetched; `0` fetches all the pages of results. Keep it low on large collections without a region or time filter.

Press **Search items** to run the search. The number of items fetched, of the total matched by the service, and the time taken are shown below the button.

:::{warning}
The attribute filter is applied by the service, and only services supporting the STAC API *Filter* extension do so. Check the **Service** tab: if **Filter** shows *no*, the filter is silently ignored and the results are not filtered by attributes. Earth Search, for example, doesn't support it. On static catalogs the filter is applied locally, so it always works.
:::

## The items

:::{figure} ../images/apps/stacbrowser/search_results.png
:alt: The search results
:width: 100%
:align: center

The items found by a search over South Tyrol in July and August 2025. The orange rectangle is the search region, the green shapes the item footprints.
:::

The **Items** tab lists the items found, with:

| Column | Content |
|---|---|
| Id | the item id |
| Datetime (UTC) | the acquisition time |
| Version, Status | the version and its status, for catalogs using the STAC *Version* extension |
| EPSG | the projection of the item data |
| Cloud % | the cloud cover, for optical imagery |
| Platform | the platform, for example the satellite |
| Assets | the number of assets of the item |
| HM readable | how many of those assets the HortonMachine is able to read |

The columns can be sorted by clicking on their header. Selecting an item highlights its footprint on the map in red; a double-click zooms the map to it.

The panel on the right shows the selected item in three tabs:

Preview
: the thumbnail of the item, when the catalog provides one.

Assets
: the assets of the item, with their key, title, media type, the HortonMachine handler able to read them (or *✗ unsupported*), projection and address.

Metadata
: the full metadata of the item, as a tree.

:::{figure} ../images/apps/stacbrowser/item_preview.png
:alt: An item selected, with its thumbnail
:width: 100%
:align: center

A cloud-free item selected: its footprint is highlighted on the map and its thumbnail shown in the Preview tab.
:::

:::{figure} ../images/apps/stacbrowser/item_assets.png
:alt: The assets of an item
:width: 100%
:align: center

The assets of the selected item. The `cloud`, `granule_metadata` and `product_metadata` assets have no HortonMachine handler.
:::

## Downloading

The **Download preview** tab shows what a download of the search results would contain.

:::{figure} ../images/apps/stacbrowser/download_preview.png
:alt: The download preview
:width: 100%
:align: center

The download preview: asset keys on top, the single assets that would be downloaded below.
:::

The upper table lists the asset keys found in the results, for example the single bands of a satellite image. For each key it shows the media types, in how many items it appears, how many of those the HortonMachine can read, and with which handler. Tick the **Use** column to include a key in the download. **Select readable keys** ticks all the keys that can be read, **Select none** unticks them all.

The lower table lists every single asset of the chosen keys, one row per item. The line above it summarizes how many assets there are and how many can be read.

The buttons between the two tables:

Check access
: checks with a request to the server that the selected assets (or the first 50 if none are selected) can be reached, and reads their size. The results appear in the **Access** and **Size** columns.

Copy URLs
: copies the addresses of all the assets to the clipboard, one per line.

Export CSV...
: saves the list of assets, with item id, datetime, asset key, type, handler, EPSG, access, size and address, as a semicolon separated CSV file.

To download, select the rows of the assets in the lower table (Shift or Ctrl click to select more) and right-click on them.

:::{figure} ../images/apps/stacbrowser/download_context_menu.png
:alt: The download context menu
:width: 100%
:align: center

The context menu on four selected `blue` band assets, after checking their access.
:::

Download whole files...
: downloads the complete files, independently of the search region. The application first checks the size of the files and asks for confirmation, showing the total size and the list of files, then asks for the destination folder.

Export rasters clipped to bbox (GeoTIFF)...
: reads only the part of each raster covering the search region and saves it as a GeoTIFF. Only available when a valid search region is set.

Copy URL
: copies the addresses of the selected assets to the clipboard.

The downloaded files are named after the item id and the original file name, since many catalogs use the same file names (like `B02.tif`) in every item. Their status and size are updated in the table as the downloads proceed; the whole operation can be stopped with **Cancel** in the status bar.

### Clipped exports

The clipped export first asks for the resolution of the output rasters, in the units of each asset's projection (meters for UTM). It proposes the native resolution of the data when the catalog declares it.

:::{figure} ../images/apps/stacbrowser/export_resolution.png
:alt: The resolution request of the clipped export
:width: 60%
:align: center

The resolution of the clipped rasters, proposed from the native 10 m resolution of the Sentinel-2 blue band.
:::

For each raster the search region is transformed into the projection of the asset and intersected with the item footprint. The application then shows the size in columns and rows of each output before reading anything. Rasters outside the region, or larger than 100 million cells, are skipped. Only the parts of the files covering the region are read, which is fast for Cloud Optimized GeoTIFFs.

The output files are named `<item id>_<asset key>_clip.tif`.

### Style files

Some catalogs provide style files (QGIS `.qml` or OGC `.sld`) as assets next to their rasters. When the rasters to download or export have styles, the confirmation dialog lets you choose which styles to save with them.

The first chosen style of each format is saved with the same name as the raster, so that QGIS applies a `.qml` style automatically when the raster is loaded. The other styles of the same format get a suffix taken from their file name.

## Service information and log

The **Service** tab shows the address of the catalog, the number of collections, the time taken to read the landing page and the collections, the authentication used, and which parts of the STAC API the service declares to support (its *conformance*). This is the place to check, for example, whether the attribute filter is supported.

:::{figure} ../images/apps/stacbrowser/service.png
:alt: The Service tab
:width: 100%
:align: center

The Service tab for Earth Search: item search is supported, the Filter extension is not.
:::

The **Log** tab records every operation with its time: connections, searches with their filters, downloads, and the full error messages when something fails. **Clear** empties it. When a download or an export fails, the reason is shown in the **Access** column and in detail in the log.

:::{figure} ../images/apps/stacbrowser/log.png
:alt: The Log tab
:width: 100%
:align: center

The log of a connection and a search.
:::
