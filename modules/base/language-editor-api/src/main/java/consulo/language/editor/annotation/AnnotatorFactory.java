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
package consulo.language.editor.annotation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 27-Jun-22
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface AnnotatorFactory extends LanguageExtension {
  ExtensionPointCacheKey<AnnotatorFactory, ByLanguageValue<List<AnnotatorFactory>>> KEY = ExtensionPointCacheKey.create("AnnotatorFactory", LanguageOneToMany.build(true));

  @Nonnull
  static List<AnnotatorFactory> forLanguage(@Nonnull Project project, @Nonnull Language language) {
    return project.getExtensionPoint(AnnotatorFactory.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nullable
  Annotator createAnnotator();
}
