/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.testframework.sm.runner.history;

import consulo.execution.executor.Executor;
import consulo.execution.action.Location;
import consulo.execution.configuration.RunProfile;
import consulo.execution.ui.console.Filter;
import consulo.process.ProcessHandler;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.TestProxyFilterProvider;
import consulo.execution.ui.console.ConsoleView;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.project.Project;
import consulo.navigation.Navigatable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.io.File;

public class ImportedTestConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {
  private final @Nullable SMTRunnerConsoleProperties myProperties;
  private final File myFile;
  private final ProcessHandler myHandler;

  public ImportedTestConsoleProperties(@Nullable SMTRunnerConsoleProperties properties,
                                       File file,
                                       ProcessHandler handler,
                                       Project project, RunProfile runConfiguration,
                                       String frameworkName,
                                       Executor executor) {
    super(project, runConfiguration, frameworkName, executor);
    myProperties = properties;
    myFile = file;
    myHandler = handler;
  }

  @Override
  public OutputToGeneralTestEventsConverter createTestEventsConverter(@Nonnull String testFrameworkName,
                                                                      @Nonnull TestConsoleProperties consoleProperties) {
    return new ImportedToGeneralTestEventsConverter(testFrameworkName, consoleProperties, myFile, myHandler);
  }

  @Override
  public boolean isIdBasedTestTree() {
    return false;
  }

  @Override
  public boolean isPrintTestingStartedTime() {
    return false;
  }

  @javax.annotation.Nullable
  @Override
  public Navigatable getErrorNavigatable(@Nonnull Location<?> location,
                                         @Nonnull String stacktrace) {
    return myProperties == null ? null : myProperties.getErrorNavigatable(location, stacktrace);
  }

  @Nullable
  @Override
  public Navigatable getErrorNavigatable(@Nonnull Project project,
                                         @Nonnull String stacktrace) {
    return myProperties == null ? null : myProperties.getErrorNavigatable(project, stacktrace);
  }

  @Override
  public void addStackTraceFilter(Filter filter) {
    if (myProperties != null) {
      myProperties.addStackTraceFilter(filter);
    }
  }

  @Override
  public boolean fixEmptySuite() {
    return myProperties != null && myProperties.fixEmptySuite();
  }

  @Override
  @Nullable
  public SMTestLocator getTestLocator() {
    return myProperties == null ? null : myProperties.getTestLocator();
  }

  @Override
  @javax.annotation.Nullable
  public TestProxyFilterProvider getFilterProvider() {
    return myProperties == null ? null : myProperties.getFilterProvider();
  }

  @Override
  @Nullable
  public AbstractRerunFailedTestsAction createRerunFailedTestsAction(ConsoleView consoleView) {
    return myProperties == null ? null : myProperties.createRerunFailedTestsAction(consoleView);
  }

  @Override
  public void appendAdditionalActions(DefaultActionGroup actionGroup, JComponent parent, TestConsoleProperties target) {
    if (myProperties != null) {
      myProperties.appendAdditionalActions(actionGroup, parent, this);
    }
  }
}
