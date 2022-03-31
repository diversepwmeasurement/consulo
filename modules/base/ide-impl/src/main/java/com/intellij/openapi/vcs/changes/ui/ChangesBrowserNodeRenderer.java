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
package com.intellij.openapi.vcs.changes.ui;

import consulo.project.Project;
import com.intellij.openapi.util.BooleanGetter;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkRenderer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.language.editor.ui.SpeedSearchUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class ChangesBrowserNodeRenderer extends ColoredTreeCellRenderer {

  @Nonnull
  private final BooleanGetter myShowFlatten;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final IssueLinkRenderer myIssueLinkRenderer;
  private final boolean myHighlightProblems;

  public ChangesBrowserNodeRenderer(@Nonnull Project project, @Nonnull BooleanGetter showFlattenGetter, boolean highlightProblems) {
    myShowFlatten = showFlattenGetter;
    myProject = project;
    myHighlightProblems = highlightProblems;
    myIssueLinkRenderer = new IssueLinkRenderer(project, this);
  }

  public boolean isShowFlatten() {
    return myShowFlatten.get();
  }

  public void customizeCellRenderer(@Nonnull JTree tree,
                                    Object value,
                                    boolean selected,
                                    boolean expanded,
                                    boolean leaf,
                                    int row,
                                    boolean hasFocus) {
    ChangesBrowserNode node = (ChangesBrowserNode)value;
    node.render(this, selected, expanded, hasFocus);
    SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, true, selected);
  }

  protected void appendFileName(@Nullable VirtualFile vFile, @Nonnull String fileName, Color color) {
    ChangesFileNameDecorator decorator = !myProject.isDefault() ? ChangesFileNameDecorator.getInstance(myProject) : null;

    if (decorator != null) {
      decorator.appendFileName(this, vFile, fileName, color, myHighlightProblems);
    }
    else {
      append(fileName, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color));
    }
  }

  public void appendTextWithIssueLinks(@Nonnull String text, @Nonnull SimpleTextAttributes baseStyle) {
    myIssueLinkRenderer.appendTextWithLinks(text, baseStyle);
  }
}
