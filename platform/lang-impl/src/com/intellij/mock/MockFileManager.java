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
package com.intellij.mock;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.RequiredWriteAction;

import java.util.List;

/**
 * @author peter
 */
public class MockFileManager implements FileManager {
  private final PsiManagerEx myManager;
  private final FactoryMap<VirtualFile,FileViewProvider> myViewProviders = new FactoryMap<VirtualFile, FileViewProvider>() {
    @Override
    protected FileViewProvider create(final VirtualFile key) {
      return new SingleRootFileViewProvider(myManager, key);
    }
  };

  @Override
  @NotNull
  public FileViewProvider createFileViewProvider(@NotNull final VirtualFile file, final boolean physical) {
    return new SingleRootFileViewProvider(myManager, file, physical);
  }

  public MockFileManager(final PsiManagerEx manager) {
    myManager = manager;
  }

  @Override
  public void dispose() {
    throw new UnsupportedOperationException("Method dispose is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiFile findFile(@NotNull VirtualFile vFile) {
    return getCachedPsiFile(vFile);
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiDirectory findDirectory(@NotNull VirtualFile vFile) {
    throw new UnsupportedOperationException("Method findDirectory is not yet implemented in " + getClass().getName());
  }

  @RequiredWriteAction
  @Override
  public void reloadFromDisk(@NotNull PsiFile file) //Q: move to PsiFile(Impl)?
  {
    throw new UnsupportedOperationException("Method reloadFromDisk is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiFile getCachedPsiFile(@NotNull VirtualFile vFile) {
    final FileViewProvider provider = findCachedViewProvider(vFile);
    return provider.getPsi(provider.getBaseLanguage());
  }

  @Override
  public void cleanupForNextTest() {
    myViewProviders.clear();
  }

  @RequiredReadAction
  @Override
  public FileViewProvider findViewProvider(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException("Method findViewProvider is not yet implemented in " + getClass().getName());
  }

  @RequiredReadAction
  @Override
  public FileViewProvider findCachedViewProvider(@NotNull VirtualFile file) {
    return myViewProviders.get(file);
  }

  @RequiredReadAction
  @Override
  public void setViewProvider(@NotNull VirtualFile virtualFile, FileViewProvider fileViewProvider) {
    myViewProviders.put(virtualFile, fileViewProvider);
  }

  @Override
  @NotNull
  public List<PsiFile> getAllCachedFiles() {
    throw new UnsupportedOperationException("Method getAllCachedFiles is not yet implemented in " + getClass().getName());
  }
}
