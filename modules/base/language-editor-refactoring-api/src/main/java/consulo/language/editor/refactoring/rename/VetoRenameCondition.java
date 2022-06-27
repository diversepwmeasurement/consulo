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
package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionList;
import consulo.language.psi.PsiElement;

/**
 * @author VISTALL
 * @since 16-Apr-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface VetoRenameCondition {
  ExtensionList<VetoRenameCondition, Application> EP = ExtensionList.of(VetoRenameCondition.class);

  @RequiredReadAction
  boolean isVetoed(PsiElement element);
}
