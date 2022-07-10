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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.vcs.RepositoryLocation;
import consulo.vcs.VcsException;
import consulo.vcs.versionBrowser.CommittedChangeList;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yole
 */
@TopicAPI(ComponentScope.PROJECT)
public interface CommittedChangesListener {
  void changesLoaded(RepositoryLocation location, List<CommittedChangeList> changes);

  void incomingChangesUpdated(@Nullable final List<CommittedChangeList> receivedChanges);

  void changesCleared();

  void presentationChanged();

  void refreshErrorStatusChanged(@Nullable VcsException lastError);
}
