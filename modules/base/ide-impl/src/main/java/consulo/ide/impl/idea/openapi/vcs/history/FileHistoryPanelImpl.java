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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.application.CommonBundle;
import consulo.language.editor.PlatformDataKeys;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.application.AllIcons;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.CopyProvider;
import consulo.ide.impl.idea.ide.actions.RefreshAction;
import consulo.application.ApplicationManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.undoRedo.CommandProcessor;
import consulo.language.editor.WriteCommandAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.JBColor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ide.impl.idea.openapi.fileEditor.*;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.document.FileDocumentManager;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.ui.PanelWithActionsAndCloseButton;
import consulo.util.lang.Clock;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vcs.*;
import consulo.ide.impl.idea.openapi.vcs.actions.AnnotateRevisionActionBase;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.CreatePatchFromChangesAction;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.IssueLinkRenderer;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TableLinkMouseListener;
import consulo.ide.impl.idea.openapi.vcs.impl.AbstractVcsHelperImpl;
import consulo.ide.impl.idea.openapi.vcs.ui.ReplaceFileConfirmationDialog;
import consulo.vcs.*;
import consulo.vcs.change.VcsDirtyScopeManager;
import consulo.vcs.history.*;
import consulo.vcs.versionBrowser.CommittedChangeList;
import consulo.ide.impl.idea.openapi.vcs.vfs.VcsFileSystem;
import consulo.ide.impl.idea.openapi.vcs.vfs.VcsVirtualFile;
import consulo.ide.impl.idea.openapi.vcs.vfs.VcsVirtualFolder;
import consulo.vcs.change.Change;
import consulo.vcs.change.ContentRevision;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.ui.ex.action.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ide.impl.idea.ui.*;
import consulo.ui.ex.content.ContentManager;
import consulo.ide.impl.idea.ui.dualView.CellWrapper;
import consulo.ide.impl.idea.ui.dualView.DualView;
import consulo.ide.impl.idea.ui.dualView.DualViewColumnInfo;
import consulo.ide.impl.idea.ui.dualView.TreeTableView;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awt.util.Alarm;
import consulo.application.util.function.AsynchConsumer;
import consulo.ide.impl.idea.util.Consumer;
import consulo.util.lang.TreeItem;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.util.DateFormatUtil;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.table.TableViewModel;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * author: lesya
 */
public class FileHistoryPanelImpl extends PanelWithActionsAndCloseButton implements EditorColorsListener, CopyProvider {
  private static final Logger LOG = Logger.getInstance(FileHistoryPanelImpl.class);
  private static final String COMMIT_MESSAGE_TITLE = VcsBundle.message("label.selected.revision.commit.message");
  private static final String VCS_HISTORY_ACTIONS_GROUP = "VcsHistoryActionsGroup";

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final AbstractVcs myVcs;
  private final VcsHistoryProvider myProvider;
  @Nonnull
  private final FileHistoryRefresherI myRefresherI;
  @Nonnull
  private final DiffFromHistoryHandler myDiffHandler;
  @Nonnull
  private final FilePath myFilePath;
  @Nullable
  private final VcsRevisionNumber myStartingRevision;
  @Nonnull
  private final AsynchConsumer<VcsHistorySession> myHistoryPanelRefresh;
  @Nonnull
  private final Map<VcsRevisionNumber, Integer> myRevisionsOrder = ContainerUtil.newHashMap();
  @Nonnull
  private final Map<VcsFileRevision, VirtualFile> myRevisionToVirtualFile = ContainerUtil.newHashMap();
  @Nonnull
  private final Comparator<VcsFileRevision> myRevisionsInOrderComparator = (o1, o2) -> {
    // descending
    return Comparing.compare(myRevisionsOrder.get(o2.getRevisionNumber()), myRevisionsOrder.get(o1.getRevisionNumber()));
  };
  @Nonnull
  private final DetailsPanel myDetails;
  @Nonnull
  private final DualView myDualView;
  @Nullable
  private final JComponent myAdditionalDetails;
  @Nullable
  private final java.util.function.Consumer<VcsFileRevision> myRevisionSelectionListener;
  private VcsHistorySession myHistorySession;
  private VcsFileRevision myBottomRevisionForShowDiff;
  private volatile boolean myInRefresh;
  private List<Object> myTargetSelection;
  private boolean myIsStaticAndEmbedded;
  private Splitter myDetailsSplitter;
  private Splitter mySplitter;

  public FileHistoryPanelImpl(@Nonnull AbstractVcs vcs,
                              @Nonnull FilePath filePath,
                              VcsHistorySession session,
                              VcsHistoryProvider provider,
                              ContentManager contentManager,
                              @Nonnull FileHistoryRefresherI refresherI,
                              final boolean isStaticEmbedded) {
    this(vcs, filePath, null, session, provider, contentManager, refresherI, isStaticEmbedded);
  }

  public FileHistoryPanelImpl(@Nonnull AbstractVcs vcs,
                              @Nonnull FilePath filePath,
                              @Nullable VcsRevisionNumber startingRevision,
                              VcsHistorySession session,
                              VcsHistoryProvider provider,
                              ContentManager contentManager,
                              @Nonnull FileHistoryRefresherI refresherI,
                              final boolean isStaticEmbedded) {
    super(contentManager, provider.getHelpId() != null ? provider.getHelpId() : "reference.versionControl.toolwindow.history",
          !isStaticEmbedded);
    myProject = vcs.getProject();
    myIsStaticAndEmbedded = false;
    myVcs = vcs;
    myProvider = provider;
    myRefresherI = refresherI;
    myHistorySession = session;
    myFilePath = filePath;
    myStartingRevision = startingRevision;

    DiffFromHistoryHandler customDiffHandler = provider.getHistoryDiffHandler();
    myDiffHandler = customDiffHandler == null ? new StandardDiffFromHistoryHandler() : customDiffHandler;

    myDetails = new DetailsPanel(myProject);

    refreshRevisionsOrder();

    final VcsDependentHistoryComponents components = provider.getUICustomization(session, this);
    myAdditionalDetails = components.getDetailsComponent();
    myRevisionSelectionListener = components.getRevisionListener();

    final DualViewColumnInfo[] columns = createColumnList(myProject, provider, components.getColumns());
    String storageKey = "FileHistory." + provider.getClass().getName();
    final HistoryAsTreeProvider treeHistoryProvider = myHistorySession.getHistoryAsTreeProvider();
    if (treeHistoryProvider != null) {
      myDualView = new DualView(new TreeNodeOnVcsRevision(null, treeHistoryProvider.createTreeOn(myHistorySession.getRevisionList())),
                                columns, storageKey, myVcs.getProject());
    }
    else {
      myDualView =
              new DualView(new TreeNodeOnVcsRevision(null, ContainerUtil.map(myHistorySession.getRevisionList(), TreeItem::new)), columns,
                           storageKey, myVcs.getProject());
      myDualView.switchToTheFlatMode();
    }
    new TableSpeedSearch(myDualView.getFlatView()).setComparator(new SpeedSearchComparator(false));
    final TableLinkMouseListener listener = new TableLinkMouseListener();
    listener.installOn(myDualView.getFlatView());
    listener.installOn(myDualView.getTreeView());
    setEmptyText(CommonBundle.getLoadingTreeNodeText());

    setupDualView(addToGroup(true, new DefaultActionGroup(null, false)));
    if (isStaticEmbedded) {
      setIsStaticAndEmbedded(true);
    }

    myHistoryPanelRefresh = new AsynchConsumer<VcsHistorySession>() {
      public void finished() {
        if (treeHistoryProvider != null) {
          // scroll tree view to most recent change
          final TreeTableView treeView = myDualView.getTreeView();
          final int lastRow = treeView.getRowCount() - 1;
          if (lastRow >= 0) {
            treeView.scrollRectToVisible(treeView.getCellRect(lastRow, 0, true));
          }
        }
        myInRefresh = false;
        myTargetSelection = null;

        mySplitter.revalidate();
        mySplitter.repaint();
      }

      public void accept(VcsHistorySession vcsHistorySession) {
        FileHistoryPanelImpl.this.refresh(vcsHistorySession);
      }
    };

    Alarm updateAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    // todo react to event?
    updateAlarm.addRequest(new Runnable() {
      public void run() {
        if (myVcs.getProject().isDisposed()) {
          return;
        }
        boolean refresh = ApplicationManager.getApplication().isActive()
                          && !myInRefresh
                          && myHistorySession.shouldBeRefreshed();

        updateAlarm.cancelAllRequests();
        if (updateAlarm.isDisposed()) return;
        updateAlarm.addRequest(this, 20000);

        if (refresh) {
          refreshUiAndScheduleDataRefresh(true);
        }
      }
    }, 20000);

    init();

    chooseView();

    Disposer.register(myProject, this);
  }

  private static void makeBold(Component component) {
    if (component instanceof JComponent) {
      JComponent jComponent = (JComponent)component;
      Font font = jComponent.getFont();
      if (font != null) {
        jComponent.setFont(font.deriveFont(Font.BOLD));
      }
    }
    else if (component instanceof Container) {
      Container container = (Container)component;
      for (int i = 0; i < container.getComponentCount(); i++) {
        makeBold(container.getComponent(i));
      }
    }
  }

  @Nonnull
  public static String getPresentableText(@Nonnull VcsFileRevision revision, boolean withMessage) {
    // implementation reflected by consulo.ide.impl.idea.vcs.log.ui.frame.VcsLogGraphTable.getPresentableText()
    StringBuilder sb = new StringBuilder();
    sb.append(FileHistoryPanelImpl.RevisionColumnInfo.toString(revision, true)).append(" ");
    sb.append(revision.getAuthor());
    long time = revision.getRevisionDate().getTime();
    sb.append(" on ").append(DateFormatUtil.formatDate(time)).append(" at ").append(DateFormatUtil.formatTime(time));
    if (revision instanceof VcsFileRevisionEx) {
      if (!Comparing.equal(revision.getAuthor(), ((VcsFileRevisionEx)revision).getCommitterName())) {
        sb.append(" (committed by ").append(((VcsFileRevisionEx)revision).getCommitterName()).append(")");
      }
    }
    if (withMessage) {
      sb.append(" ").append(MessageColumnInfo.getSubject(revision));
    }
    return sb.toString();
  }

  /**
   * Checks if the given historyPanel shows the history for given path and revision number.
   */
  static boolean sameHistories(@Nonnull FileHistoryPanelImpl historyPanel,
                               @Nonnull FilePath path,
                               @Nullable VcsRevisionNumber startingRevisionNumber) {
    String existingRevision = historyPanel.getStartingRevision() == null ? null : historyPanel.getStartingRevision().asString();
    String newRevision = startingRevisionNumber == null ? null : startingRevisionNumber.asString();
    return historyPanel.getFilePath().equals(path) && Comparing.equal(existingRevision, newRevision);
  }

  @CalledInAwt
  void scheduleRefresh(boolean canUseLastRevision) {
    refreshUiAndScheduleDataRefresh(canUseLastRevision);
  }

  @Nullable
  public VcsRevisionNumber getStartingRevision() {
    return myStartingRevision;
  }

  @Nonnull
  private DualViewColumnInfo[] createColumnList(@Nonnull Project project,
                                                @Nonnull VcsHistoryProvider provider,
                                                @Nullable ColumnInfo[] additionalColumns) {
    ArrayList<DualViewColumnInfo> columns = new ArrayList<>();
    columns.add(new RevisionColumnInfo(myRevisionsInOrderComparator));
    if (!provider.isDateOmittable()) columns.add(new DateColumnInfo());
    columns.add(new AuthorColumnInfo());
    ArrayList<DualViewColumnInfo> additionalColumnInfo = new ArrayList<>();
    if (additionalColumns != null) {
      for (ColumnInfo additionalColumn : additionalColumns) {
        additionalColumnInfo.add(new TreeNodeColumnInfoWrapper(additionalColumn));
      }
    }
    columns.addAll(additionalColumnInfo);
    columns.add(new MessageColumnInfo(project));
    return columns.toArray(new DualViewColumnInfo[columns.size()]);
  }

  private void refresh(final VcsHistorySession session) {
    myHistorySession = session;
    refreshRevisionsOrder();
    HistoryAsTreeProvider treeHistoryProvider = session.getHistoryAsTreeProvider();

    if (myHistorySession.getRevisionList().isEmpty()) {
      adjustEmptyText();
    }

    if (treeHistoryProvider != null) {
      myDualView.setRoot(new TreeNodeOnVcsRevision(null,
                                                   treeHistoryProvider.createTreeOn(myHistorySession.getRevisionList())),
                         myTargetSelection);
    }
    else {
      myDualView.setRoot(new TreeNodeOnVcsRevision(null, ContainerUtil.map(myHistorySession.getRevisionList(), TreeItem::new)),
                         myTargetSelection);
    }

    myDualView.expandAll();
    myDualView.repaint();
  }

  private void adjustEmptyText() {
    VirtualFile virtualFile = myFilePath.getVirtualFile();
    if ((virtualFile == null || !virtualFile.isValid()) && !myFilePath.getIOFile().exists()) {
      setEmptyText("File " + myFilePath.getName() + " not found");
    }
    else if (myInRefresh) {
      setEmptyText(CommonBundle.getLoadingTreeNodeText());
    }
    else {
      setEmptyText(StatusText.DEFAULT_EMPTY_TEXT);
    }
  }

  private void setEmptyText(@Nonnull String emptyText) {
    myDualView.setEmptyText(emptyText);
  }

  protected void addActionsTo(DefaultActionGroup group) {
    addToGroup(false, group);
  }

  private void setupDualView(@Nonnull DefaultActionGroup group) {
    myDualView.setShowGrid(true);
    PopupHandler.installPopupHandler(myDualView.getTreeView(), group, ActionPlaces.UPDATE_POPUP, ActionManager.getInstance());
    PopupHandler.installPopupHandler(myDualView.getFlatView(), group, ActionPlaces.UPDATE_POPUP, ActionManager.getInstance());
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myDualView);

    myDualView.addListSelectionListener(e -> updateMessage());

    myDualView.setRootVisible(false);

    myDualView.expandAll();

    final TreeCellRenderer defaultCellRenderer = myDualView.getTree().getCellRenderer();

    final Getter<VcsHistorySession> sessionGetter = () -> myHistorySession;
    myDualView.setTreeCellRenderer(new MyTreeCellRenderer(defaultCellRenderer, sessionGetter));

    myDualView.setCellWrapper(new MyCellWrapper(sessionGetter));

    myDualView.installDoubleClickHandler(new MyDiffAction());

    final TableView flatView = myDualView.getFlatView();
    TableViewModel sortableModel = flatView.getTableViewModel();
    sortableModel.setSortable(true);

    final RowSorter<? extends TableModel> rowSorter = flatView.getRowSorter();
    if (rowSorter != null) {
      rowSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
    }
  }

  private void updateMessage() {
    List<TreeNodeOnVcsRevision> selection = getSelection();
    myDetails.update(selection);
    if (selection.isEmpty()) {
      return;
    }
    if (myRevisionSelectionListener != null) {
      myRevisionSelectionListener.accept(selection.get(0).getRevision());
    }
  }

  @Nonnull
  protected JComponent createCenterPanel() {
    mySplitter = new OnePixelSplitter(true, "vcs.history.splitter.proportion", getConfiguration().FILE_HISTORY_SPLITTER_PROPORTION);
    mySplitter.setFirstComponent(myDualView);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myDetails);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));

    myDetailsSplitter = new OnePixelSplitter(false, "vcs.history.details.splitter.proportion", 0.5f);
    myDetailsSplitter.setFirstComponent(scrollPane);
    myDetailsSplitter.setSecondComponent(myAdditionalDetails);

    setupDetails();
    return mySplitter;
  }

  private void setupDetails() {
    boolean showDetails = !myIsStaticAndEmbedded && getConfiguration().SHOW_FILE_HISTORY_DETAILS;
    myDualView.setViewBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    mySplitter.setSecondComponent(showDetails ? myDetailsSplitter : null);
  }

  private void chooseView() {
    if (getConfiguration().SHOW_FILE_HISTORY_AS_TREE) {
      myDualView.switchToTheTreeMode();
    }
    else {
      myDualView.switchToTheFlatMode();
    }
  }

  private VcsConfiguration getConfiguration() {
    return VcsConfiguration.getInstance(myVcs.getProject());
  }

  @Nonnull
  private DefaultActionGroup addToGroup(boolean popup, DefaultActionGroup result) {
    if (popup) {
      result.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
    }

    final MyDiffAction diffAction = new MyDiffAction();
    diffAction.registerCustomShortcutSet(CommonShortcuts.getDiff(), null);
    result.add(diffAction);

    result.add(ActionManager.getInstance().getAction("Vcs.ShowDiffWithLocal"));

    final AnAction diffGroup = ActionManager.getInstance().getAction(VCS_HISTORY_ACTIONS_GROUP);
    if (diffGroup != null) result.add(diffGroup);
    result.add(new MyCreatePatch());
    result.add(new MyGetVersionAction());
    result.add(new MyAnnotateAction());
    AnAction[] additionalActions = myProvider.getAdditionalActions(() -> refreshUiAndScheduleDataRefresh(true));
    if (additionalActions != null) {
      for (AnAction additionalAction : additionalActions) {
        if (popup || additionalAction.getTemplatePresentation().getIcon() != null) {
          result.add(additionalAction);
        }
      }
    }
    result.add(new RefreshFileHistoryAction());
    if (!myIsStaticAndEmbedded) {
      result.add(new MyShowDetailsAction());
    }

    if (!popup && myHistorySession != null && myHistorySession.getHistoryAsTreeProvider() != null) {
      result.add(new MyShowAsTreeAction());
    }

    return result;
  }

  private void refreshUiAndScheduleDataRefresh(boolean canUseLastRevisionCheck) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      if (myInRefresh) return;
      myInRefresh = true;
      myTargetSelection = myDualView.getFlatView().getSelectedObjects();

      mySplitter.revalidate();
      mySplitter.repaint();

      myRefresherI.run(true, canUseLastRevisionCheck);
    });
  }

  @Nonnull
  public AsynchConsumer<VcsHistorySession> getHistoryPanelRefresh() {
    return myHistoryPanelRefresh;
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    VcsFileRevision firstSelectedRevision = getFirstSelectedRevision();
    if (CommonDataKeys.NAVIGATABLE == dataId) {
      List selectedItems = getSelection();
      if (selectedItems.size() != 1) return null;
      if (!myHistorySession.isContentAvailable(firstSelectedRevision)) {
        return null;
      }
      VirtualFile virtualFileForRevision = createVirtualFileForRevision(firstSelectedRevision);
      if (virtualFileForRevision != null) {
        return new OpenFileDescriptorImpl(myVcs.getProject(), virtualFileForRevision);
      }
      else {
        return null;
      }
    }
    else if (CommonDataKeys.PROJECT== dataId) {
      return myVcs.getProject();
    }
    else if (VcsDataKeys.VCS_FILE_REVISION== dataId) {
      return firstSelectedRevision;
    }
    else if (VcsDataKeys.VCS_NON_LOCAL_HISTORY_SESSION== dataId && myHistorySession != null) {
      return !myHistorySession.hasLocalSource();
    }
    else if (VcsDataKeys.VCS== dataId) {
      return myVcs.getKeyInstanceMethod();
    }
    else if (VcsDataKeys.VCS_FILE_REVISIONS== dataId) {
      return getSelectedRevisions();
    }
    else if (VcsDataKeys.REMOTE_HISTORY_CHANGED_LISTENER== dataId) {
      return (Consumer<String>)s -> myDualView.rebuild();
    }
    else if (VcsDataKeys.CHANGES== dataId) {
      return getChanges();
    }
    else if (VcsDataKeys.VCS_VIRTUAL_FILE== dataId) {
      if (firstSelectedRevision == null) return null;
      return createVirtualFileForRevision(firstSelectedRevision);
    }
    else if (VcsDataKeys.FILE_PATH== dataId) {
      return myFilePath;
    }
    else if (VcsDataKeys.IO_FILE== dataId) {
      return myFilePath.getIOFile();
    }
    else if (CommonDataKeys.VIRTUAL_FILE== dataId) {
      VirtualFile virtualFile = getVirtualFile();
      return virtualFile == null || !virtualFile.isValid() ? null : virtualFile;
    }
    else if (VcsDataKeys.FILE_HISTORY_PANEL== dataId) {
      return this;
    }
    else if (VcsDataKeys.HISTORY_SESSION== dataId) {
      return myHistorySession;
    }
    else if (VcsDataKeys.HISTORY_PROVIDER== dataId) {
      return myProvider;
    }
    else if (PlatformDataKeys.COPY_PROVIDER == dataId) {
      return this;
    }
    else {
      return super.getData(dataId);
    }
  }

  @Nullable
  private Change[] getChanges() {
    final VcsFileRevision[] revisions = getSelectedRevisions();

    if (revisions.length > 0) {
      Arrays.sort(revisions, (o1, o2) -> o1.getRevisionNumber().compareTo(o2.getRevisionNumber()));

      for (VcsFileRevision revision : revisions) {
        if (!myHistorySession.isContentAvailable(revision)) {
          return null;
        }
      }

      final ContentRevision startRevision = new LoadedContentRevision(myFilePath, revisions[0], myVcs.getProject());
      final ContentRevision endRevision = (revisions.length == 1) ? new CurrentContentRevision(myFilePath) :
                                          new LoadedContentRevision(myFilePath, revisions[revisions.length - 1], myVcs.getProject());

      return new Change[]{new Change(startRevision, endRevision)};
    }
    return null;
  }

  private VirtualFile createVirtualFileForRevision(VcsFileRevision revision) {
    if (!myRevisionToVirtualFile.containsKey(revision)) {
      FilePath filePath = (revision instanceof VcsFileRevisionEx ? ((VcsFileRevisionEx)revision).getPath() : myFilePath);
      myRevisionToVirtualFile.put(revision, filePath.isDirectory()
                                            ? new VcsVirtualFolder(filePath.getPath(), null, VcsFileSystem.getInstance())
                                            : new VcsVirtualFile(filePath.getPath(), revision, VcsFileSystem.getInstance()));
    }
    return myRevisionToVirtualFile.get(revision);
  }

  private List<TreeNodeOnVcsRevision> getSelection() {
    //noinspection unchecked
    return myDualView.getSelection();
  }

  @Nullable
  private VcsFileRevision getFirstSelectedRevision() {
    List<TreeNodeOnVcsRevision> selection = getSelection();
    if (selection.isEmpty()) return null;
    return selection.get(0).getRevision();
  }

  @Nonnull
  public VcsFileRevision[] getSelectedRevisions() {
    List<TreeNodeOnVcsRevision> selection = getSelection();
    VcsFileRevision[] result = new VcsFileRevision[selection.size()];
    for (int i = 0; i < selection.size(); i++) {
      result[i] = selection.get(i).getRevision();
    }
    return result;
  }

  public void dispose() {
    myDualView.dispose();
  }

  @Nonnull
  public FileHistoryRefresherI getRefresher() {
    return myRefresherI;
  }

  @Nonnull
  public FilePath getFilePath() {
    return myFilePath;
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    return myFilePath.getVirtualFile();
  }

  private void refreshRevisionsOrder() {
    final List<VcsFileRevision> list = myHistorySession.getRevisionList();
    myRevisionsOrder.clear();

    int cnt = 0;
    for (VcsFileRevision revision : list) {
      myRevisionsOrder.put(revision.getRevisionNumber(), cnt);
      ++cnt;
    }
  }

  public void setIsStaticAndEmbedded(boolean isStaticAndEmbedded) {
    myIsStaticAndEmbedded = isStaticAndEmbedded;
    myDualView.setZipByHeight(isStaticAndEmbedded);
    myDualView.getFlatView().updateColumnSizes();
    if (myIsStaticAndEmbedded) {
      disableClose();
      myDualView.getFlatView().getTableHeader().setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
      myDualView.getTreeView().getTableHeader().setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
      myDualView.getFlatView().setBorder(null);
      myDualView.getTreeView().setBorder(null);
    }
  }

  public void setBottomRevisionForShowDiff(VcsFileRevision bottomRevisionForShowDiff) {
    myBottomRevisionForShowDiff = bottomRevisionForShowDiff;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof FileHistoryPanelImpl && sameHistories((FileHistoryPanelImpl)obj, myFilePath, myStartingRevision);
  }

  @Override
  public int hashCode() {
    int result = myFilePath.hashCode();
    result = 31 * result + (myStartingRevision != null ? myStartingRevision.asString().hashCode() : 0); // NB: asString to conform to equals
    return result;
  }

  @Override
  public void performCopy(@Nonnull DataContext dataContext) {
    String text = StringUtil.join(getSelectedRevisions(), revision -> getPresentableText(revision, true), "\n");
    CopyPasteManager.getInstance().setContents(new StringSelection(text));
  }

  @Override
  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    return getSelection().size() > 0;
  }

  @Override
  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return true;
  }

  @Override
  public void globalSchemeChange(EditorColorsScheme scheme) {
    updateMessage();
  }

  public static class RevisionColumnInfo extends VcsColumnInfo<VcsRevisionNumber> {
    private final Comparator<VcsFileRevision> myComparator;

    public RevisionColumnInfo(Comparator<VcsFileRevision> comparator) {
      super(VcsBundle.message("column.name.revision.version"));
      myComparator = comparator;
    }

    static String toString(VcsFileRevision o, boolean shortVersion) {
      VcsRevisionNumber number = o.getRevisionNumber();
      return shortVersion && number instanceof ShortVcsRevisionNumber
             ? ((ShortVcsRevisionNumber)number).toShortString()
             : number.asString();
    }

    @Override
    protected VcsRevisionNumber getDataOf(VcsFileRevision object) {
      return object.getRevisionNumber();
    }

    @Override
    public Comparator<VcsFileRevision> getComparator() {
      return myComparator;
    }

    public String valueOf(VcsFileRevision object) {
      return toString(object, true);
    }

    @Override
    public String getPreferredStringValue() {
      return StringUtil.repeatSymbol('m', 10);
    }
  }

  public static class DateColumnInfo extends VcsColumnInfo<String> {
    public DateColumnInfo() {
      super(VcsBundle.message("column.name.revision.date"));
    }

    @Nonnull
    static String toString(VcsFileRevision object) {
      Date date = object.getRevisionDate();
      if (date == null) return "";
      return DateFormatUtil.formatPrettyDateTime(date);
    }

    protected String getDataOf(VcsFileRevision object) {
      return toString(object);
    }

    public int compare(VcsFileRevision o1, VcsFileRevision o2) {
      return Comparing.compare(o1.getRevisionDate(), o2.getRevisionDate());
    }

    @Override
    public String getPreferredStringValue() {
      return DateFormatUtil.formatPrettyDateTime(Clock.getTime() + 1000);
    }
  }

  private static class AuthorCellRenderer extends ColoredTableCellRenderer {
    private String myTooltipText;

    /**
     * @noinspection MethodNamesDifferingOnlyByCase
     */
    public void setTooltipText(final String text) {
      myTooltipText = text;
    }

    @Override
    protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
      setToolTipText(myTooltipText);
      if (selected || hasFocus) {
        setBackground(table.getSelectionBackground());
        setForeground(table.getSelectionForeground());
      }
      else {
        setBackground(table.getBackground());
        setForeground(table.getForeground());
      }
      if (value != null) append(value.toString());
    }
  }

  public static class AuthorColumnInfo extends VcsColumnInfo<String> {
    private final TableCellRenderer AUTHOR_RENDERER = new AuthorCellRenderer();

    public AuthorColumnInfo() {
      super(VcsBundle.message("column.name.revision.list.author"));
    }

    static String toString(VcsFileRevision o) {
      VcsFileRevision rev = o;
      if (o instanceof TreeNodeOnVcsRevision) {
        rev = ((TreeNodeOnVcsRevision)o).getRevision();
      }
      if (rev instanceof VcsFileRevisionEx) {
        if (!Comparing.equal(rev.getAuthor(), ((VcsFileRevisionEx)rev).getCommitterName())) {
          return o.getAuthor() + "*";
        }
      }
      return o.getAuthor();
    }

    protected String getDataOf(VcsFileRevision object) {
      return toString(object);
    }

    @Override
    public TableCellRenderer getRenderer(VcsFileRevision revision) {
      return AUTHOR_RENDERER;
    }

    @Override
    public TableCellRenderer getCustomizedRenderer(VcsFileRevision value, TableCellRenderer renderer) {
      if (renderer instanceof AuthorCellRenderer) {
        VcsFileRevision revision = value;
        if (value instanceof TreeNodeOnVcsRevision) {
          revision = ((TreeNodeOnVcsRevision)value).getRevision();
        }

        if (revision instanceof VcsFileRevisionEx) {
          VcsFileRevisionEx ex = (VcsFileRevisionEx)revision;
          StringBuilder sb = new StringBuilder(StringUtil.notNullize(ex.getAuthor()));
          if (ex.getAuthorEmail() != null) sb.append(" &lt;").append(ex.getAuthorEmail()).append("&gt;");
          if (ex.getCommitterName() != null && !Comparing.equal(ex.getAuthor(), ex.getCommitterName())) {
            sb.append(", via ").append(ex.getCommitterName());
            if (ex.getCommitterEmail() != null) sb.append(" &lt;").append(ex.getCommitterEmail()).append("&gt;");
          }
          ((AuthorCellRenderer)renderer).setTooltipText(sb.toString());
        }
      }

      return renderer;
    }

    @Override
    @NonNls
    public String getPreferredStringValue() {
      return StringUtil.repeatSymbol('m', 14);
    }
  }

  public static class MessageColumnInfo extends VcsColumnInfo<String> {
    private final ColoredTableCellRenderer myRenderer;
    private final IssueLinkRenderer myIssueLinkRenderer;

    public MessageColumnInfo(Project project) {
      super(COMMIT_MESSAGE_TITLE);
      myRenderer = new ColoredTableCellRenderer() {
        protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
          setOpaque(selected);
          if (value instanceof String) {
            String message = (String)value;
            myIssueLinkRenderer.appendTextWithLinks(message);
          }
        }
      };
      myIssueLinkRenderer = new IssueLinkRenderer(project, myRenderer);
    }

    @Nonnull
    public static String getSubject(@Nonnull VcsFileRevision object) {
      final String originalMessage = object.getCommitMessage();
      if (originalMessage == null) return "";

      int index = StringUtil.indexOfAny(originalMessage, "\n\r");
      return index == -1 ? originalMessage : originalMessage.substring(0, index);
    }

    protected String getDataOf(VcsFileRevision object) {
      return getSubject(object);
    }

    @Override
    public String getPreferredStringValue() {
      return StringUtil.repeatSymbol('m', 80);
    }

    public TableCellRenderer getRenderer(VcsFileRevision p0) {
      return myRenderer;
    }
  }

  private static class LoadedContentRevision implements ByteBackedContentRevision {
    private final FilePath myFile;
    private final VcsFileRevision myRevision;
    private final Project myProject;

    private LoadedContentRevision(final FilePath file, final VcsFileRevision revision, final Project project) {
      myFile = file;
      myRevision = revision;
      myProject = project;
    }

    public String getContent() throws VcsException {
      try {
        return VcsHistoryUtil.loadRevisionContentGuessEncoding(myRevision, myFile.getVirtualFile(), myProject);
      }
      catch (IOException e) {
        throw new VcsException(VcsBundle.message("message.text.cannot.load.revision", e.getLocalizedMessage()));
      }
    }

    @Nullable
    @Override
    public byte[] getContentAsBytes() throws VcsException {
      try {
        return VcsHistoryUtil.loadRevisionContent(myRevision);
      }
      catch (IOException e) {
        throw new VcsException(VcsBundle.message("message.text.cannot.load.revision", e.getLocalizedMessage()));
      }
    }

    @Nonnull
    public FilePath getFile() {
      return myFile;
    }

    @Nonnull
    public VcsRevisionNumber getRevisionNumber() {
      return myRevision.getRevisionNumber();
    }
  }

  abstract static class AbstractActionForSomeSelection extends AnAction implements DumbAware {
    private final int mySuitableSelectedElements;
    private final FileHistoryPanelImpl mySelectionProvider;

    public AbstractActionForSomeSelection(String name,
                                          String description,
                                          @Nonnull Image icon,
                                          int suitableSelectionSize,
                                          FileHistoryPanelImpl tableProvider) {
      super(name, description, icon);
      mySuitableSelectedElements = suitableSelectionSize;
      mySelectionProvider = tableProvider;
    }

    protected abstract void executeAction(AnActionEvent e);

    public boolean isEnabled() {
      return mySelectionProvider.getSelection().size() == mySuitableSelectedElements;
    }

    public void actionPerformed(@Nonnull AnActionEvent e) {
      if (!isEnabled()) return;
      executeAction(e);
    }

    public void update(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setVisible(true);
      presentation.setEnabled(isEnabled());
    }
  }

  abstract static class VcsColumnInfo<T extends Comparable<T>> extends DualViewColumnInfo<VcsFileRevision, String>
          implements Comparator<VcsFileRevision> {
    public VcsColumnInfo(String name) {
      super(name);
    }

    protected abstract T getDataOf(VcsFileRevision o);

    public Comparator<VcsFileRevision> getComparator() {
      return this;
    }

    public String valueOf(VcsFileRevision object) {
      T result = getDataOf(object);
      return result == null ? "" : result.toString();
    }

    public int compare(VcsFileRevision o1, VcsFileRevision o2) {
      return Comparing.compare(getDataOf(o1), getDataOf(o2));
    }

    public boolean shouldBeShownIsTheTree() {
      return true;
    }

    public boolean shouldBeShownIsTheTable() {
      return true;
    }
  }

  private static class MyTreeCellRenderer implements TreeCellRenderer {
    private final TreeCellRenderer myDefaultCellRenderer;
    private final Getter<VcsHistorySession> myHistorySession;

    public MyTreeCellRenderer(final TreeCellRenderer defaultCellRenderer, final Getter<VcsHistorySession> historySession) {
      myDefaultCellRenderer = defaultCellRenderer;
      myHistorySession = historySession;
    }

    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      final Component result = myDefaultCellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

      final TreePath path = tree.getPathForRow(row);
      if (path == null) return result;
      final VcsFileRevision revision = row >= 0 ? (VcsFileRevision)path.getLastPathComponent() : null;

      if (revision != null) {
        if (myHistorySession.get().isCurrentRevision(revision.getRevisionNumber())) {
          makeBold(result);
        }
        if (!selected && myHistorySession.get().isCurrentRevision(revision.getRevisionNumber())) {
          result.setBackground(new JBColor(new Color(188, 227, 231), new Color(188, 227, 231)));
        }
        ((JComponent)result).setOpaque(false);
      }
      else if (selected) {
        result.setBackground(UIUtil.getTableSelectionBackground());
      }
      else {
        result.setBackground(UIUtil.getTableBackground());
      }

      return result;
    }
  }

  private static class MyCellWrapper implements CellWrapper {
    private final Getter<VcsHistorySession> myHistorySession;

    public MyCellWrapper(final Getter<VcsHistorySession> historySession) {
      myHistorySession = historySession;
    }

    public void wrap(Component component,
                     JTable table,
                     Object value,
                     boolean isSelected,
                     boolean hasFocus,
                     int row,
                     int column,
                     Object treeNode) {
      VcsFileRevision revision = (VcsFileRevision)treeNode;
      if (revision == null) return;
      if (myHistorySession.get().isCurrentRevision(revision.getRevisionNumber())) {
        makeBold(component);
      }
    }
  }

  private static class FolderPatchCreationTask extends Task.Backgroundable {
    private final AbstractVcs myVcs;
    private final TreeNodeOnVcsRevision myRevision;
    private CommittedChangeList myList;
    private VcsException myException;

    private FolderPatchCreationTask(@Nonnull AbstractVcs vcs, final TreeNodeOnVcsRevision revision) {
      super(vcs.getProject(), VcsBundle.message("create.patch.loading.content.progress"), true);
      myVcs = vcs;
      myRevision = revision;
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
      final CommittedChangesProvider provider = myVcs.getCommittedChangesProvider();
      if (provider == null) return;
      final RepositoryLocation changedRepositoryPath = myRevision.getChangedRepositoryPath();
      if (changedRepositoryPath == null) return;
      final VcsVirtualFile vf =
              new VcsVirtualFile(changedRepositoryPath.toPresentableString(), myRevision.getRevision(), VcsFileSystem.getInstance());
      try {
        myList = AbstractVcsHelperImpl.getRemoteList(myVcs, myRevision.getRevisionNumber(), vf);
        //myList = provider.getOneList(vf, myRevision.getRevisionNumber());
      }
      catch (VcsException e1) {
        myException = e1;
      }
    }

    @Override
    public void onSuccess() {
      AbstractVcsHelper helper = AbstractVcsHelper.getInstance((Project)myProject);
      if (myException != null) {
        helper.showError(myException, VcsBundle.message("create.patch.error.title", myException.getMessage()));
      }
      else if (myList == null) {
        helper.showError(null, "Can not load changelist contents");
      }
      else {
        CreatePatchFromChangesAction.createPatch((Project)myProject, myList.getComment(), new ArrayList<>(myList.getChanges()));
      }
    }
  }

  private class MyShowAsTreeAction extends ToggleAction implements DumbAware {
    public MyShowAsTreeAction() {
      super(VcsBundle.message("action.name.show.files.as.tree"), null, AllIcons.General.SmallConfigurableVcs);
    }

    public boolean isSelected(AnActionEvent e) {
      return getConfiguration().SHOW_FILE_HISTORY_AS_TREE;
    }

    public void setSelected(AnActionEvent e, boolean state) {
      getConfiguration().SHOW_FILE_HISTORY_AS_TREE = state;
      chooseView();
    }
  }

  private class MyDiffAction extends AbstractActionForSomeSelection {
    public MyDiffAction() {
      super(VcsBundle.message("action.name.compare"), VcsBundle.message("action.description.compare"), PlatformIconGroup.actionsDiff(), 2, FileHistoryPanelImpl.this);
    }

    protected void executeAction(AnActionEvent e) {
      List<TreeNodeOnVcsRevision> sel = getSelection();

      int selectionSize = sel.size();
      if (selectionSize > 1) {
        List<VcsFileRevision> selectedRevisions =
                ContainerUtil.sorted(ContainerUtil.map(sel, TreeNodeOnVcsRevision::getRevision), myRevisionsInOrderComparator);
        VcsFileRevision olderRevision = selectedRevisions.get(0);
        VcsFileRevision newestRevision = selectedRevisions.get(sel.size() - 1);
        myDiffHandler.showDiffForTwo(e.getRequiredData(CommonDataKeys.PROJECT), myFilePath, olderRevision, newestRevision);
      }
      else if (selectionSize == 1) {
        final TableView<TreeNodeOnVcsRevision> flatView = myDualView.getFlatView();
        final int selectedRow = flatView.getSelectedRow();
        VcsFileRevision revision = getFirstSelectedRevision();

        VcsFileRevision previousRevision;
        if (selectedRow == (flatView.getRowCount() - 1)) {
          // no previous
          previousRevision = myBottomRevisionForShowDiff != null ? myBottomRevisionForShowDiff : VcsFileRevision.NULL;
        }
        else {
          previousRevision = flatView.getRow(selectedRow + 1).getRevision();
        }

        if (revision != null) {
          myDiffHandler.showDiffForOne(e, e.getRequiredData(CommonDataKeys.PROJECT), myFilePath, previousRevision, revision);
        }
      }
    }

    public void update(final AnActionEvent e) {
      super.update(e);
      final int selectionSize = getSelection().size();
      e.getPresentation().setEnabled(selectionSize > 0 && isEnabled());
    }

    public boolean isEnabled() {
      final int selectionSize = getSelection().size();
      if (selectionSize == 1) {
        List<TreeNodeOnVcsRevision> sel = getSelection();
        return myHistorySession.isContentAvailable(sel.get(0));
      }
      else if (selectionSize > 1) {
        return isDiffEnabled();
      }
      return false;
    }

    private boolean isDiffEnabled() {
      List<TreeNodeOnVcsRevision> sel = getSelection();
      return myHistorySession.isContentAvailable(sel.get(0)) && myHistorySession.isContentAvailable(sel.get(sel.size() - 1));
    }
  }

  private class MyGetVersionAction extends AbstractActionForSomeSelection {
    public MyGetVersionAction() {
      super(VcsBundle.message("action.name.get.file.content.from.repository"),
            VcsBundle.message("action.description.get.file.content.from.repository"), PlatformIconGroup.actionsGet(), 1, FileHistoryPanelImpl.this);
    }

    @Override
    public boolean isEnabled() {
      return super.isEnabled() && myFilePath.getVirtualFileParent() != null &&
             myHistorySession.isContentAvailable(getFirstSelectedRevision()) && !myFilePath.isDirectory();
    }

    protected void executeAction(AnActionEvent e) {
      if (ChangeListManager.getInstance(myVcs.getProject()).isFreezedWithNotification(null)) return;
      final VcsFileRevision revision = getFirstSelectedRevision();
      VirtualFile virtualFile = getVirtualFile();
      if (virtualFile != null) {
        if (!new ReplaceFileConfirmationDialog(myVcs.getProject(), VcsBundle.message("acton.name.get.revision"))
                .confirmFor(new VirtualFile[]{virtualFile})) {
          return;
        }
      }

      getVersion(revision);
      refreshFile(revision);
    }

    private void refreshFile(VcsFileRevision revision) {
      Runnable refresh = null;
      final VirtualFile vf = getVirtualFile();
      if (vf == null) {
        final LocalHistoryAction action = startLocalHistoryAction(revision);
        final VirtualFile vp = myFilePath.getVirtualFileParent();
        if (vp != null) {
          refresh = () -> vp.refresh(false, true, action::finish);
        }
      }
      else {
        refresh = () -> vf.refresh(false, false);
      }
      if (refresh != null) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(refresh, "Refreshing Files...", false, myVcs.getProject());
      }
    }

    private void getVersion(final VcsFileRevision revision) {
      final VirtualFile file = getVirtualFile();
      final Project project = myVcs.getProject();

      new Task.Backgroundable(project, VcsBundle.message("show.diff.progress.title")) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          final LocalHistoryAction action = file != null ? startLocalHistoryAction(revision) : LocalHistoryAction.NULL;
          final byte[] revisionContent;
          try {
            revisionContent = VcsHistoryUtil.loadRevisionContent(revision);
          }
          catch (final IOException | VcsException e) {
            LOG.info(e);
            ApplicationManager.getApplication().invokeLater(
                    () -> Messages.showMessageDialog(VcsBundle.message("message.text.cannot.load.revision", e.getLocalizedMessage()),
                                                     VcsBundle.message("message.title.get.revision.content"), Messages.getInformationIcon()));
            return;
          }
          catch (ProcessCanceledException ex) {
            return;
          }

          ApplicationManager.getApplication().invokeLater(() -> {
            try {
              new WriteCommandAction.Simple(project) {
                @Override
                protected void run() throws Throwable {
                  if (file != null &&
                      !file.isWritable() &&
                      ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(file).hasReadonlyFiles()) {
                    return;
                  }

                  try {
                    write(revisionContent);
                  }
                  catch (IOException e) {
                    Messages.showMessageDialog(VcsBundle.message("message.text.cannot.save.content", e.getLocalizedMessage()),
                                               VcsBundle.message("message.title.get.revision.content"), Messages.getErrorIcon());
                  }
                }
              }.execute();
              if (file != null) {
                VcsDirtyScopeManager.getInstance(project).fileDirty(file);
              }
            }
            finally {
              action.finish();
            }
          });
        }
      }.queue();
    }

    private LocalHistoryAction startLocalHistoryAction(final VcsFileRevision revision) {
      return LocalHistory.getInstance().startAction(createGetActionTitle(revision));
    }

    private String createGetActionTitle(final VcsFileRevision revision) {
      return VcsBundle.message("action.name.for.file.get.version", myFilePath.getPath(), revision.getRevisionNumber());
    }

    private void write(byte[] revision) throws IOException {
      VirtualFile virtualFile = getVirtualFile();
      if (virtualFile == null) {
        writeContentToIOFile(revision);
      }
      else {
        Document document = null;
        if (!virtualFile.getFileType().isBinary()) {
          document = FileDocumentManager.getInstance().getDocument(virtualFile);
        }
        if (document == null) {
          virtualFile.setBinaryContent(revision);
        }
        else {
          writeContentToDocument(document, revision);
        }
      }
    }

    private void writeContentToIOFile(byte[] revisionContent) throws IOException {
      FileUtil.writeToFile(myFilePath.getIOFile(), revisionContent);
    }

    private void writeContentToDocument(final Document document, byte[] revisionContent) throws IOException {
      final String content = StringUtil.convertLineSeparators(new String(revisionContent, myFilePath.getCharset().name()));

      CommandProcessor.getInstance().executeCommand(myVcs.getProject(), () -> document.replaceString(0, document.getTextLength(), content),
                                                    VcsBundle.message("message.title.get.version"), null);
    }
  }

  private class MyAnnotateAction extends AnnotateRevisionActionBase implements DumbAware {
    public MyAnnotateAction() {
      super(VcsBundle.message("annotate.action.name"), VcsBundle.message("annotate.action.description"), AllIcons.Actions.Annotate);
      setShortcutSet(ActionManager.getInstance().getAction("Annotate").getShortcutSet());
    }

    @Nullable
    @Override
    protected Editor getEditor(@Nonnull AnActionEvent e) {
      VirtualFile virtualFile = getVirtualFile();
      if (virtualFile == null) return null;

      Editor editor = e.getData(CommonDataKeys.EDITOR);
      if (editor != null) {
        VirtualFile editorFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (Comparing.equal(editorFile, virtualFile)) return editor;
      }

      FileEditor fileEditor = FileEditorManager.getInstance(myProject).getSelectedEditor(virtualFile);
      if (fileEditor instanceof TextEditor) {
        return ((TextEditor)fileEditor).getEditor();
      }
      return null;
    }

    @Nullable
    @Override
    protected AbstractVcs getVcs(@Nonnull AnActionEvent e) {
      return myVcs;
    }

    @Nullable
    @Override
    protected VirtualFile getFile(@Nonnull AnActionEvent e) {
      final Boolean nonLocal = e.getData(VcsDataKeys.VCS_NON_LOCAL_HISTORY_SESSION);
      if (Boolean.TRUE.equals(nonLocal)) return null;

      VirtualFile file = e.getData(VcsDataKeys.VCS_VIRTUAL_FILE);
      if (file == null || file.isDirectory()) return null;
      if (myFilePath.getFileType().isBinary()) return null;
      return file;
    }

    @Nullable
    @Override
    protected VcsFileRevision getFileRevision(@Nonnull AnActionEvent e) {
      VcsFileRevision revision = e.getData(VcsDataKeys.VCS_FILE_REVISION);

      if (!myHistorySession.isContentAvailable(revision)) return null;

      return revision;
    }
  }

  private class RefreshFileHistoryAction extends RefreshAction implements DumbAware {
    public RefreshFileHistoryAction() {
      super(VcsBundle.message("action.name.refresh"), VcsBundle.message("action.description.refresh"), AllIcons.Actions.Refresh);
      registerShortcutOn(FileHistoryPanelImpl.this);
    }

    public void actionPerformed(AnActionEvent e) {
      if (myInRefresh) return;
      refreshUiAndScheduleDataRefresh(false);
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(!myInRefresh);
    }
  }

  public class MyCreatePatch extends DumbAwareAction {
    private final CreatePatchFromChangesAction myUsualDelegate;

    public MyCreatePatch() {
      super(VcsBundle.message("action.name.create.patch.for.selected.revisions"),
            VcsBundle.message("action.description.create.patch.for.selected.revisions"), PlatformIconGroup.filetypesPatch());
      myUsualDelegate = new CreatePatchFromChangesAction();
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      if (myFilePath.isDirectory()) {
        final List<TreeNodeOnVcsRevision> selection = getSelection();
        if (selection.size() != 1) return;
        ProgressManager.getInstance().run(new FolderPatchCreationTask(myVcs, selection.get(0)));
      }
      else {
        myUsualDelegate.actionPerformed(e);
      }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setVisible(true);
      if (myFilePath.isNonLocal()) {
        e.getPresentation().setEnabled(false);
        return;
      }
      boolean enabled = (!myFilePath.isDirectory()) || myProvider.supportsHistoryForDirectories();
      final int selectionSize = getSelection().size();
      if (enabled && (!myFilePath.isDirectory())) {
        // in order to do not load changes only for action update
        enabled = (selectionSize > 0) && (selectionSize < 3);
      }
      else if (enabled) {
        enabled = selectionSize == 1 && getSelection().get(0).getChangedRepositoryPath() != null;
      }
      e.getPresentation().setEnabled(enabled);
    }
  }

  private class MyShowDetailsAction extends ToggleAction implements DumbAware {

    public MyShowDetailsAction() {
      super("Show Details", "Display details panel", AllIcons.Actions.Preview);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return getConfiguration().SHOW_FILE_HISTORY_DETAILS;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      getConfiguration().SHOW_FILE_HISTORY_DETAILS = state;
      setupDetails();
    }
  }

  private class TreeNodeColumnInfoWrapper<T extends Comparable<T>> extends FileHistoryColumnWrapper<T> {
    TreeNodeColumnInfoWrapper(@Nonnull ColumnInfo<VcsFileRevision, T> additionalColumn) {
      super(additionalColumn);
    }

    @Override
    protected DualView getDualView() {
      return myDualView;
    }
  }
}
