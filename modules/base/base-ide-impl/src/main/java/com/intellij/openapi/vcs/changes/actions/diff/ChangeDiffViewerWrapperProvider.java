/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.actions.diff;

import consulo.component.extension.ExtensionPointName;
import consulo.progress.ProcessCanceledException;
import consulo.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolder;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.impl.DiffViewerWrapper;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.util.ThreeState;
import javax.annotation.Nonnull;

public interface ChangeDiffViewerWrapperProvider {
  ExtensionPointName<ChangeDiffViewerWrapperProvider> EP_NAME =
          ExtensionPointName.create("consulo.base.openapi.vcs.changes.actions.diff.ChangeDiffViewerWrapperProvider");

  @Nonnull
  ThreeState isEquals(@Nonnull Change change1, @Nonnull Change change2);

  boolean canCreate(@javax.annotation.Nullable Project project, @Nonnull Change change);

  @Nonnull
  DiffViewerWrapper process(@Nonnull ChangeDiffRequestProducer presentable,
                            @Nonnull UserDataHolder context,
                            @Nonnull ProgressIndicator indicator) throws DiffRequestProducerException, ProcessCanceledException;
}
