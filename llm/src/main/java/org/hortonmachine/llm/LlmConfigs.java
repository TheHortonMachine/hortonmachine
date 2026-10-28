/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.llm;

import java.util.function.Function;

import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.utils.PreferencesHandler;

/**
 * Reads and saves the {@link AssistantConfig}.
 *
 * <p>Each value is taken from the first place that has it:</p>
 * <ol>
 * <li>the java system properties ({@value #KEY_KIND}, {@value #KEY_URL}, {@value #KEY_MODEL},
 * {@value #KEY_API_KEY}, {@value #KEY_TIMEOUT}, {@value #KEY_HISTORY});</li>
 * <li>the environment variables with the same names in upper case and with underscores
 * (e.g. <code>HM_LLM_URL</code>);</li>
 * <li>the user preferences, as saved by {@link #save(AssistantConfig)};</li>
 * <li>for the url of an Ollama server, the <code>OLLAMA_HOST</code> variable of Ollama itself.
 * It comes after the preferences since it is often set only to open the local server to the
 * network, and must not hide a server chosen in the settings;</li>
 * <li>the defaults: local Ollama, no model, {@value AssistantConfig#DEFAULT_TIMEOUT_SECONDS} s,
 * {@value AssistantConfig#DEFAULT_HISTORY_TURNS} turns of discussion.</li>
 * </ol>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class LlmConfigs {

    public static final String KEY_KIND = "hm.llm.kind";
    public static final String KEY_URL = "hm.llm.url";
    public static final String KEY_MODEL = "hm.llm.model";
    public static final String KEY_API_KEY = "hm.llm.apikey";
    public static final String KEY_TIMEOUT = "hm.llm.timeout";
    public static final String KEY_HISTORY = "hm.llm.history";

    private static final String OLLAMA_HOST = "OLLAMA_HOST";
    private static final int OLLAMA_DEFAULT_PORT = 11434;

    private LlmConfigs() {
    }

    /**
     * @return the configuration from system properties, environment, preferences and defaults.
     */
    public static AssistantConfig resolve() {
        return resolve(System::getProperty, System::getenv, key -> PreferencesHandler.getPreference(key, (String) null));
    }

    /**
     * Resolve the configuration from the given sources.
     *
     * @param systemProperties gives the system property of a key, or <code>null</code>.
     * @param environment gives the environment variable of a name, or <code>null</code>.
     * @param preferences gives the saved preference of a key, or <code>null</code>.
     * @return the configuration.
     */
    static AssistantConfig resolve( Function<String, String> systemProperties, Function<String, String> environment,
            Function<String, String> preferences ) {
        Function<String, String> lookup = key -> firstValue(systemProperties.apply(key), environment.apply(toEnvName(key)),
                preferences.apply(key));

        ServerKind kind = ServerKind.OLLAMA;
        String kindValue = lookup.apply(KEY_KIND);
        if (kindValue != null) {
            try {
                kind = ServerKind.valueOf(kindValue.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // keep the default
            }
        }

        String url = lookup.apply(KEY_URL);
        if (url == null && kind == ServerKind.OLLAMA) {
            String ollamaHost = environment.apply(OLLAMA_HOST);
            if (ollamaHost != null && !ollamaHost.isBlank()) {
                url = normalizeOllamaHost(ollamaHost);
            }
        }
        if (url == null) {
            url = AssistantConfig.DEFAULT_OLLAMA_URL;
        }

        int timeout = parseInt(lookup.apply(KEY_TIMEOUT), AssistantConfig.DEFAULT_TIMEOUT_SECONDS);
        int historyTurns = Math.max(0, parseInt(lookup.apply(KEY_HISTORY), AssistantConfig.DEFAULT_HISTORY_TURNS));

        return new AssistantConfig(kind, url.trim(), lookup.apply(KEY_MODEL), lookup.apply(KEY_API_KEY), timeout,
                historyTurns);
    }

    private static int parseInt( String value, int defaultValue ) {
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // use the default
            }
        }
        return defaultValue;
    }

    /**
     * Save a configuration in the user preferences.
     *
     * @param config the configuration to save.
     */
    public static void save( AssistantConfig config ) {
        PreferencesHandler.setPreference(KEY_KIND, config.serverKind().name());
        PreferencesHandler.setPreference(KEY_URL, config.baseUrl());
        PreferencesHandler.setPreference(KEY_MODEL, config.model());
        PreferencesHandler.setPreference(KEY_API_KEY, config.apiKey());
        PreferencesHandler.setPreference(KEY_TIMEOUT, String.valueOf(config.timeoutSeconds()));
        PreferencesHandler.setPreference(KEY_HISTORY, String.valueOf(config.historyTurns()));
    }

    /**
     * Turn the <code>OLLAMA_HOST</code> value, which can lack scheme and port and is often the
     * address the server listens on (<code>0.0.0.0</code>), into a url to connect to.
     *
     * @param host the value of the variable.
     * @return the url.
     */
    static String normalizeOllamaHost( String host ) {
        String url = host.trim();
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        url = url.replace("://0.0.0.0", "://localhost");
        String afterScheme = url.substring(url.indexOf("://") + 3);
        int pathStart = afterScheme.indexOf('/');
        String authority = pathStart == -1 ? afterScheme : afterScheme.substring(0, pathStart);
        boolean hasPort = authority.startsWith("[") ? authority.contains("]:") : authority.contains(":");
        if (!hasPort) {
            String scheme = url.substring(0, url.indexOf("://") + 3);
            String path = pathStart == -1 ? "" : afterScheme.substring(pathStart);
            url = scheme + authority + ":" + OLLAMA_DEFAULT_PORT + path;
        }
        return url;
    }

    /**
     * @return the environment variable for a key, e.g. <code>HM_LLM_URL</code> for <code>hm.llm.url</code>.
     */
    static String toEnvName( String key ) {
        return key.toUpperCase().replace('.', '_');
    }

    private static String firstValue( String... values ) {
        for( String value : values ) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
