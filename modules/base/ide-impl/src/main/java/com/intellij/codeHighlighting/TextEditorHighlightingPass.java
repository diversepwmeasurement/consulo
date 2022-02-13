// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeHighlighting;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import consulo.language.editor.rawHighlight.impl.HighlightInfoImpl;
import consulo.document.Document;
import consulo.editor.colorScheme.EditorColorsScheme;
import consulo.application.progress.ProgressIndicator;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import com.intellij.util.ArrayUtilRt;
import consulo.language.util.IncorrectOperationException;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class TextEditorHighlightingPass implements HighlightingPass {
  public static final TextEditorHighlightingPass[] EMPTY_ARRAY = new TextEditorHighlightingPass[0];
  @Nullable
  protected final Document myDocument;
  @Nonnull
  protected final Project myProject;
  private final boolean myRunIntentionPassAfter;
  private final long myInitialDocStamp;
  private final long myInitialPsiStamp;
  private volatile int[] myCompletionPredecessorIds = ArrayUtilRt.EMPTY_INT_ARRAY;
  private volatile int[] myStartingPredecessorIds = ArrayUtilRt.EMPTY_INT_ARRAY;
  private volatile int myId;
  private volatile boolean myDumb;
  private EditorColorsScheme myColorsScheme;

  protected TextEditorHighlightingPass(@Nonnull final Project project, @Nullable final Document document, boolean runIntentionPassAfter) {
    myDocument = document;
    myProject = project;
    myRunIntentionPassAfter = runIntentionPassAfter;
    myInitialDocStamp = document == null ? 0 : document.getModificationStamp();
    myInitialPsiStamp = PsiModificationTracker.SERVICE.getInstance(myProject).getModificationCount();
  }

  protected TextEditorHighlightingPass(@Nonnull final Project project, @Nullable final Document document) {
    this(project, document, true);
  }

  @RequiredReadAction
  @Override
  public final void collectInformation(@Nonnull ProgressIndicator progress) {
    if (!isValid()) return; //Document has changed.
    if (!(progress instanceof DaemonProgressIndicator)) {
      throw new IncorrectOperationException("Highlighting must be run under DaemonProgressIndicator, but got: " + progress);
    }
    myDumb = DumbService.getInstance(myProject).isDumb();
    doCollectInformation(progress);
  }

  @Nullable
  public EditorColorsScheme getColorsScheme() {
    return myColorsScheme;
  }

  public void setColorsScheme(@Nullable EditorColorsScheme colorsScheme) {
    myColorsScheme = colorsScheme;
  }

  protected boolean isDumbMode() {
    return myDumb;
  }

  protected boolean isValid() {
    if (isDumbMode() && !DumbService.isDumbAware(this)) {
      return false;
    }

    if (PsiModificationTracker.SERVICE.getInstance(myProject).getModificationCount() != myInitialPsiStamp) {
      return false;
    }

    if (myDocument != null) {
      if (myDocument.getModificationStamp() != myInitialDocStamp) return false;
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
      return file != null && file.isValid();
    }

    return true;
  }

  @Override
  public final void applyInformationToEditor() {
    if (!isValid()) return; // Document has changed.
    if (DumbService.getInstance(myProject).isDumb() && !DumbService.isDumbAware(this)) {
      Document document = getDocument();
      PsiFile file = document == null ? null : PsiDocumentManager.getInstance(myProject).getPsiFile(document);
      if (file != null) {
        DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap().markFileUpToDate(getDocument(), getId());
      }
      return;
    }
    doApplyInformationToEditor();
  }

  public abstract void doCollectInformation(@Nonnull ProgressIndicator progress);

  public abstract void doApplyInformationToEditor();

  public final int getId() {
    return myId;
  }

  public final void setId(final int id) {
    myId = id;
  }

  @Nonnull
  public List<HighlightInfoImpl> getInfos() {
    return Collections.emptyList();
  }

  @Nonnull
  public final int[] getCompletionPredecessorIds() {
    return myCompletionPredecessorIds;
  }

  public final void setCompletionPredecessorIds(@Nonnull int[] completionPredecessorIds) {
    myCompletionPredecessorIds = completionPredecessorIds;
  }

  @Nullable
  public Document getDocument() {
    return myDocument;
  }

  @Nonnull
  public final int[] getStartingPredecessorIds() {
    return myStartingPredecessorIds;
  }

  public final void setStartingPredecessorIds(@Nonnull final int[] startingPredecessorIds) {
    myStartingPredecessorIds = startingPredecessorIds;
  }

  @Override
  @NonNls
  public String toString() {
    return (getClass().isAnonymousClass() ? getClass().getSuperclass() : getClass()).getSimpleName() + "; id=" + getId();
  }

  public boolean isRunIntentionPassAfter() {
    return myRunIntentionPassAfter;
  }
}
