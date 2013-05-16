/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.task.ui;

import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author Denis Zhdanov
 * @since 5/13/13 4:18 PM
 */
public class ExternalSystemTasksTree extends Tree {

  private static final int COLLAPSE_STATE_PROCESSING_DELAY_MILLIS = 200;

  @NotNull private static final Comparator<TreePath> PATH_COMPARATOR = new Comparator<TreePath>() {
    @Override
    public int compare(TreePath o1, TreePath o2) {
      return o2.getPathCount() - o1.getPathCount();
    }
  };
  
  @NotNull private final Alarm myCollapseStateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  /** Holds list of paths which 'expand/collapse' state should be restored. */
  @NotNull private final Set<TreePath> myPathsToProcessCollapseState = ContainerUtilRt.newHashSet();
  
  @NotNull private final Map<String/*tree path*/, Boolean/*expanded*/> myExpandedStateHolder;

  private boolean mySuppressCollapseTracking;

  public ExternalSystemTasksTree(@NotNull ExternalSystemTasksTreeModel model,
                                 @NotNull Map<String/*tree path*/, Boolean/*expanded*/> expandedStateHolder)
  {
    super(model);
    myExpandedStateHolder = expandedStateHolder;
    setRootVisible(false);

    model.addTreeModelListener(new TreeModelAdapter() {
      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        scheduleCollapseStateAppliance(e.getTreePath());
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
        scheduleCollapseStateAppliance(e.getTreePath());
      }
    });
  }

  /**
   * Schedules 'collapse/expand' state restoring for the given path. We can't do that immediately from the tree model listener
   * as there is a possible case that other listeners have not been notified about the model state change, hence, attempt to define
   * 'collapse/expand' state may bring us to the inconsistent state.
   *
   * @param path  target path
   */
  private void scheduleCollapseStateAppliance(@NotNull TreePath path) {
    myPathsToProcessCollapseState.add(path);
    myCollapseStateAlarm.cancelAllRequests();
    myCollapseStateAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        // We assume that the paths collection is modified only from the EDT, so, ConcurrentModificationException doesn't have
        // a chance.
        // Another thing is that we sort the paths in order to process the longest first. That is related to the JTree specifics
        // that it automatically expands parent paths on child path expansion.
        List<TreePath> paths = new ArrayList<TreePath>(myPathsToProcessCollapseState);
        myPathsToProcessCollapseState.clear();
        Collections.sort(paths, PATH_COMPARATOR);
        for (TreePath treePath : paths) {
          applyCollapseState(treePath);
        }
        final TreePath rootPath = new TreePath(getModel().getRoot());
        if (isCollapsed(rootPath)) {
          expandPath(rootPath);
        }
      }
    }, COLLAPSE_STATE_PROCESSING_DELAY_MILLIS);
  }

  /**
   * Applies stored 'collapse/expand' state to the node located at the given path.
   *
   * @param path  target path
   */
  private void applyCollapseState(@NotNull TreePath path) {
    final String key = getPath(path);
    final Boolean expanded = myExpandedStateHolder.get(key);
    if (expanded == null) {
      return;
    }
    boolean s = mySuppressCollapseTracking;
    mySuppressCollapseTracking = true;
    try {
      if (expanded) {
        expandPath(path);
      }
      else {
        collapsePath(path);
      }
    }
    finally {
      mySuppressCollapseTracking = s;
    }
  }

  @NotNull
  private static String getPath(@NotNull TreePath path) {
    StringBuilder buffer = new StringBuilder();
    for (TreePath current = path; current != null; current = current.getParentPath()) {
      buffer.append(current.getLastPathComponent().toString()).append('/');
    }
    buffer.setLength(buffer.length() - 1);
    return buffer.toString();
  }

}