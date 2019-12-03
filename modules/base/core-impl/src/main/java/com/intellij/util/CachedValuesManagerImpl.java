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
package com.intellij.util;

import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author ven
 */
@Singleton
public class CachedValuesManagerImpl extends CachedValuesManager {
  private final Project myProject;
  private final CachedValuesFactory myFactory;

  @Inject
  public CachedValuesManagerImpl(Project project, CachedValuesFactory factory) {
    myProject = project;
    myFactory = factory == null ? new DefaultCachedValuesFactory(project) : factory;
  }

  @Nonnull
  @Override
  public <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider, boolean trackValue) {
    return myFactory.createCachedValue(provider, trackValue);
  }

  @Nonnull
  @Override
  public <T,P> ParameterizedCachedValue<T,P> createParameterizedCachedValue(@Nonnull ParameterizedCachedValueProvider<T,P> provider, boolean trackValue) {
    return myFactory.createParameterizedCachedValue(provider, trackValue);
  }

  @Override
  @Nullable
  public <T, D extends UserDataHolder> T getCachedValue(@Nonnull D dataHolder,
                                                        @Nonnull Key<CachedValue<T>> key,
                                                        @Nonnull CachedValueProvider<T> provider,
                                                        boolean trackValue) {
    CachedValue<T> value;
    if (dataHolder instanceof UserDataHolderEx) {
      UserDataHolderEx dh = (UserDataHolderEx)dataHolder;
      value = dh.getUserData(key);
      if (value instanceof CachedValueBase && !((CachedValueBase)value).isFromMyProject(myProject)) {
        value = null;
        dh.putUserData(key, null);
      }
      if (value == null) {
        value = createCachedValue(provider, trackValue);
        assert ((CachedValueBase)value).isFromMyProject(myProject);
        value = dh.putUserDataIfAbsent(key, value);
      }
    }
    else {
      synchronized (dataHolder) {
        value = dataHolder.getUserData(key);
        if (value instanceof CachedValueBase && !((CachedValueBase)value).isFromMyProject(myProject)) {
          value = null;
        }
        if (value == null) {
          value = createCachedValue(provider, trackValue);
          dataHolder.putUserData(key, value);
        }
      }
    }
    return value.getValue();
  }

  public Project getProject() {
    return myProject;
  }
}
