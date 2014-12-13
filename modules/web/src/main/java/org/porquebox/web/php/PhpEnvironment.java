/*
 * Copyright 2008-2013 Red Hat, Inc, and individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.porquebox.web.php;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.logging.Logger;
import com.caucho.quercus.QuercusEngine;
import com.caucho.quercus.QuercusContext;

public class PhpEnvironment {

    public PhpEnvironment(QuercusEngine runtime, HttpServletRequest request) throws IOException {
        initializeEnv( runtime, request );
    }

    public PhpEnvironment(QuercusEngine runtime, HttpServletRequest request, String path) throws IOException {
        initializeEnv( runtime, request );
        this.path = path;
    }

    private void initializeEnv(QuercusEngine runtime, HttpServletRequest request) throws IOException {
        this.request = request;
        this.runtime = runtime;

        String pathInfo = request.getRequestURI();
        if (pathInfo.startsWith( request.getContextPath() )) {
            pathInfo = pathInfo.substring( request.getContextPath().length() );
        }
        if (pathInfo.startsWith( request.getServletPath() )) {
            pathInfo = pathInfo.substring( request.getServletPath().length() );
        }

        QuercusContext env = runtime.getQuercus();
        env.setServerEnv("REQUEST_METHOD", request.getMethod());
        env.setServerEnv("SCRIPT_NAME", request.getContextPath() + request.getServletPath());
        env.setServerEnv("PATH_INFO", pathInfo);
        env.setServerEnv("QUERY_STRING", request.getQueryString() == null ? "" : request.getQueryString() );
        env.setServerEnv("SERVER_NAME", request.getServerName());
        env.setServerEnv("SERVER_PORT", request.getServerPort() + "" );
        env.setServerEnv("CONTENT_TYPE", request.getContentType() == null ? "" : request.getContentType());
        env.setServerEnv("REQUEST_URI", request.getContextPath() + request.getServletPath() + pathInfo );
        env.setServerEnv("REMOTE_ADDR", request.getRemoteAddr() );

        if (request.getContentLength() >= 0) {
            env.setServerEnv( "CONTENT_LENGTH", Integer.toString(request.getContentLength()) );
        }

        if ("https".equals( request.getScheme() )) {
            env.setServerEnv( "HTTPS", "on" );
        }

        if (request.getHeaderNames() != null) {
            for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
                String headerName = headerNames.nextElement();
                String envName = "HTTP_" + headerName.toUpperCase().replace( '-', '_' );

                String value = request.getHeader( headerName );

                env.setServerEnv( envName, value );
            }
        }

        // env.setServerEnv( env.createString("servlet_request"), QuercusContext.objectToValue((Object) request) );
        // env.setServerEnv( env.createString("java.servlet_request"), QuercusContext.objectToValue((Object) request) );

        if (log.isTraceEnabled()) {
            // log.trace( "Created: " + env.inspect() );
        }
    }

    public QuercusEngine getEnv() {
        return runtime;
    }

    public QuercusEngine getEngine() {
        return runtime;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void close() {
        //explicitly close the inputstream, but leave the err stream open,
        //as closing that detaches it from the log forever!

        if (this.input != null) {
            try {
                this.input.close();
            } catch (IOException e) {
                log.error( "Error closing php.input", e );
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String setPath(String path) {
        return this.path = path;
    }

    private static final Logger log = Logger.getLogger( PhpEnvironment.class );

    private QuercusEngine runtime;
    private InputStream input = null;
    private HttpServletRequest request;
    private String path = null;
}
