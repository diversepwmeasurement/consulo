/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.editor.colors.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.component.extension.Extension;
import consulo.component.extension.ExtensionList;

/**
 * A way to provide additional colors to color schemes.
 * http://youtrack.jetbrains.com/issue/IDEA-98261
 *
 * @author VISTALL
 * @since 12.1
 */
@Extension(name = "additionalTextAttributes", component = Application.class)
public class AdditionalTextAttributesEP extends AbstractExtensionPointBean {
  public static final ExtensionList<AdditionalTextAttributesEP, Application> EP = ExtensionList.of(AdditionalTextAttributesEP.class);

  @Attribute("scheme")
  public String scheme;

  @Attribute("file")
  public String file;
}
