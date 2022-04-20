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
package com.intellij.refactoring.introduce.inplace;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateStateImpl;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.Result;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRange;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.RefactoringActionHandler;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.editor.template.TextResult;
import consulo.language.psi.*;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.popup.Balloon;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.StartMarkAction;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * User: anna
 * Date: 3/15/11
 */
public abstract class AbstractInplaceIntroducer<V extends PsiNameIdentifierOwner, E extends PsiElement> extends
                                                                                                        InplaceVariableIntroducer<E> {
  protected V myLocalVariable;
  protected RangeMarker myLocalMarker;

  protected final String myExprText;
  private final String myLocalName;

  public static final Key<AbstractInplaceIntroducer> ACTIVE_INTRODUCE = Key.create("ACTIVE_INTRODUCE");

  private EditorEx myPreview;
  private final JComponent myPreviewComponent;

  private DocumentAdapter myDocumentAdapter;
  protected final JPanel myWholePanel;
  private boolean myFinished = false;

  public AbstractInplaceIntroducer(Project project,
                                   Editor editor,
                                   E expr,
                                   @Nullable V localVariable,
                                   E[] occurrences,
                                   String title,
                                   final FileType languageFileType) {
    super(null, editor, project, title, occurrences, expr);
    myLocalVariable = localVariable;
    if (localVariable != null) {
      final PsiElement nameIdentifier = localVariable.getNameIdentifier();
      if (nameIdentifier != null) {
        myLocalMarker = createMarker(nameIdentifier);
      }
    }
    else {
      myLocalMarker = null;
    }
    myExprText = getExpressionText(expr);
    myLocalName = localVariable != null ? localVariable.getName() : null;

    myPreview = createPreviewComponent(project, languageFileType);
    myPreviewComponent = new JPanel(new BorderLayout());
    myPreviewComponent.add(myPreview.getComponent(), BorderLayout.CENTER);
    myPreviewComponent.setBorder(JBUI.Borders.empty(2, 2, 6, 2));

    myWholePanel = new JPanel(new GridBagLayout());
    myWholePanel.setBorder(null);

    showDialogAdvertisement(getActionName());
  }

  @Nullable
  protected String getExpressionText(E expr) {
    return expr != null ? expr.getText() : null;
  }

  protected final void setPreviewText(final String text) {
    if (myPreview == null) return; //already disposed
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        myPreview.getDocument().replaceString(0, myPreview.getDocument().getTextLength(), text);
      }
    });
  }

  protected final JComponent getPreviewComponent() {
    return myPreviewComponent;
  }

  protected final Editor getPreviewEditor() {
    return myPreview;
  }


  @Override
  protected StartMarkAction startRename() throws StartMarkAction.AlreadyStartedException {
    return StartMarkAction.start(myEditor.getDocument(), myProject, getCommandName());
  }

  /**
   * Returns ID of the action the shortcut of which is used to show the non-in-place refactoring dialog.
   *
   * @return action ID
   */
  protected abstract String getActionName();

  /**
   * Creates an initial version of the declaration for the introduced element. Note that this method is not called in a write action
   * and most likely needs to create one itself.
   *
   * @param replaceAll whether all occurrences are going to be replaced
   * @param names      the suggested names for the declaration
   * @return the declaration
   */
  @Nullable
  protected abstract V createFieldToStartTemplateOn(boolean replaceAll, String[] names);

  /**
   * Returns the suggested names for the introduced element.
   *
   * @param replaceAll whether all occurrences are going to be replaced
   * @param variable   introduced element declaration, if already created.
   * @return the suggested names
   */
  protected abstract String[] suggestNames(boolean replaceAll, @Nullable V variable);

  protected abstract void performIntroduce();
  protected void performPostIntroduceTasks() {}

  public abstract boolean isReplaceAllOccurrences();
  public abstract void setReplaceAllOccurrences(boolean allOccurrences);
  @Override
  protected abstract JComponent getComponent();

  protected abstract void saveSettings(@Nonnull V variable);
  @Override
  protected abstract V getVariable();

  public abstract E restoreExpression(PsiFile containingFile, V variable, RangeMarker marker, String exprText);

  /**
   * Begins the in-place refactoring operation.
   *
   * @return true if the in-place refactoring was successfully started, false if it failed to start and a dialog should be shown instead.
   */
  public boolean startInplaceIntroduceTemplate() {
    final boolean replaceAllOccurrences = isReplaceAllOccurrences();
    final Ref<Boolean> result = new Ref<Boolean>();
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      @Override
      public void run() {
        final String[] names = suggestNames(replaceAllOccurrences, getLocalVariable());
        final V variable = createFieldToStartTemplateOn(replaceAllOccurrences, names);
        boolean started = false;
        if (variable != null) {
          int caretOffset = getCaretOffset();
          myEditor.getCaretModel().moveToOffset(caretOffset);
          myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

          final LinkedHashSet<String> nameSuggestions = new LinkedHashSet<String>();
          nameSuggestions.add(variable.getName());
          nameSuggestions.addAll(Arrays.asList(names));
          initOccurrencesMarkers();
          setElementToRename(variable);
          updateTitle(getVariable());
          started = AbstractInplaceIntroducer.super.performInplaceRefactoring(nameSuggestions);
          if (started) {
            myDocumentAdapter = new DocumentAdapter() {
              @Override
              public void documentChanged(DocumentEvent e) {
                if (myPreview == null) return;
                final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(myEditor);
                if (templateState != null) {
                  final TextResult value = templateState.getVariableValue(InplaceRefactoring.PRIMARY_VARIABLE_NAME);
                  if (value != null) {
                    updateTitle(getVariable(), value.getText());
                  }
                }
              }
            };
            myEditor.getDocument().addDocumentListener(myDocumentAdapter);
            updateTitle(getVariable());
            if (TemplateManagerImpl.getTemplateStateImpl(myEditor) != null) {
              myEditor.putUserData(ACTIVE_INTRODUCE, AbstractInplaceIntroducer.this);
            }
          }
        }
        result.set(started);
        if (!started) {
          finish(true);
        }
      }

    }, getCommandName(), getCommandName());
    return result.get();
  }

  protected int getCaretOffset() {
    RangeMarker r;
    if (myLocalMarker != null) {
      final PsiReference reference = myExpr != null ? myExpr.getReference() : null;
      if (reference != null && reference.resolve() == myLocalVariable) {
        r = myExprMarker;
      } else {
        r = myLocalMarker;
      }
    }
    else {
      r = myExprMarker;
    }
    return r != null ? r.getStartOffset() : 0;
  }

  protected void updateTitle(@Nullable V variable, String value) {
    if (variable == null) return;

    final String variableText = variable.getText();
    final PsiElement identifier = variable.getNameIdentifier();
    if (identifier != null) {
      final int startOffsetInParent = identifier.getStartOffsetInParent();
      setPreviewText(variableText.substring(0, startOffsetInParent) + value + variableText.substring(startOffsetInParent + identifier.getTextLength()));
    } else {
      setPreviewText(variableText.replaceFirst(variable.getName(), value));
    }
    revalidate();
  }

  protected void updateTitle(@Nullable V variable) {
    if (variable == null) return;
    setPreviewText(variable.getText());
    revalidate();
  }

  protected void revalidate() {
    myWholePanel.revalidate();
    if (myTarget != null) {
      myBalloon.revalidate(new PositionTracker.Static<Balloon>(myTarget));
    }
  }

  private boolean myShouldSelect = true;
  @Override
  protected boolean shouldSelectAll() {
    return myShouldSelect;
  }

  public void restartInplaceIntroduceTemplate() {
    Runnable restartTemplateRunnable = new Runnable() {
      @Override
      public void run() {
        final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(myEditor);
        if (templateState != null) {
          myEditor.putUserData(INTRODUCE_RESTART, true);
          try {
            final TextRange range = templateState.getCurrentVariableRange();
            if (range != null && range.isEmpty()) {
              final String[] names = suggestNames(isReplaceAllOccurrences(), getLocalVariable());
              ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                  myEditor.getDocument().insertString(myEditor.getCaretModel().getOffset(), names[0]);
                }
              });
            }
            templateState.gotoEnd(true);
            try {
              myShouldSelect = false;
              startInplaceIntroduceTemplate();
            }
            finally {
              myShouldSelect = true;
            }
          }
          finally {
            myEditor.putUserData(INTRODUCE_RESTART, false);
          }
        }
        updateTitle(getVariable());
      }
    };
    CommandProcessor.getInstance().executeCommand(myProject, restartTemplateRunnable, getCommandName(), getCommandName());
  }

  @Override
  protected void restoreSelection() {
    if (!shouldSelectAll()) {
      myEditor.getSelectionModel().removeSelection();
    }
  }

  public String getInputName() {
    return myInsertedName;
  }


  @Override
  public void finish(boolean success) {
    myFinished = true;
    final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(myEditor);
    if (templateState != null) {
      myEditor.putUserData(ACTIVE_INTRODUCE, null);
    }
    if (myDocumentAdapter != null) {
      myEditor.getDocument().removeDocumentListener(myDocumentAdapter);
    }
    if (myBalloon == null) {
      releaseIfNotRestart();
    }
    super.finish(success);
    if (success) {
      PsiDocumentManager.getInstance(myProject).commitAllDocuments();
      final V variable = getVariable();
      if (variable == null) {
        return;
      }
      restoreState(variable);
    }
  }

  @Override
  protected void releaseResources() {
    super.releaseResources();
    if (myPreview == null) return;

    EditorFactory.getInstance().releaseEditor(myPreview);
    myPreview = null;
  }

  @Override
  protected void addReferenceAtCaret(Collection<PsiReference> refs) {
    final V variable = getLocalVariable();
    if (variable != null) {
      for (PsiReference reference : ReferencesSearch.search(variable)) {
        refs.add(reference);
      }
    } else {
      refs.clear();
    }
  }

  @Override
  @RequiredReadAction
  protected void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
    if (isReplaceAllOccurrences()) {
      for (E expression : getOccurrences()) {
        LOG.assertTrue(expression.isValid(), expression.getText());
        stringUsages.add(Pair.<PsiElement, TextRange>create(expression, new TextRange(0, expression.getTextLength())));
      }
    }  else if (getExpr() != null) {
      correctExpression();
      final E expr = getExpr();
      LOG.assertTrue(expr.isValid(), expr.getText());
      stringUsages.add(Pair.<PsiElement, TextRange>create(expr, new TextRange(0, expr.getTextLength())));
    }

    final V localVariable = getLocalVariable();
    if (localVariable != null) {
      final PsiElement nameIdentifier = localVariable.getNameIdentifier();
      if (nameIdentifier != null) {
        int length = nameIdentifier.getTextLength();
        stringUsages.add(Pair.<PsiElement, TextRange>create(nameIdentifier, new TextRange(0, length)));
      }
    }
  }

  protected void correctExpression() {}

  @Override
  protected void addHighlights(@Nonnull Map<TextRange, TextAttributes> ranges,
                               @Nonnull Editor editor,
                               @Nonnull Collection<RangeHighlighter> highlighters,
                               @Nonnull HighlightManager highlightManager) {
    final TextAttributes attributes =
      EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    final V variable = getVariable();
    if (variable != null) {
      final String name = variable.getName();
      LOG.assertTrue(name != null, variable);
      final int variableNameLength = name.length();
      if (isReplaceAllOccurrences()) {
        for (RangeMarker marker : getOccurrenceMarkers()) {
          final int startOffset = marker.getStartOffset();
          highlightManager.addOccurrenceHighlight(editor, startOffset, startOffset + variableNameLength, attributes, 0, highlighters, null);
        }
      }
      else if (getExpr() != null) {
        final int startOffset = getExprMarker().getStartOffset();
        highlightManager.addOccurrenceHighlight(editor, startOffset, startOffset + variableNameLength, attributes, 0, highlighters, null);
      }
    }

    for (RangeHighlighter highlighter : highlighters) {
      highlighter.setGreedyToLeft(true);
      highlighter.setGreedyToRight(true);
    }
  }

  protected void restoreState(final V psiField) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        final PsiFile containingFile = psiField.getContainingFile();
        final RangeMarker exprMarker = getExprMarker();
        if (exprMarker != null) {
          myExpr = restoreExpression(containingFile, psiField, exprMarker, myExprText);
          if (myExpr != null && myExpr.isPhysical()) {
            myExprMarker = createMarker(myExpr);
          }
        }
        if (myLocalMarker != null) {
          final PsiElement refVariableElement = containingFile.findElementAt(myLocalMarker.getStartOffset());
          if (refVariableElement != null) {
            final PsiElement parent = refVariableElement.getParent();
            if (parent instanceof PsiNamedElement) {
              ((PsiNamedElement)parent).setName(myLocalName);
            }
          }

          final V localVariable = getLocalVariable();
          if (localVariable != null && localVariable.isPhysical()) {
            myLocalVariable = localVariable;
            final PsiElement nameIdentifier = localVariable.getNameIdentifier();
            if (nameIdentifier != null) {
              myLocalMarker = createMarker(nameIdentifier);
            }
          }
        }
        final List<RangeMarker> occurrenceMarkers = getOccurrenceMarkers();
        for (int i = 0, occurrenceMarkersSize = occurrenceMarkers.size(); i < occurrenceMarkersSize; i++) {
          RangeMarker marker = occurrenceMarkers.get(i);
          if (getExprMarker() != null && marker.getStartOffset() == getExprMarker().getStartOffset() && myExpr != null) {
            myOccurrences[i] = myExpr;
            continue;
          }
          final E psiExpression =
             restoreExpression(containingFile, psiField, marker, getLocalVariable() != null ? myLocalName : myExprText);
          if (psiExpression != null) {
            myOccurrences[i] = psiExpression;
          }
        }

        myOccurrenceMarkers = null;
        deleteTemplateField(psiField);
      }
    });
  }

  protected void deleteTemplateField(V psiField) {
    if (psiField.isValid()) {
      psiField.delete();
    }
  }

  @Override
  protected boolean performRefactoring() {
    final String newName = getInputName();
    if (getLocalVariable() == null && myExpr == null ||
        newName == null ||
        getLocalVariable() != null && !getLocalVariable().isValid() ||
        myExpr != null && !myExpr.isValid()) {
      super.moveOffsetAfter(false);
      return false;
    }
    if (getLocalVariable() != null) {
      new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
        @Override
        protected void run(Result result) throws Throwable {
          getLocalVariable().setName(myLocalName);
        }
      }.execute();
    }

    if (!isIdentifier(newName, myExpr != null ? myExpr.getLanguage() : getLocalVariable().getLanguage())) return false;
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      @Override
      public void run() {
        performIntroduce();
      }
    }, getCommandName(), getCommandName());

    V variable = getVariable();
    if (variable != null) {
      saveSettings(variable);
    }
    return false;
  }

  @Override
  protected void moveOffsetAfter(boolean success) {
    if (getLocalVariable() != null && getLocalVariable().isValid()) {
      myEditor.getCaretModel().moveToOffset(getLocalVariable().getTextOffset());
      myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }
    else if (getExprMarker() != null) {
      final RangeMarker exprMarker = getExprMarker();
      if (exprMarker.isValid()) {
        myEditor.getCaretModel().moveToOffset(exprMarker.getStartOffset());
        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
      }
    }
    super.moveOffsetAfter(success);
    if (myLocalMarker != null && !isRestart()) {
      myLocalMarker.dispose();
    }
    if (success) {
      performPostIntroduceTasks();
    }
  }

  @Override
  protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
    return super.startsOnTheSameElement(handler, element) || getLocalVariable() == element;
  }

  public V getLocalVariable() {
    if (myLocalVariable != null && myLocalVariable.isValid()) {
      return myLocalVariable;
    }
    if (myLocalMarker != null) {
      V variable = getVariable();
      PsiFile containingFile;
      if (variable != null) {
        containingFile = variable.getContainingFile();
      } else {
        containingFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
      }
      PsiNameIdentifierOwner identifierOwner = PsiTreeUtil.getParentOfType(containingFile.findElementAt(myLocalMarker.getStartOffset()),
                                                                           PsiNameIdentifierOwner.class, false);
      return identifierOwner != null && identifierOwner.getClass() == myLocalVariable.getClass() ? (V)identifierOwner : null;

    }
    return myLocalVariable;
  }

  public void stopIntroduce(Editor editor) {
    final TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (templateState != null) {
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          templateState.gotoEnd(true);
        }
      };
      CommandProcessor.getInstance().executeCommand(myProject, runnable, getCommandName(), getCommandName());
    }
  }

  @Override
  protected void navigateToAlreadyStarted(Document oldDocument, int exitCode) {
    finish(true);
    super.navigateToAlreadyStarted(oldDocument, exitCode);
  }

  @Override
  protected void showBalloon() {
    if (myFinished) return;
    super.showBalloon();
  }

  public boolean startsOnTheSameElement(E expr, V localVariable) {
    if (myExprMarker != null && myExprMarker.isValid() && expr != null && myExprMarker.getStartOffset() == expr.getTextOffset()) {
      return true;
    }

    if (myLocalMarker != null &&
        myLocalMarker.isValid() &&
        localVariable != null &&
        myLocalMarker.getStartOffset() == localVariable.getTextOffset()) {
      return true;
    }
    return isRestart();
  }

  @Nullable
  public static AbstractInplaceIntroducer getActiveIntroducer(@Nullable Editor editor) {
    if (editor == null) return null;
    return editor.getUserData(ACTIVE_INTRODUCE);
  }
}
