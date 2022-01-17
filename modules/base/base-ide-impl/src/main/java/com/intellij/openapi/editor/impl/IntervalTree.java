/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.editor.impl;

import consulo.application.util.function.Processor;
import javax.annotation.Nonnull;

interface IntervalTree<T> {
  boolean processAll(@Nonnull Processor<? super T> processor);

  boolean processOverlappingWith(int start, int end, @Nonnull Processor<? super T> processor);

  boolean processContaining(int offset, @Nonnull Processor<? super T> processor);

  boolean removeInterval(@Nonnull T interval);

  boolean processOverlappingWithOutside(int start, int end, @Nonnull Processor<? super T> processor);
}
