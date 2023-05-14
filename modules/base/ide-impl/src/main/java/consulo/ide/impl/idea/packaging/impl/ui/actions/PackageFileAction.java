/**
 * @author cdr
 */
package consulo.ide.impl.idea.packaging.impl.ui.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.compiler.CompilerBundle;
import consulo.compiler.CompilerManager;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.util.lang.Clock;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.wm.WindowManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.ide.impl.idea.packaging.impl.artifacts.ArtifactBySourceFileFinder;
import consulo.util.lang.SyncDateFormat;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PackageFileAction extends AnAction {
  private static final SyncDateFormat TIME_FORMAT = new SyncDateFormat(new SimpleDateFormat("h:mm:ss a"));

  public PackageFileAction() {
    super(CompilerBundle.message("action.name.package.file"), CompilerBundle.message("action.description.package.file"), null);
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    boolean visible = false;
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null) {
      final List<VirtualFile> files = getFilesToPackage(e, project);
      if (!files.isEmpty()) {
        visible = true;
        e.getPresentation().setText(files.size() == 1 ? CompilerBundle.message("action.name.package.file") : CompilerBundle.message("action.name.package.files"));
      }
    }

    e.getPresentation().setVisible(visible);
  }

  @Nonnull
  private static List<VirtualFile> getFilesToPackage(@Nonnull AnActionEvent e, @Nonnull Project project) {
    final VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    if (files == null) return Collections.emptyList();

    List<VirtualFile> result = new ArrayList<VirtualFile>();
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final CompilerManager compilerManager = CompilerManager.getInstance(project);
    for (VirtualFile file : files) {
      if (file == null || file.isDirectory() ||
          fileIndex.isInSourceContent(file) && compilerManager.isCompilableFileType(file.getFileType())) {
        return Collections.emptyList();
      }
      final Collection<? extends Artifact> artifacts = ArtifactBySourceFileFinder.getInstance(project).findArtifacts(file);
      for (Artifact artifact : artifacts) {
        if (!StringUtil.isEmpty(artifact.getOutputPath())) {
          result.add(file);
          break;
        }
      }
    }
    return result;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    FileDocumentManager.getInstance().saveAllDocuments();
    final List<VirtualFile> files = getFilesToPackage(event, project);
    Artifact[] allArtifacts = ArtifactManager.getInstance(project).getArtifacts();
    PackageFileWorker.startPackagingFiles(project, files, allArtifacts, new Runnable() {
      public void run() {
        setStatusText(project, files);
      }
    });
  }

  private static void setStatusText(Project project, List<VirtualFile> files) {
    if (!files.isEmpty()) {
      StringBuilder fileNames = new StringBuilder();
      for (VirtualFile file : files) {
        if (fileNames.length() != 0) fileNames.append(", ");
        fileNames.append("'").append(file.getName()).append("'");
      }
      String time = TIME_FORMAT.format(Clock.getTime());
      final String statusText = CompilerBundle.message("status.text.file.has.been.packaged", files.size(), fileNames, time);
      WindowManager.getInstance().getStatusBar(project).setInfo(statusText);
    }
  }
}
