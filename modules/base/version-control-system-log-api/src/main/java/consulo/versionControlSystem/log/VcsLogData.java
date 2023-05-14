/*
 * Copyright 2013-2023 consulo.io
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

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 21/01/2023
 */
public interface VcsLogData extends Disposable, VcsLogDataProvider {
  @Nonnull
  Project getProject();

  @Nonnull
  Map<VirtualFile, VcsUser> getCurrentUser();

  @Nonnull
  Set<VcsUser> getAllUsers();

  @Nonnull
  VcsLogProvider getLogProvider(@Nonnull VirtualFile root);
}
