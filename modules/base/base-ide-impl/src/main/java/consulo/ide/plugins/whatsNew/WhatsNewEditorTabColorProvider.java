/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.plugins.whatsNew;

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.ui.LightColors;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15/11/2021
 */
public class WhatsNewEditorTabColorProvider implements EditorTabColorProvider, DumbAware {
  @Nullable
  @Override
  public Color getEditorTabColor(Project project, VirtualFile file) {
    if (file instanceof WhatsNewVirtualFile) {
      return LightColors.SLIGHTLY_GREEN;
    }
    return null;
  }
}
