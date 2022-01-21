/*
 * Copyright 2013-2016 consulo.io
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
package consulo.psi.search.scope;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import consulo.project.Project;
import consulo.project.content.ProjectFileIndex;
import consulo.project.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.search.scope.packageSet.AbstractPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.Colored;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23:40/19.09.13
 */
@Colored(color = "e7fadb", darkVariant = "2A3B2C")
public class TestResourcesScope extends NamedScope {
  public static final String NAME = IdeBundle.message("predefined.scope.test.resources.name");

  public TestResourcesScope() {
    super(NAME, AllIcons.Modules.TestResourcesRoot, new AbstractPackageSet("test-rsc:*..*") {
      @Override
      public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
        final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        return file != null && index.isInTestResource(file);
      }
    });
  }

  @Nonnull
  @Override
  public Image getIconForProjectView() {
    return createOffsetIcon();
  }
}
