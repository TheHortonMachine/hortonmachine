# Settings

The Settings application edits the preferences shared by all the HortonMachine applications and modules: network proxy, character encoding, interface scaling, SSH keys, native libraries and credentials for data services.

## Launching

Start the application with the `hm-settings` launcher (see [Launching an application](../installation.md#launching-an-application)).

The settings are organized in tabs, and are saved when the window is closed. They apply to the applications started afterwards.

:::{figure} ../images/apps/settings/proxy.png
:alt: The Settings application
:width: 80%
:align: center

The Settings application, with the proxy tab.
:::

## Proxy

When the internet is reached through a proxy, check **enable proxy** and insert its **host** and **port**, and its **user** and **password** if it requires authentication. The proxy is applied at startup by the [WMS to GeoTIFF](wms2geotiff.md), [Database Viewer](dbviewer.md), [Geoscript Console](geoscript.md), [Style Editor](sld.md), [Form Builder](gforms.md) and [Quick Folder](quickfolder.md) applications.

## Internationalization

Charset
: the character encoding used to read and write some files, like the attributes of shapefiles and the forms. Leave it empty to use the default one; `UTF-8` is a safe choice for new data.

Component Orientation
: the direction of the user interface, left to right or right to left.

## Preferences

:::{figure} ../images/apps/settings/preferences.png
:alt: The preferences tab
:width: 80%
:align: center

The folder of the preferences database and the interface scaling.
:::

Folder to store the preferences database
: the folder where the preferences of the applications are stored, like the recent files, the saved connections of the Database Viewer or the style templates.

UI scaling to apply
: a factor to enlarge the fonts and the components of the interface, useful on high resolution screens: `1.2` enlarges them by 20%.

## SSH

The path to the private key, and its passphrase, used by the tools that connect to remote servers through SSH, for example to reach a database through an SSH tunnel.

## Native libs

The folder containing the SpatiaLite loadable modules (`mod_spatialite`), needed to work with SpatiaLite databases when they are not installed in a standard location of the system.

## Copernicus

The API token and the download folder used by the modules that download data from the Copernicus climate services. These are not needed for the Copernicus DEM used by the 3D terrain of the [Simple NWW Viewer](nwwviewer.md), which is open data.
