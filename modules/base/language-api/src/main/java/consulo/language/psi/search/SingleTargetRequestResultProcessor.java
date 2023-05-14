// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceService;
import consulo.language.psi.ReferenceRange;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author peter
 */
public final class SingleTargetRequestResultProcessor extends RequestResultProcessor {
  private static final PsiReferenceService ourReferenceService = PsiReferenceService.getService();
  private final PsiElement myTarget;

  public SingleTargetRequestResultProcessor(@Nonnull PsiElement target) {
    super(target);
    myTarget = target;
  }

  @Override
  public boolean processTextOccurrence(@Nonnull PsiElement element, int offsetInElement, @Nonnull final Processor<? super PsiReference> consumer) {
    if (!myTarget.isValid()) {
      return false;
    }

    final List<PsiReference> references = ourReferenceService.getReferences(element, new PsiReferenceService.Hints(myTarget, offsetInElement));
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < references.size(); i++) {
      PsiReference ref = references.get(i);
      ProgressManager.checkCanceled();
      if (ReferenceRange.containsOffsetInElement(ref, offsetInElement) && ref.isReferenceTo(myTarget) && !consumer.process(ref)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("HardCodedStringLiteral")
  @Override
  public String toString() {
    return "SingleTarget: " + myTarget;
  }
}
