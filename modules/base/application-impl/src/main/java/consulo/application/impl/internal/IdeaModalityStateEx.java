// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.collection.WeakList;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.*;

public final class IdeaModalityStateEx extends IdeaModalityState {
  private final WeakList<Object> myModalEntities = new WeakList<>();
  private static final Set<Object> ourTransparentEntities = Collections.newSetFromMap(Maps.newConcurrentWeakHashMap());

  @SuppressWarnings("unused")
  public IdeaModalityStateEx() {
  } // used by reflection to initialize NON_MODAL

  IdeaModalityStateEx(@Nonnull Collection<Object> modalEntities) {
    myModalEntities.addAll(modalEntities);
  }

  @Nonnull
  List<Object> getModalEntities() {
    return myModalEntities.toStrongList();
  }

  @Nonnull
  public IdeaModalityState appendProgress(@Nonnull ProgressIndicator progress) {
    return appendEntity(progress);
  }

  @Nonnull
  IdeaModalityStateEx appendEntity(@Nonnull Object anEntity) {
    List<Object> modalEntities = getModalEntities();
    List<Object> list = new ArrayList<>(modalEntities.size() + 1);
    list.addAll(modalEntities);
    list.add(anEntity);
    return new IdeaModalityStateEx(list);
  }

  void forceModalEntities(List<Object> entities) {
    myModalEntities.clear();
    myModalEntities.addAll(entities);
  }

  @Override
  public boolean dominates(@Nonnull IdeaModalityState anotherState) {
    if (anotherState == IdeaModalityState.any()) return false;
    if (myModalEntities.isEmpty()) return false;

    List<Object> otherEntities = ((IdeaModalityStateEx)anotherState).getModalEntities();
    for (Object entity : getModalEntities()) {
      if (!otherEntities.contains(entity) && !ourTransparentEntities.contains(entity)) return true; // I have entity which is absent in anotherState
    }
    return false;
  }

  @NonNls
  public String toString() {
    return this == NON_MODAL ? "ModalityState.NON_MODAL" : "ModalityState:{" + StringUtil.join(getModalEntities(), it -> "[" + it + "]", ", ") + "}";
  }

  void removeModality(Object modalEntity) {
    myModalEntities.remove(modalEntity);
  }

  void markTransparent() {
    Object element = ContainerUtil.getLastItem(getModalEntities(), null);
    if (element != null) {
      ourTransparentEntities.add(element);
    }
  }

  static void unmarkTransparent(@Nonnull Object modalEntity) {
    ourTransparentEntities.remove(modalEntity);
  }
}