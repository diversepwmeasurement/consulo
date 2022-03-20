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
 * Date: May 13, 2002
 * Time: 9:51:34 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.codeEditor.EditorBundle;
import consulo.codeEditor.impl.EditorActionUtil;
import consulo.dataContext.DataContext;
import consulo.undoRedo.CommandProcessor;
import com.intellij.openapi.editor.*;
import consulo.codeEditor.impl.action.EditorAction;
import consulo.codeEditor.action.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import consulo.codeEditor.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUIUtil;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TabAction extends EditorAction {
  public TabAction() {
    super(new Handler());
    setInjectedContext(true);
  }

  private static class Handler extends EditorWriteActionHandler {
    public Handler() {
      super(true);
    }

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
      if (caret == null) {
        caret = editor.getCaretModel().getPrimaryCaret();
      }
      CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.EDIT_COMMAND_GROUP);
      CommandProcessor.getInstance().setCurrentCommandName(EditorBundle.message("typing.command.name"));
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      insertTabAtCaret(editor, caret, project);
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
      return !editor.isOneLineMode() && !((EditorEx)editor).isEmbeddedIntoDialogWrapper() && !editor.isViewer();
    }
  }

  private static void insertTabAtCaret(Editor editor, @Nonnull Caret caret, @Nullable Project project) {
    EditorUIUtil.hideCursorInEditor(editor);
    int columnNumber;
    if (caret.hasSelection()) {
      columnNumber = editor.visualToLogicalPosition(caret.getSelectionStartPosition()).column;
    }
    else {
      columnNumber = editor.getCaretModel().getLogicalPosition().column;
    }

    CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(project);

    final Document doc = editor.getDocument();
    CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptionsByDocument(project, doc);

    int tabSize = indentOptions.INDENT_SIZE;
    int spacesToAddCount = tabSize - columnNumber % Math.max(1,tabSize);

    boolean useTab = editor.getSettings().isUseTabCharacter(project);

    CharSequence chars = doc.getCharsSequence();
    if (useTab && indentOptions.SMART_TABS) {
      int offset = editor.getCaretModel().getOffset();
      while (offset > 0) {
        offset--;
        if (chars.charAt(offset) == '\t') continue;
        if (chars.charAt(offset) == '\n') break;
        useTab = false;
        break;
      }
    }

    doc.startGuardedBlockChecking();
    try {
      EditorModificationUtil.insertStringAtCaret(editor, useTab ? "\t" : StringUtil.repeatSymbol(' ', spacesToAddCount), false, true);
    }
    catch (ReadOnlyFragmentModificationException e) {
      EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
    }
    finally {
      doc.stopGuardedBlockChecking();
    }
  }
}
