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

package org.porquebox.core.datasource;

import java.sql.Driver;

import org.jboss.as.connector.services.driver.InstalledDriver;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import com.caucho.quercus.QuercusEngine;
import org.projectodd.polyglot.core.AsyncService;
import org.porquebox.core.datasource.db.Adapter;
import org.porquebox.core.runtime.PhpRuntimeFactory;
//import org.porquebox.core.util.RuntimeHelper;

public class DriverService extends AsyncService<Driver> {

    public DriverService(String applicationDirectory, Adapter adapter) {
        this.applicationDirectory = applicationDirectory;
        this.adapter = adapter;
    }

    @Override
    public void startAsync(final StartContext context) throws Exception {
        this.driver = instantiateDriver();
        log.debug( "driver: " + this.driver );
        this.installedDriver = createInstalledDriver();

        DriverRegistry registry = this.driverRegistryInjector.getValue();
        registry.registerInstalledDriver( installedDriver );
    }

    @Override
    public void stop(StopContext context) {
        this.driverRegistryInjector.getValue().unregisterInstalledDriver( this.installedDriver );
    }

    protected Driver instantiateDriver() throws Exception {
        QuercusEngine php = this.runtimeInjector.getValue();

        synchronized (php) {
/*
            php.setCurrentDirectory( this.applicationDirectory );

            RuntimeHelper.require( php, "bundler/setup" );
            RuntimeHelper.require( php, this.adapter.getRequirePath() );

            String phpDriverClassName = this.adapter.getPhpDriverClassName();
            if (phpDriverClassName != null) {
                RuntimeHelper.evalScriptlet( php, phpDriverClassName + ".load_driver if " + phpDriverClassName + ".respond_to?(:load_driver)" );
            }

            ClassLoader classLoader = php.getJPhpClassLoader();
            final Class<? extends Driver> driverClass = classLoader.loadClass( this.adapter.getDriverClassName() ).asSubclass( Driver.class );
            Driver driver = driverClass.newInstance();
*/
            return null;
        }
    }

    protected InstalledDriver createInstalledDriver() {
        int majorVersion = this.driver.getMajorVersion();
        int minorVersion = this.driver.getMinorVersion();
        boolean compliant = this.driver.jdbcCompliant();
        return new InstalledDriver( this.adapter.getId(), this.driver.getClass()
                .getName(), null, null, majorVersion, minorVersion, compliant );
    }

    @Override
    public Driver getValue() throws IllegalStateException,
            IllegalArgumentException {
        return this.driver;
    }

    public Injector<QuercusEngine> getRuntimeInjector() {
        return this.runtimeInjector;
    }

    public Injector<PhpRuntimeFactory> getRuntimeFactoryInjector() {
        return this.runtimeFactoryInjector;
    }

    public Injector<DriverRegistry> getDriverRegistryInjector() {
        return this.driverRegistryInjector;
    }

    private static final Logger log = Logger.getLogger( "org.porquebox.core.db" );

    private InjectedValue<QuercusEngine> runtimeInjector = new InjectedValue<QuercusEngine>();
    private InjectedValue<PhpRuntimeFactory> runtimeFactoryInjector = new InjectedValue<PhpRuntimeFactory>();

    private InjectedValue<DriverRegistry> driverRegistryInjector = new InjectedValue<DriverRegistry>();

    private String applicationDirectory;
    private Adapter adapter;

    private Driver driver;
    private InstalledDriver installedDriver;

}
