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

package com.intellij.openapi.util;

import consulo.application.util.function.ThrowableComputable;

import javax.annotation.Nonnull;

import java.lang.reflect.Constructor;

/**
 * @author peter
 */
public abstract class LazyInstance<T> extends NotNullLazyValue<T> {
  @Nonnull
  public static <T> LazyInstance<T> createInstance(@Nonnull final ThrowableComputable<Class<T>, ClassNotFoundException> value) {
    return new LazyInstance<T>() {
      @Nonnull
      @Override
      protected Class<T> getInstanceClass() throws ClassNotFoundException {
        return value.compute();
      }
    };
  }

  @Nonnull
  protected abstract Class<T> getInstanceClass() throws ClassNotFoundException;

  @Override
  @Nonnull
  protected final T compute() {
    try {
      Class<T> tClass = getInstanceClass();
      Constructor<T> constructor = tClass.getConstructor();
      constructor.setAccessible(true);
      return tClass.newInstance();
    }
    catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
