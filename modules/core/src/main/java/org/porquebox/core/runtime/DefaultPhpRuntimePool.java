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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.pool.ManagedPool;

/**
 * Php interpreter pool of discrete, non-shared interpreters.
 * 
 * <p>
 * This pool supports minimum and maximum sizes, and ensures each client gets a
 * unique interpreter.
 * </p>
 * 
 * @author Bob McWhirter <bmcwhirt@redhat.com>
 * 
 */
public class DefaultPhpRuntimePool extends ManagedPool<QuercusEngine> implements PhpRuntimePool, BasicPhpRuntimePoolMBean {

    /**
     * Construct with a factory.
     * 
     * @param factory
     *            The factory to create interpreters.
     */
    public DefaultPhpRuntimePool(PhpRuntimeFactory factory) {
        super( factory );
    }
    
    public DefaultPhpRuntimePool() {
    }

    @Override
    public QuercusEngine borrowRuntime(String requester) throws Exception {
        return borrowInstance(requester);
    }

    @Override
    public void returnRuntime(QuercusEngine runtime) {
        releaseInstance( runtime );
    }

    /**
     * Retrieve the interpreter factory.
     * 
     * @return The interpreter factory.
     */
    public PhpRuntimeFactory getPhpRuntimeFactory() {
        return (PhpRuntimeFactory) getInstanceFactory();
    }

    @Override
    public Object evaluate(String code) throws Exception {
        QuercusEngine runtime = null;
        
        try {
            runtime = borrowRuntime( "anonymous-evaluate" );
            return runtime.execute(code);
        } finally {
            if ( runtime != null ) {
                returnRuntime( runtime );
            }
        }
    }
    
    private Set<String> collectNames(Collection<QuercusEngine> instances) {
        Set<String> names = new HashSet<String>();
        
        for ( QuercusEngine each : instances ) {
            names.add( "" + each.hashCode() );
        }
        
        return names;
    }

    @Override
    public Set<String> getAllRuntimeNames() {
        return collectNames( getAllInstances() );
    }

    @Override
    public PhpRuntimePool duplicate() {
        DefaultPhpRuntimePool duplicate = new DefaultPhpRuntimePool( getPhpRuntimeFactory() );
        duplicate.setMinimumInstances( getMinimumInstances() );
        duplicate.setMaximumInstances( getMaximumInstances() );
        duplicate.setName( getName() );
        duplicate.setDeferUntilRequested( isDeferredUntilRequested() );
        duplicate.setNamespaceContextSelector( getNamespaceContextSelector() );
        return duplicate;
    }

}
