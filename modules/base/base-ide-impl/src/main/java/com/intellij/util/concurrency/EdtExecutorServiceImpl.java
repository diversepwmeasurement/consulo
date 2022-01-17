// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.concurrency;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} implementation which
 * delegates tasks to the EDT for execution.
 */
class EdtExecutorServiceImpl extends EdtExecutorService {
  private EdtExecutorServiceImpl() {
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    Application application = ApplicationManager.getApplication();
    if (application == null) {
      SwingUtilities.invokeLater(command);
    }
    else {
      execute(command, (ModalityState)application.getAnyModalityState());
    }
  }

  @Override
  public void execute(@Nonnull Runnable command, @Nonnull ModalityState modalityState) {
    Application application = ApplicationManager.getApplication();
    if (application == null) {
      SwingUtilities.invokeLater(command);
    }
    else {
      application.invokeLater(command, modalityState);
    }
  }

  @Nonnull
  @Override
  public Future<?> submit(@Nonnull Runnable command, @Nonnull ModalityState modalityState) {
    RunnableFuture<?> future = newTaskFor(command, null);
    execute(future, modalityState);
    return future;
  }

  @Nonnull
  @Override
  public <T> Future<T> submit(@Nonnull Callable<T> task, @Nonnull ModalityState modalityState) {
    RunnableFuture<T> future = newTaskFor(task);
    execute(future, modalityState);
    return future;
  }

  @Override
  public void shutdown() {
    AppScheduledExecutorService.error();
  }

  @Nonnull
  @Override
  public List<Runnable> shutdownNow() {
    return AppScheduledExecutorService.error();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) {
    AppScheduledExecutorService.error();
    return false;
  }

  static final EdtExecutorService INSTANCE = new EdtExecutorServiceImpl();
}
