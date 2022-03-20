/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.codeInsight.hint.HintManager;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.impl.action.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import consulo.document.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import consulo.document.util.DocumentUtil;
import consulo.annotation.access.RequiredWriteAction;

import javax.annotation.Nullable;

/**
 * @author yole
 */
public abstract class ConvertIndentsActionBase extends EditorAction {
  protected ConvertIndentsActionBase() {
    super(null);
    setupHandler(new Handler());
  }

  public static int convertIndentsToTabs(Document document, int tabSize, TextRange textRange) {
    return processIndents(document, tabSize, textRange, tabIndentBuilder);
  }

  public static int convertIndentsToSpaces(Document document, int tabSize, TextRange textRange) {
    return processIndents(document, tabSize, textRange, spaceIndentBuilder);
  }

  private interface IndentBuilder {
    String buildIndent(int length, int tabSize);
  }

  private static int processIndents(Document document, int tabSize, TextRange textRange, IndentBuilder indentBuilder) {
    int[] changedLines = {0};
    DocumentUtil.executeInBulk(document, true, () -> {
      int startLine = document.getLineNumber(textRange.getStartOffset());
      int endLine = document.getLineNumber(textRange.getEndOffset());
      for (int line = startLine; line <= endLine; line++) {
        int indent = 0;
        final int lineStart = document.getLineStartOffset(line);
        final int lineEnd = document.getLineEndOffset(line);
        int indentEnd = lineEnd;
        for(int offset = Math.max(lineStart, textRange.getStartOffset()); offset < lineEnd; offset++) {
          char c = document.getCharsSequence().charAt(offset);
          if (c == ' ') {
            indent++;
          }
          else if (c == '\t') {
            indent = ((indent / tabSize) + 1) * tabSize;
          }
          else {
            indentEnd = offset;
            break;
          }
        }
        if (indent > 0) {
          String oldIndent = document.getCharsSequence().subSequence(lineStart, indentEnd).toString();
          String newIndent = indentBuilder.buildIndent(indent, tabSize);
          if (!oldIndent.equals(newIndent)) {
            document.replaceString(lineStart, indentEnd, newIndent);
            changedLines[0]++;
          }
        }
      }
    });
    return changedLines[0];
  }

  private static IndentBuilder tabIndentBuilder = new IndentBuilder() {
    @Override
    public String buildIndent(int length, int tabSize) {
      return StringUtil.repeatSymbol('\t', length / tabSize) + StringUtil.repeatSymbol(' ', length % tabSize);
    }
  };

  private static IndentBuilder spaceIndentBuilder = new IndentBuilder() {
    @Override
    public String buildIndent(int length, int tabSize) {
      return StringUtil.repeatSymbol(' ', length);
    }
  };

  protected abstract int performAction(Editor editor, TextRange textRange);

  private class Handler extends EditorWriteActionHandler {
    @RequiredWriteAction
    @Override
    public void executeWriteAction(final Editor editor, @Nullable Caret caret, DataContext dataContext) {
      final SelectionModel selectionModel = editor.getSelectionModel();
      int changedLines = 0;
      if (selectionModel.hasSelection()) {
        changedLines = performAction(editor, new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd()));
      }
      else {
        changedLines += performAction(editor, new TextRange(0, editor.getDocument().getTextLength()));
      }
      if (changedLines == 0) {
        HintManager.getInstance().showInformationHint(editor, "All lines already have requested indentation");
      }
      else {
        HintManager.getInstance().showInformationHint(editor, "Changed indentation in " + changedLines + (changedLines == 1 ? " line" : " lines"));
      }
    }
  }
}
