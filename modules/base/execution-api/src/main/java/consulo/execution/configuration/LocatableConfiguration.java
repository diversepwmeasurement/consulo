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
package consulo.execution.configuration;

import jakarta.annotation.Nullable;

/**
 * Base interface that should be used for configurations that can be created from context (a location in a file) by implementations of
 * {@link consulo.ide.impl.idea.execution.actions.RunConfigurationProducer}
 *
 * @author anna
 * @see LocatableConfigurationBase
 */
public interface LocatableConfiguration extends RunConfiguration {
  /**
   * Checks if the current name for the run configuration was automatically generated.
   *
   * @return true if the name was generated by {@link #suggestedName()}, false if it was manually entered or changed by the user.
   */
  boolean isGeneratedName();

  /**
   * Returns the default name for the run configuration based on its settings (such as the name of class or file to run).
   *
   * @return the suggested name for the configuration, or null if no name could be suggested based on the current settings (for example,
   * the name of the file to run isn't yet specified). NOTE: Please don't return hard-coded strings which are not based on the current
   * run configuration settings (such as "Unnamed").
   */
  @Nullable
  String suggestedName();
}
