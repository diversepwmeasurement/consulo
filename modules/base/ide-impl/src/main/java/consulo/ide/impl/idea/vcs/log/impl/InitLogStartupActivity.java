/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.vcs.log.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.component.messagebus.MessageBusConnection;
import consulo.vcs.ProjectLevelVcsManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

/**
* @author VISTALL
* @since 29-Jun-22
*/
@ExtensionImpl
public class InitLogStartupActivity implements PostStartupActivity, DumbAware {
  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    VcsProjectLog projectLog = VcsProjectLog.getInstance(project);

    MessageBusConnection connection = project.getMessageBus().connect(project);
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, projectLog::recreateLog);
    if (projectLog.hasDvcsRoots()) {
      ApplicationManager.getApplication().invokeLater(projectLog::createLog);
    }
  }
}
