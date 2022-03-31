// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.find.FindInProjectSearchEngine;
import consulo.find.FindModel;
import com.intellij.find.ngrams.TrigramIndex;
import consulo.ide.impl.psi.search.GlobalSearchScopeUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.UsageSearchContext;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.TrigramBuilder;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.psi.impl.cache.CacheManager;
import consulo.application.util.function.Processors;
import com.intellij.util.containers.ContainerUtil;
import consulo.language.psi.stub.DumbModeAccessType;
import consulo.language.psi.stub.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class IdeaIndexBasedFindInProjectSearchEngine implements FindInProjectSearchEngine {
  @Override
  public
  @Nullable
  FindInProjectSearcher createSearcher(@Nonnull FindModel findModel, @Nonnull Project project) {
    return new MyFindInProjectSearcher(project, findModel);
  }

  private static class MyFindInProjectSearcher implements FindInProjectSearcher {
    private
    @Nonnull
    final ProjectFileIndex myFileIndex;
    private
    @Nonnull
    final FileBasedIndexImpl myFileBasedIndex;
    private
    @Nonnull
    final Project myProject;
    private
    @Nonnull
    final FindModel myFindModel;

    private final boolean myHasTrigrams;
    private final String myStringToFindInIndices;

    MyFindInProjectSearcher(@Nonnull Project project, @Nonnull FindModel findModel) {
      myProject = project;
      myFindModel = findModel;
      myFileIndex = ProjectFileIndex.getInstance(myProject);
      myFileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
      String stringToFind = findModel.getStringToFind();

      if (findModel.isRegularExpressions()) {
        stringToFind = FindInProjectUtil.buildStringToFindForIndicesFromRegExp(stringToFind, project);
      }

      myStringToFindInIndices = stringToFind;

      myHasTrigrams = hasTrigrams(myStringToFindInIndices);
    }

    @Override
    public
    @Nonnull
    Collection<VirtualFile> searchForOccurrences() {
      String stringToFind = getStringToFindInIndexes(myFindModel, myProject);

      if (stringToFind.isEmpty() || (DumbService.getInstance(myProject).isDumb() && !FileBasedIndex.isIndexAccessDuringDumbModeEnabled())) {
        return Collections.emptySet();
      }


      final GlobalSearchScope scope = GlobalSearchScopeUtil.toGlobalSearchScope(FindInProjectUtil.getScopeFromModel(myProject, myFindModel), myProject);

      final Set<Integer> keys = new HashSet<>();
      TrigramBuilder.processTrigrams(stringToFind, new TrigramBuilder.TrigramProcessor() {
        @Override
        public boolean test(int value) {
          keys.add(value);
          return true;
        }
      });

      if (!keys.isEmpty()) {
        final List<VirtualFile> hits = new ArrayList<>();
        FileBasedIndex.getInstance().ignoreDumbMode(() -> {
          FileBasedIndex.getInstance().getFilesWithKey(TrigramIndex.INDEX_ID, keys, Processors.cancelableCollectProcessor(hits), scope);
        }, DumbModeAccessType.RAW_INDEX_DATA_ACCEPTABLE);

        return Collections.unmodifiableCollection(hits);
      }

      final Set<VirtualFile> resultFiles = new HashSet<>();

      PsiSearchHelper helper = PsiSearchHelper.getInstance(myProject);
      helper.processCandidateFilesForText(scope, UsageSearchContext.ANY, myFindModel.isCaseSensitive(), stringToFind, file -> {
        ContainerUtil.addIfNotNull(resultFiles, file);
        return true;
      });

      // in case our word splitting is incorrect
      CacheManager cacheManager = CacheManager.getInstance(myProject);
      VirtualFile[] filesWithWord = cacheManager.getVirtualFilesWithWord(stringToFind, UsageSearchContext.ANY, scope, myFindModel.isCaseSensitive());
      return Collections.unmodifiableCollection(Arrays.asList(filesWithWord));
    }

    @Override
    public boolean isReliable() {
      if (DumbService.isDumb(myProject)) return false;

      // a local scope may be over a non-indexed file
      if (myFindModel.getCustomScope() instanceof LocalSearchScope) return false;

      if (myHasTrigrams) return true;

      // $ is used to separate words when indexing plain-text files but not when indexing
      // Java identifiers, so we can't consistently break a string containing $ characters into words
      return myFindModel.isWholeWordsOnly() && myStringToFindInIndices.indexOf('$') < 0 && !StringUtil.getWordsIn(myStringToFindInIndices).isEmpty();
    }

    @Override
    public boolean isCovered(@Nonnull VirtualFile file) {
      return myHasTrigrams && isCoveredByIndex(file) && (myFileIndex.isInContent(file) || myFileIndex.isInLibrary(file));
    }

    private boolean isCoveredByIndex(@Nonnull VirtualFile file) {
      FileType fileType = file.getFileType();
      return TrigramIndex.isIndexable(fileType) && myFileBasedIndex.isIndexingCandidate(file, TrigramIndex.INDEX_ID);
    }

    private static boolean hasTrigrams(@Nonnull String text) {
      return !TrigramBuilder.processTrigrams(text, new TrigramBuilder.TrigramProcessor() {
        @Override
        public boolean test(int value) {
          return false;
        }
      });
    }

    @Nonnull
    private static String getStringToFindInIndexes(@Nonnull FindModel findModel, @Nonnull Project project) {
      String stringToFind = findModel.getStringToFind();

      if (findModel.isRegularExpressions()) {
        stringToFind = FindInProjectUtil.buildStringToFindForIndicesFromRegExp(stringToFind, project);
      }

      return stringToFind;
    }
  }
}
