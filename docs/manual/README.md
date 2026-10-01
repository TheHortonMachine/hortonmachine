# HortonMachine user manual

The HortonMachine user manual, in MyST Markdown for Sphinx, built on [Read the Docs](https://readthedocs.org/) through `.readthedocs.yaml` at the repository root.

## Building locally

```sh
./build_locally.sh
```

This creates a local virtualenv (`.venv/`, git-ignored) on first run, installs `requirements.txt` into it, and builds into `_build/html/` (also git-ignored). Open `_build/html/index.html` directly in a browser.

## File structure

| Path | Covers |
|---|---|
| `index.md` | entry point: intro, license, help, and the table of contents |
| `installation.md` | download, Java, launchers, memory |
| `apps/index.md` | the list of applications |
| `apps/<app>.md` | one chapter per application |
| `images/apps/<app>/` | the screenshots of each application |
| `modules/index.md` | the modules section, with its table of contents |
| `modules/<folder>/index.md` | one page per Spatial Toolbox folder (the module `@Label`, without the `HortonMachine/` prefix), with its table of contents |
| `modules/<folder>/<module>.md` | one page per module, in the folder of its `@Label` |
| `modules/geoframe/<model>/` | the GeoFrame models, like `erm/`, each with its workflow page (`index.md`) and the pages of its modules |
| `modules/modules.txt` | the list of the documented module classes |
| `modules/generated/<Module>.md` | the module references, generated from the annotations: do not edit |
| `modules/examples/` | the example scripts included in the pages |

The modules are documented in the `modules/` section, with their screenshots in `images/modules/`.

## Module references

The description, parameters and references of each module are generated from its OMS annotations (`@Description`, `@Label`, `@Keywords`, `@Status`, `@UI`, `@Unit`, `@In`, `@Out`, `@Bibliography`) by `ModuleDocsGenerator` in the `modules` project, for the classes listed in `modules/modules.txt`. The pages include them with `{include}`. Always document the module classes without the `Oms` prefix.

To change a module reference, change the annotations and regenerate the fragments. From the repository root:

```sh
mvn -o install -pl modules -am -DskipTests
mvn -o exec:java -pl modules \
    -Dexec.mainClass=org.hortonmachine.docs.ModuleDocsGenerator \
    -Dexec.args="docs/manual/modules/modules.txt docs/manual/modules/generated"
```

The `TestModuleDocs` test fails when the committed fragments differ from the annotations:

```sh
mvn -o test -pl modules -am -Dtest=TestModuleDocs -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false
```

