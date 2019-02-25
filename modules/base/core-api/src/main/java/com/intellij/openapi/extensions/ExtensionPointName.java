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

package com.intellij.openapi.extensions;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author mike
 */
public class ExtensionPointName<T> {
  private final String myName;

  public ExtensionPointName(@NonNls final String name) {
    myName = name;
  }

  public static <T> ExtensionPointName<T> create(@NonNls final String name) {
    return new ExtensionPointName<T>(name);
  }

  public String getName() {
    return myName;
  }

  @Override
  public String toString() {
    return myName;
  }

  @Nonnull
  public T[] getExtensions() {
    return getExtensions(Application.get());
  }

  @Nonnull
  public T[] getExtensions(@Nonnull ComponentManager componentManager) {
    return componentManager.getExtensions(this);
  }

  @Nullable
  public <V extends T> V findExtension(@Nonnull Class<V> instanceOf) {
    return findExtension(Application.get(), instanceOf);
  }

  @Nullable
  public <V extends T> V findExtension(@Nonnull ComponentManager componentManager, @Nonnull Class<V> instanceOf) {
    return ContainerUtil.findInstance(getExtensions(componentManager), instanceOf);
  }
}
