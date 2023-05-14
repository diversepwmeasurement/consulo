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
package consulo.language.spellcheker.tokenizer;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.spellcheker.tokenizer.splitter.TokenSplitter;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class TokenizerBase<T extends PsiElement> extends Tokenizer<T> {
  public static <T extends PsiElement> TokenizerBase<T> create(TokenSplitter splitter) {
    return new TokenizerBase<T>(splitter);
  }

  private final TokenSplitter mySplitter;

  public TokenizerBase(TokenSplitter splitter) {
    mySplitter = splitter;
  }

  @RequiredReadAction
  @Override
  public void tokenize(@Nonnull T element, TokenConsumer consumer) {
    if (element instanceof PsiLanguageInjectionHost && InjectedLanguageManager.getInstance(element.getProject()).getInjectedPsiFiles(element) != null) {
      return;
    }
    consumer.consumeToken(element, mySplitter);
  }
}
