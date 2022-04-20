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

package com.intellij.refactoring.move.moveFilesOrDirectories;

import com.intellij.ide.util.DirectoryChooserUtil;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.RefactoringSettings;
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.Function;
import consulo.language.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MoveFilesOrDirectoriesUtil {
  private static final Logger LOG = Logger.getInstance(MoveFilesOrDirectoriesUtil.class);

  private MoveFilesOrDirectoriesUtil() {
  }

  /**
   * Moves the specified directory to the specified parent directory. Does not process non-code usages!
   *
   * @param aDirectory          the directory to move.
   * @param destDirectory the directory to move {@code dir} into.
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  public static void doMoveDirectory(final PsiDirectory aDirectory, final PsiDirectory destDirectory) throws IncorrectOperationException {
    PsiManager manager = aDirectory.getManager();
    // do actual move
    checkMove(aDirectory, destDirectory);

    try {
      aDirectory.getVirtualFile().move(manager, destDirectory.getVirtualFile());
    }
    catch (IOException e) {
      throw new IncorrectOperationException(e);
    }
    DumbService.getInstance(manager.getProject()).completeJustSubmittedTasks();
  }

  /**
   * Moves the specified file to the specified directory. Does not process non-code usages! file may be invalidated, need to be refreshed before use, like {@code newDirectory.findFile(file.getName())}
   *
   * @param file         the file to move.
   * @param newDirectory the directory to move the file into.
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  public static void doMoveFile(@Nonnull PsiFile file, @Nonnull PsiDirectory newDirectory) throws IncorrectOperationException {
    // the class is already there, this is true when multiple classes are defined in the same file
    if (!newDirectory.equals(file.getContainingDirectory())) {
      // do actual move
      checkMove(file, newDirectory);

      VirtualFile vFile = file.getVirtualFile();
      if (vFile == null) {
        throw new IncorrectOperationException("Non-physical file: " + file + " (" + file.getClass() + ")");
      }

      try {
        vFile.move(file.getManager(), newDirectory.getVirtualFile());
      }
      catch (IOException e) {
        throw new IncorrectOperationException(e);
      }
    }
  }

  /**
   * @param elements should contain PsiDirectories or PsiFiles only
   */
  public static void doMove(final Project project,
                            final PsiElement[] elements,
                            final PsiElement[] targetElement,
                            final MoveCallback moveCallback) {
    doMove(project, elements, targetElement, moveCallback, null);
  }

  /**
   * @param elements should contain PsiDirectories or PsiFiles only if adjustElements == null
   */
  public static void doMove(final Project project,
                            final PsiElement[] elements,
                            final PsiElement[] targetElement,
                            final MoveCallback moveCallback,
                            final Function<PsiElement[], PsiElement[]> adjustElements) {
    if (adjustElements == null) {
      for (PsiElement element : elements) {
        if (!(element instanceof PsiFile) && !(element instanceof PsiDirectory)) {
          throw new IllegalArgumentException("unexpected element type: " + element);
        }
      }
    }

    final PsiDirectory targetDirectory = resolveToDirectory(project, targetElement[0]);
    if (targetElement[0] != null && targetDirectory == null) return;

    final PsiElement[] newElements = adjustElements != null ? adjustElements.fun(elements) : elements;

    final PsiDirectory initialTargetDirectory = getInitialTargetDirectory(targetDirectory, elements);

    final MoveFilesOrDirectoriesDialog.Callback doRun = new MoveFilesOrDirectoriesDialog.Callback() {
      @Override
      public void run(final MoveFilesOrDirectoriesDialog moveDialog) {
        CommandProcessor.getInstance().executeCommand(project, () -> {
          final PsiDirectory targetDirectory1 = moveDialog != null ? moveDialog.getTargetDirectory() : initialTargetDirectory;
          if (targetDirectory1 == null) {
            LOG.error("It is null! The target directory, it is null!");
            return;
          }

          Collection<PsiElement> toCheck = ContainerUtil.newArrayList((PsiElement)targetDirectory1);
          for (PsiElement e : newElements) {
            toCheck.add(e instanceof PsiFileSystemItem && e.getParent() != null ? e.getParent() : e);
          }
          if (!CommonRefactoringUtil.checkReadOnlyStatus(project, toCheck, false)) {
            return;
          }

          targetElement[0] = targetDirectory1;

          try {
            final int[] choice = elements.length > 1 || elements[0] instanceof PsiDirectory ? new int[]{-1} : null;
            final List<PsiElement> els = new ArrayList<>();
            for (final PsiElement psiElement : newElements) {
              if (psiElement instanceof PsiFile) {
                final PsiFile file = (PsiFile)psiElement;
                if (CopyFilesOrDirectoriesHandler.checkFileExist(targetDirectory1, choice, file, file.getName(), "Move")) continue;
              }
              checkMove(psiElement, targetDirectory1);
              els.add(psiElement);
            }
            final Runnable callback = () -> {
              if (moveDialog != null) moveDialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            };
            if (els.isEmpty()) {
              callback.run();
              return;
            }
            new MoveFilesOrDirectoriesProcessor(project, els.toArray(new PsiElement[els.size()]), targetDirectory1,
                                                RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE,
                                                false, false, moveCallback, callback).run();
          }
          catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(),
                                                   "refactoring.moveFile", project);
          }
        }, MoveHandler.REFACTORING_NAME, null);
      }
    };

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      doRun.run(null);
    }
    else {
      final MoveFilesOrDirectoriesDialog moveDialog = new MoveFilesOrDirectoriesDialog(project, doRun);
      moveDialog.setData(newElements, initialTargetDirectory, "refactoring.moveFile");
      moveDialog.show();
    }
  }

  @Nullable
  public static PsiDirectory resolveToDirectory(final Project project, final PsiElement element) {
    if (!(element instanceof PsiDirectoryContainer)) {
      return (PsiDirectory)element;
    }

    PsiDirectory[] directories = ((PsiDirectoryContainer)element).getDirectories();
    switch (directories.length) {
      case 0:
        return null;
      case 1:
        return directories[0];
      default:
        return DirectoryChooserUtil.chooseDirectory(directories, directories[0], project, new HashMap<>());
    }

  }

  @Nullable
  private static PsiDirectory getCommonDirectory(PsiElement[] movedElements) {
    PsiDirectory commonDirectory = null;

    for (PsiElement movedElement : movedElements) {
      final PsiDirectory containingDirectory;
      if (movedElement instanceof PsiDirectory) {
        containingDirectory = ((PsiDirectory)movedElement).getParentDirectory();
      }
      else {
        final PsiFile containingFile = movedElement.getContainingFile();
        containingDirectory = containingFile == null ? null : containingFile.getContainingDirectory();
      }

      if (containingDirectory != null) {
        if (commonDirectory == null) {
          commonDirectory = containingDirectory;
        }
        else {
          if (commonDirectory != containingDirectory) {
            return null;
          }
        }
      }
    }
    return commonDirectory;
  }

  @Nullable
  public static PsiDirectory getInitialTargetDirectory(PsiDirectory initialTargetElement, final PsiElement[] movedElements) {
    PsiDirectory initialTargetDirectory = initialTargetElement;
    if (initialTargetDirectory == null) {
      if (movedElements != null) {
        final PsiDirectory commonDirectory = getCommonDirectory(movedElements);
        if (commonDirectory != null) {
          initialTargetDirectory = commonDirectory;
        }
        else {
          initialTargetDirectory = getContainerDirectory(movedElements[0]);
        }
      }
    }
    return initialTargetDirectory;
  }

  @Nullable
  private static PsiDirectory getContainerDirectory(final PsiElement psiElement) {
    if (psiElement instanceof PsiDirectory) {
      return (PsiDirectory)psiElement;
    }
    else if (psiElement != null) {
      return psiElement.getContainingFile().getContainingDirectory();
    }
    else {
      return null;
    }
  }

  /**
   * Checks if it is possible to move the specified PSI element under the specified container,
   * and throws an exception if the move is not possible. Does not actually modify anything.
   *
   * @param element      the element to check the move possibility.
   * @param newContainer the target container element to move into.
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  public static void checkMove(@Nonnull PsiElement element, @Nonnull PsiElement newContainer) throws IncorrectOperationException {
    if (element instanceof PsiDirectoryContainer) {
      PsiDirectory[] dirs = ((PsiDirectoryContainer)element).getDirectories();
      if (dirs.length == 0) {
        throw new IncorrectOperationException();
      }
      else if (dirs.length > 1) {
        throw new IncorrectOperationException(
                "Moving of packages represented by more than one physical directory is not supported.");
      }
      checkMove(dirs[0], newContainer);
      return;
    }

    //element.checkDelete(); //move != delete + add
    newContainer.checkAdd(element);
    checkIfMoveIntoSelf(element, newContainer);
  }

  public static void checkIfMoveIntoSelf(PsiElement element, PsiElement newContainer) throws IncorrectOperationException {
    PsiElement container = newContainer;
    while (container != null) {
      if (container == element) {
        if (element instanceof PsiDirectory) {
          if (element == newContainer) {
            throw new IncorrectOperationException("Cannot place directory into itself.");
          }
          else {
            throw new IncorrectOperationException("Cannot place directory into its subdirectory.");
          }
        }
        else {
          throw new IncorrectOperationException();
        }
      }
      container = container.getParent();
    }
  }
}
