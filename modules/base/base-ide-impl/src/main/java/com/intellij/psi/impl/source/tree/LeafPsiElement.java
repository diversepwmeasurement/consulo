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

package com.intellij.psi.impl.source.tree;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import consulo.logging.Logger;
import consulo.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.ResolveScopeManager;
import com.intellij.psi.impl.SharedPsiElementImplUtil;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import consulo.lang.LanguageVersion;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;

public class LeafPsiElement extends LeafElement implements PsiElement, NavigationItem {
  private static final Logger LOG = Logger.getInstance(LeafPsiElement.class);

  public LeafPsiElement(@Nonnull IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  public PsiElement getFirstChild() {
    return null;
  }

  @Override
  public PsiElement getLastChild() {
    return null;
  }

  @Override
  public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
  }

  @Override
  public PsiElement getParent() {
    return SharedImplUtil.getParent(this);
  }

  @Override
  public PsiElement getNextSibling() {
    return SharedImplUtil.getNextSibling(this);
  }

  @Override
  public PsiElement getPrevSibling() {
    return SharedImplUtil.getPrevSibling(this);
  }

  @Override
  public PsiFile getContainingFile() {
    final PsiFile file = SharedImplUtil.getContainingFile(this);
    if (file == null || !file.isValid()) invalid();
    return file;
  }

  @Contract("-> fail")
  private void invalid() {
    ProgressIndicatorProvider.checkCanceled();

    final StringBuilder builder = new StringBuilder();
    TreeElement element = this;
    while (element != null) {
      if (element != this) builder.append(" / ");
      builder.append(element.getClass().getName()).append(':').append(element.getElementType());
      element = element.getTreeParent();
    }

    throw new PsiInvalidElementAccessException(this, builder.toString());
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return this;
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return SharedPsiElementImplUtil.findReferenceAt(this, offset);
  }

  @Override
  public PsiElement copy() {
    ASTNode elementCopy = copyElement();
    return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
  }

  @Override
  public boolean isValid() {
    return SharedImplUtil.isValid(this);
  }

  @Override
  public boolean isWritable() {
    return SharedImplUtil.isWritable(this);
  }

  @Override
  public PsiReference getReference() {
    return null;
  }

  @Override
  @Nonnull
  public PsiReference[] getReferences() {
    return SharedPsiElementImplUtil.getReferences(this);
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void delete() throws IncorrectOperationException {
    LOG.assertTrue(getTreeParent() != null);
    CheckUtil.checkWritable(this);
    getTreeParent().deleteChildInternal(this);
    invalidate();
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    CheckUtil.checkWritable(this);
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    return SharedImplUtil.doReplace(this, this, newElement);
  }

  @Override
  public String toString() {
    return "PsiElement" + "(" + getElementType().toString() + ")";
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitElement(this);
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState state, PsiElement lastParent, @Nonnull PsiElement place) {
    return true;
  }

  @Override
  public PsiElement getContext() {
    return getParent();
  }

  @Override
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  public PsiElement getOriginalElement() {
    return this;
  }

  @Override
  public boolean isPhysical() {
    PsiFile file = getContainingFile();
    return file != null && file.isPhysical();
  }

  @Override
  @Nonnull
  public GlobalSearchScope getResolveScope() {
    return ResolveScopeManager.getElementResolveScope(this);
  }

  @Override
  @Nonnull
  public SearchScope getUseScope() {
    return ResolveScopeManager.getElementUseScope(this);
  }

  @Override
  @Nonnull
  public Project getProject() {
    Project project = ProjectCoreUtil.theOnlyOpenProject();
    if (project != null) {
      return project;
    }
    final PsiManager manager = getManager();
    if (manager == null) invalid();
    return manager.getProject();
  }

  @Override
  @Nonnull
  public Language getLanguage() {
    return getElementType().getLanguage();
  }

  @Nonnull
  @Override
  public LanguageVersion getLanguageVersion() {
    return PsiTreeUtil.getLanguageVersion(this);
  }

  @Override
  public ASTNode getNode() {
    return this;
  }

  @Override
  public PsiElement getPsi() {
    return this;
  }

  @Override
  public ItemPresentation getPresentation() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public void navigate(boolean requestFocus) {
    final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(this);
    if (descriptor != null) {
      descriptor.navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return PsiNavigationSupport.getInstance().canNavigate(this);
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @Override
  public boolean isEquivalentTo(final PsiElement another) {
    return this == another;
  }
}
