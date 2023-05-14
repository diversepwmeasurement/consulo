/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff.actions.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.diff.tools.util.DiffDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.dataContext.DataProvider;
import consulo.util.dataholder.Key;
import consulo.navigation.Navigatable;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class DiffNavigatableArrayRule implements GetDataRule<Navigatable[]> {
  @Nonnull
  @Override
  public Key<Navigatable[]> getKey() {
    return DiffDataKeys.NAVIGATABLE_ARRAY;
  }

  @Override
  public Navigatable[] getData(@Nonnull DataProvider dataProvider) {
    final Navigatable element = dataProvider.getDataUnchecked(DiffDataKeys.NAVIGATABLE);
    if (element == null) {
      return null;
    }

    return new Navigatable[]{element};
  }
}

