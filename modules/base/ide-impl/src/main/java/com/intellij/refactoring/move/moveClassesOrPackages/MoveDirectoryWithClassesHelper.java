package com.intellij.refactoring.move.moveClassesOrPackages;

import consulo.component.extension.ExtensionPointName;
import consulo.component.extension.Extensions;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.document.util.ProperTextRange;
import consulo.language.psi.search.ReferencesSearch;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import consulo.usage.UsageInfo;
import com.intellij.util.Function;
import consulo.util.collection.MultiMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ksafonov
 */
public abstract class MoveDirectoryWithClassesHelper {
  private static final ExtensionPointName<MoveDirectoryWithClassesHelper> EP_NAME =
    ExtensionPointName.create("consulo.refactoring.moveDirectoryWithClassesHelper");

  public abstract void findUsages(Collection<PsiFile> filesToMove, PsiDirectory[] directoriesToMove, Collection<UsageInfo> result,
                                  boolean searchInComments, boolean searchInNonJavaFiles, Project project);

  public abstract boolean move(PsiFile file,
                                  PsiDirectory moveDestination,
                                  Map<PsiElement, PsiElement> oldToNewElementsMapping,
                                  List<PsiFile> movedFiles,
                                  RefactoringElementListener listener);

  public abstract void postProcessUsages(UsageInfo[] usages, Function<PsiDirectory, PsiDirectory> newDirMapper);

  public abstract void beforeMove(PsiFile psiFile);

  public abstract void afterMove(PsiElement newElement);

  public void preprocessUsages(Project project,
                               Set<PsiFile> files,
                               UsageInfo[] infos,
                               PsiDirectory directory,
                               MultiMap<PsiElement, String> conflicts) {}

  public static MoveDirectoryWithClassesHelper[] findAll() {
    return Extensions.getExtensions(EP_NAME);
  }


  public static class Default extends MoveDirectoryWithClassesHelper {

    @Override
    public void findUsages(Collection<PsiFile> filesToMove,
                           PsiDirectory[] directoriesToMove,
                           Collection<UsageInfo> result,
                           boolean searchInComments,
                           boolean searchInNonJavaFiles,
                           Project project) {
      for (PsiFile file : filesToMove) {
        for (PsiReference reference : ReferencesSearch.search(file)) {
          result.add(new MyUsageInfo(reference, file));
        }
      }
      for (PsiDirectory psiDirectory : directoriesToMove) {
        for (PsiReference reference : ReferencesSearch.search(psiDirectory)) {
          result.add(new MyUsageInfo(reference, psiDirectory));
        }
      }
    }

    @Override
    public void postProcessUsages(UsageInfo[] usages, Function<PsiDirectory, PsiDirectory> newDirMapper) {
      for (UsageInfo usage : usages) {
        if (usage instanceof MyUsageInfo) {
          PsiReference reference = usage.getReference();
          if (reference != null) {
            PsiFileSystemItem file = ((MyUsageInfo)usage).myFile;
            if (file instanceof PsiDirectory) {
              file = newDirMapper.fun((PsiDirectory)file);
            }
            reference.bindToElement(file);
          }
        }
      }
    }

    @Override
    public boolean move(PsiFile psiFile,
                           PsiDirectory moveDestination,
                           Map<PsiElement, PsiElement> oldToNewElementsMapping,
                           List<PsiFile> movedFiles,
                           RefactoringElementListener listener) {
      if (moveDestination.equals(psiFile.getContainingDirectory())) {
        return false;
      }

      MoveFileHandler.forElement(psiFile).prepareMovedFile(psiFile, moveDestination, oldToNewElementsMapping);

      PsiFile moving = moveDestination.findFile(psiFile.getName());
      if (moving == null) {
        MoveFilesOrDirectoriesUtil.doMoveFile(psiFile, moveDestination);
      }
      moving = moveDestination.findFile(psiFile.getName());
      movedFiles.add(moving);
      listener.elementMoved(psiFile);
      return true;
    }

    @Override
    public void beforeMove(PsiFile psiFile) {
    }

    @Override
    public void afterMove(PsiElement newElement) {
    }

    private static class MyUsageInfo extends UsageInfo {
      private final PsiFileSystemItem myFile;

      public MyUsageInfo(@Nonnull PsiReference reference, PsiFileSystemItem file) {
        super(reference);
        myFile = file;
      }

      @Override
      @Nullable
      public PsiReference getReference() {
        PsiElement element = getElement();
        if (element == null) {
          return null;
        }
        else {
          final ProperTextRange rangeInElement = getRangeInElement();
          return rangeInElement != null ? element.findReferenceAt(rangeInElement.getStartOffset()) : element.getReference();
        }
      }
    }
  }
}
