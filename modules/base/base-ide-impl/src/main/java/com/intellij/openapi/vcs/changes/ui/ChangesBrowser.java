/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public class ChangesBrowser extends ChangesBrowserBase<Change> {

  public ChangesBrowser(Project project,
                        List<? extends ChangeList> changeLists,
                        List<Change> changes,
                        ChangeList initialListSelection,
                        boolean capableOfExcludingChanges,
                        boolean highlightProblems,
                        @Nullable Runnable inclusionListener,
                        MyUseCase useCase, @Nullable VirtualFile toSelect) {
    super(project, changes, capableOfExcludingChanges, highlightProblems, inclusionListener, useCase, toSelect, Change.class);

    init();
    setInitialSelection(changeLists, changes, initialListSelection);
    rebuildList();
  }

  @Nonnull
  protected DefaultTreeModel buildTreeModel(final List<Change> changes, ChangeNodeDecorator changeNodeDecorator, boolean showFlatten) {
    TreeModelBuilder builder = new TreeModelBuilder(myProject, showFlatten);
    return builder.buildModel(changes, changeNodeDecorator);
  }

  @Nonnull
  protected List<Change> getSelectedObjects(@Nonnull final ChangesBrowserNode<Change> node) {
    return node.getAllChangesUnder();
  }

  @Nullable
  protected Change getLeadSelectedObject(@Nonnull final ChangesBrowserNode node) {
    final Object o = node.getUserObject();
    if (o instanceof Change) {
      return (Change)o;
    }
    return null;
  }

  @Nonnull
  @Override
  public List<Change> getSelectedChanges() {
    return myViewer.getSelectedChanges();
  }

  @Nonnull
  @Override
  public List<Change> getAllChanges() {
    return myViewer.getChanges();
  }

  @Nonnull
  @Override
  public List<Change> getCurrentDisplayedChanges() {
    return myChangesToDisplay != null ? myChangesToDisplay : super.getCurrentDisplayedChanges();
  }

  @Nonnull
  @Override
  public List<Change> getCurrentIncludedChanges() {
    return ContainerUtil.newArrayList(myViewer.getIncludedChanges());
  }

  @Nonnull
  @Override
  public List<Change> getCurrentDisplayedObjects() {
    return getCurrentDisplayedChanges();
  }

  public enum MyUseCase {
    LOCAL_CHANGES,
    COMMITTED_CHANGES
  }
}
