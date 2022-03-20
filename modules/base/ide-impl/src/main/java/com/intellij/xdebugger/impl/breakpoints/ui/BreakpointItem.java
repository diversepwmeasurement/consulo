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
package com.intellij.xdebugger.impl.breakpoints.ui;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsManager;
import consulo.codeEditor.impl.DocumentMarkupModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.navigation.Navigatable;
import com.intellij.ui.ColoredListCellRenderer;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.SimpleColoredComponent;
import com.intellij.ui.popup.util.DetailView;
import com.intellij.ui.popup.util.ItemWrapper;
import consulo.debugger.ui.DebuggerColors;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.swing.*;

public abstract class BreakpointItem extends ItemWrapper implements Comparable<BreakpointItem>, Navigatable {
  public static final Key<Object> EDITOR_ONLY = Key.create("EditorOnly");

  public abstract void saveState();

  public abstract Object getBreakpoint();

  public abstract boolean isEnabled();

  public abstract void setEnabled(boolean state);

  public abstract boolean isDefaultBreakpoint();

  protected static void showInEditor(DetailView panel, VirtualFile virtualFile, int line) {
    TextAttributes attributes =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(DebuggerColors.BREAKPOINT_ATTRIBUTES);

    DetailView.PreviewEditorState state = DetailView.PreviewEditorState.create(virtualFile, line, attributes);

    if (state.equals(panel.getEditorState())) {
      return;
    }

    panel.navigateInPreviewEditor(state);

    TextAttributes softerAttributes = attributes.clone();
    ColorValue backgroundColor = softerAttributes.getBackgroundColor();
    if (backgroundColor != null) {
      // FIXME [VISTALL] softer is not supported softerAttributes.setBackgroundColor(ColorUtil.softer(backgroundColor));
    }

    final Editor editor = panel.getEditor();
    final MarkupModel editorModel = editor.getMarkupModel();
    final MarkupModel documentModel =
            DocumentMarkupModel.forDocument(editor.getDocument(), editor.getProject(), false);

    for (RangeHighlighter highlighter : documentModel.getAllHighlighters()) {
      if (highlighter.getUserData(DebuggerColors.BREAKPOINT_HIGHLIGHTER_KEY) == Boolean.TRUE) {
        final int line1 = editor.offsetToLogicalPosition(highlighter.getStartOffset()).line;
        if (line1 != line) {
          editorModel.addLineHighlighter(line1,
                                         DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER + 1, softerAttributes);
        }
      }
    }
  }

  @Override
  public void updateAccessoryView(JComponent component) {
    final JCheckBox checkBox = (JCheckBox)component;
    checkBox.setSelected(isEnabled());
  }

  @Override
  public void setupRenderer(ColoredListCellRenderer renderer, Project project, boolean selected) {
    setupGenericRenderer(renderer, true);
  }

  @Override
  public void setupRenderer(ColoredTreeCellRenderer renderer, Project project, boolean selected) {
    boolean plainView = renderer.getTree().getClientProperty("plainView") != null;
    setupGenericRenderer(renderer, plainView);
  }


  public abstract void setupGenericRenderer(SimpleColoredComponent renderer, boolean plainView);

  public abstract Image getIcon();

  public abstract String getDisplayText();

  protected void dispose() {}

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BreakpointItem item = (BreakpointItem)o;

    if (getBreakpoint() != null ? !getBreakpoint().equals(item.getBreakpoint()) : item.getBreakpoint() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getBreakpoint() != null ? getBreakpoint().hashCode() : 0;
  }
}
