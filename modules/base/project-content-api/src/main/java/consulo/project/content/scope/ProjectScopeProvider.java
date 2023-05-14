/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
 * @author max
 */
package consulo.project.content.scope;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class ProjectScopeProvider {
  public static ProjectScopeProvider getInstance(Project project) {
    return project.getInstance(ProjectScopeProvider.class);
  }

  @Nonnull
  public abstract ProjectAwareSearchScope getEverythingScope();

  @Nonnull
  public abstract ProjectAwareSearchScope getLibrariesScope();

  /**
   * @return Scope for all things inside the project: files in the project content plus files in libraries/libraries sources
   */
  @Nonnull
  public abstract ProjectAwareSearchScope getAllScope();

  @Nonnull
  public abstract ProjectAwareSearchScope getProjectScope();

  @Nonnull
  public abstract ProjectAwareSearchScope getContentScope();
}
