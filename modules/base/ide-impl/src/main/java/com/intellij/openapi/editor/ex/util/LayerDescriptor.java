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

package com.intellij.openapi.editor.ex.util;

import consulo.editor.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class LayerDescriptor {
  private final SyntaxHighlighter myLayerHighlighter;
  private final String myTokenSeparator;
  private final TextAttributesKey myBackground;

  public LayerDescriptor(@Nonnull SyntaxHighlighter layerHighlighter, @Nonnull String tokenSeparator, @Nullable TextAttributesKey background) {
    myBackground = background;
    myLayerHighlighter = layerHighlighter;
    myTokenSeparator = tokenSeparator;
  }
  public LayerDescriptor(@Nonnull SyntaxHighlighter layerHighlighter, @Nonnull String tokenSeparator) {
    this(layerHighlighter, tokenSeparator, null);
  }

  @Nonnull
  public SyntaxHighlighter getLayerHighlighter() {
    return myLayerHighlighter;
  }

  @Nonnull
  public String getTokenSeparator() {
    return myTokenSeparator;
  }

  @Nullable
  public TextAttributesKey getBackgroundKey() {
    return myBackground;
  }
}
