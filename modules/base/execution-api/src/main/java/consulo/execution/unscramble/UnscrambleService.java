/*
 * Copyright 2013-2023 consulo.io
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
package consulo.execution.unscramble;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04/02/2023
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface UnscrambleService {
  @RequiredUIAccess
  default void showAsync() {
    showAsync(null, null);
  }

  @RequiredUIAccess
  default void showAsync(@Nullable String stackTrace) {
    showAsync(stackTrace, null);
  }

  @RequiredUIAccess
  void showAsync(@Nullable String stackTrace, @Nullable StacktraceAnalyzer stacktraceAnalyzer);
}
