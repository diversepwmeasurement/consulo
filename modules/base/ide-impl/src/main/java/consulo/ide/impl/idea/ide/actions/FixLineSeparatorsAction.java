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
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class FixLineSeparatorsAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    final VirtualFile[] vFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    if (project == null || vFiles == null) return;
    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      public void run() {
        for (VirtualFile vFile : vFiles) {
          fixSeparators(vFile);
        }
      }
    }, "fixing line separators", null);
  }

  private static void fixSeparators(VirtualFile vFile) {
    VfsUtilCore.visitChildrenRecursively(vFile, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (!file.isDirectory() && !file.getFileType().isBinary()) {
          final Document document = FileDocumentManager.getInstance().getDocument(file);
          if (areSeparatorsBroken(document)) {
            fixSeparators(document);
          }
        }
        return true;
      }
    });
  }

  private static boolean areSeparatorsBroken(Document document) {
    final int count = document.getLineCount();
    for (int i = 1; i < count; i += 2) {
      if (document.getLineStartOffset(i) != document.getLineEndOffset(i)) {
        return false;
      }
    }
    return true;    
  }

  private static void fixSeparators(final Document document) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        int i = 1;
        while(i < document.getLineCount()) {
          final int start = document.getLineEndOffset(i);
          final int end = document.getLineEndOffset(i) + document.getLineSeparatorLength(i);
          document.deleteString(start, end);
          i++;
        }
      }
    });
  }
}
