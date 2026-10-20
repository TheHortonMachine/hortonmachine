/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The editor of the notebooks of the HortonMachine server.
 *
 * The cells are edited with CodeMirror; runs are posted with the token of the session, the outputs
 * arrive as server sent events. The outputs are built with DOM nodes and text, never from the html
 * of a notebook file: tables come from their JSON, markdown is escaped before its few tags are added.
 *
 * Developed with the assistance of an AI coding agent; design, review and validation by the author.
 */
(function () {
    'use strict';

    var TABLE_MIME = 'application/vnd.hortonmachine.table+json';
    var AUTOSAVE_MS = 30000;

    var root = document.getElementById('notebook');
    var base = root.dataset.base;
    var csrf = root.dataset.csrf;
    var cellsEl = document.getElementById('nb-cells');
    var stateEl = document.getElementById('nb-state');
    var savedEl = document.getElementById('nb-saved');
    var helpEl = document.getElementById('nb-help');

    /** The cells, in order: {id, type, el, editor, countEl, outputsEl, mdEl, lastCount}. */
    var cells = [];
    var selected = null;
    var dirty = false;
    var saving = false;

    // ---- requests ----

    function post(path, params) {
        var body = new URLSearchParams(params || {});
        body.set('csrf', csrf);
        return fetch(base + path, { method: 'POST', body: body, credentials: 'same-origin' }).then(function (response) {
            if (!response.ok) {
                return response.text().then(function (text) {
                    throw new Error(text || response.statusText);
                });
            }
            return response;
        });
    }

    function showError(error) {
        savedEl.textContent = 'Error: ' + (error && error.message ? error.message : error);
        savedEl.className = 'nb-saved error';
    }

    function newId() {
        var bytes = new Uint8Array(6);
        window.crypto.getRandomValues(bytes);
        return Array.prototype.map.call(bytes, function (b) {
            return ('0' + b.toString(16)).slice(-2);
        }).join('');
    }

    function joinText(value) {
        if (Array.isArray(value)) {
            return value.join('');
        }
        return value == null ? '' : String(value);
    }

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) {
            node.className = className;
        }
        if (text != null) {
            node.textContent = text;
        }
        return node;
    }

    // ---- markdown ----

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    /** The inline markup of an escaped line: code, bold, italic and links to http(s) or relative urls. */
    function inline(escaped) {
        var codes = [];
        var text = escaped.replace(/`([^`]+)`/g, function (match, code) {
            codes.push(code);
            return '\u0000' + (codes.length - 1) + '\u0000';
        });
        text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
            .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
            .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, function (match, label, url) {
                // the url is escaped already; only web and relative links
                if (/^(https?:\/\/|\/|\.{0,2}\/|#)/i.test(url) || !/^[a-z][a-z0-9+.-]*:/i.test(url)) {
                    return '<a href="' + url + '" target="_blank" rel="noopener noreferrer">' + label + '</a>';
                }
                return label;
            });
        return text.replace(/\u0000(\d+)\u0000/g, function (match, index) {
            return '<code>' + codes[Number(index)] + '</code>';
        });
    }

    function markdown(source) {
        var lines = source.split('\n');
        var html = [];
        var paragraph = [];
        var list = null;
        var i = 0;

        function flushParagraph() {
            if (paragraph.length) {
                html.push('<p>' + paragraph.map(function (line) { return inline(escapeHtml(line)); }).join('<br>') + '</p>');
                paragraph = [];
            }
        }
        function flushList() {
            if (list) {
                html.push('<' + list.tag + '>' + list.items.map(function (item) {
                    return '<li>' + inline(escapeHtml(item)) + '</li>';
                }).join('') + '</' + list.tag + '>');
                list = null;
            }
        }

        while (i < lines.length) {
            var line = lines[i];
            var match;
            if (/^```/.test(line)) {
                flushParagraph();
                flushList();
                var code = [];
                i++;
                while (i < lines.length && !/^```/.test(lines[i])) {
                    code.push(lines[i]);
                    i++;
                }
                html.push('<pre><code>' + escapeHtml(code.join('\n')) + '</code></pre>');
            } else if ((match = /^(#{1,6})\s+(.*)$/.exec(line))) {
                flushParagraph();
                flushList();
                var level = match[1].length;
                html.push('<h' + level + '>' + inline(escapeHtml(match[2])) + '</h' + level + '>');
            } else if (/^\s*([-*_])(\s*\1){2,}\s*$/.test(line)) {
                flushParagraph();
                flushList();
                html.push('<hr>');
            } else if ((match = /^\s*[-*+]\s+(.*)$/.exec(line)) || (match = /^\s*\d+[.)]\s+(.*)$/.exec(line))) {
                flushParagraph();
                var tag = /^\s*\d/.test(line) ? 'ol' : 'ul';
                if (list && list.tag !== tag) {
                    flushList();
                }
                if (!list) {
                    list = { tag: tag, items: [] };
                }
                list.items.push(match[1]);
            } else if ((match = /^>\s?(.*)$/.exec(line))) {
                flushParagraph();
                flushList();
                html.push('<blockquote>' + inline(escapeHtml(match[1])) + '</blockquote>');
            } else if (line.trim() === '') {
                flushParagraph();
                flushList();
            } else {
                flushList();
                paragraph.push(line);
            }
            i++;
        }
        flushParagraph();
        flushList();
        return html.join('\n');
    }

    // ---- outputs ----

    function renderTable(table) {
        var wrapper = el('div', 'scroll nb-table');
        var tableEl = el('table');
        var head = el('tr');
        (table.header || []).forEach(function (name) {
            head.appendChild(el('th', null, name));
        });
        tableEl.appendChild(head);
        (table.rows || []).forEach(function (row) {
            var tr = el('tr');
            row.forEach(function (value) {
                tr.appendChild(el('td', null, value));
            });
            tableEl.appendChild(tr);
        });
        wrapper.appendChild(tableEl);
        var rows = (table.rows || []).length;
        if (table.totalRows > rows) {
            wrapper.appendChild(el('p', 'muted', 'Showing ' + rows + ' of ' + table.totalRows + ' rows.'));
        }
        return wrapper;
    }

    function renderOutput(output) {
        var type = output.output_type;
        if (type === 'stream') {
            return el('pre', 'nb-stream' + (output.name === 'stderr' ? ' nb-stderr' : ''), joinText(output.text));
        }
        if (type === 'error') {
            var text = output.ename + ': ' + output.evalue;
            var traceback = (output.traceback || []).join('\n');
            return el('pre', 'nb-error', traceback ? text + '\n' + traceback : text);
        }
        var data = output.data || {};
        if (data[TABLE_MIME]) {
            return renderTable(data[TABLE_MIME]);
        }
        if (data['image/png']) {
            var img = el('img', 'nb-image');
            img.alt = joinText(data['text/plain']) || 'image';
            img.src = 'data:image/png;base64,' + joinText(data['image/png']).replace(/\s/g, '');
            return img;
        }
        if (data['text/plain'] != null) {
            return el('pre', 'nb-text', joinText(data['text/plain']));
        }
        return el('p', 'muted', '(an output that this page doesn\'t show)');
    }

    function appendOutput(cell, output) {
        var last = cell.outputsEl.lastElementChild;
        if (output.output_type === 'stream' && last && last.dataset.stream === output.name) {
            last.textContent += joinText(output.text);
            return;
        }
        var node = renderOutput(output);
        if (output.output_type === 'stream') {
            node.dataset.stream = output.name;
        }
        cell.outputsEl.appendChild(node);
    }

    function setCount(cell, text) {
        cell.countEl.textContent = '[' + text + ']';
    }

    // ---- cells ----

    function cellIndex(cell) {
        return cells.indexOf(cell);
    }

    function select(cell) {
        if (selected && selected.el) {
            selected.el.classList.remove('selected');
        }
        selected = cell;
        if (cell) {
            cell.el.classList.add('selected');
        }
    }

    function markDirty() {
        dirty = true;
        savedEl.textContent = 'Not saved';
        savedEl.className = 'nb-saved muted';
    }

    function showMarkdown(cell, rendered) {
        if (rendered) {
            cell.mdEl.innerHTML = markdown(cell.editor.getValue()) || '<p class="muted">Empty text: double click to write.</p>';
            cell.mdEl.hidden = false;
            cell.editorEl.hidden = true;
        } else {
            cell.mdEl.hidden = true;
            cell.editorEl.hidden = false;
            cell.editor.refresh();
            cell.editor.focus();
        }
    }

    function toolButton(label, title, handler) {
        var button = el('button', 'link', label);
        button.type = 'button';
        button.title = title;
        button.addEventListener('click', function (event) {
            event.stopPropagation();
            handler();
        });
        return button;
    }

    function createCell(data, index) {
        var cell = { id: data.id || newId(), type: data.type === 'markdown' ? 'markdown' : 'code' };
        cell.el = el('div', 'nb-cell nb-' + cell.type);
        // focusable, for the keys of the command mode
        cell.el.tabIndex = -1;
        var gutter = el('div', 'nb-gutter');
        cell.countEl = el('span', 'nb-count');
        if (cell.type === 'code') {
            setCount(cell, data.executionCount != null ? data.executionCount : ' ');
            var runButton = toolButton('▶', 'Run (Ctrl+Enter)', function () {
                runCell(cell);
            });
            runButton.className = 'nb-run';
            gutter.appendChild(runButton);
        }
        gutter.appendChild(cell.countEl);
        cell.el.appendChild(gutter);

        var body = el('div', 'nb-body');
        cell.editorEl = el('div', 'nb-editor');
        body.appendChild(cell.editorEl);
        cell.mdEl = el('div', 'nb-markdown');
        cell.mdEl.hidden = true;
        body.appendChild(cell.mdEl);
        cell.outputsEl = el('div', 'nb-outputs');
        body.appendChild(cell.outputsEl);
        cell.el.appendChild(body);

        var tools = el('div', 'nb-tools');
        tools.appendChild(toolButton('↑', 'Move up', function () { move(cell, -1); }));
        tools.appendChild(toolButton('↓', 'Move down', function () { move(cell, 1); }));
        tools.appendChild(toolButton('✕', 'Delete the cell', function () { remove(cell, true); }));
        cell.el.appendChild(tools);

        if (index == null || index >= cells.length) {
            cellsEl.appendChild(cell.el);
            cells.push(cell);
        } else {
            cellsEl.insertBefore(cell.el, cells[index].el);
            cells.splice(index, 0, cell);
        }

        cell.editor = CodeMirror(cell.editorEl, {
            value: data.source || '',
            // fencedCodeBlockHighlighting colors ```groovy blocks with the groovy mode
            mode: cell.type === 'code' ? 'groovy' : { name: 'markdown', fencedCodeBlockHighlighting: true },
            indentUnit: 4,
            indentWithTabs: false,
            lineWrapping: cell.type === 'markdown',
            matchBrackets: cell.type === 'code',
            autoCloseBrackets: cell.type === 'code',
            viewportMargin: Infinity,
            extraKeys: {
                'Shift-Enter': function () { runCell(cell, true); },
                'Ctrl-Enter': function () { runCell(cell, false); },
                'Cmd-Enter': function () { runCell(cell, false); },
                'Alt-Enter': function () {
                    runCell(cell, false);
                    addCell('code');
                },
                'Esc': function () { commandMode(cell); },
                'Ctrl-S': function () { save(); },
                'Cmd-S': function () { save(); },
                'Tab': function (cm) {
                    cm.execCommand(cm.somethingSelected() ? 'indentMore' : 'insertSoftTab');
                },
                'Shift-Tab': 'indentLess'
            }
        });
        cell.editor.on('change', markDirty);
        cell.editor.on('focus', function () { select(cell); });
        cell.el.addEventListener('click', function () { select(cell); });

        if (cell.type === 'markdown') {
            cell.mdEl.addEventListener('dblclick', function () { showMarkdown(cell, false); });
            showMarkdown(cell, (data.source || '').trim() !== '');
        } else {
            (data.outputs || []).forEach(function (output) {
                appendOutput(cell, output);
            });
        }
        return cell;
    }

    function addCell(type) {
        var index = selected ? cellIndex(selected) + 1 : cells.length;
        var cell = createCell({ type: type, source: '' }, index);
        if (type === 'markdown') {
            showMarkdown(cell, false);
        }
        select(cell);
        cell.editor.focus();
        markDirty();
        return cell;
    }

    function move(cell, delta) {
        var index = cellIndex(cell);
        var target = index + delta;
        if (target < 0 || target >= cells.length) {
            return;
        }
        cells.splice(index, 1);
        cells.splice(target, 0, cell);
        cellsEl.removeChild(cell.el);
        if (target >= cells.length - 1) {
            cellsEl.appendChild(cell.el);
        } else {
            cellsEl.insertBefore(cell.el, cells[target + 1].el);
        }
        cell.editor.refresh();
        markDirty();
    }

    /** The last deleted cell, for Z, and the copied one, for V. */
    var lastDeleted = null;
    var clipboard = null;

    function cellData(cell) {
        return { type: cell.type, source: cell.editor.getValue() };
    }

    /**
     * @param ask true to ask before deleting a cell with content, as from its button.
     */
    function remove(cell, ask) {
        if (ask && cell.editor.getValue().trim() !== '' && !window.confirm('Delete this cell?')) {
            return;
        }
        var index = cellIndex(cell);
        lastDeleted = { data: cellData(cell), index: index };
        cells.splice(index, 1);
        cellsEl.removeChild(cell.el);
        if (selected === cell) {
            select(cells[Math.min(index, cells.length - 1)] || null);
        }
        if (!cells.length) {
            addCell('code');
        }
        markDirty();
    }

    /**
     * Insert a cell at an index, in command mode.
     */
    function insertCell(data, index) {
        var cell = createCell({ type: data.type, source: data.source }, index);
        select(cell);
        commandMode(cell);
        markDirty();
        return cell;
    }

    /**
     * Change a cell into a code or text cell, keeping its source and id; the outputs are lost.
     */
    function convert(cell, type) {
        if (cell.type === type) {
            return;
        }
        var index = cellIndex(cell);
        var source = cell.editor.getValue();
        cells.splice(index, 1);
        cellsEl.removeChild(cell.el);
        var converted = createCell({ id: cell.id, type: type, source: source }, index);
        select(converted);
        commandMode(converted);
        markDirty();
    }

    // ---- command mode, as in Jupyter ----

    function commandMode(cell) {
        if (cell.editor.hasFocus()) {
            cell.editor.getInputField().blur();
        }
        if (cell.type === 'markdown' && cell.editor.getValue().trim() !== '') {
            showMarkdown(cell, true);
        }
        select(cell);
        cell.el.focus({ preventScroll: true });
    }

    function editMode(cell) {
        if (cell.type === 'markdown') {
            showMarkdown(cell, false);
        } else {
            cell.editor.focus();
        }
    }

    function selectNear(delta) {
        var index = selected ? cellIndex(selected) + delta : 0;
        var cell = cells[Math.max(0, Math.min(cells.length - 1, index))];
        if (cell) {
            commandMode(cell);
            cell.el.scrollIntoView({ block: 'nearest' });
        }
    }

    var lastKey = null;
    var lastKeyTime = 0;

    /** @return true if the key is the second of a pair typed quickly, as D D. */
    function isDouble(key) {
        var now = Date.now();
        var double = lastKey === key && now - lastKeyTime < 800;
        lastKey = double ? null : key;
        lastKeyTime = now;
        return double;
    }

    function onCommandKey(event) {
        var cell = selected;
        if (!cell) {
            return false;
        }
        var key = event.key;
        var index = cellIndex(cell);
        if (key === 'Enter' && event.shiftKey) {
            runCell(cell, false);
            selectNear(1);
            if (index === cells.length - 1) {
                insertCell({ type: 'code', source: '' }, cells.length);
            }
        } else if (key === 'Enter' && (event.ctrlKey || event.metaKey)) {
            runCell(cell, false);
        } else if (key === 'Enter' && event.altKey) {
            runCell(cell, false);
            insertCell({ type: 'code', source: '' }, index + 1);
        } else if (event.ctrlKey || event.metaKey || event.altKey) {
            return false;
        } else if (key === 'Enter') {
            editMode(cell);
        } else if (key === 'ArrowUp' || key === 'k') {
            selectNear(-1);
        } else if (key === 'ArrowDown' || key === 'j') {
            selectNear(1);
        } else if (key === 'a') {
            insertCell({ type: 'code', source: '' }, index);
        } else if (key === 'b') {
            insertCell({ type: 'code', source: '' }, index + 1);
        } else if (key === 'm') {
            convert(cell, 'markdown');
        } else if (key === 'y') {
            convert(cell, 'code');
        } else if (key === 'd') {
            if (isDouble('d')) {
                remove(cell, false);
                if (selected) {
                    commandMode(selected);
                }
            }
        } else if (key === 'z') {
            if (lastDeleted) {
                insertCell(lastDeleted.data, Math.min(lastDeleted.index, cells.length));
                lastDeleted = null;
            }
        } else if (key === 'c') {
            clipboard = cellData(cell);
        } else if (key === 'x') {
            clipboard = cellData(cell);
            remove(cell, false);
            if (selected) {
                commandMode(selected);
            }
        } else if (key === 'v') {
            if (clipboard) {
                insertCell(clipboard, index + 1);
            }
        } else if (key === 'i') {
            if (isDouble('i')) {
                post('/interrupt').catch(showError);
            }
        } else if (key === '0') {
            if (isDouble('0') && window.confirm('Restart the kernel? The variables are lost, the outputs stay.')) {
                post('/restart').catch(showError);
            }
        } else if (key === 's') {
            save();
        } else if (key === 'h') {
            helpEl.open = !helpEl.open;
        } else if (key === 'Escape' && helpEl.open) {
            helpEl.open = false;
        } else {
            return false;
        }
        return true;
    }

    document.addEventListener('keydown', function (event) {
        var target = event.target;
        // the keys of the editors and of the other fields are theirs
        if (target.closest && (target.closest('.CodeMirror') || /^(INPUT|TEXTAREA|SELECT|BUTTON)$/.test(target.tagName))) {
            return;
        }
        if (onCommandKey(event)) {
            event.preventDefault();
        }
    });

    function focusNext(cell) {
        var index = cellIndex(cell);
        var next = cells[index + 1] || addCell('code');
        select(next);
        if (next.type === 'markdown' && !next.mdEl.hidden) {
            next.el.scrollIntoView({ block: 'nearest' });
        } else {
            next.editor.focus();
        }
    }

    function runCell(cell, moveNext) {
        if (cell.type === 'markdown') {
            showMarkdown(cell, true);
        } else {
            cell.outputsEl.textContent = '';
            setCount(cell, '*');
            cell.el.classList.remove('nb-failed');
            post('/run', { cell: cell.id, code: cell.editor.getValue() }).catch(function (error) {
                setCount(cell, ' ');
                showError(error);
            });
        }
        if (moveNext) {
            focusNext(cell);
        }
    }

    function runAll() {
        // the kernel runs them in the order of the posts
        var chain = Promise.resolve();
        cells.forEach(function (cell) {
            if (cell.type === 'markdown') {
                showMarkdown(cell, true);
                return;
            }
            chain = chain.then(function () {
                cell.outputsEl.textContent = '';
                setCount(cell, '*');
                return post('/run', { cell: cell.id, code: cell.editor.getValue() });
            });
        });
        chain.catch(showError);
    }

    // ---- saving ----

    function save() {
        if (saving) {
            return Promise.resolve();
        }
        saving = true;
        var data = cells.map(function (cell) {
            return { id: cell.id, type: cell.type, source: cell.editor.getValue() };
        });
        return post('/save', { cells: JSON.stringify(data) }).then(function () {
            dirty = false;
            savedEl.textContent = 'Saved ' + new Date().toLocaleTimeString();
            savedEl.className = 'nb-saved muted';
        }).catch(showError).then(function () {
            saving = false;
        });
    }

    // ---- events of the kernel ----

    var STATES = { off: 'kernel off', starting: 'kernel starting...', idle: 'kernel ready', busy: 'kernel busy' };

    function setState(state) {
        stateEl.textContent = STATES[state] || state;
        stateEl.className = 'nb-state nb-state-' + state;
    }

    function findCell(id) {
        for (var i = 0; i < cells.length; i++) {
            if (cells[i].id === id) {
                return cells[i];
            }
        }
        return null;
    }

    function onEvent(event) {
        if (event.type === 'status') {
            setState(event.state);
            return;
        }
        var cell = findCell(event.cell);
        if (!cell) {
            return;
        }
        if (event.type === 'queued') {
            cell.outputsEl.textContent = '';
            setCount(cell, '*');
        } else if (event.type === 'start') {
            cell.lastCount = event.count;
            cell.el.classList.add('nb-running');
        } else if (event.type === 'output') {
            appendOutput(cell, event.output);
        } else if (event.type === 'done') {
            cell.el.classList.remove('nb-running');
            setCount(cell, cell.lastCount != null && event.status !== 'interrupted' ? cell.lastCount : ' ');
            cell.el.classList.toggle('nb-failed', event.status !== 'ok');
            // the outputs are saved with the notebook
            markDirty();
        }
    }

    function listen() {
        var source = new EventSource(base + '/events');
        source.onmessage = function (message) {
            try {
                onEvent(JSON.parse(message.data));
            } catch (error) {
                // not an event of the notebook
            }
        };
        source.onerror = function () {
            stateEl.textContent = 'disconnected, retrying...';
            stateEl.className = 'nb-state nb-state-off';
        };
    }

    // ---- start ----

    root.querySelector('.nb-buttons').addEventListener('click', function (event) {
        var action = event.target.dataset ? event.target.dataset.action : null;
        if (action === 'save') {
            save();
        } else if (action === 'download') {
            save().then(function () {
                if (!dirty) {
                    window.location.href = base + '/download';
                }
            });
        } else if (action === 'add-code') {
            addCell('code');
        } else if (action === 'add-markdown') {
            addCell('markdown');
        } else if (action === 'run-all') {
            runAll();
        } else if (action === 'interrupt') {
            post('/interrupt').catch(showError);
        } else if (action === 'restart') {
            if (window.confirm('Restart the kernel? The variables are lost, the outputs stay.')) {
                post('/restart').catch(showError);
            }
        }
    });

    document.addEventListener('keydown', function (event) {
        if ((event.ctrlKey || event.metaKey) && event.key === 's') {
            event.preventDefault();
            save();
        }
    });

    window.addEventListener('beforeunload', function (event) {
        if (dirty) {
            event.preventDefault();
            event.returnValue = '';
        }
    });

    window.setInterval(function () {
        if (dirty) {
            save();
        }
    }, AUTOSAVE_MS);

    fetch(base + '/content', { credentials: 'same-origin' }).then(function (response) {
        if (!response.ok) {
            throw new Error(response.statusText);
        }
        return response.json();
    }).then(function (notebook) {
        cellsEl.textContent = '';
        (notebook.cells || []).forEach(function (data) {
            createCell(data);
        });
        if (!cells.length) {
            createCell({ type: 'code', source: '' });
        }
        select(cells[0]);
        setState(notebook.state);
        dirty = false;
        savedEl.textContent = '';
        listen();
    }).catch(showError);
})();
