/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.components.impl;

import com.intellij.application.options.PathMacrosImpl;
import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.PathMacroUtil;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import com.intellij.util.containers.FactoryMap;
import consulo.security.impl.PrivilegedAction;
import org.jdom.Element;
import javax.annotation.Nullable;

import java.util.*;

public class BasePathMacroManager extends PathMacroManager {
  private PathMacrosImpl myPathMacros;

  public BasePathMacroManager(@Nullable PathMacros pathMacros) {
    myPathMacros = (PathMacrosImpl)pathMacros;
  }

  protected static void addFileHierarchyReplacements(ExpandMacroToPathMap result, String macroName, @javax.annotation.Nullable String path) {
    if (path == null) return;
    addFileHierarchyReplacements(result, getLocalFileSystem().findFileByPath(path), "$" + macroName + "$");
  }

  private static void addFileHierarchyReplacements(ExpandMacroToPathMap result, @Nullable VirtualFile f, String macro) {
    if (f == null) return;
    addFileHierarchyReplacements(result, f.getParent(), macro + "/..");
    result.put(macro, StringUtil.trimEnd(f.getPath(), "/"));
  }

  protected static void addFileHierarchyReplacements(ReplacePathToMacroMap result, String macroName, @javax.annotation.Nullable String path, @Nullable String stopAt) {
    if (path == null) return;

    String macro = "$" + macroName + "$";
    path = StringUtil.trimEnd(FileUtil.toSystemIndependentName(path), "/");
    boolean overwrite = true;
    while (StringUtil.isNotEmpty(path) && path.contains("/")) {
      result.addReplacement(path, macro, overwrite);

      if (path.equals(stopAt)) {
        break;
      }

      macro += "/..";
      overwrite = false;
      path = StringUtil.getPackageName(path, '/');
    }
  }

  private static VirtualFileSystem getLocalFileSystem() {
    // Use VFM directly because of mocks in tests.
    return VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
  }

  public ExpandMacroToPathMap getExpandMacroMap() {
    ExpandMacroToPathMap result = new ExpandMacroToPathMap();
    for (Map.Entry<String, String> entry : PathMacroUtil.getGlobalSystemMacros().entrySet()) {
      result.addMacroExpand(entry.getKey(), entry.getValue());
    }
    getPathMacros().addMacroExpands(result);
    return result;
  }

  protected ReplacePathToMacroMap getReplacePathMap() {
    ReplacePathToMacroMap result = new ReplacePathToMacroMap();
    for (Map.Entry<String, String> entry : PathMacroUtil.getGlobalSystemMacros().entrySet()) {
      result.addMacroReplacement(entry.getValue(), entry.getKey());
    }
    getPathMacros().addMacroReplacements(result);
    return result;
  }

  @Override
  public TrackingPathMacroSubstitutor createTrackingSubstitutor() {
    return new MyTrackingPathMacroSubstitutor();
  }

  @Override
  public String expandPath(final String path) {
    return getExpandMacroMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public String collapsePath(final String path) {
    return getReplacePathMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void collapsePathsRecursively(final Element element) {
    getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive, true);
  }

  @Override
  public String collapsePathsRecursively(final String text) {
    return getReplacePathMap().substituteRecursively(text, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void expandPaths(final Element element) {
    getExpandMacroMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void collapsePaths(final Element element) {
    getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
  }

  public PathMacrosImpl getPathMacros() {
    if (myPathMacros == null) {
      myPathMacros = (PathMacrosImpl)PathMacros.getInstance();
    }

    return myPathMacros;
  }

  private class MyTrackingPathMacroSubstitutor implements TrackingPathMacroSubstitutor {
    private final Map<String, Set<String>> myMacroToComponentNames = FactoryMap.create(k -> new HashSet<>());

    private final Map<String, Set<String>> myComponentNameToMacros = FactoryMap.create(k -> new HashSet<>());

    public MyTrackingPathMacroSubstitutor() {
    }

    @Override
    public void reset() {
      myMacroToComponentNames.clear();
      myComponentNameToMacros.clear();
    }

    @Override
    public String expandPath(final String path) {
      return getExpandMacroMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
    }

    @Override
    public String collapsePath(final String path) {
      return getReplacePathMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
    }

    @Override
    public void expandPaths(final Element element) {
      PrivilegedAction.runPrivilegedAction(() -> getExpandMacroMap().substitute(element, SystemInfo.isFileSystemCaseSensitive));
    }

    @Override
    public void collapsePaths(final Element element) {
      getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
    }

    public int hashCode() {
      return getExpandMacroMap().hashCode();
    }

    @Override
    public void invalidateUnknownMacros(final Set<String> macros) {
      for (final String macro : macros) {
        final Set<String> components = myMacroToComponentNames.get(macro);
        for (final String component : components) {
          myComponentNameToMacros.remove(component);
        }

        myMacroToComponentNames.remove(macro);
      }
    }

    @Override
    public Set<String> getComponents(final Collection<String> macros) {
      final Set<String> result = new HashSet<String>();
      for (String macro : myMacroToComponentNames.keySet()) {
        if (macros.contains(macro)) {
          result.addAll(myMacroToComponentNames.get(macro));
        }
      }

      return result;
    }

    @Override
    public Set<String> getUnknownMacros(final String componentName) {
      final Set<String> result = new HashSet<String>();
      result.addAll(componentName == null ? myMacroToComponentNames.keySet() : myComponentNameToMacros.get(componentName));
      return Collections.unmodifiableSet(result);
    }

    @Override
    public void addUnknownMacros(final String componentName, final Collection<String> unknownMacros) {
      if (unknownMacros.isEmpty()) return;
      
      for (String unknownMacro : unknownMacros) {
        final Set<String> stringList = myMacroToComponentNames.get(unknownMacro);
        stringList.add(componentName);
      }

      myComponentNameToMacros.get(componentName).addAll(unknownMacros);
    }
  }

  protected static boolean pathsEqual(@Nullable String path1, @javax.annotation.Nullable String path2) {
    return path1 != null && path2 != null &&
           FileUtil.pathsEqual(FileUtil.toSystemIndependentName(path1), FileUtil.toSystemIndependentName(path2));
  }
}
