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
package consulo.ide.impl.idea.openapi.vcs.ex;

import consulo.ide.impl.idea.openapi.vcs.VcsShowConfirmationOptionImpl;
import consulo.ide.impl.idea.openapi.vcs.VcsShowOptionsSettingImpl;
import consulo.ide.impl.idea.openapi.vcs.update.UpdateInfoTree;
import consulo.project.Project;
import consulo.ui.ex.content.ContentManager;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.versionControlSystem.update.UpdatedFiles;

import jakarta.annotation.Nonnull;
import java.util.List;

public abstract class ProjectLevelVcsManagerEx extends ProjectLevelVcsManager {
  public static ProjectLevelVcsManagerEx getInstanceEx(Project project) {
    return (ProjectLevelVcsManagerEx)project.getComponent(ProjectLevelVcsManager.class);
  }

  public abstract ContentManager getContentManager();

  @Nonnull
  public abstract VcsShowConfirmationOptionImpl getConfirmation(VcsConfiguration.StandardConfirmation option);

  public abstract List<VcsShowOptionsSettingImpl> getAllOptions();

  public abstract List<VcsShowConfirmationOptionImpl> getAllConfirmations();

  public abstract void notifyDirectoryMappingChanged();

  public abstract UpdateInfoTree showUpdateProjectInfo(UpdatedFiles updatedFiles,
                                                       String displayActionName,
                                                       ActionInfo actionInfo,
                                                       boolean canceled);

  public abstract void fireDirectoryMappingsChanged();

  public abstract String haveDefaultMapping();
}
