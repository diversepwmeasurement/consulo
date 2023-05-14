/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.task;

import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Abstraction layer for executing gradle tasks.
 *
 * @author Denis Zhdanov
 * @since 3/14/13 5:04 PM
 */
public interface ExternalSystemTaskManager<S extends ExternalSystemExecutionSettings> {

  void executeTasks(@Nonnull ExternalSystemTaskId id,
                    @Nonnull List<String> taskNames,
                    @Nonnull String projectPath,
                    @Nullable S settings,
                    @Nonnull final List<String> vmOptions,
                    @Nonnull List<String> scriptParameters,
                    @Nullable String debuggerSetup,
                    @Nonnull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException;

  boolean cancelTask(@Nonnull ExternalSystemTaskId id, @Nonnull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException;
}
