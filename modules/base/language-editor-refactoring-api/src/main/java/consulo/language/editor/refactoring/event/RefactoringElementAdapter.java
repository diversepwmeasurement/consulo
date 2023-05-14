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
package consulo.language.editor.refactoring.event;

import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public abstract class RefactoringElementAdapter implements RefactoringElementListener, UndoRefactoringElementListener {
  @Override
  public final void elementMoved(@Nonnull PsiElement newElement) {
    elementRenamedOrMoved(newElement);
  }

  protected abstract void elementRenamedOrMoved(@Nonnull PsiElement newElement);

  @Override
  public final void elementRenamed(@Nonnull PsiElement newElement) {
    elementRenamedOrMoved(newElement);
  }
}
