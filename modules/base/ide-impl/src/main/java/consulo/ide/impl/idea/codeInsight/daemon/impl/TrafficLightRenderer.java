// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.AllIcons;
import consulo.application.HeavyProcessLatch;
import consulo.application.PowerSaveMode;
import consulo.application.ReadAction;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorBundle;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.MarkupModelListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.compiler.ProblemsView;
import consulo.component.ProcessCanceledException;
import consulo.configurable.ConfigurationException;
import consulo.diff.DiffUserDataKeys;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.Language;
import consulo.language.editor.*;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.HighlightLevelUtil;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.internal.daemon.ConfigureInspectionsAction;
import consulo.language.editor.impl.internal.daemon.DaemonEditorPopup;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.editor.impl.internal.markup.*;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.internal.HighlightingSettingsPerFile;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.lang.DeprecatedMethodException;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class TrafficLightRenderer implements ErrorStripeRenderer, Disposable {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final Document myDocument;
  private final DaemonCodeAnalyzerImpl myDaemonCodeAnalyzer;
  private final SeverityRegistrarImpl mySeverityRegistrar;
  private Image icon;
  String statistics;
  String statusLabel;
  String statusExtraLine;
  boolean passStatusesVisible;
  final Map<ProgressableTextEditorHighlightingPass, Pair<JProgressBar, JLabel>> passes = new LinkedHashMap<>();
  static final int MAX = 100;
  boolean progressBarsEnabled;
  Boolean progressBarsCompleted;

  /**
   * array filled with number of highlighters with a given severity.
   * errorCount[idx] == number of highlighters of severity with index idx in this markup model.
   * severity index can be obtained via consulo.ide.impl.idea.codeInsight.daemon.impl.SeverityRegistrar#getSeverityIdx(consulo.ide.impl.idea.lang.annotation.HighlightSeverity)
   */
  protected int[] errorCount;

  /**
   * @deprecated Please use {@link #TrafficLightRenderer(Project, Document)} instead
   */
  @Deprecated
  public TrafficLightRenderer(Project project, Document document, PsiFile psiFile) {
    this(project, document);
    DeprecatedMethodException.report("Please use TrafficLightRenderer(Project, Document) instead");
  }

  public TrafficLightRenderer(@Nonnull Project project, @Nonnull Document document) {
    myProject = project;
    myDaemonCodeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    myDocument = document;
    mySeverityRegistrar = (SeverityRegistrarImpl)SeverityRegistrar.getSeverityRegistrar(myProject);

    refresh(null);

    final MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
    model.addMarkupModelListener(this, new MarkupModelListener() {
      @Override
      public void afterAdded(@Nonnull RangeHighlighter highlighter) {
        incErrorCount(highlighter, 1);
      }

      @Override
      public void beforeRemoved(@Nonnull RangeHighlighter highlighter) {
        incErrorCount(highlighter, -1);
      }
    });
    UIUtil.invokeLaterIfNeeded(() -> {
      for (RangeHighlighter rangeHighlighter : model.getAllHighlighters()) {
        incErrorCount(rangeHighlighter, 1);
      }
    });
  }

  private PsiFile getPsiFile() {
    return ReadAction.compute(() -> PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument));
  }

  @Nonnull
  public SeverityRegistrar getSeverityRegistrar() {
    return mySeverityRegistrar;
  }

  public void refresh(@Nullable EditorMarkupModel editorMarkupModel) {
    int maxIndex = mySeverityRegistrar.getSeverityMaxIndex();
    if (errorCount != null && maxIndex + 1 == errorCount.length) return;
    errorCount = new int[maxIndex + 1];
  }

  @Override
  public void dispose() {
  }

  private void incErrorCount(RangeHighlighter highlighter, int delta) {
    HighlightInfoImpl info = HighlightInfoImpl.fromRangeHighlighter(highlighter);
    if (info == null) return;
    HighlightSeverity infoSeverity = info.getSeverity();
    if (infoSeverity.myVal <= HighlightSeverity.INFORMATION.myVal) return;
    final int severityIdx = mySeverityRegistrar.getSeverityIdx(infoSeverity);
    if (severityIdx != -1) {
      errorCount[severityIdx] += delta;
    }
  }

  public boolean isValid() {
    return getPsiFile() != null;
  }

  protected static final class DaemonCodeAnalyzerStatus {
    public boolean errorAnalyzingFinished; // all passes done
    List<ProgressableTextEditorHighlightingPass> passes = Collections.emptyList();
    public int[] errorCount = ArrayUtil.EMPTY_INT_ARRAY;
    // Used in Rider
    public String reasonWhyDisabled;
    // Used in Rider
    public String reasonWhySuspended;

    private HeavyProcessLatch.Type heavyProcessType;

    public DaemonCodeAnalyzerStatus() {
    }

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder("DS: finished=" + errorAnalyzingFinished);
      s.append("; pass statuses: ").append(passes.size()).append("; ");
      for (ProgressableTextEditorHighlightingPass passStatus : passes) {
        s.append(String.format("(%s %2.0f%% %b)", passStatus.getPresentableName(), passStatus.getProgress() * 100, passStatus.isFinished()));
      }
      s.append("; error count: ").append(errorCount.length).append(": ").append(IntLists.newArrayList(errorCount));
      return s.toString();
    }
  }

  @Nonnull
  protected DaemonCodeAnalyzerStatus getDaemonCodeAnalyzerStatus(@Nonnull SeverityRegistrar severityRegistrar) {
    DaemonCodeAnalyzerStatus status = new DaemonCodeAnalyzerStatus();
    PsiFile psiFile = getPsiFile();
    if (psiFile == null) {
      status.reasonWhyDisabled = DaemonBundle.message("process.title.no.file");
      status.errorAnalyzingFinished = true;
      return status;
    }
    if (myProject.isDisposed()) {
      status.reasonWhyDisabled = DaemonBundle.message("process.title.project.is.disposed");
      status.errorAnalyzingFinished = true;
      return status;
    }
    if (!myDaemonCodeAnalyzer.isHighlightingAvailable(psiFile)) {
      if (!psiFile.isPhysical()) {
        status.reasonWhyDisabled = DaemonBundle.message("process.title.file.is.generated");
        status.errorAnalyzingFinished = true;
        return status;
      }
      if (psiFile instanceof PsiCompiledElement) {
        status.reasonWhyDisabled = DaemonBundle.message("process.title.file.is.decompiled");
        status.errorAnalyzingFinished = true;
        return status;
      }
      final FileType fileType = psiFile.getFileType();
      if (fileType.isBinary()) {
        status.reasonWhyDisabled = DaemonBundle.message("process.title.file.is.binary");
        status.errorAnalyzingFinished = true;
        return status;
      }
      status.reasonWhyDisabled = DaemonBundle.message("process.title.highlighting.is.disabled.for.this.file");
      status.errorAnalyzingFinished = true;
      return status;
    }

    FileViewProvider provider = psiFile.getViewProvider();
    Set<Language> languages = provider.getLanguages();
    HighlightingSettingsPerFile levelSettings = HighlightingSettingsPerFile.getInstance(myProject);
    boolean shouldHighlight = languages.isEmpty();
    for (Language language : languages) {
      PsiFile root = provider.getPsi(language);
      FileHighlightingSetting level = levelSettings.getHighlightingSettingForRoot(root);
      shouldHighlight |= level != FileHighlightingSetting.SKIP_HIGHLIGHTING;
    }
    if (!shouldHighlight) {
      status.reasonWhyDisabled = DaemonBundle.message("process.title.highlighting.level.is.none");
      status.errorAnalyzingFinished = true;
      return status;
    }

    if (HeavyProcessLatch.INSTANCE.isRunning()) {
      HeavyProcessLatch.Operation op = ContainerUtil.find(HeavyProcessLatch.INSTANCE.getRunningOperations(), o -> o.getType() != HeavyProcessLatch.Type.Syncing);
      if (op == null) {
        status.reasonWhySuspended = DaemonBundle.message("process.title.heavy.operation.is.running");
        status.heavyProcessType = HeavyProcessLatch.Type.Processing;
      }
      else {
        status.reasonWhySuspended = op.getDisplayName();
        status.heavyProcessType = op.getType();
      }
      return status;
    }

    status.errorCount = errorCount.clone();

    List<ProgressableTextEditorHighlightingPass> passesToShowProgressFor = myDaemonCodeAnalyzer.getPassesToShowProgressFor(myDocument);
    status.passes = ContainerUtil.filter(passesToShowProgressFor, p -> !StringUtil.isEmpty(p.getPresentableName()) && p.getProgress() >= 0);

    status.errorAnalyzingFinished = myDaemonCodeAnalyzer.isAllAnalysisFinished(psiFile);
    status.reasonWhySuspended = myDaemonCodeAnalyzer.isUpdateByTimerEnabled() ? null : DaemonBundle.message("process.title.highlighting.is.paused.temporarily");
    fillDaemonCodeAnalyzerErrorsStatus(status, severityRegistrar);

    return status;
  }

  protected void fillDaemonCodeAnalyzerErrorsStatus(@Nonnull DaemonCodeAnalyzerStatus status, @Nonnull SeverityRegistrar severityRegistrar) {
  }

  @Nonnull
  protected final Project getProject() {
    return myProject;
  }

  @Override
  public void paint(@Nonnull Component c, Graphics g, @Nonnull Rectangle r) {
    DaemonCodeAnalyzerStatus status = getDaemonCodeAnalyzerStatus(mySeverityRegistrar);
    Icon icon = TargetAWT.to(getIcon(status));
    icon.paintIcon(c, g, r.x, r.y);
  }

  @Override
  public int getSquareSize() {
    return HighlightDisplayLevel.getEmptyIconDim();
  }

  @Nonnull
  private Image getIcon(@Nonnull DaemonCodeAnalyzerStatus status) {
    updatePanel(status);
    Image icon = this.icon;
    if (PowerSaveMode.isEnabled() || status.reasonWhySuspended != null || status.reasonWhyDisabled != null || status.errorAnalyzingFinished) {
      return icon;
    }
    return AllIcons.General.InspectionsEye;
  }

  // return true if panel needs to be rebuilt
  boolean updatePanel(@Nonnull DaemonCodeAnalyzerStatus status) {
    progressBarsEnabled = false;
    progressBarsCompleted = null;
    statistics = "";
    passStatusesVisible = false;
    statusLabel = null;
    statusExtraLine = null;

    boolean result = false;
    if (!status.passes.equals(new ArrayList<>(passes.keySet()))) {
      // passes set has changed
      rebuildPassesMap(status);
      result = true;
    }

    if (PowerSaveMode.isEnabled()) {
      statusLabel = DaemonBundle.message("label.code.analysis.is.disabled.in.power.save.mode");
      status.errorAnalyzingFinished = true;
      icon = AllIcons.General.InspectionsPowerSaveMode;
      return result;
    }
    if (status.reasonWhyDisabled != null) {
      statusLabel = DaemonBundle.message("label.no.analysis.has.been.performed");
      statusExtraLine = "(" + status.reasonWhyDisabled + ")";
      passStatusesVisible = true;
      progressBarsCompleted = Boolean.FALSE;
      icon = AllIcons.General.InspectionsTrafficOff;
      return result;
    }
    if (status.reasonWhySuspended != null) {
      statusLabel = DaemonBundle.message("label.code.analysis.has.been.suspended");
      statusExtraLine = "(" + status.reasonWhySuspended + ")";
      passStatusesVisible = true;
      progressBarsCompleted = Boolean.FALSE;
      icon = AllIcons.General.InspectionsPause;
      return result;
    }

    int lastNotNullIndex = ArrayUtil.lastIndexOfNot(status.errorCount, 0);
    Image icon = lastNotNullIndex == -1 ? AllIcons.General.InspectionsOK : mySeverityRegistrar.getRendererIconByIndex(lastNotNullIndex);

    if (status.errorAnalyzingFinished) {
      boolean isDumb = DumbService.isDumb(myProject);
      if (isDumb) {
        statusLabel = DaemonBundle.message("label.shallow.analysis.completed");
        statusExtraLine = DaemonBundle.message("label.complete.results.will.be.available.after.indexing");
      }
      else {
        statusLabel = "";
      }
      progressBarsCompleted = Boolean.TRUE;
    }
    else {
      statusLabel = DaemonBundle.message("performing.code.analysis");
      passStatusesVisible = true;
      progressBarsEnabled = true;
      progressBarsCompleted = null;
    }

    int currentSeverityErrors = 0;
    StringBuilder text = new StringBuilder();
    for (int i = lastNotNullIndex; i >= 0; i--) {
      int count = status.errorCount[i];
      if (count > 0) {
        final HighlightSeverity severity = mySeverityRegistrar.getSeverityByIndex(i);
        String name = count > 1 ? StringUtil.pluralize(StringUtil.toLowerCase(severity.getName())) : StringUtil.toLowerCase(severity.getName());
        text.append(status.errorAnalyzingFinished ? DaemonBundle.message("errors.found", count, name) : DaemonBundle.message("errors.found.so.far", count, name)).append("<br/>");
        currentSeverityErrors += count;
      }
    }
    if (currentSeverityErrors == 0) {
      text.append(status.errorAnalyzingFinished ? DaemonBundle.message("no.errors.or.warnings.found") : DaemonBundle.message("no.errors.or.warnings.found.so.far")).append("<br/>");
    }
    statistics = XmlStringUtil.wrapInHtml(text.toString());

    this.icon = icon;
    return result;
  }

  private void rebuildPassesMap(@Nonnull DaemonCodeAnalyzerStatus status) {
    passes.clear();
    for (ProgressableTextEditorHighlightingPass pass : status.passes) {
      JProgressBar progressBar = new JProgressBar(0, MAX);
      progressBar.setMaximum(MAX);
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, progressBar);
      JLabel percentLabel = new JLabel();
      percentLabel.setText(TrafficProgressPanel.MAX_TEXT);
      passes.put(pass, Pair.create(progressBar, percentLabel));
    }
  }

  @Override
  @Nonnull
  public AnalyzerStatus getStatus(@Nonnull Editor editor) {
    if (PowerSaveMode.isEnabled()) {
      return new AnalyzerStatus(AllIcons.General.InspectionsPowerSaveMode, "Code analysis is disabled in power save mode", "", () -> createUIController(editor));
    }
    else {
      DaemonCodeAnalyzerStatus status = getDaemonCodeAnalyzerStatus(mySeverityRegistrar);
      List<StatusItem> statusItems = new ArrayList<>();
      Image mainIcon = null;

      String title = "";
      String details = "";
      boolean isDumb = DumbService.isDumb(myProject);
      if (status.errorAnalyzingFinished) {
        if (isDumb) {
          title = DaemonBundle.message("shallow.analysis.completed");
          details = DaemonBundle.message("shallow.analysis.completed.details");
        }
      }
      else {
        title = DaemonBundle.message("performing.code.analysis");
      }

      int[] errorCount = status.errorCount;
      for (int i = errorCount.length - 1; i >= 0; i--) {
        int count = errorCount[i];
        if (count > 0) {
          HighlightSeverity severity = mySeverityRegistrar.getSeverityByIndex(i);
          String name = StringUtil.toLowerCase(severity.getName());
          if (count > 1) {
            name = StringUtil.pluralize(name);
          }

          Image icon = mySeverityRegistrar.getRendererIconByIndex(i);
          statusItems.add(new StatusItem(Integer.toString(count), icon, name));

          if (mainIcon == null) {
            mainIcon = icon;
          }
        }
      }

      if (!statusItems.isEmpty()) {
        if (mainIcon == null) {
          mainIcon = AllIcons.General.InspectionsOK;
        }
        AnalyzerStatus result = new AnalyzerStatus(mainIcon, title, "", () -> createUIController(editor)).
                withNavigation().
                withExpandedStatus(statusItems);

        //noinspection ConstantConditions
        return status.errorAnalyzingFinished ? result : result.withAnalyzingType(AnalyzingType.PARTIAL).
                withPasses(ContainerUtil.map(status.passes, p -> new PassWrapper(p.getPresentableName(), p.getProgress(), p.isFinished())));
      }
      if (StringUtil.isNotEmpty(status.reasonWhyDisabled)) {
        return new AnalyzerStatus(AllIcons.General.InspectionsTrafficOff, DaemonBundle.message("no.analysis.performed"), status.reasonWhyDisabled, () -> createUIController(editor))
                .withTextStatus(DaemonBundle.message("iw.status.off"));
      }
      if (StringUtil.isNotEmpty(status.reasonWhySuspended)) {
        return new AnalyzerStatus(AllIcons.General.InspectionsPause, DaemonBundle.message("analysis.suspended"), status.reasonWhySuspended, () -> createUIController(editor)).
                withTextStatus(status.heavyProcessType != null ? status.heavyProcessType.toString() : DaemonBundle.message("iw.status.paused"));
      }
      if (status.errorAnalyzingFinished) {
        return isDumb
               ? new AnalyzerStatus(AllIcons.General.InspectionsPause, title, details, () -> createUIController(editor)).
                withTextStatus(DaemonBundle.message("heavyProcess.type.indexing"))
               : new AnalyzerStatus(AllIcons.General.InspectionsOK, DaemonBundle.message("no.errors.or.warnings.found"), details, () -> createUIController(editor));
      }

      //noinspection ConstantConditions
      return new AnalyzerStatus(AllIcons.General.InspectionsEye, title, details, () -> createUIController(editor)).
              withTextStatus(DaemonBundle.message("iw.status.analyzing")).
              withAnalyzingType(AnalyzingType.EMPTY).
              withPasses(ContainerUtil.map(status.passes, p -> new PassWrapper(p.getPresentableName(), p.getProgress(), p.isFinished())));
    }
  }

  @Nonnull
  protected UIController createUIController(@Nonnull Editor editor) {
    boolean mergeEditor = editor.getUserData(DiffUserDataKeys.MERGE_EDITOR_FLAG) == Boolean.TRUE;
    return editor.getEditorKind() == EditorKind.DIFF && !mergeEditor ? new SimplifiedUIController() : new DefaultUIController();
  }

  protected abstract class AbstractUIController implements UIController {
    private final boolean inLibrary;
    private final List<LanguageHighlightLevel> myLevelsList;
    private List<HectorComponentPanel> myAdditionalPanels = Collections.emptyList();

    AbstractUIController() {
      PsiFile psiFile = getPsiFile();
      if (psiFile != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        assert virtualFile != null;
        inLibrary = fileIndex.isInLibrary(virtualFile) && !fileIndex.isInContent(virtualFile);
      }
      else {
        inLibrary = false;
      }

      myLevelsList = initLevels();
    }

    private
    @Nonnull
    List<LanguageHighlightLevel> initLevels() {
      List<LanguageHighlightLevel> result = new ArrayList<>();
      PsiFile psiFile = getPsiFile();
      if (psiFile != null && !getProject().isDisposed()) {
        FileViewProvider viewProvider = psiFile.getViewProvider();
        HighlightingLevelManager hlManager = HighlightingLevelManager.getInstance(getProject());
        for (Language language : viewProvider.getLanguages()) {
          PsiFile psiRoot = viewProvider.getPsi(language);
          result.add(new LanguageHighlightLevel(language.getID(), getHighlightLevel(hlManager.shouldHighlight(psiRoot), hlManager.shouldInspect(psiRoot))));
        }
      }
      return result;
    }

    @Override
    @Nonnull
    public List<InspectionsLevel> getAvailableLevels() {
      return inLibrary ? Arrays.asList(InspectionsLevel.NONE, InspectionsLevel.ERRORS) : Arrays.asList(InspectionsLevel.values());
    }

    @Nonnull
    @Override
    public List<LanguageHighlightLevel> getHighlightLevels() {
      return Collections.unmodifiableList(myLevelsList);
    }

    @Override
    public void setHighLightLevel(@Nonnull LanguageHighlightLevel level) {
      PsiFile psiFile = getPsiFile();
      if (psiFile != null && !getProject().isDisposed() && !myLevelsList.contains(level)) {
        FileViewProvider viewProvider = psiFile.getViewProvider();

        Language language = Language.findLanguageByID(level.getLangID());
        if (language != null) {
          PsiElement root = viewProvider.getPsi(language);
          if (level.getLevel() == InspectionsLevel.NONE) {
            HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.SKIP_HIGHLIGHTING);
          }
          else if (level.getLevel() == InspectionsLevel.ERRORS) {
            HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.SKIP_INSPECTION);
          }
          else {
            HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.FORCE_HIGHLIGHTING);
          }

          myLevelsList.replaceAll(l -> l.getLangID().equals(level.getLangID()) ? level : l);

          InjectedLanguageManager.getInstance(getProject()).dropFileCaches(psiFile);
          myDaemonCodeAnalyzer.restart();
        }
      }
    }

    @Override
    public void fillHectorPanels(@Nonnull Container container, @Nonnull GridBag gc) {
      PsiFile psiFile = getPsiFile();
      if (psiFile != null) {
        myAdditionalPanels = HectorComponentPanelsProvider.EP_NAME.getExtensionList(getProject()).
                stream().map(hp -> hp.createConfigurable(psiFile)).filter(p -> p != null).collect(Collectors.toList());

        for (HectorComponentPanel p : myAdditionalPanels) {
          JComponent c;
          try {
            p.reset();
            c = p.createComponent();
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (Throwable e) {
            Logger.getInstance(TrafficLightRenderer.class).error(e);
            continue;
          }

          if (c != null) {
            container.add(c, gc.nextLine().next().fillCellHorizontally().coverLine().weightx(1.0));
          }
        }
      }
    }

    @Override
    public boolean canClosePopup() {
      if (myAdditionalPanels.isEmpty()) {
        return true;
      }
      if (myAdditionalPanels.stream().allMatch(p -> p.canClose())) {
        PsiFile psiFile = getPsiFile();
        if (myAdditionalPanels.stream().filter(p -> p.isModified()).peek(TrafficLightRenderer::applyPanel).count() > 0) {
          if (psiFile != null) {
            InjectedLanguageManager.getInstance(getProject()).dropFileCaches(psiFile);
          }
          myDaemonCodeAnalyzer.restart();
        }
        return true;
      }
      return false;
    }

    @Override
    public void onClosePopup() {
      myAdditionalPanels.forEach(p -> p.disposeUIResources());
      myAdditionalPanels = Collections.emptyList();
    }

    @Override
    public void openProblemsView() {
      ProblemsView.showCurrentFileProblems(getProject());
    }
  }

  private static void applyPanel(@Nonnull HectorComponentPanel panel) {
    try {
      panel.apply();
    }
    catch (ConfigurationException ignored) {
    }
  }

  @Nonnull
  private static InspectionsLevel getHighlightLevel(boolean highlight, boolean inspect) {
    if (!highlight && !inspect) {
      return InspectionsLevel.NONE;
    }
    else if (highlight && !inspect) {
      return InspectionsLevel.ERRORS;
    }
    else {
      return InspectionsLevel.ALL;
    }
  }

  public class DefaultUIController extends AbstractUIController {
    private final List<AnAction> myMenuActions = initActions();

    private
    @Nonnull
    List<AnAction> initActions() {
      List<AnAction> result = new ArrayList<>();
      result.add(new ConfigureInspectionsAction());
      result.add(DaemonEditorPopup.createGotoGroup());

      result.add(AnSeparator.create());
      result.add(new ToggleAction(EditorBundle.message("iw.show.import.tooltip")) {
        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
          PsiFile psiFile = getPsiFile();
          return psiFile != null && myDaemonCodeAnalyzer.isImportHintsEnabled(psiFile);
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
          PsiFile psiFile = getPsiFile();
          if (psiFile != null) {
            myDaemonCodeAnalyzer.setImportHintsEnabled(psiFile, state);
          }
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
          super.update(e);
          e.getPresentation().setEnabled(myDaemonCodeAnalyzer.isAutohintsAvailable(getPsiFile()));
        }

        @Override
        public boolean isDumbAware() {
          return true;
        }
      });

      return result;
    }

    @Override
    public
    @Nonnull
    List<AnAction> getActions() {
      return myMenuActions;
    }

    @Override
    public boolean enableToolbar() {
      return true;
    }
  }

  public class SimplifiedUIController extends AbstractUIController {
    @Override
    public boolean enableToolbar() {
      return false;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
      return Collections.emptyList();
    }
  }
}
