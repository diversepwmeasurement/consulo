/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.tools.holders;

import com.intellij.diff.DiffContext;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.util.DiffUtil;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorEx;
import consulo.project.Project;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.FocusListener;

public class TextEditorHolder extends EditorHolder {
  @Nonnull
  protected final EditorEx myEditor;

  public TextEditorHolder(@Nonnull EditorEx editor) {
    myEditor = editor;
  }

  @Nonnull
  public EditorEx getEditor() {
    return myEditor;
  }

  @Override
  public void dispose() {
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myEditor.getComponent();
  }

  @Override
  public void installFocusListener(@Nonnull FocusListener listener) {
    myEditor.getContentComponent().addFocusListener(listener);
  }

  @javax.annotation.Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myEditor.getContentComponent();
  }

  //
  // Build
  //

  @Nonnull
  public static TextEditorHolder create(@javax.annotation.Nullable Project project, @Nonnull DocumentContent content) {
    EditorEx editor = DiffUtil.createEditor(content.getDocument(), project, false, true);
    DiffUtil.configureEditor(editor, content, project);
    return new TextEditorHolder(editor);
  }

  public static class TextEditorHolderFactory extends EditorHolderFactory<TextEditorHolder> {
    public static TextEditorHolderFactory INSTANCE = new TextEditorHolderFactory();

    @Override
    @Nonnull
    public TextEditorHolder create(@Nonnull DiffContent content, @Nonnull DiffContext context) {
      return TextEditorHolder.create(context.getProject(), (DocumentContent)content);
    }

    @Override
    public boolean canShowContent(@Nonnull DiffContent content, @Nonnull DiffContext context) {
      if (content instanceof DocumentContent) return true;
      return false;
    }

    @Override
    public boolean wantShowContent(@Nonnull DiffContent content, @Nonnull DiffContext context) {
      if (content instanceof DocumentContent) return true;
      return false;
    }
  }
}
