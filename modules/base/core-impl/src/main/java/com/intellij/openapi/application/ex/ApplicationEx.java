/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.application.ex;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author max
 */
public interface ApplicationEx extends Application {
  String LOCATOR_FILE_NAME = ".home";

  /**
   * Loads the application configuration from the specified path
   *
   * @param optionsPath Path to /config folder
   * @throws IOException
   */
  void load(@Nullable String optionsPath) throws IOException;

  boolean isLoaded();

  /**
   * @return true if this thread is inside read action.
   * @see #runReadAction(Runnable)
   */
  boolean holdsReadLock();

  /**
   * @return true if the EDT is performing write action right now.
   * @see #runWriteAction(Runnable)
   */
  boolean isWriteActionInProgress();

  /**
   * @return true if the EDT started to acquire write action but has not acquired it yet.
   * @see #runWriteAction(Runnable)
   */
  boolean isWriteActionPending();

  void doNotSave();

  void doNotSave(boolean value);

  boolean isDoNotSave();

  /**
   * @param force         if true, no additional confirmations will be shown. The application is guaranteed to exit
   * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
   *                      a corresponding confirmation will be shown with the possibility to cancel the operation
   */
  void exit(boolean force, boolean exitConfirmed);

  /**
   * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
   *                      a corresponding confirmation will be shown with the possibility to cancel the operation
   */
  void restart(boolean exitConfirmed);

  /**
   * Runs modal process. For internal use only, see {@link Task}
   */
  @RequiredUIAccess
  default boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, Project project) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
  }

  /**
   * Runs modal process. For internal use only, see {@link Task}
   */
  @RequiredUIAccess
  default boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, @Nullable Project project, JComponent parentComponent) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, parentComponent, null);
  }

  /**
   * Runs modal process. For internal use only, see {@link Task}
   */
  @RequiredUIAccess
  boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                              @Nonnull String progressTitle,
                                              boolean canBeCanceled,
                                              @Nullable Project project,
                                              JComponent parentComponent,
                                              final String cancelText);

  @RequiredUIAccess
  void assertIsDispatchThread(@Nullable JComponent component);

  void assertTimeConsuming();

  /**
   * Grab the lock and run the action, in a non-blocking fashion
   *
   * @return true if action was run while holding the lock, false if was unable to get the lock and action was not run
   */
  boolean tryRunReadAction(@Nonnull Runnable action);

  boolean isInImpatientReader();

  default void executeByImpatientReader(@Nonnull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
    throw new UnsupportedOperationException();
  }

  default boolean runWriteActionWithCancellableProgressInDispatchThread(@Nonnull String title,
                                                                        @Nullable Project project,
                                                                        @Nullable JComponent parentComponent,
                                                                        @Nonnull Consumer<? super ProgressIndicator> action) {
    throw new UnsupportedOperationException();
  }

  default boolean runWriteActionWithNonCancellableProgressInDispatchThread(@Nonnull String title,
                                                                           @Nullable Project project,
                                                                           @Nullable JComponent parentComponent,
                                                                           @Nonnull Consumer<? super ProgressIndicator> action) {
    throw new UnsupportedOperationException();
  }
}
