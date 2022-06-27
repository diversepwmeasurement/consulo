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
 * User: Vladislav.Kaznacheev
 * Date: Jul 4, 2007
 * Time: 3:59:52 PM
 */
package consulo.ide.impl.idea.openapi.keymap;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.ex.action.AnAction;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.util.lang.function.Condition;

import javax.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface KeymapExtension {
  ExtensionPointName<KeymapExtension> EXTENSION_POINT_NAME = ExtensionPointName.create(KeymapExtension.class);

  @Nullable
  KeymapGroup createGroup(Condition<AnAction> filtered, Project project);
}