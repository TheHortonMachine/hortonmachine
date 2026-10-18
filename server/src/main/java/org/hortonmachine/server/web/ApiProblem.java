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
package org.hortonmachine.server.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.Context;

/**
 * An error of the api, answered as RFC 7807 problem details, with the exception types of OGC API Processes.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ApiProblem extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private static final String OGC_TYPES = "http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/";
    public static final String NO_SUCH_PROCESS = OGC_TYPES + "no-such-process";
    public static final String NO_SUCH_JOB = OGC_TYPES + "no-such-job";
    public static final String RESULT_NOT_READY = OGC_TYPES + "result-not-ready";
    public static final String INVALID_PARAMETER = "http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/invalid-parameter-value";
    public static final String UNAUTHORIZED = "about:blank";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public final int status;
    public final String type;
    public final String title;
    /** More members, as the errors of each parameter, or <code>null</code>. */
    public final Map<String, Object> extra;

    public ApiProblem( int status, String type, String title, String detail ) {
        this(status, type, title, detail, null);
    }

    public ApiProblem( int status, String type, String title, String detail, Map<String, Object> extra ) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
        this.extra = extra;
    }

    /**
     * Answer the request with this problem.
     */
    public void send( Context ctx ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("title", title);
        body.put("status", status);
        if (getMessage() != null) {
            body.put("detail", getMessage());
        }
        if (extra != null) {
            body.putAll(extra);
        }
        ctx.status(status).contentType("application/problem+json").result(MAPPER.writeValueAsString(body));
    }
}
