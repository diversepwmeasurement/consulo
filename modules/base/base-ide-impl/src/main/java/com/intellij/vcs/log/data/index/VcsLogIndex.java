/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.vcs.log.data.index;

import consulo.virtualFileSystem.VirtualFile;
import com.intellij.vcs.log.VcsLogDetailsFilter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;

public interface VcsLogIndex {
  void scheduleIndex(boolean full);

  boolean isIndexed(int commit);

  boolean isIndexed(@Nonnull VirtualFile root);

  void markForIndexing(int commit, @Nonnull VirtualFile root);

  boolean canFilter(@Nonnull List<VcsLogDetailsFilter> filters);

  @Nonnull
  Set<Integer> filter(@Nonnull List<VcsLogDetailsFilter> detailsFilters);

  @Nullable
  String getFullMessage(int index);

  void markCorrupted();
}
