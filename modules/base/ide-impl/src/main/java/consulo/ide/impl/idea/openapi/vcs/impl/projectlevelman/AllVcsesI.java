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
package consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.vcs.AbstractVcs;
import consulo.vcs.VcsDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author Irina.Chernushina
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface AllVcsesI {
  @Nullable
  AbstractVcs getByName(String name);

  @Nullable
  VcsDescriptor getDescriptor(final String name);

  VcsDescriptor[] getAll();

  @Nonnull
  Collection<AbstractVcs> getSupportedVcses();

  boolean isEmpty();
}
