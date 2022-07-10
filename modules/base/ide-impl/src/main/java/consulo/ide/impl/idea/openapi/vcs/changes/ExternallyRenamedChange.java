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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.vcs.FilePath;
import consulo.vcs.VcsBundle;
import consulo.vcs.change.Change;
import consulo.vcs.change.ContentRevision;

import javax.annotation.Nullable;

public class ExternallyRenamedChange extends Change {
  private String myRenamedTargetName;
  private boolean myCopied;
  private final String myOriginUrl;

  public ExternallyRenamedChange(final ContentRevision beforeRevision, final ContentRevision afterRevision, final String originUrl) {
    super(beforeRevision, afterRevision);
    myOriginUrl = originUrl;
    myCopied = true;
  }

  public void setRenamedOrMovedTarget(final FilePath target) {
    myMoved = myRenamed = false;

    if ((getBeforeRevision() != null) || (getAfterRevision() == null)) {
      // not external rename or move
      return;
    }
    final FilePath localPath = ChangesUtil.getFilePath(this);
    if (localPath.getIOFile().getAbsolutePath().equals(target.getIOFile().getAbsolutePath())) {
      // not rename or move
      return;
    }

    if (Comparing.equal(target.getParentPath(), localPath.getParentPath())) {
      myRenamed = true;
    } else {
      myMoved = true;
    }
    myCopied = false;

    myRenamedTargetName = target.getName();
    myRenameOrMoveCached = true;
  }

  @Override
  public String getOriginText(final Project project) {
    if (myCopied) {
      return VcsBundle.message("change.file.copied.from.text", myOriginUrl);
    }
    return super.getOriginText(project);
  }

  @Nullable
  protected String getRenamedText() {
    if (myRenamedTargetName != null) {
      return VcsBundle.message((getBeforeRevision() != null) ? "change.file.renamed.to.text" : "change.file.renamed.from.text", myRenamedTargetName);
    }
    return super.getRenamedText();
  }

  @Nullable
  protected String getMovedText(final Project project) {
    if (myRenamedTargetName != null) {
      return VcsBundle.message((getBeforeRevision() != null) ? "change.file.moved.to.text" : "change.file.moved.from.text", myRenamedTargetName);
    }
    return super.getMovedText(project);
  }

  public boolean isCopied() {
    return myCopied;
  }

  public void setCopied(final boolean copied) {
    myCopied = copied;
  }

  public String getOriginUrl() {
    return myOriginUrl;
  }
}
