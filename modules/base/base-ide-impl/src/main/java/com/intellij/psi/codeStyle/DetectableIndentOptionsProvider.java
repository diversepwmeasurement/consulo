// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.lang.LangBundle;
import com.intellij.lang.LanguageFormatting;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnSeparator;
import com.intellij.openapi.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiBinaryFile;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import static com.intellij.psi.codeStyle.DetectAndAdjustIndentOptionsTask.getDefaultIndentOptions;

/**
 * @author Rustam Vishnyakov
 */
public class DetectableIndentOptionsProvider extends FileIndentOptionsProvider {
  private static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("Automatic indent detection", NotificationDisplayType.STICKY_BALLOON, true);

  private boolean myIsEnabledInTest;
  private final Map<VirtualFile, IndentOptions> myDiscardedOptions = ContainerUtil.createWeakMap();

  @Nullable
  @Override
  public IndentOptions getIndentOptions(@Nonnull CodeStyleSettings settings, @Nonnull PsiFile file) {
    if (!isEnabled(settings, file)) {
      return null;
    }

    Project project = file.getProject();
    PsiDocumentManager psiManager = PsiDocumentManager.getInstance(project);
    Document document = psiManager.getDocument(file);
    if (document == null) {
      return null;
    }

    TimeStampedIndentOptions options;
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (document) {
      options = getValidCachedIndentOptions(file, document);

      if (options != null) {
        return options;
      }

      options = getDefaultIndentOptions(file, document);
      options.associateWithDocument(document);
    }

    scheduleDetectionInBackground(project, document, options);

    return options;
  }

  protected void scheduleDetectionInBackground(@Nonnull Project project, @Nonnull Document document, @Nonnull TimeStampedIndentOptions options) {
    new DetectAndAdjustIndentOptionsTask(project, document, options).scheduleInBackgroundForCommittedDocument();
  }

  @Override
  public boolean useOnFullReformat() {
    return false;
  }

  @TestOnly
  public void setEnabledInTest(boolean isEnabledInTest) {
    myIsEnabledInTest = isEnabledInTest;
  }

  private boolean isEnabled(@Nonnull CodeStyleSettings settings, @Nonnull PsiFile file) {
    if (!file.isValid() || !file.isWritable() || file instanceof PsiBinaryFile || file instanceof PsiCompiledFile || ScratchUtil.isScratch(file.getVirtualFile())) {
      return false;
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return myIsEnabledInTest;
    }
    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null || vFile instanceof LightVirtualFile || myDiscardedOptions.containsKey(vFile)) return false;
    return LanguageFormatting.INSTANCE.forContext(file) != null && settings.AUTODETECT_INDENTS;
  }

  @TestOnly
  @Nullable
  public static DetectableIndentOptionsProvider getInstance() {
    return FileIndentOptionsProvider.EP_NAME.findExtension(DetectableIndentOptionsProvider.class);
  }


  private void disableForFile(@Nonnull VirtualFile file, @Nonnull IndentOptions indentOptions) {
    myDiscardedOptions.put(file, indentOptions);
  }

  public TimeStampedIndentOptions getValidCachedIndentOptions(PsiFile file, Document document) {
    IndentOptions options = IndentOptions.retrieveFromAssociatedDocument(file);
    if (options instanceof TimeStampedIndentOptions) {
      final IndentOptions defaultIndentOptions = getDefaultIndentOptions(file, document);
      final TimeStampedIndentOptions cachedInDocument = (TimeStampedIndentOptions)options;
      if (!cachedInDocument.isOutdated(document, defaultIndentOptions)) {
        return cachedInDocument;
      }
    }
    return null;
  }

  private static void showDisabledDetectionNotification(@Nonnull Project project) {
    DetectionDisabledNotification notification = new DetectionDisabledNotification(project);
    notification.notify(project);
  }

  private static final class DetectionDisabledNotification extends Notification {
    private DetectionDisabledNotification(Project project) {
      super(NOTIFICATION_GROUP.getDisplayId(), ApplicationBundle.message("code.style.indent.detector.notification.content"), "", NotificationType.INFORMATION);
      addAction(new ReEnableDetection(project, this));
      addAction(new ShowIndentDetectionOptionAction(ApplicationBundle.message("code.style.indent.provider.notification.settings")));
    }
  }

  private static final class ShowIndentDetectionOptionAction extends DumbAwareAction {
    private ShowIndentDetectionOptionAction(@Nullable String text) {
      super(text);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), "preferences.sourceCode", "detect indent");
    }
  }

  private static final class ReEnableDetection extends DumbAwareAction {
    private final Project myProject;
    private final Notification myNotification;

    private ReEnableDetection(@Nonnull Project project, Notification notification) {
      super(ApplicationBundle.message("code.style.indent.provider.notification.re.enable"));
      myProject = project;
      myNotification = notification;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      CodeStyle.getSettings(myProject).AUTODETECT_INDENTS = true;
      notifyIndentOptionsChanged(myProject, null);
      myNotification.expire();
    }
  }

  private static boolean areDetected(@Nonnull IndentOptions indentOptions) {
    return indentOptions instanceof TimeStampedIndentOptions && ((TimeStampedIndentOptions)indentOptions).isDetected();
  }

  @Nullable
  @Override
  public IndentStatusBarUIContributor getIndentStatusBarUiContributor(@Nonnull IndentOptions indentOptions) {
    return new MyUIContributor(indentOptions);
  }


  private final class MyUIContributor extends IndentStatusBarUIContributor {
    private MyUIContributor(IndentOptions options) {
      super(options);
    }

    @Override
    public AnAction[] getActions(@Nonnull PsiFile file) {
      IndentOptions indentOptions = getIndentOptions();
      List<AnAction> actions = new ArrayList<>();
      final VirtualFile virtualFile = file.getVirtualFile();
      final Project project = file.getProject();
      final IndentOptions projectOptions = CodeStyle.getSettings(project).getIndentOptions(file.getFileType());
      final String projectOptionsTip = StringUtil.capitalizeWords(IndentStatusBarUIContributor.getIndentInfo(projectOptions), true);
      if (indentOptions instanceof TimeStampedIndentOptions) {
        if (((TimeStampedIndentOptions)indentOptions).isDetected()) {
          actions.add(DumbAwareAction.create(ApplicationBundle.message("code.style.indent.detector.reject", projectOptionsTip), e -> {
            disableForFile(virtualFile, indentOptions);
            notifyIndentOptionsChanged(project, file);
          }));
          actions.add(DumbAwareAction.create(ApplicationBundle.message("code.style.indent.detector.reindent", projectOptionsTip), e -> {
            disableForFile(virtualFile, indentOptions);
            notifyIndentOptionsChanged(project, file);
            CommandProcessor.getInstance()
                    .runUndoTransparentAction(() -> ApplicationManager.getApplication().runWriteAction(() -> CodeStyleManager.getInstance(project).adjustLineIndent(file, file.getTextRange())));
            myDiscardedOptions.remove(virtualFile);
          }));
          actions.add(AnSeparator.getInstance());
        }
      }
      else if (myDiscardedOptions.containsKey(virtualFile)) {
        final IndentOptions discardedOptions = myDiscardedOptions.get(virtualFile);
        final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document != null) {
          actions.add(DumbAwareAction
                              .create(ApplicationBundle.message("code.style.indent.detector.apply", IndentStatusBarUIContributor.getIndentInfo(discardedOptions), ColorUtil.toHex(JBColor.GRAY)), e -> {
                                myDiscardedOptions.remove(virtualFile);
                                discardedOptions.associateWithDocument(document);
                                notifyIndentOptionsChanged(project, file);
                              }));
          actions.add(AnSeparator.getInstance());
        }
      }
      return actions.toArray(AnAction.EMPTY_ARRAY);
    }

    @Override
    public
    @Nonnull
    AnAction createDisableAction(@Nonnull Project project) {
      return DumbAwareAction.create(ApplicationBundle.message("code.style.indent.detector.disable"), e -> {
        CodeStyle.getSettings(project).AUTODETECT_INDENTS = false;
        myDiscardedOptions.clear();
        notifyIndentOptionsChanged(project, null);
        showDisabledDetectionNotification(project);
      });
    }

    @Override
    public String getHint() {
      if (areDetected(getIndentOptions())) {
        return LangBundle.message("indent.option.detected");
      }
      return null;
    }

    @Override
    public boolean areActionsAvailable(@Nonnull VirtualFile file) {
      return areDetected(getIndentOptions()) || myDiscardedOptions.containsKey(file);
    }


  }
}
