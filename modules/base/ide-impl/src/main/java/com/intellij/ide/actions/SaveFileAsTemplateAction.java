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

package com.intellij.ide.actions;

import com.intellij.ide.fileTemplates.impl.AllFileTemplatesConfigurable;
import com.intellij.ide.fileTemplates.ui.ConfigureTemplatesDialog;
import consulo.component.extension.Extensions;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

public class SaveFileAsTemplateAction extends AnAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e){
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    String fileText = e.getData(PlatformDataKeys.FILE_TEXT);
    VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    String extension = file.getExtension();
    String nameWithoutExtension = file.getNameWithoutExtension();
    AllFileTemplatesConfigurable fileTemplateOptions = new AllFileTemplatesConfigurable(project);
    ConfigureTemplatesDialog dialog = new ConfigureTemplatesDialog(project, fileTemplateOptions);
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    for(SaveFileAsTemplateHandler handler: Extensions.getExtensions(SaveFileAsTemplateHandler.EP_NAME)) {
      String textFromHandler = handler.getTemplateText(psiFile, fileText, nameWithoutExtension);
      if (textFromHandler != null) {
        fileText = textFromHandler;
        break;
      }
    }
    fileTemplateOptions.createNewTemplate(nameWithoutExtension, extension, fileText);
    dialog.show();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    String fileText = e.getData(PlatformDataKeys.FILE_TEXT);
    e.getPresentation().setEnabled((fileText != null) && (file != null));
  }
}
