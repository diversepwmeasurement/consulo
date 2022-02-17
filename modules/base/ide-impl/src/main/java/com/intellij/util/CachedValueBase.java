// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.document.Document;
import consulo.project.Project;
import com.intellij.openapi.util.*;
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.util.CachedValueProfiler;
import consulo.language.psi.util.CachedValueProvider;
import com.intellij.reference.SoftReference;
import com.intellij.util.containers.NotNullList;
import consulo.application.util.function.Computable;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Reference;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
public abstract class CachedValueBase<T> {
  private static final Logger LOG = Logger.getInstance(CachedValueImpl.class);
  private final boolean myTrackValue;
  private volatile SoftReference<Data<T>> myData;

  protected CachedValueBase(boolean trackValue) {
    myTrackValue = trackValue;
  }

  @Nonnull
  private Data<T> computeData(Computable<? extends CachedValueProvider.Result<T>> doCompute) {
    CachedValueProvider.Result<T> result;
    CachedValueProfiler.ValueTracker tracker;
    if (CachedValueProfiler.isProfiling()) {
      try (CachedValueProfiler.Frame frame = CachedValueProfiler.newFrame()) {
        result = doCompute.compute();
        tracker = frame.newValueTracker(result);
      }
    }
    else {
      result = doCompute.compute();
      tracker = null;
    }
    if (result == null) {
      return new Data<>(null, ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_LONG_ARRAY, null);
    }
    T value = result.getValue();
    Object[] inferredDependencies = normalizeDependencies(result);
    long[] inferredTimeStamps = new long[inferredDependencies.length];
    for (int i = 0; i < inferredDependencies.length; i++) {
      inferredTimeStamps[i] = getTimeStamp(inferredDependencies[i]);
    }
    return new Data<>(value, inferredDependencies, inferredTimeStamps, tracker);
  }

  @Nullable
  private synchronized Data<T> cacheOrGetData(@Nullable Data<T> expected, @Nullable Data<T> updatedValue) {
    if (expected != getRawData()) return null;

    if (updatedValue != null) {
      setData(updatedValue);
      return updatedValue;
    }
    return expected;
  }

  private synchronized void setData(@Nullable Data<T> data) {
    myData = data == null ? null : new SoftReference<>(data);
  }

  @Nonnull
  protected Object[] normalizeDependencies(@Nonnull CachedValueProvider.Result<T> result) {
    Object[] items = result.getDependencyItems();
    T value = result.getValue();
    Object[] rawDependencies = myTrackValue && value != null ? ArrayUtil.append(items, value) : items;

    List<Object> flattened = new NotNullList<>(rawDependencies.length);
    collectDependencies(flattened, rawDependencies);
    return ArrayUtil.toObjectArray(flattened);
  }

  public void clear() {
    setData(null);
  }

  public boolean hasUpToDateValue() {
    return getUpToDateOrNull() != null;
  }

  @Nullable
  public final Data<T> getUpToDateOrNull() {
    Data<T> data = getRawData();
    return data != null && checkUpToDate(data) ? data : null;
  }

  private boolean checkUpToDate(@Nonnull Data<T> data) {
    if (isUpToDate(data)) {
      return true;
    }
    if (data.trackingInfo != null) {
      data.trackingInfo.onValueInvalidated();
    }
    return false;
  }

  @Nullable
  private Data<T> getRawData() {
    return SoftReference.dereference(myData);
  }

  protected boolean isUpToDate(@Nonnull Data<T> data) {
    for (int i = 0; i < data.myDependencies.length; i++) {
      Object dependency = data.myDependencies[i];
      if (isDependencyOutOfDate(dependency, data.myTimeStamps[i])) return false;
    }

    return true;
  }

  protected boolean isDependencyOutOfDate(@Nonnull Object dependency, long oldTimeStamp) {
    if (dependency instanceof CachedValueBase) {
      return !((CachedValueBase<?>)dependency).hasUpToDateValue();
    }
    final long timeStamp = getTimeStamp(dependency);
    return timeStamp < 0 || timeStamp != oldTimeStamp;
  }

  @Nonnull
  private static void collectDependencies(@Nonnull List<Object> resultingDeps, Object[] dependencies) {
    for (Object dependency : dependencies) {
      if (dependency == ObjectUtils.NULL) continue;
      if (dependency instanceof Object[]) {
        collectDependencies(resultingDeps, (Object[])dependency);
      }
      else {
        resultingDeps.add(dependency);
      }
    }
  }

  protected long getTimeStamp(@Nonnull Object dependency) {
    if (dependency instanceof VirtualFile) {
      return ((VirtualFile)dependency).getModificationStamp();
    }
    if (dependency instanceof ModificationTracker) {
      return ((ModificationTracker)dependency).getModificationCount();
    }
    else if (dependency instanceof Reference) {
      Object original = ((Reference<?>)dependency).get();
      if (original == null) return -1;
      return getTimeStamp(original);
    }
    else if (dependency instanceof SimpleReference) {
      Object original = ((SimpleReference<?>)dependency).get();
      if (original == null) return -1;
      return getTimeStamp(original);
    }
    else if (dependency instanceof Document) {
      return ((Document)dependency).getModificationStamp();
    }
    else if (dependency instanceof CachedValueBase) {
      // to check for up to date for a cached value dependency we use .isUpToDate() method, not the timestamp
      return 0;
    }
    else {
      LOG.error("Wrong dependency type: " + dependency.getClass());
      return -1;
    }
  }

  public T setValue(@Nonnull CachedValueProvider.Result<T> result) {
    Data<T> data = computeData(() -> result);
    setData(data);
    return data.getValue();
  }

  public abstract boolean isFromMyProject(@Nonnull Project project);

  public abstract Object getValueProvider();

  protected static class Data<T> implements Getter<T> {
    private final T myValue;
    @Nonnull
    private final Object[] myDependencies;
    @Nonnull
    private final long[] myTimeStamps;
    final
    @Nullable
    CachedValueProfiler.ValueTracker trackingInfo;

    Data(T value, @Nonnull Object[] dependencies, @Nonnull long[] timeStamps, @Nullable CachedValueProfiler.ValueTracker trackingInfo) {
      myValue = value;
      myDependencies = dependencies;
      myTimeStamps = timeStamps;
      this.trackingInfo = trackingInfo;
    }

    @Nonnull
    public Object[] getDependencies() {
      return myDependencies;
    }

    @Nonnull
    public long[] getTimeStamps() {
      return myTimeStamps;
    }

    @Override
    public final T get() {
      return getValue();
    }

    public T getValue() {
      if (trackingInfo != null) {
        trackingInfo.onValueUsed();
      }
      return myValue;
    }
  }

  @Nullable
  protected <P> T getValueWithLock(P param) {
    Data<T> data = getUpToDateOrNull();
    if (data != null) {
      if (IdempotenceChecker.areRandomChecksEnabled()) {
        IdempotenceChecker.applyForRandomCheck(data, getValueProvider(), () -> computeData(() -> doCompute(param)));
      }
      return data.getValue();
    }

    RecursionGuard.StackStamp stamp = RecursionManager.markStack();

    Supplier<Data<T>> calcData = () -> computeData(() -> doCompute(param));
    data = RecursionManager.doPreventingRecursion(this, true, calcData);
    if (data == null) {
      data = calcData.get();
    }
    else if (stamp.mayCacheNow()) {
      while (true) {
        Data<T> alreadyComputed = getRawData();
        boolean reuse = alreadyComputed != null && checkUpToDate(alreadyComputed);
        if (reuse) {
          IdempotenceChecker.checkEquivalence(alreadyComputed, data, getValueProvider().getClass(), calcData);
        }
        Data<T> toReturn = cacheOrGetData(alreadyComputed, reuse ? null : data);
        if (toReturn != null) {
          if (data != toReturn && data.trackingInfo != null) {
            data.trackingInfo.onValueRejected();
          }
          return toReturn.getValue();
        }
      }
    }
    return data.getValue();
  }

  protected abstract <P> CachedValueProvider.Result<T> doCompute(P param);

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + getValueProvider() + "}";
  }
}
