/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.openapi.application.impl;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.project.Project;
import consulo.application.*;
import consulo.application.constraint.Expiration;
import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * from kotlin
 */
public class AppUIExecutorImpl extends BaseExpirableExecutorMixinImpl<AppUIExecutorImpl> implements AppUIExecutor {
  private static class MyEdtExecutor implements Executor {
    private final IdeaModalityState modality;

    private MyEdtExecutor(IdeaModalityState modalityState) {
      modality = modalityState;
    }

    @Override
    public void execute(@Nonnull Runnable command) {
      Application application = Application.get();
      if (application.isDispatchThread() && !IdeaModalityState.current().dominates(modality)) {
        command.run();
      }
      else {
        application.invokeLater(command, modality);
      }
    }
  }

  private static class MyWtExecutor implements Executor {
    private final IdeaModalityState modality;

    private MyWtExecutor(IdeaModalityState modalityState) {
      modality = modalityState;
    }

    @Override
    public void execute(@Nonnull Runnable command) {
      Application application = Application.get();
      if (application.isWriteThread() && !IdeaModalityState.current().dominates(modality)) {
        command.run();
      }
      else {
        application.invokeLaterOnWriteThread(command, modality);
      }
    }
  }

  private static Executor getExecutorForThread(ExecutionThread thread, IdeaModalityState modality) {
    switch (thread) {
      case EDT:
        return new MyEdtExecutor(modality);
      case WT:
        return new MyWtExecutor(modality);
      default:
        throw new UnsupportedOperationException(thread.name());
    }
  }

  private final IdeaModalityState modality;
  private final ExecutionThread thread;

  public AppUIExecutorImpl(IdeaModalityState modalityState, ExecutionThread thread) {
    super(new ContextConstraint[0], new BooleanSupplier[0], Collections.emptySet(), getExecutorForThread(thread, modalityState));
    modality = modalityState;
    this.thread = thread;
  }

  public AppUIExecutorImpl(IdeaModalityState modalityState, ExecutionThread thread, ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirableHandles) {
    super(constraints, cancellationConditions, expirableHandles, getExecutorForThread(thread, modalityState));
    this.thread = thread;
    this.modality = modalityState;
  }

  @Nonnull
  @Override
  protected AppUIExecutorImpl cloneWith(ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirationSet) {
    return new AppUIExecutorImpl(modality, thread, constraints, cancellationConditions, expirationSet);
  }

  @Nonnull
  @Override
  public AppUIExecutor later() {
    int edtEventCount = UIAccess.isUIThread() ? UIAccess.current().getEventCount() : -1;
    return withConstraint(new ContextConstraint() {
      private volatile boolean usedOnce;

      @Override
      public boolean isCorrectContext() {
        switch (thread) {
          case EDT:
            if (edtEventCount == -1) {
              return UIAccess.isUIThread();
            }
            return usedOnce || edtEventCount != UIAccess.current().getEventCount();
          case WT:
            return usedOnce;
          default:
            throw new UnsupportedOperationException();
        }
      }

      @Override
      public void schedule(Runnable runnable) {
        dispatchLaterUnconstrained(() -> {
          usedOnce = true;
          runnable.run();
        });
      }

      @Override
      public String toString() {
        return "later";
      }
    });
  }

  @Nonnull
  @Override
  public AppUIExecutor withDocumentsCommitted(@Nonnull ComponentManager project) {
    return withConstraint(new WithDocumentsCommitted((Project)project, modality), project);
  }

  @Nonnull
  @Override
  public AppUIExecutor inSmartMode(@Nonnull ComponentManager project) {
    return withConstraint(new InSmartMode((Project)project), project);
  }

  @Nonnull
  @Override
  public AppUIExecutor inTransaction(@Nonnull Disposable parentDisposable) {
    TransactionId id = TransactionGuard.getInstance().getContextTransaction();
    return withConstraint(new ContextConstraint() {
      @Override
      public boolean isCorrectContext() {
        return TransactionGuard.getInstance().getContextTransaction() != null;
      }

      @Override
      public void schedule(Runnable runnable) {
        // The Application instance is passed as a disposable here to ensure the runnable is always invoked,
        // regardless expiration state of the proper parentDisposable. In case the latter is disposed,
        // a continuation is resumed with a cancellation exception anyway (.expireWith() takes care of that).
        TransactionGuard.getInstance().submitTransaction(ApplicationManager.getApplication(), id, runnable);
      }

      @Override
      public String toString() {
        return "inTransaction";
      }
    }).expireWith(parentDisposable);
  }

  @Override
  public void dispatchLaterUnconstrained(Runnable runnable) {
    switch (thread) {
      case EDT:
        ApplicationManager.getApplication().invokeLater(runnable, modality);
        break;
      case WT:
        ApplicationManager.getApplication().invokeLaterOnWriteThread(runnable, modality);
        break;
    }
  }
}
