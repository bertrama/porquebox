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

package org.porquebox.web.php.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.deployment.ServletContextAttribute;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.msc.service.ServiceName;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.projectodd.polyglot.web.servlet.FiveHundredServlet;
import org.projectodd.polyglot.web.servlet.StaticResourceServlet;
import org.porquebox.core.as.CoreServices;
import org.porquebox.web.as.WebServices;
import org.porquebox.web.php.PhpMetaData;
import org.porquebox.web.servlet.PhpFilter;
import org.porquebox.web.servlet.SendfileFilter;

/**
 * <pre>
 * Stage: DESCRIBE
 *    In: PhpApplicationMetaData, WebMetaData, JBossWebMetaData
 *   Out: WebMetaData, JBossWebMetaData
 * </pre>
 *
 * Makes the JBossWebMetaData depend on the PhpApplicationPool, and sets up
 * Java servlet filters to delegate to the Php application
 */
public class PhpWebApplicationInstaller implements DeploymentUnitProcessor {

    /** Default session timeout (30 minutes). */
    public static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

    public static final String SENDFILE_FILTER_NAME = "porquebox.sendfile";
    public static final String PHP_FILTER_NAME = "porquebox.php";

    public static final String STATIC_RESROUCE_SERVLET_NAME = "porquebox.static";
    public static final String STATIC_RESOURCE_SERVLET_CLASS_NAME = StaticResourceServlet.class.getName();

    public static final String FIVE_HUNDRED_SERVLET_NAME = "porquebox.500";
    public static final String FIVE_HUNDRED_SERVLET_CLASS_NAME = FiveHundredServlet.class.getName();

    public static final String LOCALHOST_MBEAN_NAME = "jboss.web:host=localhost,type=Host";

    public static final String EXPANDED_WAR_URL_ATTACHMENT_NAME = "org.jboss.web.expandedWarURL";

    public PhpWebApplicationInstaller() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        ResourceRoot resourceRoot = unit.getAttachment( Attachments.DEPLOYMENT_ROOT );
        PhpMetaData phpAppMetaData = unit.getAttachment( PhpMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData == null) {
            return;
        }

        unit.addToAttachmentList( Attachments.RESOURCE_ROOTS, resourceRoot );

        WarMetaData warMetaData = unit.getAttachment( WarMetaData.ATTACHMENT_KEY );
        WebMetaData webMetaData = warMetaData.getWebMetaData();

        if (webMetaData == null) {
            webMetaData = new WebMetaData();
            ServiceName jgroups = ChannelFactoryService.getServiceName( null );
            if (phaseContext.getServiceRegistry().getService( jgroups ) != null) {
                webMetaData.setDistributable( new EmptyMetaData() );
            }
            warMetaData.setWebMetaData( webMetaData );
        }

        Map<String, WebFragmentMetaData> webFragmentsMetaData = warMetaData.getWebFragmentsMetaData();

        if (webFragmentsMetaData == null) {
            webFragmentsMetaData = new HashMap<String, WebFragmentMetaData>();
            warMetaData.setWebFragmentsMetaData( webFragmentsMetaData );
        }

        JBossWebMetaData jbossWebMetaData = warMetaData.getJBossWebMetaData();

        if (jbossWebMetaData == null) {
            jbossWebMetaData = new JBossWebMetaData();
            warMetaData.setJBossWebMetaData( jbossWebMetaData );
        }

        setUpSessionConfig( jbossWebMetaData, phpAppMetaData );

        setUpSendfileFilter( webMetaData );
        setUpPhpFilter( unit, phpAppMetaData, webMetaData );
        setUpStaticResourceServlet( phpAppMetaData, webMetaData, false );
        ensureSomeServlet( phpAppMetaData, webMetaData );
        try {
            jbossWebMetaData.setContextRoot( phpAppMetaData.getContextPath() );
            setUpPoolDependency( phpAppMetaData, jbossWebMetaData );
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException( e );
        }

        jbossWebMetaData.setVirtualHosts( phpAppMetaData.getHosts() );

        ServletContextAttribute serviceRegistryValue = new ServletContextAttribute( "service.registry", unit.getServiceRegistry() );
        unit.addToAttachmentList( ServletContextAttribute.ATTACHMENT_KEY, serviceRegistryValue );

        ServletContextAttribute componentResolverNameValue = new ServletContextAttribute( "component.resolver.service-name",
                WebServices.phpApplicationComponentResolver( unit ) );
        unit.addToAttachmentList( ServletContextAttribute.ATTACHMENT_KEY, componentResolverNameValue );
        unit.addToAttachmentList( Attachments.WEB_DEPENDENCIES, WebServices.phpApplicationComponentResolver( unit ) );

        ServletContextAttribute runtimePoolNameValue = new ServletContextAttribute( "runtime.pool.service-name", CoreServices.runtimePoolName( unit, "web" ) );
        unit.addToAttachmentList( ServletContextAttribute.ATTACHMENT_KEY, runtimePoolNameValue );
        unit.addToAttachmentList( Attachments.WEB_DEPENDENCIES, CoreServices.runtimeStartPoolName( unit, "web" ) );
    }

    private void setUpSessionConfig(JBossWebMetaData jbossWebMetaData, PhpMetaData phpAppMetaData) {
        int timeout = phpAppMetaData.getSessionTimeout();

        SessionConfigMetaData sessionConfig = new SessionConfigMetaData();

        if (timeout < 0) {
            sessionConfig.setSessionTimeout( DEFAULT_SESSION_TIMEOUT_MINUTES );
        } else {
            sessionConfig.setSessionTimeout( timeout / 60 ); // convert seconds to minutes
        }

        jbossWebMetaData.setSessionConfig(  sessionConfig );

    }

    protected void setUpPhpFilter(DeploymentUnit unit, PhpMetaData phpAppMetaData, WebMetaData webMetaData) {
        FilterMetaData phpFilter = new FilterMetaData();
        phpFilter.setId( PHP_FILTER_NAME );
        phpFilter.setFilterClass( PhpFilter.class.getName() );
        phpFilter.setFilterName( PHP_FILTER_NAME );

        List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();
        ParamValueMetaData phpAppFactory = new ParamValueMetaData();
        phpAppFactory.setParamName( PhpFilter.PHP_APP_DEPLOYMENT_INIT_PARAM );
        phpAppFactory.setParamValue( unit.getName() );
        initParams.add( phpAppFactory );

        phpAppFactory = new ParamValueMetaData();
        phpAppFactory.setParamName( "execute" );
        phpAppFactory.setParamValue( phpAppMetaData.getExecute() );
        initParams.add( phpAppFactory );

        phpAppFactory = new ParamValueMetaData();
        phpAppFactory.setParamName( "exclude" );
        phpAppFactory.setParamValue( phpAppMetaData.getExclude() );
        initParams.add( phpAppFactory );

        phpFilter.setInitParam( initParams );

        FiltersMetaData filters = webMetaData.getFilters();

        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters( filters );
        }

        filters.add( phpFilter );

        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName( PHP_FILTER_NAME );
        filterMapping.setUrlPatterns( Collections.singletonList( "*" ) );

        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();

        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            webMetaData.setFilterMappings( filterMappings );
        }

        filterMappings.add( filterMapping );
    }

    protected void setUpSendfileFilter(WebMetaData webMetaData) {
        FilterMetaData sendfileFilter = new FilterMetaData();
        sendfileFilter.setId(SENDFILE_FILTER_NAME);
        sendfileFilter.setFilterClass(SendfileFilter.class.getName());
        sendfileFilter.setFilterName(SENDFILE_FILTER_NAME);

        FiltersMetaData filters = webMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters(filters);
        }
        filters.add(sendfileFilter);

        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName(SENDFILE_FILTER_NAME);
        filterMapping.setUrlPatterns(Collections.singletonList("*"));

        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            webMetaData.setFilterMappings(filterMappings);
        }
        filterMappings.add(filterMapping);
    }

    protected void setUpStaticResourceServlet(PhpMetaData phpAppMetaData, WebMetaData webMetaData, boolean enablePageCache) {
        ServletsMetaData servlets = webMetaData.getServlets();
        if (servlets == null) {
            servlets = new ServletsMetaData();
            webMetaData.setServlets( servlets );
        }

        List<ServletMappingMetaData> servletMappings = webMetaData.getServletMappings();
        if (servletMappings == null) {
            servletMappings = new ArrayList<ServletMappingMetaData>();
            webMetaData.setServletMappings( servletMappings );
        }

        if (phpAppMetaData.getStaticPathPrefix() != null) {
            JBossServletMetaData staticServlet = new JBossServletMetaData();
            staticServlet.setServletClass( STATIC_RESOURCE_SERVLET_CLASS_NAME );
            staticServlet.setServletName( STATIC_RESROUCE_SERVLET_NAME );
            staticServlet.setId( STATIC_RESROUCE_SERVLET_NAME );

            List<ParamValueMetaData> paramsList = new ArrayList<ParamValueMetaData>();
            ParamValueMetaData resourceRootParam = new ParamValueMetaData();
            resourceRootParam.setParamName( "resource.root" );
            resourceRootParam.setParamValue( phpAppMetaData.getStaticPathPrefix() );
            paramsList.add( resourceRootParam );

            if (enablePageCache) {
                // TODO: Let users configure the cache directory and default extension
                ParamValueMetaData cacheDirectoryParam = new ParamValueMetaData();
                cacheDirectoryParam.setParamName( "cache.directory" );
                cacheDirectoryParam.setParamValue( phpAppMetaData.getStaticPathPrefix() );
                paramsList.add( cacheDirectoryParam );
                ParamValueMetaData cacheExtensionParam = new ParamValueMetaData();
                cacheExtensionParam.setParamName( "cache.extension" );
                cacheExtensionParam.setParamValue( ".html" );
                paramsList.add( cacheExtensionParam );
            }

            staticServlet.setInitParam( paramsList );
            servlets.add( staticServlet );

            ServletMappingMetaData staticMapping = new ServletMappingMetaData();
            staticMapping.setServletName( STATIC_RESROUCE_SERVLET_NAME );
            staticMapping.setUrlPatterns( Collections.singletonList( "/*" ) );

            servletMappings.add( staticMapping );
        }
    }

    protected void ensureSomeServlet(PhpMetaData phpAppMetaData, WebMetaData webMetaData) {
        ServletsMetaData servlets = webMetaData.getServlets();

        if (servlets.isEmpty()) {
            JBossServletMetaData fiveHundredServlet = new JBossServletMetaData();
            fiveHundredServlet.setServletClass( FIVE_HUNDRED_SERVLET_CLASS_NAME );
            fiveHundredServlet.setServletName( FIVE_HUNDRED_SERVLET_NAME );
            fiveHundredServlet.setId( FIVE_HUNDRED_SERVLET_NAME );
            servlets.add( fiveHundredServlet );

            ServletMappingMetaData fiveHundredMapping = new ServletMappingMetaData();
            fiveHundredMapping.setServletName( FIVE_HUNDRED_SERVLET_NAME );
            fiveHundredMapping.setUrlPatterns( Collections.singletonList( "/*" ) );

            List<ServletMappingMetaData> servletMappings = webMetaData.getServletMappings();
            servletMappings.add( fiveHundredMapping );
        }
    }

    protected void setUpPoolDependency(PhpMetaData phpAppMetaData, JBossWebMetaData jbossWebMetaData) {
        List<String> depends = jbossWebMetaData.getDepends();

        if (depends == null) {
            depends = new ArrayList<String>();
            jbossWebMetaData.setDepends( depends );
        }

        depends.add( phpAppMetaData.getPhpApplicationPoolName() );
    }

    protected MimeMappingMetaData createMimeMapping(String extension, String mimeType) {
        MimeMappingMetaData mapping = new MimeMappingMetaData();
        mapping.setExtension( extension );
        mapping.setMimeType( mimeType );
        return mapping;
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.web.php" );

}
