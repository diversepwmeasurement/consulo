/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 6:49:27 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.project.Project;

import javax.annotation.Nullable;

public class CodeBlockEndWithSelectionAction extends EditorAction {
  public CodeBlockEndWithSelectionAction() {
    super(new Handler());
    setInjectedContext(true);
  }

  private static class Handler extends EditorActionHandler {
    public Handler() {
      super(true);
    }

    @Override
    public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project != null) {
        CodeBlockUtil.moveCaretToCodeBlockEnd(project, editor, true);
      }
    }
  }
}
