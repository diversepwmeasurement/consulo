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

package com.intellij.codeEditor.printing;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.PsiFile;

public class PrintAction extends AnAction implements DumbAware {
  public PrintAction() {
    super();

  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    PrintManager.executePrint(dataContext);
  }

  @Override
  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    VirtualFile file = dataContext.getData(PlatformDataKeys.VIRTUAL_FILE);
    if(file != null && file.isDirectory()) {
      presentation.setEnabled(true);
      return;
    }
    Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = dataContext.getData(LangDataKeys.PSI_FILE);
    presentation.setEnabled(psiFile != null || editor != null);
  }

}