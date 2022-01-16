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
package com.intellij.openapi.vcs.impl;

import consulo.component.extension.ExtensionPointName;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author irengrig
 *         Date: 2/28/11
 *         Time: 1:12 PM
 */
public interface VcsBaseContentProvider {
  ExtensionPointName<VcsBaseContentProvider> EP_NAME = ExtensionPointName.create("consulo.base.vcs.baseContentProvider");

  @Nullable
  BaseContent getBaseRevision(@Nonnull VirtualFile file);

  boolean isSupported(@Nonnull VirtualFile file);

  interface BaseContent {
    @Nonnull
    VcsRevisionNumber getRevisionNumber();

    @Nullable
    String loadContent();
  }
}
