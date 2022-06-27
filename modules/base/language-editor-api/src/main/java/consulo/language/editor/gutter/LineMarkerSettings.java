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
package consulo.language.editor.gutter;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class LineMarkerSettings {
  @Nonnull
  public static LineMarkerSettings getInstance() {
    return Application.get().getInstance(LineMarkerSettings.class);
  }

  public abstract boolean isEnabled(GutterIconDescriptor descriptor);

  public abstract void setEnabled(GutterIconDescriptor descriptor, boolean selected);
}
