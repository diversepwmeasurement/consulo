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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangesViewRefresher;
import consulo.vcs.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFileManager;
import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author yole
 * @since 02.11.2006
 */
public class RefreshAction extends AnAction implements DumbAware {
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    doRefresh(project);
  }

  public static void doRefresh(final Project project) {
    if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;

    FileDocumentManager.getInstance().saveAllDocuments();
    invokeCustomRefreshes(project);

    VirtualFileManager.getInstance().asyncRefresh(new Runnable() {
      public void run() {
        // already called in EDT or under write action
        if (!project.isDisposed()) {
          VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
        }
      }
    });
  }

  private static void invokeCustomRefreshes(@Nonnull Project project) {
    List<ChangesViewRefresher> extensions = ChangesViewRefresher.EP_NAME.getExtensionList();
    for (ChangesViewRefresher refresher : extensions) {
      refresher.refresh(project);
    }
  }
}
