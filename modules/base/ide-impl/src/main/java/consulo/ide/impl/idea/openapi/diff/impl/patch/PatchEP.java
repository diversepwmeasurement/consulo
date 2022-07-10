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
package consulo.ide.impl.idea.openapi.diff.impl.patch;

import consulo.component.extension.ExtensionPointName;
import consulo.vcs.change.CommitContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author irengrig
 *         Date: 7/11/11
 *         Time: 11:43 AM
 */
public interface PatchEP {
  ExtensionPointName<PatchEP> EP_NAME = ExtensionPointName.create("consulo.patch.extension");
  @Nonnull
  String getName();
  /**
   * @param path - before path, if exist, otherwise after path
   * @param commitContext
   */
  @Nullable
  CharSequence provideContent(@Nonnull final String path, CommitContext commitContext);
  /**
   * @param path - before path, if exist, otherwise after path
   * @param commitContext
   */
  void consumeContent(@Nonnull final String path, @Nonnull final CharSequence content, CommitContext commitContext);
  /**
   * @param path - before path, if exist, otherwise after path
   * @param commitContext
   */
  void consumeContentBeforePatchApplied(@Nonnull final String path,
                                        @Nonnull final CharSequence content,
                                        CommitContext commitContext);
}
