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
package com.intellij.psi.search.scope;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.search.scope.packageSet.AbstractPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.Colored;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@Colored(color = "e7fadb", darkVariant = "2A3B2C")
public class TestsScope extends NamedScope {
  public static final String NAME = IdeBundle.message("predefined.scope.tests.name");
  public TestsScope() {
    super(NAME, AllIcons.Modules.TestRoot, new AbstractPackageSet("test:*..*") {
      @Override
      public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
        return file != null && TestSourcesFilter.isTestSources(file, project);
      }
    });
  }

  @Override
  public String getDefaultColorName() {
    return "Green";
  }

  @Nonnull
  @Override
  public Image getIconForProjectView() {
    return createOffsetIcon();
  }
}
