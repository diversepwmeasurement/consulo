/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.openapi.editor.colors.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.BundledColorSchemeProvider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
@ExtensionImpl
public class DefaultBundledColorSchemeProvider implements BundledColorSchemeProvider {
  @Nonnull
  @Override
  public String[] getColorSchemeFiles() {
    return new String[] {
      "/colorSchemes/Default.xml",
      "/colorSchemes/IDEA.xml",
      "/colorSchemes/Consulo Light.xml",
      "/colorSchemes/Darcula.xml",
      "/colorSchemes/all_hallows_eve.xml",
      "/colorSchemes/blackboard.xml",
      "/colorSchemes/cobalt.xml",
      "/colorSchemes/github.xml",
      "/colorSchemes/monokai.xml",
      "/colorSchemes/rails_casts.xml",
      "/colorSchemes/twilight.xml",
      "/colorSchemes/vibrant_ink.xml",
      "/colorSchemes/WarmNeon.xml",
      "/colorSchemes/Visual Studio Light.xml",
      "/colorSchemes/Visual Studio Dark.xml"
    };
  }
}
