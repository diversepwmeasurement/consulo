/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.openapi.impl;

import consulo.project.Project;
import consulo.util.lang.EmptyRunnable;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.RefactoringImpl;
import consulo.ide.impl.idea.refactoring.SafeDeleteRefactoring;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteProcessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author dsl
 */
public class SafeDeleteRefactoringImpl extends RefactoringImpl<SafeDeleteProcessor> implements SafeDeleteRefactoring {
  SafeDeleteRefactoringImpl(Project project, PsiElement[] elements) {
    super(SafeDeleteProcessor.createInstance(project, EmptyRunnable.INSTANCE, elements, true, true));
  }

  @Override
  public List<PsiElement> getElements() {
    final PsiElement[] elements = myProcessor.getElements();
    return Collections.unmodifiableList(Arrays.asList(elements));
  }

  @Override
  public boolean isSearchInComments() {
    return myProcessor.isSearchInCommentsAndStrings();
  }

  @Override
  public void setSearchInComments(boolean value) {
    myProcessor.setSearchInCommentsAndStrings(value);
  }

  @Override
  public void setSearchInNonJavaFiles(boolean value) {
    myProcessor.setSearchNonJava(value);
  }

  @Override
  public boolean isSearchInNonJavaFiles() {
    return myProcessor.isSearchNonJava();
  }
}
