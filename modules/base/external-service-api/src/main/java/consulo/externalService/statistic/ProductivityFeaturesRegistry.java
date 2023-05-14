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
package consulo.externalService.statistic;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * User: anna
 * Date: Feb 3, 2005
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ProductivityFeaturesRegistry {
  @Nonnull
  public abstract Set<String> getFeatureIds();

  public abstract FeatureDescriptor getFeatureDescriptor(@Nonnull String id);

  public abstract GroupDescriptor getGroupDescriptor(@Nonnull String id);

  @Nonnull
  public static ProductivityFeaturesRegistry getInstance() {
    return Application.get().getInstance(ProductivityFeaturesRegistry.class);
  }
}
