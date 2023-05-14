/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log;

import consulo.util.dataholder.Key;
import consulo.versionControlSystem.log.event.VcsLogListener;

import jakarta.annotation.Nonnull;

public interface VcsLogUi {
  Key<VcsLogUi> KEY = Key.create(VcsLogUi.class);

  @Nonnull
  VcsLogFilterUi getFilterUi();

  @Nonnull
  VcsLogDataPack getDataPack();

  void addLogListener(@Nonnull VcsLogListener listener);

  void removeLogListener(@Nonnull VcsLogListener listener);

  boolean areGraphActionsEnabled();

  boolean isMultipleRoots();

  boolean isHighlighterEnabled(@Nonnull String id);
}
