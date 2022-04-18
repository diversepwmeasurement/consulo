// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.IntentionsUI;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.language.editor.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateStateImpl;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.application.statistic.FeatureUsageTracker;
import com.intellij.featureStatistics.FeatureUsageTrackerImpl;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.application.ApplicationManager;
import consulo.application.TransactionGuard;
import consulo.application.WriteAction;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import com.intellij.openapi.util.Pair;
import consulo.application.util.registry.Registry;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.ide.impl.psi.stubs.StubTextInconsistencyException;
import com.intellij.util.PairProcessor;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author mike
 */
public class ShowIntentionActionsHandler implements CodeInsightActionHandler {

  @Override
  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    invoke(project, editor, file, false);
  }

  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file, boolean showFeedbackOnEmptyMenu) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
      file = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file);
    }

    final LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup != null) {
      lookup.showElementActions(null);
      return;
    }

    final DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    letAutoImportComplete(editor, file, codeAnalyzer);

    ShowIntentionsPass.IntentionsInfo intentions = ShowIntentionsPass.getActionsToShow(editor, file, true);
    IntentionsUI.getInstance(project).hide();

    if (HintManagerImpl.getInstanceImpl().performCurrentQuestionAction()) return;

    //intentions check isWritable before modification: if (!file.isWritable()) return;

    TemplateStateImpl state = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (state != null && !state.isFinished()) {
      return;
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    Editor finalEditor = editor;
    PsiFile finalFile = file;
    showIntentionHint(project, finalEditor, finalFile, intentions, showFeedbackOnEmptyMenu);
  }

  protected void showIntentionHint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull ShowIntentionsPass.IntentionsInfo intentions, boolean showFeedbackOnEmptyMenu) {
    if (!intentions.isEmpty()) {
      editor.getScrollingModel().runActionOnScrollingFinished(() -> {
        CachedIntentions cachedIntentions = CachedIntentions.createAndUpdateActions(project, file, editor, intentions);
        IntentionHintComponent.showIntentionHint(project, file, editor, true, cachedIntentions);
      });
    }
    else if (showFeedbackOnEmptyMenu) {
      HintManager.getInstance().showInformationHint(editor, "No context actions available at this location");
    }
  }

  private static void letAutoImportComplete(@Nonnull Editor editor, @Nonnull PsiFile file, DaemonCodeAnalyzerImpl codeAnalyzer) {
    CommandProcessor.getInstance().runUndoTransparentAction(() -> codeAnalyzer.autoImportReferenceAtCursor(editor, file));
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  public static boolean availableFor(@Nonnull PsiFile psiFile, @Nonnull Editor editor, @Nonnull IntentionAction action) {
    if (!psiFile.isValid()) return false;

    try {
      Project project = psiFile.getProject();
      action = IntentionActionDelegate.unwrap(action);
      if (action instanceof SuppressIntentionActionFromFix) {
        final ThreeState shouldBeAppliedToInjectionHost = ((SuppressIntentionActionFromFix)action).isShouldBeAppliedToInjectionHost();
        if (editor instanceof EditorWindow && shouldBeAppliedToInjectionHost == ThreeState.YES) {
          return false;
        }
        if (!(editor instanceof EditorWindow) && shouldBeAppliedToInjectionHost == ThreeState.NO) {
          return false;
        }
      }

      if (action instanceof PsiElementBaseIntentionAction) {
        PsiElementBaseIntentionAction psiAction = (PsiElementBaseIntentionAction)action;
        if (!psiAction.checkFile(psiFile)) {
          return false;
        }
        PsiElement leaf = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (leaf == null || !psiAction.isAvailable(project, editor, leaf)) {
          return false;
        }
      }
      else if (!action.isAvailable(project, editor, psiFile)) {
        return false;
      }
    }
    catch (IndexNotReadyException e) {
      return false;
    }
    return true;
  }

  @Nullable
  public static Pair<PsiFile, Editor> chooseBetweenHostAndInjected(@Nonnull PsiFile hostFile,
                                                                   @Nonnull Editor hostEditor,
                                                                   @Nullable PsiFile injectedFile,
                                                                   @Nonnull PairProcessor<? super PsiFile, ? super Editor> predicate) {
    Editor editorToApply = null;
    PsiFile fileToApply = null;

    Editor injectedEditor = null;
    if (injectedFile != null) {
      injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(hostEditor, injectedFile);
      if (predicate.process(injectedFile, injectedEditor)) {
        editorToApply = injectedEditor;
        fileToApply = injectedFile;
      }
    }

    if (editorToApply == null && hostEditor != injectedEditor && predicate.process(hostFile, hostEditor)) {
      editorToApply = hostEditor;
      fileToApply = hostFile;
    }
    if (editorToApply == null) return null;
    return Pair.create(fileToApply, editorToApply);
  }

  public static boolean chooseActionAndInvoke(@Nonnull PsiFile hostFile, @Nonnull final Editor hostEditor, @Nonnull final IntentionAction action, @Nonnull String text) {
    final Project project = hostFile.getProject();
    return chooseActionAndInvoke(hostFile, hostEditor, action, text, project);
  }

  static boolean chooseActionAndInvoke(@Nonnull PsiFile hostFile, @Nullable final Editor hostEditor, @Nonnull final IntentionAction action, @Nonnull String text, @Nonnull final Project project) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickFix");
    ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getFixesStats().registerInvocation();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    Pair<PsiFile, Editor> pair = chooseFileForAction(hostFile, hostEditor, action);
    if (pair == null) return false;

    CommandProcessor.getInstance().executeCommand(project, () -> TransactionGuard.getInstance().submitTransactionAndWait(() -> invokeIntention(action, pair.second, pair.first)), text, null);

    checkPsiTextConsistency(hostFile);

    return true;
  }

  private static void checkPsiTextConsistency(@Nonnull PsiFile hostFile) {
    if (Registry.is("ide.check.stub.text.consistency") || ApplicationManager.getApplication().isUnitTestMode() && !ApplicationInfoImpl.isInPerformanceTest()) {
      if (hostFile.isValid()) {
        StubTextInconsistencyException.checkStubTextConsistency(hostFile);
      }
    }
  }

  private static void invokeIntention(@Nonnull IntentionAction action, @Nullable Editor editor, @Nonnull PsiFile file) {
    //IntentionsCollector.getInstance().record(file.getProject(), action, file.getLanguage());
    PsiElement elementToMakeWritable = action.getElementToMakeWritable(file);
    if (elementToMakeWritable != null && !FileModificationService.getInstance().preparePsiElementsForWrite(elementToMakeWritable)) {
      return;
    }

    Runnable r = () -> action.invoke(file.getProject(), editor, file);
    if (action.startInWriteAction()) {
      WriteAction.run(r::run);
    }
    else {
      r.run();
    }
  }


  @Nullable
  public static Pair<PsiFile, Editor> chooseFileForAction(@Nonnull PsiFile hostFile, @Nullable Editor hostEditor, @Nonnull IntentionAction action) {
    if (hostEditor == null) {
      return Pair.create(hostFile, null);
    }

    PsiFile injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(hostFile, hostEditor.getCaretModel().getOffset());
    return chooseBetweenHostAndInjected(hostFile, hostEditor, injectedFile, (psiFile, editor) -> availableFor(psiFile, editor, action));
  }
}
