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

package consulo.content.internal;

import consulo.component.ComponentManager;
import consulo.content.OrderRootType;
import consulo.content.library.Library;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 *  @author dsl
 */
public interface LibraryEx extends Library {
  boolean isDisposed();

  @Nonnull
  @Override
  ModifiableModelEx getModifiableModel();

  /**
   *
   * @return not null only if it module table
   */
  @Nullable
  ComponentManager getModule();

  @Deprecated
  interface ModifiableModelEx extends ModifiableModel {
  }
}
