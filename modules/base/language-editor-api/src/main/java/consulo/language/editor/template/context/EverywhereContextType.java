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

package consulo.language.editor.template.context;

import consulo.annotation.component.ExtensionImpl;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl(id = "OTHER", order = "last")
public class EverywhereContextType extends TemplateContextType {
  public EverywhereContextType() {
    super("OTHER", "Everywhere", null);
  }

  @Override
  public boolean isInContext(@Nonnull TemplateActionContext templateActionContext) {
    return true;
  }
}
