/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.event;

import consulo.annotation.DeprecationInfo;
import consulo.ui.Component;
import consulo.ui.event.details.InputDetails;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public final class ClickEvent extends UIEvent<Component> {
  @Deprecated
  @DeprecationInfo("Use constructor with InputDetails")
  public ClickEvent(@Nonnull Component component) {
    super(component);
  }

  public ClickEvent(@Nonnull Component component, @Nullable InputDetails inputDetails) {
    super(component, inputDetails);
  }
}
