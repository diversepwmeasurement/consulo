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
package consulo.ide.impl.idea.diff.tools.binary;

import consulo.ide.impl.idea.diff.DiffContext;
import consulo.ide.impl.idea.diff.FrameDiffTool;
import consulo.diff.request.DiffRequest;
import jakarta.annotation.Nonnull;

public class BinaryDiffTool implements FrameDiffTool {
  public static final BinaryDiffTool INSTANCE = new BinaryDiffTool();

  @Nonnull
  @Override
  public DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    if (OnesideBinaryDiffViewer.canShowRequest(context, request)) return new OnesideBinaryDiffViewer(context, request);
    if (TwosideBinaryDiffViewer.canShowRequest(context, request)) return new TwosideBinaryDiffViewer(context, request);
    if (ThreesideBinaryDiffViewer.canShowRequest(context, request)) return new ThreesideBinaryDiffViewer(context, request);
    throw new IllegalArgumentException(request.toString());
  }

  @Override
  public boolean canShow(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return OnesideBinaryDiffViewer.canShowRequest(context, request) ||
           TwosideBinaryDiffViewer.canShowRequest(context, request) ||
           ThreesideBinaryDiffViewer.canShowRequest(context, request);
  }

  @Nonnull
  @Override
  public String getName() {
    return "Binary file viewer";
  }
}
