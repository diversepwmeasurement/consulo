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
package consulo.ide.impl.idea.peer.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.vcs.LocalFilePath;
import consulo.ide.impl.idea.openapi.vcs.RemoteFilePath;
import consulo.ide.impl.idea.openapi.vcs.actions.VcsContextWrapper;
import consulo.ide.impl.idea.openapi.vcs.changes.LocalChangeListImpl;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.vcs.FilePath;
import consulo.vcs.action.VcsContext;
import consulo.vcs.action.VcsContextFactory;
import consulo.vcs.change.LocalChangeList;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.function.Function;

@Singleton
@ServiceImpl
public class VcsContextFactoryImpl implements VcsContextFactory {
  @Override
  @Nonnull
  public VcsContext createCachedContextOn(@Nonnull AnActionEvent event) {
    return VcsContextWrapper.createCachedInstanceOn(event);
  }

  @Override
  @Nonnull
  public VcsContext createContextOn(@Nonnull AnActionEvent event) {
    return new VcsContextWrapper(event.getDataContext(), event.getModifiers(), event.getPlace(), event.getPresentation().getText());
  }

  @Override
  @Nonnull
  public FilePath createFilePathOn(@Nonnull VirtualFile virtualFile) {
    return createFilePath(virtualFile.getPath(), virtualFile.isDirectory());
  }

  @Override
  @Nonnull
  public FilePath createFilePathOn(@Nonnull File file) {
    String path = file.getPath();
    VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(path);
    return createFilePath(path, vf != null ? vf.isDirectory() : file.isDirectory());
  }

  @Override
  @Nonnull
  public FilePath createFilePathOn(@Nonnull final File file, @Nonnull final Function<File, Boolean> detector) {
    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
    if (virtualFile != null) {
      return createFilePathOn(virtualFile);
    }
    return createFilePathOn(file, detector.apply(file));
  }

  @Override
  @Nonnull
  public FilePath createFilePathOn(@Nonnull final File file, final boolean isDirectory) {
    return createFilePath(file.getPath(), isDirectory);
  }

  @Override
  @Nonnull
  public FilePath createFilePathOnNonLocal(@Nonnull final String path, final boolean isDirectory) {
    return new RemoteFilePath(path, isDirectory);
  }

  @Override
  @Nonnull
  public FilePath createFilePathOnDeleted(@Nonnull final File file, final boolean isDirectory) {
    return createFilePathOn(file, isDirectory);
  }

  @Override
  @Nonnull
  public FilePath createFilePathOn(@Nonnull final VirtualFile parent, @Nonnull final String name) {
    return createFilePath(parent, name, false);
  }

  @Nonnull
  @Override
  public FilePath createFilePath(@Nonnull VirtualFile parent, @Nonnull String fileName, boolean isDirectory) {
    return createFilePath(parent.getPath() + "/" + fileName, isDirectory);
  }

  @Override
  @Nonnull
  public LocalChangeList createLocalChangeList(@Nonnull Project project, @Nonnull final String name) {
    return LocalChangeListImpl.createEmptyChangeListImpl(project, name);
  }

  @Nonnull
  @Override
  public FilePath createFilePath(@Nonnull String path, boolean isDirectory) {
    return new LocalFilePath(path, isDirectory);
  }
}