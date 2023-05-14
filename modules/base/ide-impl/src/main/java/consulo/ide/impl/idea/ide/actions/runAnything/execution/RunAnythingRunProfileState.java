// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.execution;

import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.RunProfile;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.actions.runAnything.handlers.RunAnythingCommandHandler;
import consulo.process.*;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;

public class RunAnythingRunProfileState extends CommandLineState {
  public RunAnythingRunProfileState(@Nonnull ExecutionEnvironment environment, @Nonnull String originalCommand) {
    super(environment);

    RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
    if (handler != null) {
      setConsoleBuilder(handler.getConsoleBuilder(environment.getProject()));
    }
  }

  @Nonnull
  private RunAnythingRunProfile getRunProfile() {
    RunProfile runProfile = getEnvironment().getRunProfile();
    if (!(runProfile instanceof RunAnythingRunProfile)) {
      throw new IllegalStateException("Got " + runProfile + " instead of RunAnything profile");
    }
    return (RunAnythingRunProfile)runProfile;
  }

  @Nonnull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    RunAnythingRunProfile runProfile = getRunProfile();
    GeneralCommandLine commandLine = runProfile.getCommandLine();
    String originalCommand = runProfile.getOriginalCommand();
    RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);

    ProcessHandler processHandler =
      ProcessHandlerBuilder.create(commandLine)
                           .killable()
                           .shouldKillProcessSoftly(handler != null && handler.shouldKillProcessSoftly())
                           .colored()
                           .consoleType(ProcessConsoleType.EXTERNAL).build();


    processHandler.addProcessListener(new ProcessListener() {
      @Override
      public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
        int exitCode = event.getExitCode();
        print(IdeBundle.message("run.anything.console.process.finished", exitCode), ProcessOutputTypes.STDOUT);
        printCustomCommandOutput();
      }

      private void print(@Nonnull String message, @Nonnull Key consoleViewContentType) {
        processHandler.notifyTextAvailable(message, consoleViewContentType);
      }

      private void printCustomCommandOutput() {
        RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
        if (handler != null) {
          String customOutput = handler.getProcessTerminatedCustomOutput();
          if (customOutput != null) {
            print("\n", ProcessOutputTypes.STDOUT);
            print(customOutput, ProcessOutputTypes.STDOUT);
          }
        }
      }

    });
    return processHandler;
  }
}
