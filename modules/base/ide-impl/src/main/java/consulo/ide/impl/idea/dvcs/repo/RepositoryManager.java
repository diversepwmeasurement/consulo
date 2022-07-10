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
package consulo.ide.impl.idea.dvcs.repo;

import consulo.vcs.AbstractVcs;
import consulo.vcs.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * The RepositoryManager stores and maintains the mapping between VCS roots (represented by {@link VirtualFile}s)
 * and {@link Repository repositories} containing information and valuable methods specific for DVCS repositories.
 */
public interface RepositoryManager<T extends Repository> {

  @Nonnull
  AbstractVcs getVcs();

  /**
   * Returns the Repository instance which tracks the VCS repository located in the given root directory,
   * or {@code null} if the given root is not a valid registered vcs root.
   * <p/>
   * The method checks both project roots and external roots previously registered
   * via {@link #addExternalRepository(VirtualFile, Repository)}.
   */
  @javax.annotation.Nullable
  T getRepositoryForRoot(@javax.annotation.Nullable VirtualFile root);

  boolean isExternal(@Nonnull T repository);

  /**
   * Returns the {@link Repository} which the given file belongs to, or {@code null} if the file is not under any Git or Hg repository.
   */
  @javax.annotation.Nullable
  T getRepositoryForFile(@Nonnull VirtualFile file);

  /**
   * Returns the {@link Repository} which the given file belongs to, or {@code null} if the file is not under any Git ot Hg repository.
   */
  @javax.annotation.Nullable
  T getRepositoryForFile(@Nonnull FilePath file);

  /**
   * @return all repositories tracked by the manager.
   */
  @Nonnull
  List<T> getRepositories();

  /**
   * Registers a repository which doesn't belong to the project.
   */
  void addExternalRepository(@Nonnull VirtualFile root, @Nonnull T repository);

  /**
   * Removes the repository not from the project, when it is not interesting anymore.
   */
  void removeExternalRepository(@Nonnull VirtualFile root);

  boolean moreThanOneRoot();

  /**
   * Synchronously updates the specified information about repository under the given root.
   *
   * @param root root directory of the vcs repository.
   */
  void updateRepository(VirtualFile root);

  void updateAllRepositories();

  /**
   * Returns true if repositories under this repository manager are controlled synchronously.
   */
  boolean isSyncEnabled();

}
