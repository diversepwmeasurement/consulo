/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.psi.impl.source.codeStyle.lineIndent;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.codeStyle.FormattingMode;
import consulo.language.Language;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.ide.impl.psi.codeStyle.lineIndent.LineIndentProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Formatter-based line indent provider which calculates indent using formatting model.
 */
@ExtensionImpl(order = "last")
public class FormatterBasedLineIndentProvider implements LineIndentProvider {
  @Nullable
  @Override
  public String getLineIndent(@Nonnull Project project, @Nonnull Editor editor, Language language, int offset) {
    Document document = editor.getDocument();
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    documentManager.commitDocument(document);
    PsiFile file = documentManager.getPsiFile(document);
    if (file == null) return "";
    return CodeStyleManager.getInstance(project).getLineIndent(file, offset, FormattingMode.ADJUST_INDENT_ON_ENTER);
  }

  @Override
  public boolean isSuitableFor(@Nullable Language language) {
    return true;
  }
}
