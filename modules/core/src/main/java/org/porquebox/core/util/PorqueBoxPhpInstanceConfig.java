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

package org.porquebox.core.util;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.InputStream;
import java.io.OutputStream;
import org.porquebox.core.runtime.PhpLoadPathMetaData;
import com.caucho.quercus.QuercusEngine;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Value;

public class PorqueBoxPhpInstanceConfig {
  public PorqueBoxPhpInstanceConfig() {
  }
  public PorqueBoxPhpInstanceConfig(QuercusContext context) {
    importContext(context);
  }
  public PorqueBoxPhpInstanceConfig(QuercusEngine engine) {
    importContext(engine.getQuercus());
  }

  private void importContext(QuercusContext context) {
    ini        = context.getIniMap(true);
    compile    = context.isCompile();
    strict     = context.isStrict();
    profileApi = context.isProfile();
    loose      = context.isLooseParse();
  }

  public ClassLoader setClassLoader( ClassLoader set ) {
    return loader = set;
  }

   public boolean setDebug(boolean set) {
     return debug = set;
   }

  public boolean setInteractive(boolean set) {
    return interactive = set;
  }

  public Map<String, String> setEnvironment(Map<String, String> set) {
    return environment = set;
  }

  public InputStream setInput(InputStream set) {
    return input = set;
  }

  public OutputStream setOutput(OutputStream set) {
    return output = set;
  }

  public OutputStream setError(OutputStream set) {
    return error = set;
  }

  public ClassLoader setLoader(ClassLoader set) {
    return loader = set;
  }

  public List<PhpLoadPathMetaData> setLoadPaths(List<String> set) {
    loadPaths.clear();
    for (String path : set) {
      loadPaths.add(new PhpLoadPathMetaData(new File(path)));
    }
    return loadPaths;
  }

  public String getCompileMode() {
    return compileMode;
  }

  // TODO: Implement compileMode.
  private String compileMode  = "QuercusCompileMode";
  private ClassLoader loader  = null;
  private boolean debug       = false;
  private boolean compile     = false;
  private boolean strict      = false;
  private boolean loose       = false;
  private boolean profileApi  = false;
  private boolean interactive = false;
  private InputStream input   = null;
  private OutputStream output = null;
  private OutputStream error   = null;
  private Map<String, Value> ini = new HashMap<String, Value>();
  private List<PhpLoadPathMetaData> loadPaths = new LinkedList<PhpLoadPathMetaData>();
  private Map<String, String> environment = new HashMap<String, String>();
}
