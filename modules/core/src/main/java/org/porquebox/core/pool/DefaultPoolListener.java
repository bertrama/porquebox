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

package org.porquebox.core.pool;


public class DefaultPoolListener<T> implements PoolListener<T> {

    @Override
    public void instanceBorrowed(T instance, int totalInstances, int availableNow) {
    }

    @Override
    public void instanceDrained(T instance, int totalInstances, int availableNow) {
    }

    @Override
    public void instanceFilled(T instance, int totalInstances, int availableNow) {
    }

    @Override
    public void instanceReleased(T instance, int totalInstances, int availableNow) {
    }

    @Override
    public void instanceRequested(int totalInstances, int availableNow) {
    }

}
