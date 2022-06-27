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

package consulo.ide.impl.psi.file;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author spleaner
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class FileLookupInfoProvider {
  public static ExtensionPointName<FileLookupInfoProvider> EP_NAME = ExtensionPointName.create(FileLookupInfoProvider.class);

  @Nonnull
  public abstract FileType[] getFileTypes();

  @Nullable
  public abstract Pair<String, String> getLookupInfo(@Nonnull final VirtualFile file, Project project);
}
