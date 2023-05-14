/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.web.internal.layout;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebVerticalTwoComponentSplitLayoutImpl extends WebTwoComponentSplitLayoutBase<WebVerticalTwoComponentSplitLayoutImpl.Vaadin> {
  public static class Vaadin extends WebSplitLayoutVaadinBase<WebVerticalTwoComponentSplitLayoutImpl> {
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
