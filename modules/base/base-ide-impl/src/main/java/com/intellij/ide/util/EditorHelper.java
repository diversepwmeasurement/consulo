/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.ide.util;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EditorHelper {
  public static <T extends PsiElement> void openFilesInEditor(@Nonnull T[] elements) {
    final int limit = UISettings.getInstance().getEditorTabLimit();
    final int max = Math.min(limit, elements.length);
    for (int i = 0; i < max; i++) {
      openInEditor(elements[i], true);
    }
  }

  public static Editor openInEditor(@Nonnull PsiElement element) {
    FileEditor editor = openInEditor(element, true);
    return editor instanceof TextEditor ? ((TextEditor)editor).getEditor() : null;
  }

  @Nullable
  public static FileEditor openInEditor(@Nonnull PsiElement element, boolean switchToText) {
    PsiFile file;
    int offset;
    if (element instanceof PsiFile){
      file = (PsiFile)element;
      offset = -1;
    }
    else{
      file = element.getContainingFile();
      offset = element.getTextOffset();
    }
    if (file == null) return null;//SCR44414
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    OpenFileDescriptor descriptor = new OpenFileDescriptor(element.getProject(), virtualFile, offset);
    Project project = element.getProject();
    if (offset == -1 && !switchToText) {
      FileEditorManager.getInstance(project).openEditor(descriptor, false);
    }
    else {
      FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
    }
    return FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
  }
}