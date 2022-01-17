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

package com.intellij.psi.search.searches;

import consulo.application.Application;
import com.intellij.openapi.extensions.SimpleSmartExtensionPoint;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.QueryExecutor;
import com.intellij.util.QueryFactory;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointId;
import consulo.container.plugin.PluginIds;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class ExtensibleQueryFactory<Result, Parameters> extends QueryFactory<Result, Parameters> {
  private final NotNullLazyValue<SimpleSmartExtensionPoint<QueryExecutor<Result,Parameters>>> myPoint;

  protected ExtensibleQueryFactory() {
    this(PluginIds.CONSULO_BASE.getIdString());
  }

  protected ExtensibleQueryFactory(final String epNamespace) {
    myPoint = new NotNullLazyValue<>() {
      @Override
      @Nonnull
      protected SimpleSmartExtensionPoint<QueryExecutor<Result, Parameters>> compute() {
        return new SimpleSmartExtensionPoint<>(new ArrayList<>()){
          @Override
          @Nonnull
          protected ExtensionPoint<QueryExecutor<Result, Parameters>> getExtensionPoint() {
            String epName = ExtensibleQueryFactory.this.getClass().getName();
            int pos = epName.lastIndexOf('.');
            if (pos >= 0) {
              epName = epName.substring(pos+1);
            }
            epName = epNamespace + "." + StringUtil.decapitalize(epName);
            return Application.get().getExtensionPoint(ExtensionPointId.of(epName));
          }
        };
      }
    };
  }

  public void registerExecutor(final QueryExecutor<Result, Parameters> queryExecutor, Disposable parentDisposable) {
    registerExecutor(queryExecutor);
    Disposer.register(parentDisposable, () -> unregisterExecutor(queryExecutor));
  }

  @Override
  public void registerExecutor(@Nonnull final QueryExecutor<Result, Parameters> queryExecutor) {
    myPoint.getValue().addExplicitExtension(queryExecutor);
  }

  @Override
  public void unregisterExecutor(@Nonnull final QueryExecutor<Result, Parameters> queryExecutor) {
    myPoint.getValue().removeExplicitExtension(queryExecutor);
  }

  @Override
  @Nonnull
  protected List<QueryExecutor<Result, Parameters>> getExecutors() {
    return myPoint.getValue().getExtensions();
  }
}