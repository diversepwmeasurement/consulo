// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.todo.nodes.BaseToDoNode;
import com.intellij.ide.todo.nodes.SummaryNode;
import com.intellij.ide.todo.nodes.ToDoRootNode;
import com.intellij.ide.todo.nodes.TodoTreeHelper;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.ui.tree.AbstractTreeNodeVisitor;
import javax.annotation.Nonnull;

import java.util.function.Supplier;


class TodoNodeVisitor extends AbstractTreeNodeVisitor<Object> {
  private final VirtualFile myFile;

  TodoNodeVisitor(@Nonnull Supplier<Object> supplier, VirtualFile file) {
    super(supplier, null);
    myFile = file;
  }

  @Override
  protected boolean contains(@Nonnull AbstractTreeNode node, @Nonnull Object element) {
    if (node instanceof SummaryNode || node instanceof ToDoRootNode) return true;
    if (node instanceof ProjectViewNode) {
      if (myFile == null) {
        return TodoTreeHelper.contains((ProjectViewNode)node, element);
      }
    }
    return node instanceof BaseToDoNode && ((BaseToDoNode)node).contains(element) || node instanceof ProjectViewNode && ((ProjectViewNode)node).contains(myFile);
  }
}
