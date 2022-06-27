/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.application;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.impl.internal.progress.AbstractProgressIndicatorBase;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.util.lang.TimeoutUtil;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.application.impl.internal.performance.HeavyProcessLatch;
import consulo.application.Application;
import consulo.container.util.StatCollector;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promises;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author peter
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class Preloader implements Disposable {
  private static final Logger LOG = Logger.getInstance(Preloader.class);
  private final Executor myExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Preloader pool");
  private final ProgressIndicator myIndicator = new ProgressIndicatorBase();
  private final ProgressIndicator myWrappingIndicator = new AbstractProgressIndicatorBase() {
    @Override
    public void checkCanceled() {
      checkHeavyProcessRunning();
      myIndicator.checkCanceled();
    }

    @Override
    public boolean isCanceled() {
      return myIndicator.isCanceled();
    }
  };

  private static void checkHeavyProcessRunning() {
    if (HeavyProcessLatch.INSTANCE.isRunning()) {
      TimeoutUtil.sleep(1);
    }
  }

  @Inject
  public Preloader(@Nonnull Application application, @Nonnull ProgressManager progressManager) {
    if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
      return;
    }

    StatCollector collector = new StatCollector();

    List<AsyncPromise<Void>> result = new ArrayList<>();
    for (final PreloadingActivity activity : PreloadingActivity.EP_NAME.getExtensionList()) {
      AsyncPromise<Void> promise = new AsyncPromise<>();
      result.add(promise);

      myExecutor.execute(() -> {
        if (myIndicator.isCanceled()) return;

        checkHeavyProcessRunning();
        if (myIndicator.isCanceled()) return;

        progressManager.runProcess(() -> {
          Runnable mark = collector.mark(activity.getClass().getName());
          try {
            activity.preload(myWrappingIndicator);
          }
          catch (ProcessCanceledException ignore) {
          }
          catch (Throwable e) {
            LOG.error(e);
          }
          finally {
            mark.run();
            promise.setResult(null);
          }
          LOG.info("Finished preloading " + activity);
        }, myIndicator);
      });
    }

    Promises.all(result).onSuccess(o -> collector.dump("Preload statistics", LOG::info));
  }

  @Override
  public void dispose() {
    myIndicator.cancel();
  }
}
