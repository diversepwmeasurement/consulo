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
package consulo.ide.impl.idea.vcs.log;

import consulo.versionControlSystem.log.VcsLogRootFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;

public class VcsLogRootFilterImpl implements VcsLogRootFilter {
  @Nonnull
  private final Collection<VirtualFile> myRoots;

  public VcsLogRootFilterImpl(@Nonnull Collection<VirtualFile> roots) {
    myRoots = roots;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getRoots() {
    return myRoots;
  }
}
