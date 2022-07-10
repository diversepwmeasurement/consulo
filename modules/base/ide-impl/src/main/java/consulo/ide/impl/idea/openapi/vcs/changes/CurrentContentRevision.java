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

import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.application.util.function.Computable;
import consulo.vcs.FilePath;
import consulo.vcs.history.VcsRevisionNumber;
import consulo.vcs.change.ContentRevision;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class CurrentContentRevision implements ContentRevision {
  protected FilePath myFile;

  public CurrentContentRevision(final FilePath file) {
    myFile = file;
  }

  @javax.annotation.Nullable
  public String getContent() {
    VirtualFile vFile = getVirtualFile();
    if (vFile == null) {
      myFile.refresh();
      vFile = getVirtualFile();
      if (vFile == null) return null;
    }
    final VirtualFile finalVFile = vFile;
    final Document doc = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
      public Document compute() {
        return FileDocumentManager.getInstance().getDocument(finalVFile);
    }});
    if (doc == null) return null;
    return doc.getText();
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    final VirtualFile vFile = myFile.getVirtualFile();
    if (vFile == null || !vFile.isValid()) return null;
    return vFile;
  }

  @Nonnull
  public FilePath getFile() {
    return myFile;
  }

  @Nonnull
  public VcsRevisionNumber getRevisionNumber() {
    return VcsRevisionNumber.NULL;
  }

  public static ContentRevision create(FilePath file) {
    if (file.getFileType().isBinary()) {
      return new CurrentBinaryContentRevision(file);
    }
    return new CurrentContentRevision(file);
  }

  @NonNls
  public String toString() {
    return "CurrentContentRevision:" + myFile;
  }
}
