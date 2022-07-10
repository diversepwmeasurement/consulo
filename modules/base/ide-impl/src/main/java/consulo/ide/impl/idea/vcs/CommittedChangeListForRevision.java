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
package consulo.ide.impl.idea.vcs;

import consulo.vcs.change.Change;
import consulo.vcs.history.LongRevisionNumber;
import consulo.vcs.history.VcsRevisionNumber;
import consulo.ide.impl.idea.openapi.vcs.versionBrowser.CommittedChangeListImpl;
import consulo.ide.impl.idea.openapi.vcs.versionBrowser.VcsRevisionNumberAware;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Date;

public class CommittedChangeListForRevision extends CommittedChangeListImpl implements VcsRevisionNumberAware {

  @Nonnull
  private VcsRevisionNumber myRevisionNumber;

  public CommittedChangeListForRevision(@Nonnull String subject,
                                        @Nonnull String comment,
                                        @Nonnull String committerName,
                                        @Nonnull Date commitDate,
                                        @Nonnull Collection<Change> changes,
                                        @Nonnull VcsRevisionNumber revisionNumber) {
    super(subject, comment, committerName, getLong(revisionNumber), commitDate, changes);
    myRevisionNumber = revisionNumber;
  }

  @Nonnull
  @Override
  public VcsRevisionNumber getRevisionNumber() {
    return myRevisionNumber;
  }

  private static long getLong(@Nonnull VcsRevisionNumber number) {
    if (number instanceof LongRevisionNumber) return ((LongRevisionNumber)number).getLongRevisionNumber();
    return 0;
  }
}
