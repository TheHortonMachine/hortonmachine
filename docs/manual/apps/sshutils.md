# SSH Utils

SSH Utils runs commands on a remote server through SSH, and opens SSH tunnels. The tunnels are especially useful to reach a database that a server doesn't expose to the internet, for example a PostGIS listening only on the server itself, from the [Database Viewer](dbviewer.md).

## Launching

Start the application with the `hm-utils-ssh` launcher (see [Launching an application](../installation.md#launching-an-application)).

If an SSH private key is set in the [Settings](settings.md#ssh), it is used for the connections, and shown in the window.

:::{figure} ../images/apps/sshutils/main.png
:alt: SSH Utils
:width: 80%
:align: center

SSH Utils, ready to open a tunnel to a PostGIS database on a server.
:::

## Running commands

Insert the **host**, **port** (22 by default), **user** and **password** of the server on the left, write a **command** at the top and press **run**. The output of the command is shown in the area below.

## Tunneling

A tunnel makes a port of the server reachable on your computer, through the SSH connection:

remote host
: the server to connect to with SSH (on port 22).

remote port
: the port of the service on the server, reached as `localhost` from the server itself: `5432` for PostgreSQL.

local port
: the port of your computer through which the service is reached. It can be the same as the remote one, or a different one if that is already in use locally, like `15432`.

remote user, remote password
: the SSH login on the server.

Press **create tunnel** to open it: the button becomes **disconnect tunnel**, to close it. The tunnel stays open as long as the application is running.

With the tunnel of the example above open, the database on the server is reached from the **remote** connection of the Database Viewer as if it were on your computer:

```text
jdbc:postgresql://localhost:15432/dbname
```

:::{note}
The values of the fields, passwords included, are saved in the preferences when the application is closed, to be proposed again the next time.
:::
