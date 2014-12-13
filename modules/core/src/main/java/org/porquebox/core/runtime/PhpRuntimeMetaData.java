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

import java.io.File;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.AttachmentKey;

public class PhpRuntimeMetaData {
  public static final AttachmentKey<PhpRuntimeMetaData> ATTACHMENT_KEY = AttachmentKey.create(PhpRuntimeMetaData.class);

  public enum Version {
    V_5_4
  }
  public enum RuntimeType {
    BARE, DRUPAL, ZEND, SYMPHONY
  }
  public static final Version DEFAULT_VERSION = Version.V_5_4;


  private Map<String, String> environment;
  private boolean debug       = false;
  private boolean interactive = false;
  private boolean profileApi = false;
  private RuntimeType type;
  private File baseDir;
  private List<PhpLoadPathMetaData> loadPaths = new LinkedList<PhpLoadPathMetaData>();
  private RuntimeInitializer initializer;
  private RuntimePreparer preparer;

  public RuntimePreparer setRuntimePreparer(RuntimePreparer set) {
    return preparer = set;
  }

  public RuntimePreparer getRuntimePreparer() {
    return preparer;
  }

  public RuntimeInitializer setRuntimeInitializer(RuntimeInitializer set) {
    return initializer = set;
  }

  public RuntimeInitializer getRuntimeInitializer() {
    return initializer;
  }

  public List<PhpLoadPathMetaData> appendLoadPath( PhpLoadPathMetaData append ) {
    loadPaths.add(append);
    return loadPaths;
  }
  public List<PhpLoadPathMetaData> getLoadPaths() {
    return loadPaths;
  }

  public PhpRuntimeMetaData() {
  }
  public void setDebug(boolean set) {
    debug = set;
  }
  public boolean isDebug() {
    return debug;
  }
  public boolean setInteractive(boolean set) {
    return interactive = set;
  }
  public boolean isInteractive() {
    return interactive;
  }

  public void setProfileApi(boolean set) {
    profileApi = set;
  }
  public boolean isProfileApi() {
    return profileApi;
  }

  public RuntimeType getRuntimeType() {
    return type;
  }

  public RuntimeType setRuntimeType(RuntimeType set) {
    return type = set;
  }

  public File setBaseDir(File set) {
    return baseDir = set;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public Map<String, String> setEnvironment( Map<String, String> set) {
    return environment = set;
  }

  public Map<String, String> getEnvironment() {
    return environment;
  }

  // TODO: write getters and setters for the PHP RT MD.
}
