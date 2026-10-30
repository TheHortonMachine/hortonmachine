# AI assistant

:::{warning}
The assistant is **highly experimental**. The answers of language models can be wrong even when they look right, and the small models that run on a laptop make mistakes more often: always read the SQL and the scripts before running them. Its windows, settings and behavior can still change between versions.
:::

The [Database Viewer](apps/dbviewer.md) and the [Geoscript Console](apps/geoscript.md) can be helped by an assistant based on a large language model (LLM): it answers questions, writes, fixes and explains SQL queries and scripts, knowing the database you are connected to or the HortonMachine modules and helper methods.

The assistant follows one rule: **it proposes, you act**. The model never connects to a database, never runs a query or a script, and never sees passwords. What it writes goes into the editor only when you ask for it, and runs only when you run it.

By default the model runs on your own machine through [Ollama](https://ollama.com): no account, no API key, no cost, and no data leaves the computer. Any server with an OpenAI compatible API can be used instead, local or online.

## Installing

The assistant is an optional module: it is available when the installation contains the `hm-llm` jar in its `libs` folder. Without it, the Database Viewer and the console show no assistant at all and work as before.

What the assistant needs is a server with a model. The easiest is a local Ollama.

### A local Ollama

1. Install Ollama from [ollama.com/download](https://ollama.com/download); on Linux:

   ```sh
   curl -fsSL https://ollama.com/install.sh | sh
   ```

   On Windows and macOS the installer starts Ollama in the background; on Linux it is installed as a service. It listens on `http://localhost:11434`, the address the assistant uses by default.

2. Download a model, from the assistant settings (see [Settings](#settings)) or from a terminal:

   ```sh
   ollama pull qwen2.5-coder:7b
   ```

   `qwen2.5-coder:7b` is the recommended model: a model of about 5 GB trained on code, that runs on a laptop with 16 GB of memory. Without a graphics card it works on the processor, slowly: count tens of seconds for an answer. Larger models, as `qwen2.5-coder:14b` or `32b`, give better answers if the machine has the memory, and better a graphics card, for them. The available models are listed in the [Ollama library](https://ollama.com/library).

3. Open the assistant in the Database Viewer or in the console: it connects to Ollama and uses the model.

If Ollama runs on another machine, as a shared server with a graphics card, set its address in the settings. The `OLLAMA_HOST` environment variable, if set, is also used.

### An OpenAI compatible server

The kind **OpenAI compatible** of the settings connects to any server with the OpenAI chat API (`/v1/chat/completions` and `/v1/models`):

- local servers, as [LM Studio](https://lmstudio.ai/) (`http://localhost:1234/v1`) or [vLLM](https://docs.vllm.ai/) (`http://localhost:8000/v1`);
- a server of your institution;
- hosted services, as [OpenRouter](https://openrouter.ai/) (`https://openrouter.ai/api/v1`), which gives access to many models, some of them for free with low daily limits.

These servers usually need an API key, set in the settings. Models are not downloaded from the assistant: they are managed on the server.

:::{important}
With a server that is not on your machine, what the assistant sends leaves the computer: your questions, the schema of the database, the scripts and variable names, and the sample rows when they are included. The settings warn about it. Check that this is allowed for your data before using an online service.
:::

## Settings

The **Settings** button of both assistant windows opens the assistant settings, shared by the two applications:

:::{figure} images/assistant/settings.png
:alt: The assistant settings
:width: 70%
:align: center

The settings, connected to a local Ollama with the recommended model.
:::

Server
: the kind of server, **Ollama** or **OpenAI compatible (OpenRouter, LM Studio, vLLM, hosted)**, and its address; the list proposes the usual ones for the kind, any other can be typed.

API key
: empty for a local Ollama; for the servers that need one. It is saved in plain text in the user preferences.

Timeout (s)
: how long to wait for an answer, 300 seconds by default: the first request also loads the model.

Keep model loaded (min)
: Ollama only: how long the model stays in memory after the last request, 30 minutes by default. While it is loaded, Ollama also remembers the part of the request that doesn't change, and the next requests are much faster.

Context (tokens)
: Ollama only: the size of the context of the model, 16384 by default. Request and answer must fit in it; the script assistant needs about 12000. More context uses more memory.

Discussion memory
: how many earlier questions and answers are sent again with each request, 6 by default, so that follow-ups like "now only the larger ones" are understood. 0 makes every request stand alone.

Model
: the model to use, chosen among those of the server; for OpenRouter, **Only free** lists only the free ones.

Download model
: Ollama only: downloads a model on the server, showing its progress.

**Apply and test** saves the settings and checks the server: the status tells if it is reachable, its version and its models, or what to do, for example how to install Ollama when nothing answers on the local address.

The settings can also be given, for example to set them for all the users of a machine, as Java system properties or environment variables, which win over the saved ones:

| System property | Environment variable | Value |
|---|---|---|
| `hm.llm.kind` | `HM_LLM_KIND` | `OLLAMA` or `OPENAI_COMPATIBLE` |
| `hm.llm.url` | `HM_LLM_URL` | the address of the server |
| `hm.llm.model` | `HM_LLM_MODEL` | the model |
| `hm.llm.apikey` | `HM_LLM_APIKEY` | the API key |
| `hm.llm.timeout` | `HM_LLM_TIMEOUT` | the timeout in seconds |
| `hm.llm.history` | `HM_LLM_HISTORY` | the discussion memory |
| `hm.llm.keepalive` | `HM_LLM_KEEPALIVE` | the minutes the model stays loaded |
| `hm.llm.context` | `HM_LLM_CONTEXT` | the context in tokens |

## The assistant windows

The two assistants work the same way. The window has a **request box** at the bottom and the **discussion** above it, with the questions and the answers, written as they arrive. The answers can contain text, lists and code blocks; above each code block are the links to use it.

Each request is sent with the earlier ones of the discussion, up to the discussion memory. **New conversation** (Ctrl+N) forgets them and starts over.

In the request box, Ctrl+Enter sends the request with **Ask**, Ctrl+Up and Ctrl+Down recall the earlier requests, and Ctrl+W clears the box. With an empty box, the buttons use the last request again. **Cancel** stops a request that takes too long.

Before showing a query or a script, the assistant checks it (see below): if the check fails, the error goes back to the model, which tries to correct it, up to two more times. The status line tells what happens, and the answer tells whether the final code passed the checks.

### In the Database Viewer

The **assistant** button of the toolbar of the Database Viewer opens the **SQL assistant**. It works also without a database connected, to ask, for example, how to connect to a PostGIS database; to write SQL for your data, connect to the database first.

The assistant knows the type of the database (GeoPackage, SpatiaLite, H2GIS, PostGIS) and its spatial functions, and the tables of the database with their columns, types, keys and geometry columns with their projection.

**Ask**
: answers a question, writing SQL when the question asks for data: *"which are the 10 largest lakes, with their area in km²"*.

**Fix editor SQL**
: corrects the SQL of the current editor, with the error the database gave when it was run; run it first.

**Explain editor SQL**
: explains what the SQL of the editor does.

**Include sample rows**
: sends the first rows of each table too, which helps with codes and formats in the data, but sends data to the server. Off by default.

:::{figure} images/assistant/sql_assistant.png
:alt: The SQL assistant
:width: 100%
:align: center

A question on a GeoPackage of South Tyrol: the SQL uses the real table and columns, and was checked by the database without being run.
:::

Above each SQL block of the discussion, **Add to editor** puts it in the current SQL editor, and **Copy** copies it. The SQL is never run by the assistant: run it from the editor as usual, where the **Limit result to** of the viewer applies.

Queries that only read data are checked by asking the database for their plan (`EXPLAIN`), without running them: a query with a wrong table or column name is rejected and goes back to the model. SQL that changes the database is not checked, and **Add to editor** asks for a confirmation before adding it.

Large databases have large schemas: with a local Ollama, a request that doesn't fit in the context of the model is refused, with the size it would need. Raise the **Context** in the settings, if the memory allows it.

### In the Geoscript Console

The **Assistant** button of the toolbar, or **HM > Assistant** (Ctrl+Shift+A), opens the **script assistant** of the [Geoscript Console](apps/geoscript.md).

When it opens, the assistant prepares the model, loading it and reading the part of the request that is always the same; the status line says **Ready** when done. This makes the first answer faster.

The assistant knows:

- the HortonMachine modules, with their inputs, outputs and parameters, and which outputs of a module are inputs of another, as the flow directions of `FlowDirections` for `ExtractBasin`;
- the methods of the [HM helper class](apps/geoscript.md#the-hm-helper-class) and the imports of the console;
- a set of working example scripts, on hydrology, mosaicking, resampling, reprojection, spatial databases and the download from WCS, WFS, STAC and Copernicus: the most similar ones to the request are sent with it;
- the names and types of the variables of the earlier runs of the console.

**Ask**
: answers a request, writing a script when it asks for one: *"from the dtm of the dtm variable, fill the pits, compute the flow directions and extract the basin at the outlet 1637145, 5111955"*.

**Fix editor script**
: corrects the script of the editor, or the selected lines, with the error of their last run; run it first.

**Explain editor script**
: explains the script of the editor, or the selected lines.

**Include the editor script**
: sends the script of the editor, or the selected lines, with **Ask**, so that the request can continue it: the assistant then writes only the code to add. It is on by default only for a server on your machine, since scripts contain paths and values.

:::{figure} images/assistant/script_assistant.png
:alt: The script assistant
:width: 100%
:align: center

A request on the DTM of a variable of the console: the modules are chained correctly and the checks pass, but the paths of the outputs are placeholders to set before running it. Note that this example was done with a small local model, 
therefore there are some errors that one would need to take care before running.
:::

Above each script of the discussion, **Insert at caret** inserts it in the editor, **Replace editor script** replaces the content of the editor (it can be undone), and **Copy** copies it.

The scripts are checked by compiling them, without running them, and by looking at how they use the modules: fields that a module doesn't have, classes that don't exist, or an input fed with the wrong kind of data, as a DEM given where flow directions are expected, are sent back to the model. The checks can't tell if a script does what you asked: a script that passes them can still chain the wrong modules or use wrong parameters.

## The audit log

Every request is recorded, with its time, the server and model, the full request, the answer and the extracted code, one JSON line per request, in `~/.hortonmachine/llm-audit.jsonl`, or in the file of the `hm.llm.audit` system property. It shows exactly what was sent to the server.

## Limits

- Small local models are slow on the processor alone, and the first request after a while also loads the model.
- They make mistakes: they invent column names, module fields or methods that don't exist. The checks catch many of them, not all.
- Requests that combine several steps or rare spatial functions are the most difficult: split them into smaller requests, one after the other in the same discussion.
- Larger models and online services answer better, at the cost of memory, of money, or of sending your data out of the machine.
