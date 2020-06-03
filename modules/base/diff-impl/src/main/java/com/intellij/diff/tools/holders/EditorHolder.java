/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.tools.holders;

import consulo.disposer.Disposable;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.FocusListener;

public abstract class EditorHolder implements Disposable {
  @Nonnull
  public abstract JComponent getComponent();

  @javax.annotation.Nullable
  public abstract JComponent getPreferredFocusedComponent();

  public void installFocusListener(@Nonnull FocusListener listener) {
  }
}
