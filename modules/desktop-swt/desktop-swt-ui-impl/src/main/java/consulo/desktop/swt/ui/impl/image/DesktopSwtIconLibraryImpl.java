/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl.image;

import consulo.ui.image.Image;
import consulo.ui.impl.image.BaseIconLibraryImpl;
import consulo.ui.impl.image.BaseIconLibraryManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtIconLibraryImpl extends BaseIconLibraryImpl {
  public DesktopSwtIconLibraryImpl(@Nonnull String id, @Nonnull BaseIconLibraryManager baseIconLibraryManager) {
    super(id, baseIconLibraryManager);
  }

  @Nullable
  @Override
  protected Image createImage(@Nonnull byte[] _1xData, @Nullable byte[] _2xdata, boolean isSVG, int width, int height, String groupId, String imageId) {
    return new DesktopSwtLibraryImageImpl(width, height);
  }
}
