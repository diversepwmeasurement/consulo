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

import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import com.intellij.refactoring.copy.CopyHandler;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class CopyElementAction extends AnAction {
  @Override
  public boolean startInTransaction() {
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      @Override
      public void run() {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
      }
    }, "", null);
    final Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    PsiElement[] elements;

    PsiDirectory defaultTargetDirectory;
    if (editor != null) {
      PsiElement aElement = getTargetElement(editor, project);
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return;
      elements = new PsiElement[]{aElement};
      if (aElement == null || !CopyHandler.canCopy(elements)) {
        elements = new PsiElement[]{file};
      }
      defaultTargetDirectory = file.getContainingDirectory();
    }
    else {
      PsiElement element = dataContext.getData(LangDataKeys.TARGET_PSI_ELEMENT);
      defaultTargetDirectory = element instanceof PsiDirectory ? (PsiDirectory)element : null;
      elements = dataContext.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
    }
    doCopy(elements, defaultTargetDirectory);
  }

  protected void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
    CopyHandler.doCopy(elements, defaultTargetDirectory);
  }

  @Override
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    presentation.setEnabled(false);
    if (project == null) {
      return;
    }

    Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    if (editor != null) {
      updateForEditor(dataContext, presentation);
    }
    else {
      String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
      updateForToolWindow(id, dataContext, presentation);
    }
  }

  protected void updateForEditor(DataContext dataContext, Presentation presentation) {
    Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    if (editor == null) {
      presentation.setVisible(false);
      return;
    }

    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;

    }
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

    PsiElement element = getTargetElement(editor, project);
    boolean result = element != null && CopyHandler.canCopy(new PsiElement[]{element});

    if (!result && file != null) {
      result = CopyHandler.canCopy(new PsiElement[]{file});
    }

    presentation.setEnabled(result);
    presentation.setVisible(true);
  }

  protected void updateForToolWindow(String toolWindowId, DataContext dataContext, Presentation presentation) {
    PsiElement[] elements = dataContext.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
    presentation.setEnabled(elements != null && CopyHandler.canCopy(elements));
  }

  private static PsiElement getTargetElement(final Editor editor, final Project project) {
    int offset = editor.getCaretModel().getOffset();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;
    PsiElement element = file.findElementAt(offset);
    if (element == null) element = file;
    return element;
  }
}
