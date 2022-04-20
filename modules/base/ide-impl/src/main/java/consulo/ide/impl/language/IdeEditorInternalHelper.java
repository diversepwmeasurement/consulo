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
package consulo.ide.impl.language;

import com.intellij.codeStyle.CodeStyleFacade;
import com.intellij.openapi.editor.impl.EditorHighlighterCache;
import com.intellij.ui.EditorNotifications;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.codeEditor.internal.EditorInternalHelper;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.fileEditor.FileEditorManager;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.editor.action.LanguageWordBoundaryFilter;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@Singleton
public class IdeEditorInternalHelper implements EditorInternalHelper {
  private static class FileEditorAffectCaretContext extends CaretDataContext {

    public FileEditorAffectCaretContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
      super(delegate, caret);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@Nonnull Key<T> dataId) {
      Project project = super.getData(Project.KEY);
      if (project != null) {
        FileEditorManager fm = FileEditorManager.getInstance(project);
        if (fm != null) {
          Object data = fm.getData(dataId, myCaret.getEditor(), myCaret);
          if (data != null) return (T)data;
        }
      }
      return super.getData(dataId);
    }
  }

  @Nullable
  @Override
  public String getProperIndent(Project project, Document document, int offset) {
    PsiDocumentManager.getInstance(project).commitDocument(document); // Sync document and PSI before formatting.
    return offset >= document.getTextLength() ? "" : CodeStyleFacade.getInstance(project).getLineIndent(document, offset);
  }

  @Nonnull
  @Override
  public CaretDataContext createCaretDataContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
    return new FileEditorAffectCaretContext(delegate, caret);
  }

  @Override
  public boolean ensureInjectionUpToDate(@Nonnull Caret hostCaret) {
    Editor editor = hostCaret.getEditor();
    Project project = editor.getProject();
    if (project != null && InjectedLanguageManager.getInstance(project).mightHaveInjectedFragmentAtOffset(editor.getDocument(), hostCaret.getOffset())) {
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
      return true;
    }
    return false;
  }

  @Override
  public boolean isLexemeBoundary(@Nullable Object left, @Nullable Object right) {
    IElementType leftTokenType = (IElementType)left;
    IElementType rightTokenType = (IElementType)right;
    return leftTokenType != null && rightTokenType != null && LanguageWordBoundaryFilter.INSTANCE.forLanguage(rightTokenType.getLanguage()).isWordBoundary(leftTokenType, rightTokenType);
  }

  @Override
  public void rememberEditorHighlighterForCachesOptimization(Document document, @Nonnull EditorHighlighter highlighter) {
    if (!(highlighter instanceof EmptyEditorHighlighter)) {
      EditorHighlighterCache.rememberEditorHighlighterForCachesOptimization(document, highlighter);
    }
  }

  @Override
  public void updateNotifications(Project project, VirtualFile file) {
    EditorNotifications.getInstance(project).updateNotifications(file);
  }

  @Override
  public boolean shouldUseSmartTabs(Project project, @Nonnull Editor editor) {
    if (!(editor instanceof EditorEx)) return false;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    return CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(file).SMART_TABS;
  }

  @Override
  public int calcColumnNumber(@Nullable Editor editor, @Nonnull CharSequence text, int start, int offset, int tabSize) {
    return EditorImplUtil.calcColumnNumber(editor, text, start, offset, tabSize);
  }

  @Override
  public int getLastVisualLineColumnNumber(@Nonnull Editor editor, int line) {
    return EditorImplUtil.getLastVisualLineColumnNumber(editor, line);
  }

  @Override
  public int getSpaceWidth(@Nonnull Editor editor) {
    return EditorImplUtil.getSpaceWidth(Font.PLAIN, editor);
  }
}
