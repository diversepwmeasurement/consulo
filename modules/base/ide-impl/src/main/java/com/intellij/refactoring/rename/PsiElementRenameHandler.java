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

package com.intellij.refactoring.rename;

import consulo.application.statistic.FeatureUsageTracker;
import com.intellij.ide.scratch.ScratchUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.component.extension.ExtensionPointName;
import consulo.component.extension.Extensions;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import consulo.util.lang.function.Condition;
import com.intellij.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.ide.impl.psi.meta.PsiWritableMetaData;
import consulo.language.psi.PsiUtilCore;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import consulo.usage.UsageViewUtil;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * created at Nov 13, 2001
 *
 * @author Jeka, dsl
 */
public class PsiElementRenameHandler implements RenameHandler {
  private static final Logger LOG = Logger.getInstance(PsiElementRenameHandler.class);

  public static final ExtensionPointName<Condition<PsiElement>> VETO_RENAME_CONDITION_EP = ExtensionPointName.create("consulo.vetoRenameCondition");
  public static Key<String> DEFAULT_NAME = Key.create("DEFAULT_NAME");

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = getElement(dataContext);
    if (element == null) {
      element = BaseRefactoringAction.getElementAtCaret(editor, file);
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final String newName = dataContext.getData(DEFAULT_NAME);
      if (newName != null) {
        rename(element, project, element, editor, newName);
        return;
      }
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    final PsiElement nameSuggestionContext = InjectedLanguageUtil.findElementAtNoCommit(file, editor.getCaretModel().getOffset());
    invoke(element, project, nameSuggestionContext, editor);
  }

  @Override
  public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    PsiElement element = elements.length == 1 ? elements[0] : null;
    if (element == null) element = getElement(dataContext);
    LOG.assertTrue(element != null);
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final String newName = dataContext.getData(DEFAULT_NAME);
      LOG.assertTrue(newName != null);
      rename(element, project, element, editor, newName);
    }
    else {
      invoke(element, project, element, editor);
    }
  }

  public static void invoke(PsiElement element, Project project, PsiElement nameSuggestionContext, @Nullable Editor editor) {
    if (element != null && !canRename(project, editor, element)) {
      return;
    }

    VirtualFile contextFile = PsiUtilCore.getVirtualFile(nameSuggestionContext);

    if (nameSuggestionContext != null &&
        nameSuggestionContext.isPhysical() &&
        (contextFile == null || !ScratchUtil.isScratch(contextFile) && !PsiManager.getInstance(project).isInProject(nameSuggestionContext))) {
      final String message = "Selected element is used from non-project files. These usages won't be renamed. Proceed anyway?";
      if (ApplicationManager.getApplication().isUnitTestMode()) throw new CommonRefactoringUtil.RefactoringErrorHintException(message);
      if (Messages.showYesNoDialog(project, message, RefactoringBundle.getCannotRefactorMessage(null), Messages.getWarningIcon()) != Messages.YES) {
        return;
      }
    }

    FeatureUsageTracker.getInstance().triggerFeatureUsed("refactoring.rename");

    rename(element, project, nameSuggestionContext, editor);
  }

  public static boolean canRename(Project project, Editor editor, PsiElement element) throws CommonRefactoringUtil.RefactoringErrorHintException {
    String message = renameabilityStatus(project, element);
    if (StringUtil.isNotEmpty(message)) {
      showErrorMessage(project, editor, message);
      return false;
    }
    return true;
  }

  @Nullable
  static String renameabilityStatus(Project project, PsiElement element) {
    if (element == null) return "";

    boolean hasRenameProcessor = RenamePsiElementProcessor.forElement(element) != RenamePsiElementProcessor.DEFAULT;
    boolean hasWritableMetaData = element instanceof PsiMetaOwner && ((PsiMetaOwner)element).getMetaData() instanceof PsiWritableMetaData;

    if (!hasRenameProcessor && !hasWritableMetaData && !(element instanceof PsiNamedElement)) {
      return RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.symbol.to.rename"));
    }

    if (!PsiManager.getInstance(project).isInProject(element)) {
      if (element.isPhysical()) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        if (!(virtualFile != null && NonProjectFileWritingAccessProvider.isWriteAccessAllowed(virtualFile, project))) {
          String message = RefactoringBundle.message("error.out.of.project.element", UsageViewUtil.getType(element));
          return RefactoringBundle.getCannotRefactorMessage(message);
        }
      }

      if (!element.isWritable()) {
        return RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.cannot.be.renamed"));
      }
    }

    if (InjectedLanguageUtil.isInInjectedLanguagePrefixSuffix(element)) {
      final String message = RefactoringBundle.message("error.in.injected.lang.prefix.suffix", UsageViewUtil.getType(element));
      return RefactoringBundle.getCannotRefactorMessage(message);
    }

    return null;
  }

  static void showErrorMessage(Project project, @Nullable Editor editor, String message) {
    CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("rename.title"), null);
  }

  public static void rename(PsiElement element, final Project project, PsiElement nameSuggestionContext, Editor editor) {
    rename(element, project, nameSuggestionContext, editor, null);
  }

  public static void rename(PsiElement element, final Project project, PsiElement nameSuggestionContext, Editor editor, String defaultName) {
    RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(element);
    PsiElement substituted = processor.substituteElementToRename(element, editor);
    if (substituted == null || !canRename(project, editor, substituted)) return;

    RenameDialog dialog = processor.createRenameDialog(project, substituted, nameSuggestionContext, editor);

    if (defaultName == null && ApplicationManager.getApplication().isUnitTestMode()) {
      String[] strings = dialog.getSuggestedNames();
      if (strings != null && strings.length > 0) {
        Arrays.sort(strings);
        defaultName = strings[0];
      }
      else {
        defaultName = "undefined"; // need to avoid show dialog in test
      }
    }

    if (defaultName != null) {
      try {
        dialog.performRename(defaultName);
      }
      finally {
        dialog.close(DialogWrapper.CANCEL_EXIT_CODE); // to avoid dialog leak
      }
    }
    else {
      dialog.show();
    }
  }

  @Override
  public boolean isAvailableOnDataContext(DataContext dataContext) {
    return !isVetoed(getElement(dataContext));
  }

  public static boolean isVetoed(PsiElement element) {
    if (element == null || element instanceof SyntheticElement) return true;
    for (Condition<PsiElement> condition : Extensions.getExtensions(VETO_RENAME_CONDITION_EP)) {
      if (condition.value(element)) return true;
    }
    return false;
  }

  @Nullable
  public static PsiElement getElement(final DataContext dataContext) {
    PsiElement[] elementArray = BaseRefactoringAction.getPsiElementArray(dataContext);

    if (elementArray.length != 1) {
      return null;
    }
    return elementArray[0];
  }

  @Override
  public boolean isRenaming(DataContext dataContext) {
    return isAvailableOnDataContext(dataContext);
  }
}
