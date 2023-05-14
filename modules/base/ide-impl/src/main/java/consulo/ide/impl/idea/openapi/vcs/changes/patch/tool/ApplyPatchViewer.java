/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.patch.tool;

import consulo.diff.DiffContentFactory;
import consulo.ide.impl.idea.diff.DiffContext;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.ide.impl.idea.diff.actions.ProxyUndoRedoAction;
import consulo.ide.impl.idea.diff.actions.impl.FocusOppositePaneAction;
import consulo.ide.impl.idea.diff.actions.impl.SetEditorSettingsAction;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.ide.impl.idea.diff.merge.MergeModelBase;
import consulo.diff.request.SimpleDiffRequest;
import consulo.ide.impl.idea.diff.tools.fragmented.LineNumberConvertor;
import consulo.ide.impl.idea.diff.tools.holders.TextEditorHolder;
import consulo.ide.impl.idea.diff.tools.util.*;
import consulo.ide.impl.idea.diff.tools.util.base.TextDiffSettingsHolder.TextDiffSettings;
import consulo.ide.impl.idea.diff.tools.util.base.TextDiffViewerUtil;
import consulo.ide.impl.idea.diff.tools.util.side.TwosideContentPanel;
import consulo.ide.impl.idea.diff.util.*;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.openapi.actionSystem.*;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.diff.util.Side;
import consulo.language.editor.CommonDataKeys;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.ide.impl.idea.openapi.diff.DiffBundle;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.document.impl.DocumentImpl;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.dataContext.DataProvider;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.AppliedTextPatch;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.*;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

class ApplyPatchViewer implements DataProvider, Disposable {
  public static final Logger LOG = Logger.getInstance(ApplyPatchViewer.class);

  @jakarta.annotation.Nullable
  private final Project myProject;
  @Nonnull
  private final DiffContext myContext;
  @Nonnull
  private final ApplyPatchRequest myPatchRequest;

  @Nonnull
  private final TextEditorHolder myResultHolder;
  @Nonnull
  private final TextEditorHolder myPatchHolder;
  @Nonnull
  private final EditorEx myResultEditor;
  @Nonnull
  private final EditorEx myPatchEditor;

  @Nonnull
  private final SimpleDiffPanel myPanel;
  @Nonnull
  private final TwosideContentPanel myContentPanel;

  @Nonnull
  private final MyModel myModel;

  @Nonnull
  private final FocusTrackerSupport<Side> myFocusTrackerSupport;
  @Nonnull
  private final MyPrevNextDifferenceIterable myPrevNextDifferenceIterable;
  @Nonnull
  private final StatusPanel myStatusPanel;
  @Nonnull
  private final MyFoldingModel myFoldingModel;

  @Nonnull
  private final SetEditorSettingsAction myEditorSettingsAction;

  // Changes with known AppliedTo. Ordered as in result-editor
  @Nonnull
  private final List<ApplyPatchChange> myResultChanges = new ArrayList<>();
  // All changes. Ordered as in patch-editor
  @Nonnull
  private final List<ApplyPatchChange> myPatchChanges = new ArrayList<>();
  // All changes. Ordered as in result-editor. Non-applied changes are at the very beginning with model ranges [-1. -1)
  @Nonnull
  private final List<ApplyPatchChange> myModelChanges = new ArrayList<>();

  private boolean myDisposed;

  public ApplyPatchViewer(@Nonnull DiffContext context, @Nonnull ApplyPatchRequest request) {
    myProject = context.getProject();
    myContext = context;
    myPatchRequest = request;


    DocumentContent resultContent = request.getResultContent();
    DocumentContent patchContent = DiffContentFactory.getInstance().create(new DocumentImpl("", true), resultContent);

    myResultHolder = TextEditorHolder.create(myProject, resultContent);
    myPatchHolder = TextEditorHolder.create(myProject, patchContent);

    myResultEditor = myResultHolder.getEditor();
    myPatchEditor = myPatchHolder.getEditor();

    if (isReadOnly()) myResultEditor.setViewer(true);
    myPatchEditor.setViewer(true);

    DiffUtil.disableBlitting(myResultEditor);
    DiffUtil.disableBlitting(myPatchEditor);

    ((EditorMarkupModel)myResultEditor.getMarkupModel()).setErrorStripeVisible(false);
    myResultEditor.setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);

    myPatchEditor.getGutterComponentEx().setForceShowRightFreePaintersArea(true);
    ((EditorMarkupModel)myPatchEditor.getMarkupModel()).setErrorStripeVisible(false);


    List<TextEditorHolder> holders = ContainerUtil.list(myResultHolder, myPatchHolder);
    List<EditorEx> editors = ContainerUtil.list(myResultEditor, myPatchEditor);
    JComponent resultTitle = DiffUtil.createTitle(myPatchRequest.getResultTitle());
    JComponent patchTitle = DiffUtil.createTitle(myPatchRequest.getPatchTitle());
    List<JComponent> titleComponents = DiffUtil.createSyncHeightComponents(ContainerUtil.list(resultTitle, patchTitle));

    myContentPanel = TwosideContentPanel.createFromHolders(holders);
    myContentPanel.setTitles(titleComponents);
    myPanel = new SimpleDiffPanel(myContentPanel, this, myContext);

    myModel = new MyModel(myProject, myResultEditor.getDocument());

    myFocusTrackerSupport = new FocusTrackerSupport.Twoside(holders);
    myFocusTrackerSupport.setCurrentSide(Side.LEFT);
    myPrevNextDifferenceIterable = new MyPrevNextDifferenceIterable();
    myStatusPanel = new MyStatusPanel();
    myFoldingModel = new MyFoldingModel(myResultEditor, this);


    new MyFocusOppositePaneAction().install(myPanel);
    new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(editors, myPanel);

    new TextDiffViewerUtil.EditorFontSizeSynchronizer(editors).install(this);

    myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), editors);
    myEditorSettingsAction.applyDefaults();

    if (!isReadOnly()) {
      DiffUtil.registerAction(new ApplySelectedChangesAction(true), myPanel);
      DiffUtil.registerAction(new IgnoreSelectedChangesAction(true), myPanel);
    }

    ProxyUndoRedoAction.register(myProject, myResultEditor, myContentPanel);
  }

  @Nonnull
  protected List<AnAction> createToolbarActions() {
    List<AnAction> group = new ArrayList<>();

    if (!isReadOnly()) {
      group.add(new MyToggleExpandByDefaultAction());
      group.add(myEditorSettingsAction);
      group.add(AnSeparator.getInstance());
      group.add(new ShowDiffWithLocalAction());
      group.add(new ApplyNonConflictsAction());
    }

    return group;
  }

  @Nonnull
  private List<AnAction> createEditorPopupActions() {
    List<AnAction> group = new ArrayList<>();

    if (!isReadOnly()) {
      group.add(new ApplySelectedChangesAction(false));
      group.add(new IgnoreSelectedChangesAction(false));
    }

    group.add(AnSeparator.getInstance());
    group.addAll(TextDiffViewerUtil.createEditorPopupActions());

    return group;
  }

  @Override
  public void dispose() {
    if (myDisposed) return;
    myDisposed = true;

    myFoldingModel.destroy();

    Disposer.dispose(myModel);

    Disposer.dispose(myResultHolder);
    Disposer.dispose(myPatchHolder);
  }

  //
  // Getters
  //

  public boolean isReadOnly() {
    return !DiffUtil.canMakeWritable(myResultEditor.getDocument());
  }

  @Nonnull
  public MyModel getModel() {
    return myModel;
  }

  @Nonnull
  public List<ApplyPatchChange> getModelChanges() {
    return myModelChanges;
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  @Nonnull
  public StatusPanel getStatusPanel() {
    return myStatusPanel;
  }

  @Nonnull
  public JComponent getComponent() {
    return myPanel;
  }

  @jakarta.annotation.Nullable
  public JComponent getPreferredFocusedComponent() {
    return myResultEditor.getContentComponent();
  }

  @Nonnull
  public EditorEx getResultEditor() {
    return myResultEditor;
  }

  @Nonnull
  public EditorEx getPatchEditor() {
    return myPatchEditor;
  }

  @Nonnull
  public Side getCurrentSide() {
    return myFocusTrackerSupport.getCurrentSide();
  }

  @Nonnull
  public List<ApplyPatchChange> getPatchChanges() {
    return myPatchChanges;
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (CommonDataKeys.PROJECT == dataId) {
      return myProject;
    }
    else if (DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE == dataId) {
      return myPrevNextDifferenceIterable;
    }
    return null;
  }

  @Nonnull
  public TextDiffSettings getTextSettings() {
    return TextDiffSettings.getSettings("ApplyPatch");
  }

  @Nonnull
  public FoldingModelSupport.Settings getFoldingModelSettings() {
    TextDiffSettings settings = getTextSettings();
    return new FoldingModelSupport.Settings(settings.getContextRange(), settings.isExpandByDefault());
  }

  //
  // Impl
  //

  protected void initPatchViewer() {
    final Document outputDocument = myResultEditor.getDocument();
    DiffUtil.executeWriteCommand(outputDocument, myProject, "Init merge content", () -> {
      outputDocument.setText(myPatchRequest.getLocalContent());
      if (!isReadOnly()) DiffUtil.putNonundoableOperation(myProject, outputDocument);
    });


    PatchChangeBuilder builder = new PatchChangeBuilder();
    builder.exec(myPatchRequest.getPatch().getHunks());


    Document patchDocument = myPatchEditor.getDocument();
    patchDocument.setText(builder.getPatchContent());

    LineNumberConvertor convertor = builder.getLineConvertor();
    myPatchEditor.getGutterComponentEx().setLineNumberConvertor(convertor.createConvertor1(), convertor.createConvertor2());

    IntList lines = builder.getSeparatorLines();
    for (int i = 0; i < lines.size(); i++) {
      int offset = patchDocument.getLineStartOffset(lines.get(i));
      DiffDrawUtil.createLineSeparatorHighlighter(myPatchEditor, offset, offset, BooleanGetter.TRUE);
    }


    List<PatchChangeBuilder.Hunk> hunks = builder.getHunks();

    int[] modelToPatchIndexes = DiffUtil.getSortedIndexes(hunks, (h1, h2) -> {
      LineRange lines1 = h1.getAppliedToLines();
      LineRange lines2 = h2.getAppliedToLines();
      if (lines1 == null && lines2 == null) return 0;
      if (lines1 == null) return -1;
      if (lines2 == null) return 1;
      return lines1.start - lines2.start;
    });
    int[] patchToModelIndexes = DiffUtil.invertIndexes(modelToPatchIndexes);

    List<LineRange> modelRanges = new ArrayList<>();
    for (int modelIndex = 0; modelIndex < hunks.size(); modelIndex++) {
      int patchIndex = modelToPatchIndexes[modelIndex];
      PatchChangeBuilder.Hunk hunk = hunks.get(patchIndex);
      LineRange resultRange = hunk.getAppliedToLines();

      ApplyPatchChange change = new ApplyPatchChange(hunk, modelIndex, this);

      myModelChanges.add(change);
      if (resultRange != null) myResultChanges.add(change);

      modelRanges.add(resultRange != null ? resultRange : new LineRange(-1, -1));
    }
    myModel.setChanges(modelRanges);

    for (int index : patchToModelIndexes) {
      myPatchChanges.add(myModelChanges.get(index));
    }


    myFoldingModel.install(myResultChanges, getFoldingModelSettings());

    for (ApplyPatchChange change : myModelChanges) {
      change.reinstallHighlighters();
    }
    myStatusPanel.update();


    myContentPanel.setPainter(new MyDividerPainter());

    VisibleAreaListener areaListener = (e) -> myContentPanel.repaint();
    myResultEditor.getScrollingModel().addVisibleAreaListener(areaListener);
    myPatchEditor.getScrollingModel().addVisibleAreaListener(areaListener);

    myPatchEditor.getGutterComponentEx().revalidateMarkup();


    if (myResultChanges.size() > 0) {
      scrollToChange(myResultChanges.get(0), Side.LEFT, true);
    }
  }

  public void scrollToChange(@Nonnull ApplyPatchChange change, @Nonnull Side masterSide, boolean forceScroll) {
    if (change.getResultRange() == null) {
      DiffUtil.moveCaret(myPatchEditor, change.getPatchRange().start);
      myPatchEditor.getScrollingModel().scrollToCaret(forceScroll ? ScrollType.CENTER : ScrollType.MAKE_VISIBLE);
    }
    else {
      LineRange resultRange = change.getResultRange();
      LineRange patchRange = change.getPatchAffectedRange();

      int topShift = -1;
      if (!forceScroll) {
        int masterLine = masterSide.select(resultRange.start, patchRange.start);
        EditorEx masterEditor = masterSide.select(myResultEditor, myPatchEditor);
        int targetY = masterEditor.logicalPositionToXY(new LogicalPosition(masterLine, 0)).y;
        int scrollOffset = masterEditor.getScrollingModel().getVerticalScrollOffset();
        topShift = targetY - scrollOffset;
      }

      int[] offsets = SyncScrollSupport.getTargetOffsets(myResultEditor, myPatchEditor,
                                                         resultRange.start, resultRange.end,
                                                         patchRange.start, patchRange.end,
                                                         topShift);

      DiffUtil.moveCaret(myResultEditor, resultRange.start);
      DiffUtil.moveCaret(myPatchEditor, patchRange.start);

      DiffUtil.scrollToPoint(myResultEditor, new Point(0, offsets[0]), false);
      DiffUtil.scrollToPoint(myPatchEditor, new Point(0, offsets[1]), false);
    }
  }

  //
  // Modification operations
  //

  public void repaintDivider() {
    myContentPanel.repaintDivider();
  }

  public void executeCommand(@Nullable String commandName,
                             @Nonnull final Runnable task) {
    myModel.executeMergeCommand(commandName, null, UndoConfirmationPolicy.DEFAULT, false, null, task);
  }

  class MyModel extends MergeModelBase<ApplyPatchChange.State> {
    public MyModel(@Nullable Project project, @Nonnull Document document) {
      super(project, document);
    }

    @Override
    protected void reinstallHighlighters(int index) {
      ApplyPatchChange change = myModelChanges.get(index);
      change.reinstallHighlighters();
    }

    @Nonnull
    @Override
    protected ApplyPatchChange.State storeChangeState(int index) {
      ApplyPatchChange change = myModelChanges.get(index);
      return change.storeState();
    }

    @Override
    protected void restoreChangeState(@Nonnull ApplyPatchChange.State state) {
      super.restoreChangeState(state);
      ApplyPatchChange change = myModelChanges.get(state.myIndex);

      boolean wasResolved = change.isResolved();
      change.restoreState(state);
      if (wasResolved != change.isResolved()) onChangeResolved();
    }
  }

  protected void onChangeResolved() {
    if (isDisposed()) return;
    myStatusPanel.update();
  }

  public void markChangeResolved(@Nonnull ApplyPatchChange change) {
    if (change.isResolved()) return;

    change.setResolved(true);
    myModel.invalidateHighlighters(change.getIndex());
    onChangeResolved();
  }

  public void replaceChange(@Nonnull ApplyPatchChange change) {
    LineRange resultRange = change.getResultRange();
    LineRange patchRange = change.getPatchInsertionRange();
    if (resultRange == null || change.isResolved()) return;
    if (change.getStatus() != AppliedTextPatch.HunkStatus.EXACTLY_APPLIED) return;

    List<String> newContent = DiffUtil.getLines(myPatchEditor.getDocument(), patchRange.start, patchRange.end);
    myModel.replaceChange(change.getIndex(), newContent);

    markChangeResolved(change);
  }

  private class ApplySelectedChangesAction extends ApplySelectedChangesActionBase {
    private ApplySelectedChangesAction(boolean shortcut) {
      super(shortcut);
      getTemplatePresentation().setText("Accept");
      getTemplatePresentation().setIcon(AllIcons.Actions.Checked);
      copyShortcutFrom(ActionManager.getInstance().getAction("Diff.ApplyRightSide"));
    }

    @Override
    protected boolean isEnabled(@Nonnull ApplyPatchChange change) {
      return !change.isResolved() && change.getStatus() == AppliedTextPatch.HunkStatus.EXACTLY_APPLIED;
    }

    @Override
    protected void apply(@Nonnull List<ApplyPatchChange> changes) {
      for (int i = changes.size() - 1; i >= 0; i--) {
        replaceChange(changes.get(i));
      }
    }
  }

  private class IgnoreSelectedChangesAction extends ApplySelectedChangesActionBase {
    private IgnoreSelectedChangesAction(boolean shortcut) {
      super(shortcut);
      getTemplatePresentation().setText("Ignore");
      getTemplatePresentation().setIcon(AllIcons.Diff.Remove);
      setShortcutSet(new CompositeShortcutSet(ActionManager.getInstance().getAction("Diff.IgnoreRightSide").getShortcutSet(),
                                              ActionManager.getInstance().getAction("Diff.ApplyLeftSide").getShortcutSet()));
    }

    @Override
    protected boolean isEnabled(@Nonnull ApplyPatchChange change) {
      return !change.isResolved();
    }

    @Override
    protected void apply(@Nonnull List<ApplyPatchChange> changes) {
      for (ApplyPatchChange change : changes) {
        markChangeResolved(change);
      }
    }
  }

  private abstract class ApplySelectedChangesActionBase extends DumbAwareAction {
    private final boolean myShortcut;

    public ApplySelectedChangesActionBase(boolean shortcut) {
      myShortcut = shortcut;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (myShortcut) {
        // consume shortcut even if there are nothing to do - avoid calling some other action
        e.getPresentation().setEnabledAndVisible(true);
        return;
      }

      Presentation presentation = e.getPresentation();
      Editor editor = e.getData(CommonDataKeys.EDITOR);

      Side side = Side.fromValue(ContainerUtil.list(myResultEditor, myPatchEditor), editor);
      if (side == null) {
        presentation.setEnabledAndVisible(false);
        return;
      }

      presentation.setVisible(true);
      presentation.setEnabled(isSomeChangeSelected(side));
    }

    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      final Side side = Side.fromValue(ContainerUtil.list(myResultEditor, myPatchEditor), editor);
      if (editor == null || side == null) return;

      final List<ApplyPatchChange> selectedChanges = getSelectedChanges(side);
      if (selectedChanges.isEmpty()) return;

      String title = e.getPresentation().getText() + " in patch resolve";

      executeCommand(title, () -> {
        apply(selectedChanges);
      });
    }

    private boolean isSomeChangeSelected(@Nonnull Side side) {
      EditorEx editor = side.select(myResultEditor, myPatchEditor);
      List<Caret> carets = editor.getCaretModel().getAllCarets();
      if (carets.size() != 1) return true;
      Caret caret = carets.get(0);
      if (caret.hasSelection()) return true;

      int line = editor.getDocument().getLineNumber(editor.getExpectedCaretOffset());

      List<ApplyPatchChange> changes = myModelChanges;
      for (ApplyPatchChange change : changes) {
        if (!isEnabled(change)) continue;
        LineRange range = side.select(change.getResultRange(), change.getPatchRange());
        if (range == null) continue;

        if (DiffUtil.isSelectedByLine(line, range.start, range.end)) return true;
      }
      return false;
    }

    @Nonnull
    @CalledInAwt
    private List<ApplyPatchChange> getSelectedChanges(@Nonnull Side side) {
      final BitSet lines = DiffUtil.getSelectedLines(side.select(myResultEditor, myPatchEditor));

      List<ApplyPatchChange> affectedChanges = new ArrayList<>();
      for (ApplyPatchChange change : myModelChanges) {
        if (!isEnabled(change)) continue;
        LineRange range = side.select(change.getResultRange(), change.getPatchRange());
        if (range == null) continue;

        if (DiffUtil.isSelectedByLine(lines, range.start, range.end)) {
          affectedChanges.add(change);
        }
      }
      return affectedChanges;
    }

    protected abstract boolean isEnabled(@Nonnull ApplyPatchChange change);

    @RequiredWriteAction
    protected abstract void apply(@Nonnull List<ApplyPatchChange> changes);
  }

  private class ApplyNonConflictsAction extends DumbAwareAction {
    public ApplyNonConflictsAction() {
      ActionUtil.copyFrom(this, "Diff.ApplyNonConflicts");
    }

    @Override
    public void update(AnActionEvent e) {
      boolean enabled = ContainerUtil.exists(myModelChanges, c -> {
        if (c.isResolved()) return false;
        if (c.getStatus() == AppliedTextPatch.HunkStatus.NOT_APPLIED) return false;
        return true;
      });
      e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      List<ApplyPatchChange> changes = myModelChanges;
      if (changes.isEmpty()) return;

      executeCommand("Apply Non Conflicted Changes", () -> {
        for (int i = changes.size() - 1; i >= 0; i--) {
          ApplyPatchChange change = changes.get(i);
          switch (change.getStatus()) {
            case ALREADY_APPLIED:
              markChangeResolved(change);
              break;
            case EXACTLY_APPLIED:
              replaceChange(change);
              break;
            case NOT_APPLIED:
              break;
          }
        }
      });
    }
  }

  //
  // Actions
  //

  private class MyFocusOppositePaneAction extends FocusOppositePaneAction {
    public MyFocusOppositePaneAction() {
      super(false);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      EditorEx targetEditor = getCurrentSide().other().select(myResultEditor, myPatchEditor);
      DiffUtil.requestFocus(myProject, targetEditor.getContentComponent());
    }
  }

  private class MyToggleExpandByDefaultAction extends TextDiffViewerUtil.ToggleExpandByDefaultAction {
    public MyToggleExpandByDefaultAction() {
      super(getTextSettings());
    }

    @Override
    protected void expandAll(boolean expand) {
      myFoldingModel.expandAll(expand);
    }
  }

  private class ShowDiffWithLocalAction extends DumbAwareAction {
    public ShowDiffWithLocalAction() {
      super("Compare with local content", null, AllIcons.Diff.Diff);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      DocumentContent resultContent = myPatchRequest.getResultContent();
      DocumentContent localContent = DiffContentFactory.getInstance().create(myPatchRequest.getLocalContent(), resultContent);

      SimpleDiffRequest request = new SimpleDiffRequest(myPatchRequest.getTitle(),
                                                        localContent, resultContent,
                                                        myPatchRequest.getLocalTitle(), myPatchRequest.getResultTitle());

      LogicalPosition currentPosition = DiffUtil.getCaretPosition(myResultEditor);
      request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.RIGHT, currentPosition.line));

      DiffManager.getInstance().showDiff(myProject, request, new DiffDialogHints(null, myPanel));
    }
  }

  //
  // Helpers
  //

  private class MyPrevNextDifferenceIterable extends PrevNextDifferenceIterableBase<ApplyPatchChange> {
    @Nonnull
    @Override
    protected List<ApplyPatchChange> getChanges() {
      return getCurrentSide().select(myResultChanges, myPatchChanges);
    }

    @Nonnull
    @Override
    protected EditorEx getEditor() {
      return getCurrentSide().select(myResultEditor, myPatchEditor);
    }

    @Override
    protected int getStartLine(@Nonnull ApplyPatchChange change) {
      //noinspection ConstantConditions
      return getCurrentSide().select(change.getResultRange(), change.getPatchAffectedRange()).start;
    }

    @Override
    protected int getEndLine(@Nonnull ApplyPatchChange change) {
      //noinspection ConstantConditions
      return getCurrentSide().select(change.getResultRange(), change.getPatchAffectedRange()).end;
    }

    @Override
    protected void scrollToChange(@Nonnull ApplyPatchChange change) {
      ApplyPatchViewer.this.scrollToChange(change, getCurrentSide(), true);
    }
  }

  private class MyDividerPainter implements DiffSplitter.Painter, DiffDividerDrawUtil.DividerPaintable {
    @Override
    public void paint(@Nonnull Graphics g, @Nonnull JComponent divider) {
      Graphics2D gg = DiffDividerDrawUtil.getDividerGraphics(g, divider, myPatchEditor.getComponent());

      gg.setColor(TargetAWT.to(DiffDrawUtil.getDividerColor(myPatchEditor)));
      gg.fill(gg.getClipBounds());

      DiffDividerDrawUtil.paintPolygons(gg, divider.getWidth(), myResultEditor, myPatchEditor, this);

      gg.dispose();
    }

    @Override
    public void process(@Nonnull Handler handler) {
      for (ApplyPatchChange change : myResultChanges) {
        LineRange resultRange = change.getResultRange();
        LineRange patchRange = change.getPatchRange();
        assert resultRange != null;

        ColorValue color = change.getDiffType().getColor(myPatchEditor);

        // do not abort - ranges are ordered in patch order, but they can be not ordered in terms of resultRange
        handler.process(resultRange.start, resultRange.end, patchRange.start, patchRange.end, color, change.isResolved());
      }
    }
  }

  private static class MyFoldingModel extends FoldingModelSupport {
    private final MyPaintable myPaintable = new MyPaintable(0, 1);

    public MyFoldingModel(@Nonnull EditorEx editor, @Nonnull Disposable disposable) {
      super(new EditorEx[]{editor}, disposable);
    }

    public void install(@jakarta.annotation.Nullable List<ApplyPatchChange> changes,
                        @Nonnull FoldingModelSupport.Settings settings) {
      //noinspection ConstantConditions
      Iterator<int[]> it = map(changes, fragment -> new int[]{
              fragment.getResultRange().start,
              fragment.getResultRange().end
      });
      install(it, null, settings);
    }
  }

  private class MyStatusPanel extends StatusPanel {
    @Nullable
    @Override
    protected String getMessage() {
      int totalUnresolved = 0;
      int alreadyApplied = 0;
      int notApplied = 0;
      for (ApplyPatchChange change : myPatchChanges) {
        if (change.isResolved()) continue;

        totalUnresolved++;
        switch (change.getStatus()) {
          case ALREADY_APPLIED:
            alreadyApplied++;
            break;
          case NOT_APPLIED:
            notApplied++;
            break;
          case EXACTLY_APPLIED:
            break;
        }
      }

      if (totalUnresolved == 0) {
        return DiffBundle.message("apply.somehow.status.message.all.applied", notApplied);
      }
      if (totalUnresolved == notApplied) {
        return DiffBundle.message("apply.somehow.status.message.cant.apply", notApplied);
      }
      else {
        String message = DiffBundle.message("apply.somehow.status.message.cant.apply.some", notApplied, totalUnresolved);
        if (alreadyApplied == 0) return message;
        return message + ". " + DiffBundle.message("apply.somehow.status.message.already.applied", alreadyApplied);
      }
    }
  }
}
