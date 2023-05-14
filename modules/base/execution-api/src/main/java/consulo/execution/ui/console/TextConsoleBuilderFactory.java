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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 04.08.2006
 * Time: 17:57:31
 */
package consulo.execution.ui.console;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.content.scope.SearchScope;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author dyoma
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class TextConsoleBuilderFactory {
  public abstract TextConsoleBuilder createBuilder(@Nonnull Project project);

  public abstract TextConsoleBuilder createBuilder(@Nonnull Project project, @Nonnull SearchScope scope);

  public static TextConsoleBuilderFactory getInstance() {
    return Application.get().getInstance(TextConsoleBuilderFactory.class);
  }
}