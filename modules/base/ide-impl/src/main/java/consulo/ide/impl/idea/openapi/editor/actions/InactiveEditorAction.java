/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class InactiveEditorAction extends EditorAction {
  protected InactiveEditorAction(EditorActionHandler defaultHandler) {
    super(defaultHandler);
  }

  @Override
  @Nullable
  protected Editor getEditor(@Nonnull final DataContext dataContext) {
    return dataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
  }
}