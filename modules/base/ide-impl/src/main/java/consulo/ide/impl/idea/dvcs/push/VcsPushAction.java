/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.dvcs.push;

import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.ide.impl.idea.dvcs.push.ui.VcsPushDialog;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.ServiceManager;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

public class VcsPushAction extends DumbAwareAction {

  @Nonnull
  private static Collection<Repository> collectRepositories(@Nonnull VcsRepositoryManager vcsRepositoryManager, @Nullable VirtualFile[] files) {
    if (files == null) return Collections.emptyList();
    Collection<Repository> repositories = ContainerUtil.newHashSet();
    for (VirtualFile file : files) {
      Repository repo = vcsRepositoryManager.getRepositoryForFile(file);
      if (repo != null) {
        repositories.add(repo);
      }
    }
    return repositories;
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    VcsRepositoryManager manager = ServiceManager.getService(project, VcsRepositoryManager.class);
    Collection<Repository> repositories = e.getData(CommonDataKeys.EDITOR) != null
                                          ? ContainerUtil.<Repository>emptyList()
                                          : collectRepositories(manager, e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));
    VirtualFile selectedFile = DvcsUtil.getSelectedFile(project);
    new VcsPushDialog(project, DvcsUtil.sortRepositories(repositories), selectedFile != null ? manager.getRepositoryForFile(selectedFile) : null).show();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    Project project = e.getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabledAndVisible(project != null && !ServiceManager.getService(project, VcsRepositoryManager.class).getRepositories().isEmpty());
  }
}
