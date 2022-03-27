/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ui;

import consulo.document.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import consulo.language.editor.impl.internal.completion.MinusculeMatcher;
import consulo.language.editor.impl.internal.completion.NameUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class SpeedSearchComparator {
  private MinusculeMatcher myMinusculeMatcher;
  private String myRecentSearchText;
  private final boolean myShouldMatchFromTheBeginning;
  private final boolean myShouldMatchCamelCase;

  public SpeedSearchComparator() {
    this(true);
  }

  public SpeedSearchComparator(boolean shouldMatchFromTheBeginning) {
    this(shouldMatchFromTheBeginning, false);
  }

  public SpeedSearchComparator(boolean shouldMatchFromTheBeginning, boolean shouldMatchCamelCase) {
    myShouldMatchFromTheBeginning = shouldMatchFromTheBeginning;
    myShouldMatchCamelCase = shouldMatchCamelCase;
  }

  public int matchingDegree(String pattern, String text) {
    return obtainMatcher(pattern).matchingDegree(text);
  }

  @Nullable
  public Iterable<TextRange> matchingFragments(@Nonnull String pattern, @Nonnull String text) {
    return obtainMatcher(pattern).matchingFragments(text);
  }

  private MinusculeMatcher obtainMatcher(@Nonnull String pattern) {
    if (myRecentSearchText == null || !myRecentSearchText.equals(pattern)) {
      myRecentSearchText = pattern;
      if (myShouldMatchCamelCase) {
        pattern = StringUtil.join(NameUtil.nameToWords(pattern), "*");
      }
      if (!myShouldMatchFromTheBeginning && !pattern.startsWith("*")) {
        pattern = "*" + pattern;
      }
      myMinusculeMatcher = createMatcher(pattern);
    }
    return myMinusculeMatcher;
  }

  @Nonnull
  protected MinusculeMatcher createMatcher(@Nonnull String pattern) {
    return NameUtil.buildMatcher(pattern).build();
  }

  public String getRecentSearchText() {
    return myRecentSearchText;
  }
}
