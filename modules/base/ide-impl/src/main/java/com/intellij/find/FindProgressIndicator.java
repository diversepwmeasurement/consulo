/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.find;

import consulo.find.FindBundle;
import consulo.project.Project;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import javax.annotation.Nonnull;

/**
 * @author ven
 */
public class FindProgressIndicator extends BackgroundableProcessIndicator {
  public FindProgressIndicator(@Nonnull Project project, String scopeString) {
    super(project,
          FindBundle.message("find.progress.searching.message", scopeString),
          new SearchInBackgroundOption(),
          FindBundle.message("find.progress.stop.title"),
          FindBundle.message("find.progress.stop.background.button"), true);
  }
}
