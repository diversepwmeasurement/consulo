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
package com.intellij.packaging.artifacts;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import consulo.component.util.ModificationTracker;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import consulo.component.messagebus.Topic;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;

/**
 * @author nik
 */
public abstract class ArtifactManager implements ArtifactModel {
  @Nonnull
  public static ArtifactManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ArtifactManager.class);
  }

  public static final Topic<ArtifactListener> TOPIC = Topic.create("artifacts changes", ArtifactListener.class);
  public static final Comparator<Artifact> ARTIFACT_COMPARATOR = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());

  public abstract Artifact[] getSortedArtifacts();

  public abstract ModifiableArtifactModel createModifiableModel();

  public abstract PackagingElementResolvingContext getResolvingContext();

  public abstract void addElementsToDirectory(@Nonnull Artifact artifact, @Nonnull String relativePath,
                                              @Nonnull Collection<? extends PackagingElement<?>> elements);

  public abstract void addElementsToDirectory(@Nonnull Artifact artifact, @Nonnull String relativePath,
                                              @Nonnull PackagingElement<?> element);

  public abstract ModificationTracker getModificationTracker();
}
