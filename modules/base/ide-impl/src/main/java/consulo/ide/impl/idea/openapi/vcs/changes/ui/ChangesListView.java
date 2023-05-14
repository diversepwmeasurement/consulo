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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.awt.dnd.DnDAware;
import consulo.ui.ex.awt.tree.TreeState;
import consulo.ide.impl.idea.openapi.fileChooser.actions.VirtualFileDeleteProvider;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.*;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TreeLinkMouseListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ide.impl.idea.ui.SmartExpander;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.EditSourceOnDoubleClickHandler;
import consulo.ide.impl.idea.util.EditSourceOnEnterKeyHandler;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.versionControlSystem.util.VcsUtil;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static consulo.versionControlSystem.change.ChangesUtil.getAfterRevisionsFiles;
import static consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowserNode.*;
import static consulo.ide.impl.idea.util.containers.UtilKt.getIfSingle;
import static consulo.ide.impl.idea.util.containers.UtilKt.stream;
import static java.util.stream.Collectors.toList;

// TODO: Check if we could extend DnDAwareTree here instead of directly implementing DnDAware
public class ChangesListView extends Tree implements TypeSafeDataProvider, DnDAware {
  private final Project myProject;
  private boolean myShowFlatten = false;
  private final CopyProvider myCopyProvider;

  @NonNls public static final String HELP_ID = "ideaInterface.changes";
  @NonNls public static final Key<Stream<VirtualFile>> UNVERSIONED_FILES_DATA_KEY = Key.create("ChangeListView.UnversionedFiles");
  @NonNls public static final Key<Stream<VirtualFile>> IGNORED_FILES_DATA_KEY = Key.create("ChangeListView.IgnoredFiles");
  @NonNls public static final Key<List<FilePath>> MISSING_FILES_DATA_KEY = Key.create("ChangeListView.MissingFiles");
  @NonNls public static final Key<List<LocallyDeletedChange>> LOCALLY_DELETED_CHANGES = Key.create("ChangeListView.LocallyDeletedChanges");

  public ChangesListView(@Nonnull Project project) {
    myProject = project;

    setModel(TreeModelBuilder.buildEmpty(project));

    setShowsRootHandles(true);
    setRootVisible(false);
    setDragEnabled(true);

    myCopyProvider = new ChangesBrowserNodeCopyProvider(this);

    ChangesBrowserNodeRenderer renderer = new ChangesBrowserNodeRenderer(project, () -> myShowFlatten, true);
    setCellRenderer(renderer);

    new TreeSpeedSearch(this, TO_TEXT_CONVERTER);
    SmartExpander.installOn(this);
    new TreeLinkMouseListener(renderer).installOn(this);
  }

  @Override
  public DefaultTreeModel getModel() {
    return (DefaultTreeModel)super.getModel();
  }

  public boolean isShowFlatten() {
    return myShowFlatten;
  }

  public void setShowFlatten(final boolean showFlatten) {
    myShowFlatten = showFlatten;
  }

  public void updateModel(@Nonnull DefaultTreeModel newModel) {
    TreeState state = TreeState.createOn(this, getRoot());
    state.setScrollToSelection(false);
    DefaultTreeModel oldModel = getModel();
    setModel(newModel);
    ChangesBrowserNode newRoot = getRoot();
    expandPath(new TreePath(newRoot.getPath()));
    state.applyTo(this, newRoot);
    expandDefaultChangeList(oldModel, newRoot);
  }

  private void expandDefaultChangeList(DefaultTreeModel oldModel, ChangesBrowserNode root) {
    if (((ChangesBrowserNode)oldModel.getRoot()).getFileCount() == 0 && TreeUtil.collectExpandedPaths(this).size() == 1) {
      TreeNode toExpand = null;
      for (int i = 0; i < root.getChildCount(); i++) {
        TreeNode node = root.getChildAt(i);
        if (node instanceof ChangesBrowserChangeListNode && node.getChildCount() > 0) {
          ChangeList object = ((ChangesBrowserChangeListNode)node).getUserObject();
          if (object instanceof LocalChangeList) {
            if (((LocalChangeList)object).isDefault()) {
              toExpand = node;
              break;
            }
          }
        }
      }

      if (toExpand != null) {
        expandPath(new TreePath(new Object[] {root, toExpand}));
      }
    }
  }

  @Override
  public void calcData(Key<?> key, DataSink sink) {
    if (key == VcsDataKeys.CHANGES) {
      sink.put(VcsDataKeys.CHANGES, getSelectedChanges().toArray(Change[]::new));
    }
    else if (key == VcsDataKeys.CHANGE_LEAD_SELECTION) {
      sink.put(VcsDataKeys.CHANGE_LEAD_SELECTION, getLeadSelection().toArray(Change[]::new));
    }
    else if (key == VcsDataKeys.CHANGE_LISTS) {
      sink.put(VcsDataKeys.CHANGE_LISTS, getSelectedChangeLists().toArray(ChangeList[]::new));
    }
    else if (key == CommonDataKeys.VIRTUAL_FILE_ARRAY) {
      sink.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, getSelectedFiles().toArray(VirtualFile[]::new));
    }
    else if (key == VcsDataKeys.VIRTUAL_FILE_STREAM) {
      sink.put(VcsDataKeys.VIRTUAL_FILE_STREAM, getSelectedFiles());
    }
    else if (key == CommonDataKeys.NAVIGATABLE) {
      VirtualFile file = getIfSingle(getSelectedFiles());
      if (file != null && !file.isDirectory()) {
        sink.put(CommonDataKeys.NAVIGATABLE, new OpenFileDescriptorImpl(myProject, file, 0));
      }
    }
    else if (key == CommonDataKeys.NAVIGATABLE_ARRAY) {
      sink.put(CommonDataKeys.NAVIGATABLE_ARRAY, ChangesUtil.getNavigatableArray(myProject, getSelectedFiles()));
    }
    else if (key == PlatformDataKeys.DELETE_ELEMENT_PROVIDER) {
      if (getSelectionObjectsStream().anyMatch(userObject -> !(userObject instanceof ChangeList))) {
        sink.put(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, new VirtualFileDeleteProvider());
      }
    }
    else if (key == PlatformDataKeys.COPY_PROVIDER) {
      sink.put(PlatformDataKeys.COPY_PROVIDER, myCopyProvider);
    }
    else if (key == UNVERSIONED_FILES_DATA_KEY) {
      sink.put(UNVERSIONED_FILES_DATA_KEY, getSelectedUnversionedFiles());
    }
    else if (key == IGNORED_FILES_DATA_KEY) {
      sink.put(IGNORED_FILES_DATA_KEY, getSelectedIgnoredFiles());
    }
    else if (key == VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY) {
      sink.put(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY, getSelectedModifiedWithoutEditing().collect(toList()));
    }
    else if (key == LOCALLY_DELETED_CHANGES) {
      sink.put(LOCALLY_DELETED_CHANGES, getSelectedLocallyDeletedChanges().collect(toList()));
    }
    else if (key == MISSING_FILES_DATA_KEY) {
      sink.put(MISSING_FILES_DATA_KEY, getSelectedMissingFiles().collect(toList()));
    }
    else if (VcsDataKeys.HAVE_LOCALLY_DELETED == key) {
      sink.put(VcsDataKeys.HAVE_LOCALLY_DELETED, getSelectedMissingFiles().findAny().isPresent());
    }
    else if (VcsDataKeys.HAVE_MODIFIED_WITHOUT_EDITING == key) {
      sink.put(VcsDataKeys.HAVE_MODIFIED_WITHOUT_EDITING, getSelectedModifiedWithoutEditing().findAny().isPresent());
    }
    else if (VcsDataKeys.HAVE_SELECTED_CHANGES == key) {
      sink.put(VcsDataKeys.HAVE_SELECTED_CHANGES, haveSelectedChanges());
    }
    else if (key == PlatformDataKeys.HELP_ID) {
      sink.put(PlatformDataKeys.HELP_ID, HELP_ID);
    }
    else if (key == VcsDataKeys.CHANGES_IN_LIST_KEY) {
      final TreePath selectionPath = getSelectionPath();
      if (selectionPath != null && selectionPath.getPathCount() > 1) {
        ChangesBrowserNode<?> firstNode = (ChangesBrowserNode)selectionPath.getPathComponent(1);
        if (firstNode instanceof ChangesBrowserChangeListNode) {
          sink.put(VcsDataKeys.CHANGES_IN_LIST_KEY, firstNode.getAllChangesUnder());
        }
      }
    }
  }

  @Nonnull
  private Stream<VirtualFile> getSelectedUnversionedFiles() {
    return getSelectedVirtualFiles(UNVERSIONED_FILES_TAG);
  }

  @Nonnull
  private Stream<VirtualFile> getSelectedIgnoredFiles() {
    return getSelectedVirtualFiles(IGNORED_FILES_TAG);
  }

  @Nonnull
  private Stream<VirtualFile> getSelectedModifiedWithoutEditing() {
    return getSelectedVirtualFiles(MODIFIED_WITHOUT_EDITING_TAG);
  }

  @Nonnull
  private Stream<VirtualFile> getSelectedVirtualFiles(@jakarta.annotation.Nullable Object tag) {
    return getSelectionNodesStream(tag)
            .flatMap(ChangesBrowserNode::getFilesUnderStream)
            .distinct();
  }

  @Nonnull
  private Stream<ChangesBrowserNode<?>> getSelectionNodesStream() {
    return getSelectionNodesStream(null);
  }

  @Nonnull
  private Stream<ChangesBrowserNode<?>> getSelectionNodesStream(@jakarta.annotation.Nullable Object tag) {
    return stream(getSelectionPaths())
            .filter(path -> isUnderTag(path, tag))
            .map(TreePath::getLastPathComponent)
            .map(node -> ((ChangesBrowserNode<?>)node));
  }

  @Nonnull
  private Stream<Object> getSelectionObjectsStream() {
    return getSelectionNodesStream().map(ChangesBrowserNode::getUserObject);
  }

  @Nonnull
  static Stream<VirtualFile> getVirtualFiles(@jakarta.annotation.Nullable TreePath[] paths, @Nullable Object tag) {
    return stream(paths)
            .filter(path -> isUnderTag(path, tag))
            .map(TreePath::getLastPathComponent)
            .map(node -> ((ChangesBrowserNode<?>)node))
            .flatMap(ChangesBrowserNode::getFilesUnderStream)
            .distinct();
  }

  static boolean isUnderTag(@Nonnull TreePath path, @jakarta.annotation.Nullable Object tag) {
    boolean result = true;

    if (tag != null) {
      result = path.getPathCount() > 1 && ((ChangesBrowserNode)path.getPathComponent(1)).getUserObject() == tag;
    }

    return result;
  }

  @Nonnull
  static Stream<Change> getChanges(@Nonnull Project project, @Nullable TreePath[] paths) {
    Stream<Change> changes = stream(paths)
            .map(TreePath::getLastPathComponent)
            .map(node -> ((ChangesBrowserNode<?>)node))
            .flatMap(node -> node.getObjectsUnderStream(Change.class))
            .map(Change.class::cast);
    Stream<Change> hijackedChanges = getVirtualFiles(paths, MODIFIED_WITHOUT_EDITING_TAG)
            .map(file -> toHijackedChange(project, file))
            .filter(Objects::nonNull);

    return Stream.concat(changes, hijackedChanges).distinct();
  }

  @jakarta.annotation.Nullable
  private static Change toHijackedChange(@Nonnull Project project, @Nonnull VirtualFile file) {
    VcsCurrentRevisionProxy before = VcsCurrentRevisionProxy.create(file, project);
    if (before != null) {
      ContentRevision afterRevision = new CurrentContentRevision(VcsUtil.getFilePath(file));
      return new Change(before, afterRevision, FileStatus.HIJACKED);
    }
    return null;
  }

  @Nonnull
  private Stream<LocallyDeletedChange> getSelectedLocallyDeletedChanges() {
    return getSelectionNodesStream(LOCALLY_DELETED_NODE_TAG)
            .flatMap(node -> node.getObjectsUnderStream(LocallyDeletedChange.class))
            .distinct();
  }

  @Nonnull
  private Stream<FilePath> getSelectedMissingFiles() {
    return getSelectedLocallyDeletedChanges().map(LocallyDeletedChange::getPath);
  }

  @Nonnull
  protected Stream<VirtualFile> getSelectedFiles() {
    return Stream.concat(
            getAfterRevisionsFiles(getSelectedChanges()),
            getSelectedVirtualFiles(null)
    ).distinct();
  }

  // TODO: Does not correspond to getSelectedChanges() - for instance, hijacked changes are not tracked here
  private boolean haveSelectedChanges() {
    return getSelectionNodesStream().anyMatch(
            node -> node instanceof ChangesBrowserChangeNode || node instanceof ChangesBrowserChangeListNode && node.getChildCount() > 0);
  }

  @Nonnull
  private Stream<Change> getLeadSelection() {
    return getSelectionNodesStream()
            .filter(node -> node instanceof ChangesBrowserChangeNode)
            .map(ChangesBrowserChangeNode.class::cast)
            .map(ChangesBrowserChangeNode::getUserObject)
            .distinct();
  }

  @Nonnull
  public ChangesBrowserNode<?> getRoot() {
    return (ChangesBrowserNode<?>)getModel().getRoot();
  }

  @Nonnull
  public Stream<Change> getChanges() {
    return getRoot().getObjectsUnderStream(Change.class);
  }

  @Nonnull
  public Stream<Change> getSelectedChanges() {
    return getChanges(myProject, getSelectionPaths());
  }

  @Nonnull
  private Stream<ChangeList> getSelectedChangeLists() {
    return getSelectionObjectsStream()
            .filter(userObject -> userObject instanceof ChangeList)
            .map(ChangeList.class::cast)
            .distinct();
  }

  public void setMenuActions(final ActionGroup menuGroup) {
    PopupHandler.installPopupHandler(this, menuGroup, ActionPlaces.CHANGES_VIEW_POPUP, ActionManager.getInstance());
    editSourceRegistration();
  }

  protected void editSourceRegistration() {
    EditSourceOnDoubleClickHandler.install(this);
    EditSourceOnEnterKeyHandler.install(this);
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void processMouseEvent(final MouseEvent e) {
    if (MouseEvent.MOUSE_RELEASED == e.getID() && !isSelectionEmpty() && !e.isShiftDown() && !e.isControlDown()  &&
        !e.isMetaDown() && !e.isPopupTrigger()) {
      if (isOverSelection(e.getPoint())) {
        clearSelection();
        final TreePath path = getPathForLocation(e.getPoint().x, e.getPoint().y);
        if (path != null) {
          setSelectionPath(path);
        }
      }
    }


    super.processMouseEvent(e);
  }

  @Override
  public boolean isOverSelection(final Point point) {
    return TreeUtil.isOverSelection(this, point);
  }

  @Override
  public void dropSelectionButUnderPoint(final Point point) {
    TreeUtil.dropSelectionButUnderPoint(this, point);
  }
}