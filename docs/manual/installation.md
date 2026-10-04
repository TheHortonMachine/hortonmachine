# Installation

## Download

The applications are distributed as a single archive, attached to each release on the [GitHub releases page](https://github.com/TheHortonMachine/hortonmachine/releases). Download the `hortonmachine_<version>.tar.gz` archive and extract it anywhere; there is no installer.

The extracted folder contains:

| Path | Content |
|---|---|
| `hm-<app>.sh` | the launchers for Linux and macOS |
| `hm-<app>.bat` | the launchers for Windows |
| `<app>.exe` | Windows launchers that run the matching `.bat` file without opening a console window |
| `libs/` | the HortonMachine libraries and all their dependencies |
| `natives/` | native libraries used by some applications |
| `jre/` | a Java runtime for Windows |
| `imgs/` | the splash screens |

## Java

The applications need Java 17 or newer.

- On **Windows** the archive already contains a Java runtime in the `jre` folder, which the launchers use automatically.
- On **Linux and macOS** the launchers use the `java` command found on the path. Install a Java 17+ runtime (for example from [Adoptium](https://adoptium.net/)) and check it with:

```sh
java -version
```

:::{tip}
On Linux and macOS the launchers also use a `jre` folder next to them if it contains a `bin/java` executable, so a runtime for your platform can be dropped in there, just as on Windows.
:::

## Launching an application

Each application has its own launcher, named after it. On Windows double-click the `.exe` (or the `.bat`) file; on Linux and macOS run the `.sh` file from a terminal, for example:

```sh
./hm-stacbrowser.sh
```

Some applications accept arguments on the command line. These are described in each application's chapter.

## Memory

The applications can use up to 2 GB of memory by default. If an application runs out of memory on large datasets, set the `HM_MEM` environment variable to the maximum memory to use, keeping it below the physical memory of the machine. It applies to all the launchers, so they don't need to be changed.

On Linux and macOS, for a single run:

```sh
HM_MEM=8g ./hm-dbviewer.sh
```

or for all the launchers of the session, with `export HM_MEM=8g`, which can be added to the shell profile (for example `~/.bashrc`) to make it permanent.

On Windows, `set HM_MEM=8g` in a command prompt applies to the launchers started from it. To make it permanent, also for the `.exe` launchers, add `HM_MEM` to the user environment variables in the system settings.

The value is given as a number with the unit, `m` for megabytes or `g` for gigabytes, as in `512m` or `16g`.
