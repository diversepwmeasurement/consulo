/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ui.tabs;

import consulo.fileEditor.EditorTabColorProvider;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.ui.FileColorManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author spleaner
 */
public class EditorTabColorProviderImpl implements EditorTabColorProvider, DumbAware {

  @Override
  @Nullable
  public Color getEditorTabColor(Project project, VirtualFile file) {
    FileColorManager colorManager = FileColorManager.getInstance(project);
    return colorManager.isEnabledForTabs() ? colorManager.getFileColor(file) : null;
  }

  @Nullable
  @Override
  public Color getProjectViewColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    FileColorManager colorManager = FileColorManager.getInstance(project);
    return colorManager.isEnabledForProjectView() ? colorManager.getFileColor(file) : null;
  }
}
