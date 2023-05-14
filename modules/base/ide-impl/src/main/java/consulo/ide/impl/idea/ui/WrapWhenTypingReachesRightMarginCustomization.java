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
package consulo.ide.impl.idea.ui;

import consulo.codeEditor.EditorEx;
import consulo.language.editor.ui.EditorCustomization;
import consulo.language.editor.ui.SimpleEditorCustomization;

import jakarta.annotation.Nonnull;

/**
 * @author Kirill Likhodedov
 */
public class WrapWhenTypingReachesRightMarginCustomization extends SimpleEditorCustomization {

  public static final WrapWhenTypingReachesRightMarginCustomization ENABLED = new WrapWhenTypingReachesRightMarginCustomization(true);
  public static final WrapWhenTypingReachesRightMarginCustomization DISABLED = new WrapWhenTypingReachesRightMarginCustomization(false);

  public static EditorCustomization getInstance(boolean value) {
    return value ? ENABLED : DISABLED;
  }

  private WrapWhenTypingReachesRightMarginCustomization(boolean enabled) {
    super(enabled);
  }

  @Override
  public void customize(@Nonnull EditorEx editor) {
    editor.getSettings().setWrapWhenTypingReachesRightMargin(isEnabled());
  }

}
