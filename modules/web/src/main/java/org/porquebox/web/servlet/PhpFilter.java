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

package org.porquebox.web.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.core.ApplicationFilterChain;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import com.caucho.quercus.QuercusEngine;
import org.projectodd.polyglot.web.servlet.HttpServletResponseCapture;
import org.porquebox.core.component.ComponentResolver;
import org.porquebox.core.component.ComponentEval;
import org.porquebox.core.runtime.PhpRuntimePool;
import org.porquebox.web.component.PhpApplicationComponent;
import org.porquebox.web.php.PhpEnvironment;
import org.porquebox.web.php.processors.PhpWebApplicationInstaller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class PhpFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
        ServiceRegistry registry = (ServiceRegistry) filterConfig.getServletContext().getAttribute( "service.registry" );

        ServiceName componentResolverServiceName = (ServiceName) filterConfig.getServletContext().getAttribute( "component.resolver.service-name" );
        this.componentResolver = (ComponentResolver) registry.getService( componentResolverServiceName ).getValue();
        if (this.componentResolver == null) {
            throw new ServletException( "Unable to obtain Php component resolver: " + componentResolverServiceName );
        }

        ServiceName runtimePoolServiceName = (ServiceName) filterConfig.getServletContext().getAttribute( "runtime.pool.service-name" );
        this.runtimePool = (PhpRuntimePool) registry.getService( runtimePoolServiceName ).getValue();

        if (this.runtimePool == null) {
            throw new ServletException( "Unable to obtain runtime pool: " + runtimePoolServiceName );
        }
        //preloadExec();
        executePattern = filterConfig.getInitParameter("execute");
        excludePattern = filterConfig.getInitParameter("exclude");
    }

    protected void preloadExec() throws ServletException {
        // No need to preload lazy pools
        if (this.runtimePool.isLazy()) {
            return;
        }
        QuercusEngine runtime = null;
        try {
            runtime = this.runtimePool.borrowRuntime( "php" );
            this.componentResolver.resolve( runtime );
        // TODO: Find an equivilant exception from Quercus.
        /* }
        catch (RaiseException e) {
            log.error( "Error loading exec file", e );
            log.error( "Underlying Php exception", e.getCause() );
            throw new ServletException( e ); */
        } catch (Exception e) {
            log.error( "Error loading exec file", e );
            throw new ServletException( e );
        } finally {
            if (runtime != null) {
                this.runtimePool.returnRuntime( runtime );
            }
        }
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            doFilter( (HttpServletRequest) request, (HttpServletResponse) response, chain );
        }
    }

    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (((request.getPathInfo() == null) || (request.getPathInfo().equals( "/" ))) && !(request.getRequestURI().endsWith( "/" ))) {
            String redirectUri = request.getRequestURI() + "/";
            String queryString = request.getQueryString();
            if (queryString != null) {
                redirectUri = redirectUri + "?" + queryString;
            }
            redirectUri = response.encodeRedirectURL( redirectUri );
            response.sendRedirect( redirectUri );
            return;
        }

        String servletName = "";

        try {
            servletName = ((ApplicationFilterChain) chain).getServlet().getServletConfig().getServletName();
        } catch (Exception e) {
            // If we can't fetch the name, we can be pretty sure it's not one of our servlets, in which case it really
            // doesn't matter what the name is.
        }

        if (PhpWebApplicationInstaller.FIVE_HUNDRED_SERVLET_NAME.equals(servletName) ||
                PhpWebApplicationInstaller.STATIC_RESROUCE_SERVLET_NAME.equals(servletName)) {
            boolean defaultLocation = true;
            // Only hand off requests to Php if they're handled by one of the
            if (matchExclude(request)) {
                defaultLocation = true;
            }
            else if (matchExecute(request)) {
                defaultLocation = false;
            }
            else {
                HttpServletResponseCapture responseCapture = new HttpServletResponseCapture( response );
                try {
                    chain.doFilter( request, responseCapture );
                    if (responseCapture.isError()) {
                        response.reset();
                    } else if (!request.getMethod().equals( "OPTIONS" )) {
                        // Pass HTTP OPTIONS requests through to the Php application
                        return;
                    }
                } catch (ServletException e) {
                  log.error( "Error performing request", e );
                }
                defaultLocation = true;
            }
            doPhp( request, response, defaultLocation );
        } else {
            // Bypass our Php stack entirely for any servlets defined in a
            // user's WEB-INF/web.xml
            chain.doFilter( request, response );
        }
    }

    protected void doPhp(HttpServletRequest request, HttpServletResponse response, boolean defaultLocation) throws IOException, ServletException {
        PhpEnvironment phpEnv = null;

        QuercusEngine runtime = null;
        PhpApplicationComponent phpApp;
        try {
            runtime = this.runtimePool.borrowRuntime( "php" );
            phpApp = (PhpApplicationComponent) this.componentResolver.resolve( runtime );
            if ( defaultLocation ) {
                phpEnv = new PhpEnvironment( runtime, request, ((ComponentEval) phpApp.getPhpComponent()).getLocation() );
            }
            else {
                phpEnv = new PhpEnvironment( runtime, request, request.getPathTranslated().toString() );
            }
            phpApp.call( phpEnv ).respond( response );
        // TODO: Find equivalnt for Quercus.
        /* }  catch (RaiseException e) {
            log.error( "Error invoking Php filter", e );
            log.error( "Underlying Php exception", e.getCause() );
            throw new ServletException( e );
        */
        } catch (Exception e) {
            log.error( "Error invoking Php filter", e );
            throw new ServletException( e );
        } finally {
            if (phpEnv != null) {
                phpEnv.close();
            }

            if (runtime != null) {
                this.runtimePool.returnRuntime( runtime );
            }
        }
    }

    public String setExecutePattern(String pattern) {
      return executePattern = pattern;
    }

    public String setExcludePattern(String pattern) {
      return excludePattern = pattern;
    }

    public String getExecutePattern() {
      return executePattern;
    }

    public String getExcludePattern() {
      return excludePattern;
    }

    public boolean matchExclude(HttpServletRequest request) {
      return Paths.get(request.getPathTranslated()).getFileName().toString().matches(excludePattern);
    }

    public boolean matchExecute(HttpServletRequest request) {
      return Paths.get(request.getPathTranslated()).getFileName().toString().matches(executePattern);
    }

    private static final Logger log = Logger.getLogger( PhpFilter.class );

    public static final String PHP_APP_DEPLOYMENT_INIT_PARAM = "porquebox.php.app.deployment.name";

    private ComponentResolver componentResolver;
    private PhpRuntimePool runtimePool;
    private String executePattern;
    private String excludePattern;
}
