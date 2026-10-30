# Web server

:::{warning}
The web server is **highly experimental**: its pages, options, folder layout and API can still change between versions, and it has not been used in production yet. Try it on test data, and don't expose it to the internet without a reverse proxy with HTTPS in front of it.
:::

The HortonMachine web server runs the processing modules for the users of a browser, without installing anything on their machines. The users log in, upload their data, run the modules through forms like those of the [Spatial Toolbox](apps/spatialtoolbox.md), follow their jobs and download the results.

Programs use the same server through two standard APIs: [OGC API - Processes](https://docs.ogc.org/is/18-062r2/18-062r2.html) to run the modules and a [STAC API](https://github.com/radiantearth/stac-api-spec) to find and read the files, so that the [STAC Browser](apps/stacbrowser.md), QGIS, GDAL or `pystac-client` can open the data and the results.

The pages are plain HTML, without JavaScript, so they work in any browser.

## Starting the server

The server is started with the `hm-server` launcher, `hm-server` on Linux and macOS, `hm-server.bat` on Windows (see [Launching an application](installation.md#launching-an-application)). The first time, create an administrator, then start the server:

```sh
./hm-server --workspace=/data/hm-server-workspace --create-admin=admin
./hm-server --workspace=/data/hm-server-workspace
```

`--create-admin` asks for the password twice and exits without starting the server; where there is no console to type it, as in a container, the password is read from the `HM_SERVER_ADMIN_PASSWORD` environment variable. The server then listens on port 8080:

```text
HortonMachine server 0.11.5 listening on port 8080, workspace /data/hm-server-workspace
```

Open `http://localhost:8080` in a browser and log in. Stop the server with Ctrl+C: the queued and running jobs are killed.

The modules don't run inside the server: each job is a separate process of the [command line](apps/cli.md), so a module that fails or runs out of memory doesn't stop the server. For this reason the server itself needs little memory, 1 GB by default, raised if needed with the `HM_MEM` environment variable, while the memory of the jobs is a setting of its own (see [Settings](#settings)).

## Options

`hm-server --help` lists the options, all given as `--option=value`:

| Option | Default | Meaning |
|---|---|---|
| `--port` | `8080` | the port to listen on |
| `--workspace` | `hm-server-workspace` | the folder of the data, jobs and settings, created if missing |
| `--slots` | `2` | the number of jobs running at the same time; the others wait in the queue |
| `--job-memory` | `2g` | the maximum memory of each job, as `2g`, `512m` or `4096m` |
| `--job-ttl-days` | `7` | the days the finished jobs are kept, `0` to keep them for ever |
| `--max-upload-mb` | `500` | the maximum size of an upload, all its files together |
| `--user-quota-mb` | `5120` | the maximum size of the files of each user, `0` for no limit |
| `--max-suggestions` | `500` | the maximum number of files offered for an input of a module |
| `--signed-url-minutes` | `60` | the minutes the signed links of the API work, at most 10080 (7 days), `0` for none |
| `--cog-outputs` | `true` | convert the GeoTIFF outputs of the jobs to Cloud Optimized GeoTIFFs |
| `--stac-rescan-minutes` | `10` | compare all the files with the catalog every these minutes, `0` for only at the start |
| `--db` | — | a PostgreSQL database, as `postgresql://host:port/database`, instead of SQLite in the workspace |
| `--db-user` | — | the user of the PostgreSQL database |
| `--create-admin` | — | create an administrator with this name and exit |

The options from `--slots` to `--stac-rescan-minutes` can also be changed by the administrators in the [settings page](#settings), while the server runs. The port, the workspace and the database are given only on the command line. The password of the PostgreSQL database is read only from the `HM_SERVER_DB_PASSWORD` environment variable, never written anywhere.

## The workspace

All the server keeps is in the workspace folder:

| Path | Content |
|---|---|
| `data/shared/` | the files shared with all the users, written by the administrators only |
| `data/<username>/` | the files of a user, uploaded or written by the jobs, seen only by the user and the administrators |
| `jobs/` | a folder for each job, with its parameters, log and temporary outputs |
| `hm-server.sqlite` | the SQLite database of the users, tokens, jobs and catalog, when no `--db` is given |
| `hm-server-settings.json` | the settings saved by the settings page |
| `hm-server-signing.key` | the key of the signed links of the API |
| `templates/` | optional: files that replace the built-in pages (see [Customizing the pages](#customizing-the-pages)) |

To share data with all the users, copy it into `data/shared`, with any tool: the server notices the new and changed files by itself. The first folder inside an area is a **collection** of the catalog: `data/shared/flanginec/dem/dtm.tif` belongs to the collection `flanginec`, together with the files of `data/<username>/flanginec/` of each user. The files directly in an area, outside of any folder, can be used as inputs but are not in the catalog.

The server remembers where each file comes from: uploaded by a user, written by a job, with its module and parameters, or put in the data from outside. When a file is replaced, the origin of the replaced one is kept in its history.

## Users and roles

There are two roles:

- **users** run modules, upload files in their own area, and see and kill only their own jobs;
- **administrators** also manage the users and the settings, write in the shared area, and see the jobs and files of everybody.

User names are 3 to 64 lowercase letters, digits, dots, dashes or underscores, and can't be changed, since they name the folder of the user. Passwords have at least 8 characters. After 5 failed logins in 15 minutes a user name is locked for 15 minutes.

## The pages

The menu at the top leads to the pages; the administrators get three more.

### Modules

The **Modules** page lists the modules, grouped by the folders of the Spatial Toolbox or in alphabetic order, with a search on their name, folder and keywords.

:::{figure} images/server/modules.png
:alt: The Modules page of the web server
:width: 100%
:align: center

The modules, grouped by folder.
:::

The page of a module describes it and has a form with its inputs, parameters and outputs:

- **inputs** are paths in the data, as `data/shared/flanginec/dtm.tif` or `data/jane/flanginec/pit.tif`; the field offers the files you can read, the outputs of your latest jobs first. A table of a GeoPackage follows the path after a `#`, as `data/shared/roads.gpkg#roads`;
- **parameters** are set as in the Spatial Toolbox, with their ranges and allowed values; empty fields keep the default;
- **outputs** are file names, as `pit.tif`.

Under **Where the outputs go**, the output folder is by default the folder of the first input, in your own area: for `data/shared/flanginec/dtm.tif` the outputs go in `data/<you>/flanginec/`, so that they end up next to the input and in the same collection of the catalog. Without input files they go in `data/<you>/results/`. An existing file is not replaced unless **Overwrite** is ticked.

**Run** checks the values and puts the job in the queue.

:::{figure} images/server/module.png
:alt: The form of the Gradient module
:width: 100%
:align: center

The form of the Gradient module, filled to compute the slope in degrees of a filled DTM of the user.
:::

### Jobs

The **Jobs** page lists your jobs, with their state: queued, running, done, failed or killed.

:::{figure} images/server/jobs.png
:alt: The Jobs page
:width: 100%
:align: center

Three jobs that filled the pits of a DTM, then computed the flow directions and the slope from it.
:::

The page of a job shows its parameters, its outputs, its files, which can be downloaded one by one or all together as a zip file, and its log; the page reloads by itself while the job runs. From there a job can be killed, deleted, or run again with the same parameters, to change only some of them.

The outputs of a successful job are moved from the job folder into the output folder; GeoTIFF outputs are first converted to Cloud Optimized GeoTIFFs, unless the `cog-outputs` setting is off. Deleting a job deletes its folder, but its outputs stay in the data. The finished jobs are deleted after `job-ttl-days` days.

:::{figure} images/server/job.png
:alt: The page of a finished job
:width: 100%
:align: center

A finished job: its output was written in `data/jane/flanginec`, next to its input.
:::

### Files

The **Files** page lists the files of an area, with their size and origin: users see their own area and, read-only, the shared one; administrators all the areas. Files can be uploaded in a folder of the area, the first folder being the collection; zip files are unpacked, and a shapefile needs all its files, or a zip with them. A file with the name of an existing one replaces it. The size of all your files, against the quota, is shown at the top.

:::{figure} images/server/files.png
:alt: The Files page
:width: 100%
:align: center

The files of a user: the outputs of the three jobs, with the module that wrote them.
:::

### Account

The **Account** page, reached by clicking your name in the menu, changes your password, which ends your other sessions, and manages your **API tokens**, needed by the programs that use the API. The secret of a token is shown only once, when it is created; a token that is not used anymore should be revoked.

:::{figure} images/server/account.png
:alt: The Account page
:width: 100%
:align: center

The account page, with a token used by scripts.
:::

### Administration

**Users**
: create users, change their role, disable them, set their password, and see the disk used by their files and jobs. Deleting a user deletes the files of the user too; the jobs stay until they expire.

**Status**
: the version, the uptime, the memory, the queued and running jobs, which can be killed, and the free disk, with a link to delete the finished jobs of all the users.

**Settings**
: the settings described in [Settings](#settings), and the button to renew the signing key.

:::{figure} images/server/users.png
:alt: The Users page
:width: 100%
:align: center

The users page of the administrators.
:::

:::{figure} images/server/status.png
:alt: The Status page
:width: 100%
:align: center

The status page of the administrators.
:::

## Settings

The settings page changes the options from `--slots` to `--stac-rescan-minutes` of the [table above](#options). The new values apply at once to the running server and are saved in the `hm-server-settings.json` file of the workspace. A setting given on the command line wins over the page, and is shown there locked.

:::{figure} images/server/settings.png
:alt: The Settings page
:width: 80%
:align: center

The settings page, with the options that can only be given on the command line at the bottom.
:::

**Renew signing key** makes all the signed links given by the API stop working at once, for example when one was shared by mistake.

## The API

The **API** page of the menu documents the APIs of the server, with examples for the server you are on. In short, under `/api`:

- `/api/processes` lists the modules as OGC processes, and `POST /api/processes/<Module>/execution` runs one, always asynchronously; `/api/jobs` follows the jobs and gives their results;
- `/api/stac` is a STAC API 1.0 catalog of all the files you can read, kept up to date with the uploads, the outputs of the jobs and the files put in the data from outside;
- `/api/data/files/<area>/<path>` serves the files, with HTTP range requests, so that GDAL and QGIS read only the parts of a Cloud Optimized GeoTIFF they need.

Every request but the landing page and the conformance needs a token, sent as the header `Authorization: Bearer hm_...`. A token acts as its user: it sees only the jobs and the files of the user.

```sh
TOKEN=hm_...

curl -i -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"inputs": {"inElev": "data/shared/flanginec/dtm.tif"}}' \
  http://localhost:8080/api/processes/Pitfiller/execution

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/jobs/<id>/results
```

The links to the files given by the API, as the results of the jobs and the assets of the catalog, are **signed**: for `signed-url-minutes` minutes they work without the header, so that they can be opened directly, for example by GDAL with `/vsicurl/`. Treat them like the token: they stop working when they expire, when the token is revoked or when the signing key is renewed.

To browse the catalog:

- in the [STAC Browser](apps/stacbrowser.md), connect to `http://<server>:8080/api/stac` with the authentication *Bearer token*;
- in QGIS, add a STAC connection in the Data Source Manager, with the token as the `Authorization` header of the connection;
- with `pystac-client`, pass the header when opening the catalog:

```python
from pystac_client import Client

catalog = Client.open("http://localhost:8080/api/stac", headers={"Authorization": "Bearer hm_..."})
for item in catalog.search(collections=["flanginec"]).items():
    print(item.id, item.properties["hm:path"], item.properties["hm:origin"])
```

The items of the catalog carry, besides the standard STAC properties, the path to give to the processes (`hm:path`), the area and owner, the origin of the file and, for the outputs of the jobs, the module and parameters that produced them.

## Customizing the pages

The pages are [Mustache](https://mustache.github.io/) templates. To change one, copy it from the `org/hortonmachine/server/templates` folder of the `hm-server` jar into a `templates` folder of the workspace and edit it: the files of that folder replace the built-in ones, also `hm.css`, the style sheet. The changes show at the next page load, without restarting the server.

## Running it for others

A few things to keep in mind before opening the server to other people:

- the server speaks plain HTTP: put it behind a reverse proxy with HTTPS (nginx, Apache, Caddy), also because passwords and tokens travel with every request;
- the input and output files of the jobs are checked to be inside the areas the user can read and write, but the jobs are full HortonMachine modules, running with the rights of the user that runs the server: run it as a dedicated user without other privileges;
- size `--slots` and `--job-memory` together on the memory of the machine: `slots × job-memory`, plus the server itself, must fit in it;
- back up the workspace, or the workspace and the PostgreSQL database when `--db` is used.
