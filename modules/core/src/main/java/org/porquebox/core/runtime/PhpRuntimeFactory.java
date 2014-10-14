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

package org.porquebox.core.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceRegistry;
import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.component.InjectionRegistry;
import org.porquebox.core.pool.InstanceFactory;
// TODO: Is there a real difference between RubyInstanceConfig and TorqueboxRubyInstanceConfig ?
import org.porquebox.core.util.PorqueBoxPhpInstanceConfig;

/**
 * Default Php runtime interpreter factory implementation.
 * 
 * @author Bob McWhirter <bmcwhirt@redhat.com>
 */
public class PhpRuntimeFactory implements InstanceFactory<QuercusEngine> {

    /**
     * Construct.
     */
    public PhpRuntimeFactory() {
        this( null, null );
    }

    public PhpRuntimeFactory(RuntimeInitializer initializer) {
        this( initializer, null );
    }

    /**
     * Construct with an initializer and a preparer.
     * 
     * @param initializer
     *            The initializer (or null) to use for each created runtime.
     * @param preparer
     *            The preparer (or null) to use for each created runtime.
     */
    public PhpRuntimeFactory(RuntimeInitializer initializer, RuntimePreparer preparer) {
        this.initializer = initializer;
        this.preparer = preparer;
        if (this.preparer == null) {
            this.preparer = new BaseRuntimePreparer( null );
        }
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    public void setGemPath(String gemPath) {
        this.gemPath = gemPath;
    }

    public String getGemPath() {
        return this.gemPath;
    }

    /**
     * Set the interpreter classloader.
     * 
     * @param classLoader
     *            The classloader.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Retrieve the interpreter classloader.
     * 
     * @return The classloader.
     */
    public ClassLoader getClassLoader() {
        if (this.classLoader != null) {
            return this.classLoader;
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (cl != null) {
            return cl;
        }

        return getClass().getClassLoader();
    }

    /**
     * 
     * @param debug
     *            Whether Php debug logging should be enabled or not
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Retrieve the debug mode
     * 
     * @return Whether debug logging is enabled or not
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * 
     * @param interactive
     *            Whether the runtime is marked as interactive or not
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    /**
     * Retrieve the interactive mode
     * 
     * @return Whether the runtime is marked as interactive or not
     */
    public boolean isInteractive() {
        return this.interactive;
    }

    /**
     * Retrieve the profile.api mode
     * 
     * @return Whether the Php profile.api flag is enabled or not
     */
    public boolean isProfileApi() {
        return profileApi;
    }

    /**
     * Sets the profile.api value for the Php environment.
     * 
     * @param profileApi Whether the Php profile.api flag is enabled or not.
     */
    public void setProfileApi(boolean profileApi) {
        this.profileApi = profileApi;
    }

    /**
     * Set the application-specific environment additions.
     * 
     * @param applicationEnvironment
     *            The environment.
     */
    public void setApplicationEnvironment(Map<String, String> applicationEnvironment) {
        this.applicationEnvironment = applicationEnvironment;
    }

    /**
     * Retrieve the application-specific environment additions.
     * 
     * @return The environment.
     */
    public Map<String, String> getApplicationEnvironment() {
        return this.applicationEnvironment;
    }

    /**
     * Create a new instance of a fully-initialized runtime.
     */
    public QuercusEngine createInstance(String contextInfo) throws Exception {
        return createInstance( contextInfo, true );
    }

    public QuercusEngine createInstance(String contextInfo, boolean initialize) throws Exception {

        PorqueBoxPhpInstanceConfig config = new PorqueBoxPhpInstanceConfig();

        Map<String, String> environment = createEnvironment();

        config.setLoader( getClassLoader() );
        // TODO: Compatibility version + CompileMode
        config.setDebug( this.debug );
        config.setInteractive( this.interactive );

        config.setEnvironment( environment );
        config.setInput( getInput() );
        config.setOutput( getOutput() );
        config.setError( getError() );

        List<String> loadPath = new ArrayList<String>();
        if (this.loadPaths != null) {
            loadPath.addAll( this.loadPaths );
        }

        config.setLoadPaths( loadPath );

        long startTime = logRuntimeCreationStart( config, contextInfo );

        QuercusEngine runtime = null;
        try {
            runtime = new QuercusEngine();

            preparer.prepareRuntime( runtime, contextInfo, this.serviceRegistry );

            log.debug( "Initialize? " + initialize );
            log.debug( "Initializer=" + this.initializer );
            if (initialize) {
                this.injectionRegistry.merge( runtime );
                if (this.initializer != null) {
                    this.initializer.initialize( runtime, contextInfo );
                } else {
                    log.debug( "No initializer set for runtime" );
                }
            }

            performRuntimeInitialization( runtime );
        } catch (Exception e) {
            log.error( "Failed to initialize runtime: ", e );
        } finally {
            if (runtime != null) {
                this.undisposed.add( runtime );
            }

            logRuntimeCreationComplete( config, contextInfo, startTime );
        }

        RuntimeContext.registerRuntime( runtime );

        return runtime;
    }

    private long logRuntimeCreationStart(PorqueBoxPhpInstanceConfig config, String contextInfo) {
        log.info( "Creating php runtime.");
        return System.currentTimeMillis();
    }

    private void logRuntimeCreationComplete(PorqueBoxPhpInstanceConfig config, String contextInfo, long startTime) {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        double elapsedSeconds = Math.floor( (elapsedMillis * 1.0) / 10.0 ) / 100;
        log.info( "Created php runtime: " + getFullContext( contextInfo )
                + ") in " + elapsedSeconds + "s" );
    } 

    private void logRuntimeDestroyed(PorqueBoxPhpInstanceConfig config, String contextInfo) {
        log.info( "Destroyed php runtime: ("
                + config.getCompileMode() + getFullContext( contextInfo ) + ")" );
    }

    protected String getFullContext(String contextInfo) {
        String fullContext = null;

        if (this.applicationName != null) {
            fullContext = "app: " + this.applicationName;
        }

        if (contextInfo != null) {
            if (fullContext != null) {
                fullContext += ", ";
            } else {
                fullContext = "";
            }

            fullContext += "context: " + contextInfo;
        }

        if (fullContext == null) {
            fullContext = "";
        } else {
            fullContext = ", " + fullContext;
        }

        return fullContext;
    }

    public synchronized void destroyInstance(QuercusEngine instance) {
        PorqueBoxPhpInstanceConfig config = new PorqueBoxPhpInstanceConfig(instance);
        //String contextInfo = (String) instance.getENV().get( "PORQUEBOX_CONTEXT" );
        String contextInfo = "Quercus Context Info.";
        RuntimeContext.deregisterRuntime( instance );
        if (undisposed.remove( instance )) {
            try {
                // TODO: cleanup database connections in the engine.
                // RuntimeHelper.evalScriptlet( instance, "ActiveRecord::Base.clear_all_connections! if defined?(ActiveRecord::Base)" );
            } catch (Exception e) {
                // ignorable since we're tearing down the instance anyway
            }
            // TODO: Is this equivilant to a tearDown()?
            //instance.getQuercus().close();
            logRuntimeDestroyed( config, contextInfo );
        }
    }

    private void performRuntimeInitialization(QuercusEngine runtime) {
        defineVersions( runtime );
        setApplicationName( runtime );
    }

    private void defineVersions(QuercusEngine runtime) {
        // TODO: Implement defineVersions().
        // RuntimeHelper.invokeClassMethod( runtime, "PorqueBox", "define_versions", new Object[] { log } );
    }

    private void setApplicationName(QuercusEngine runtime) {
        // TODO: Implement setApplicationName().
        //RuntimeHelper.invokeClassMethod( runtime, "PorqueBox", "application_name=", new Object[] { applicationName } );
    }

    protected Map<String, String> createEnvironment() {
        Map<String, String> env = new HashMap<String, String>();
        env.putAll( System.getenv() );

        // From javadocs:
        //
        // On UNIX systems the alphabetic case of name is typically significant,
        // while on Microsoft Windows systems it is typically not. For example,
        // the expression System.getenv("FOO").equals(System.getenv("foo")) is
        // likely to be true on Microsoft Windows.
        //
        // This means that if on Windows the env variable is set as Path,
        // we should still retrieve it.
        String path = System.getenv( "PATH" );
        if (path == null) {
            // There is no PATH (or Path) environment variable set,
            // let's create an empty one
            env.put( "PATH", "" );
        }

        String gemPath = System.getProperty( "gem.path" );

        if (gemPath == null) {
            gemPath = this.gemPath;
        }

        if ("default".equals( gemPath )) {
            env.remove( "GEM_PATH" );
            env.remove( "GEM_HOME" );
            gemPath = null;
        }

        if (gemPath != null) {
            env.put( "GEM_PATH", gemPath );
            env.put( "GEM_HOME", gemPath );
        }
        if (this.applicationEnvironment != null) {
            env.putAll( this.applicationEnvironment );
        }

        return env;
    }

    /**
     * Set the interpreter input stream.
     * 
     * @param inputStream
     *            The input stream.
     */
    public void setInput(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Retrieve the interpreter input stream.
     * 
     * @return The input stream.
     */
    public InputStream getInput() {
        return this.inputStream;
    }

    /**
     * Set the interpreter output stream.
     * 
     * @param outputStream
     *            The output stream.
     */
    public void setOutput(PrintStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Retrieve the interpreter output stream.
     * 
     * @return The output stream.
     */
    public PrintStream getOutput() {
        return this.outputStream;
    }

    /**
     * Set the interpreter error stream.
     * 
     * @param errorStream
     *            The error stream.
     */
    public void setError(PrintStream errorStream) {
        this.errorStream = errorStream;
    }

    /**
     * Retrieve the interpreter error stream.
     * 
     * @return The error stream.
     */
    public PrintStream getError() {
        return this.errorStream;
    }

    /**
     * Set the interpreter load paths.
     * 
     * <p>
     * Load paths may be either real filesystem paths or VFS URLs
     * </p>
     * 
     * @param loadPaths
     *            The list of load paths.
     */
    public void setLoadPaths(List<String> loadPaths) {
        this.loadPaths = loadPaths;
    }

    /**
     * Retrieve the interpreter load paths.
     * 
     * @return The list of load paths.
     */
    public List<String> getLoadPaths() {
        return this.loadPaths;
    }

    public void create() {
        // TODO: Implement a ClassCache.
        //this.classCache = new ClassCache<Script>( getClassLoader() );
    }

    public synchronized void destroy() {
        Set<QuercusEngine> toDispose = new HashSet<QuercusEngine>();
        toDispose.addAll( this.undisposed );

        for (QuercusEngine engine : toDispose) {
            destroyInstance( engine );
        }
        this.undisposed.clear();
    }

    // TODO: Implement a ClassCache.
    // public ClassCache getClassCache() {
        //return this.classCache;
    //}

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public ServiceRegistry getServiceRegistry() {
        return this.serviceRegistry;
    }

    public Injector<Object> getInjector(String key) {
        return this.injectionRegistry.getInjector( key );
    }

    private static final Logger log = Logger.getLogger( "org.porquebox.core.runtime" );

    /** Re-usable initializer. */
    private RuntimeInitializer initializer;

    /** Re-usable preparer. */
    private RuntimePreparer preparer;

    /** ClassLoader for interpreter. */
    private ClassLoader classLoader;

    /** Shared interpreter class cache. */
    // TODO: Implement ClassCache?
    // private ClassCache<Script> classCache;

    /** Application name. */
    private String applicationName;

    /** Load paths for the interpreter. */
    private List<String> loadPaths;

    /** Input stream for the interpreter. */
    private InputStream inputStream = System.in;

    /** Output stream for the interpreter. */
    private PrintStream outputStream = System.out;

    /** Error stream for the interpreter. */
    private PrintStream errorStream = System.err;

    /** GEM_PATH. */
    private String gemPath;

    /** Additional application environment variables. */
    private Map<String, String> applicationEnvironment;

    /** Undisposed runtimes created by this factory. */
    private Set<QuercusEngine> undisposed = Collections.synchronizedSet( new HashSet<QuercusEngine>() );

    // TODO: Investigate compile mode.
    // TODO: Investigate compatibility versions.

    /** Php debug logging enabled or not. */
    private boolean debug = false;

    /** I/O streams setup for interactive use or not */
    private boolean interactive = false;

    /** Whether the Php profile api is enabled or not */
    private boolean profileApi = false;

    private ServiceRegistry serviceRegistry;

    private InjectionRegistry injectionRegistry = new InjectionRegistry();
}
