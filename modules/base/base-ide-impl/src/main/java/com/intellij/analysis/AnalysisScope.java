/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.analysis;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.lang.injection.InjectedLanguageManager;
import consulo.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import consulo.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.project.ProjectUtilCore;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import consulo.application.util.function.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import consulo.application.util.function.Processor;
import consulo.logging.Logger;
import org.intellij.lang.annotations.MagicConstant;
import javax.annotation.Nonnull;

import java.io.File;
import java.util.*;

/**
 * @author max
 */
public class AnalysisScope {
  private static final Logger LOG = Logger.getInstance(AnalysisScope.class);

  public static final int PROJECT = 1;
  public static final int DIRECTORY = 2;
  public static final int FILE = 3;
  public static final int MODULE = 4;
  public static final int INVALID = 6;
  public static final int MODULES = 7;
  public static final int CUSTOM = 8;
  public static final int VIRTUAL_FILES = 9;
  public static final int UNCOMMITTED_FILES = 10;

  @MagicConstant(intValues = {PROJECT, DIRECTORY, FILE, MODULE, INVALID, MODULES, CUSTOM, VIRTUAL_FILES, UNCOMMITTED_FILES})
  public @interface Type { }

  @Nonnull
  private final Project myProject;
  protected List<Module> myModules;
  protected Module myModule;
  protected PsiElement myElement;
  private SearchScope myScope;
  private boolean mySearchInLibraries;
  @Type protected int myType;

  private final Set<VirtualFile> myVFiles;  // initial files and directories the scope is configured on
  protected Set<VirtualFile> myFilesSet;    // set of files (not directories) this scope consists of. calculated in initFilesSet()

  protected boolean myIncludeTestSource = true;

  public AnalysisScope(@Nonnull Project project) {
    myProject = project;
    myElement = null;
    myModules = null;
    myModule = null;
    myScope = null;
    myType = PROJECT;
    myVFiles = null;
  }

  public AnalysisScope(@Nonnull Module module) {
    myProject = module.getProject();
    myElement = null;
    myModules = null;
    myScope = null;
    myModule = module;
    myType = MODULE;
    myVFiles = null;
  }

  public AnalysisScope(@Nonnull Module[] modules) {
    myModules = Arrays.asList(modules);
    myModule = null;
    myProject = modules[0].getProject();
    myElement = null;
    myScope = null;
    myType = MODULES;
    myVFiles = null;
  }

  public AnalysisScope(@Nonnull PsiDirectory psiDirectory) {
    myProject = psiDirectory.getProject();
    myModules = null;
    myModule = null;
    myScope = null;
    myElement = psiDirectory;
    myType = DIRECTORY;
    myVFiles = null;
  }

  public AnalysisScope(@Nonnull PsiFile psiFile) {
    myProject = psiFile.getProject();
    myElement = psiFile;
    myModule = null;
    myModules = null;
    myScope = null;
    myType = FILE;
    myVFiles = null;
  }

  public AnalysisScope(@Nonnull SearchScope scope, @Nonnull Project project) {
    myProject = project;
    myElement = null;
    myModule = null;
    myModules = null;
    myScope = scope;
    myType = CUSTOM;
    mySearchInLibraries = scope instanceof GlobalSearchScope && ((GlobalSearchScope)scope).isSearchInLibraries();
    myVFiles = null;
  }

  public AnalysisScope(@Nonnull Project project, @Nonnull Collection<VirtualFile> virtualFiles) {
    myProject = project;
    myElement = null;
    myModule = null;
    myModules = null;
    myScope = null;
    myVFiles = new HashSet<VirtualFile>(virtualFiles);
    myType = VIRTUAL_FILES;
  }

  public void setScope(SearchScope scope) {
    myScope = scope;
  }

  public void setSearchInLibraries(final boolean searchInLibraries) {
    mySearchInLibraries = searchInLibraries;
  }

  public void setIncludeTestSource(final boolean includeTestSource) {
    myIncludeTestSource = includeTestSource;
  }

  @Nonnull
  protected PsiElementVisitor createFileSearcher() {
    final FileIndex fileIndex = getFileIndex();
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText(AnalysisScopeBundle.message("scanning.scope.progress.title"));
    }

    return new PsiElementVisitor() {
      @Override
      public void visitFile(@Nonnull PsiFile file) {
        if (mySearchInLibraries || !(file instanceof PsiCompiledElement)) {
          final VirtualFile virtualFile = file.getVirtualFile();
          if (virtualFile == null) return;
          if (!myIncludeTestSource) {
            if (fileIndex.isInTestSourceContent(virtualFile)) {
              return;
            }
          }
          if (!shouldHighlightFile(file)) return;
          myFilesSet.add(virtualFile);
        }
      }
    };
  }

  @Nonnull
  private FileIndex getFileIndex() {
    final FileIndex fileIndex;
    if (myModule != null) {
      fileIndex = ModuleRootManager.getInstance(myModule).getFileIndex();
    }
    else {
      fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    }
    return fileIndex;
  }

  private static String displayProjectRelativePath(@Nonnull VirtualFile virtualFile, @Nonnull Project project) {
    return ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), project, false, false);
  }

  public boolean contains(@Nonnull PsiElement psiElement) {
    VirtualFile file = psiElement.getContainingFile().getVirtualFile();
    return file != null && contains(file);
  }

  public boolean contains(@Nonnull VirtualFile file) {
    if (myFilesSet == null) {
      if (myType == CUSTOM) {
        // optimization
        if (myScope instanceof GlobalSearchScope) return ((GlobalSearchScope)myScope).contains(file);
        if (myScope instanceof LocalSearchScope) return ((LocalSearchScope)myScope).isInScope(file);
      }
      if (myType == PROJECT) {  //optimization
        final ProjectFileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
        return index.isInContent(file) && (myIncludeTestSource || !index.isInTestSourceContent(file));
      }
      initFilesSet();
    }

    return myFilesSet.contains(file);
  }

  protected void initFilesSet() {
    if (myType == FILE) {
      myFilesSet = new HashSet<VirtualFile>(1);
      myFilesSet.add(((PsiFileSystemItem)myElement).getVirtualFile());
    }
    else if (myType == DIRECTORY || myType == PROJECT || myType == MODULES || myType == MODULE || myType == CUSTOM) {
      myFilesSet = new HashSet<VirtualFile>();
      accept(createFileSearcher(), false);
    }
    else if (myType == VIRTUAL_FILES) {
      myFilesSet = new HashSet<VirtualFile>();
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
      for (Iterator<VirtualFile> iterator = myVFiles.iterator(); iterator.hasNext(); ) {
        final VirtualFile vFile = iterator.next();
        VfsUtilCore.visitChildrenRecursively(vFile, new VirtualFileVisitor() {
          @Nonnull
          @Override
          public Result visitFileEx(@Nonnull VirtualFile file) {
            boolean ignored = fileIndex.isExcluded(file);
            if (!ignored && !file.isDirectory()) {
              myFilesSet.add(file);
            }
            return ignored ? SKIP_CHILDREN : CONTINUE;
          }
        });

        if (vFile.isDirectory()) {
          iterator.remove();
        }
      }
    }
  }


  public void accept(@Nonnull final PsiElementVisitor visitor) {
    accept(visitor, true);
  }

  private void accept(@Nonnull final PsiElementVisitor visitor, final boolean clearResolveCache) {
    final boolean needReadAction = !ApplicationManager.getApplication().isReadAccessAllowed();
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    final FileIndex fileIndex = getFileIndex();
    accept(new Processor<VirtualFile>() {
      @Override
      public boolean process(VirtualFile file) {
        if (file.isDirectory()) return true;
        if (ProjectCoreUtil.isProjectOrWorkspaceFile(file)) return true;
        if (fileIndex.isInContent(file) && (myIncludeTestSource || !fileIndex.isInTestSourceContent(file))
            && !isInGeneratedSources(file, myProject)) {
          return processFile(file, visitor, psiManager, needReadAction, clearResolveCache);
        }
        return true;
      }
    });
  }

  public boolean accept(@Nonnull final Processor<VirtualFile> processor) {
    if (myType == VIRTUAL_FILES) {
      if (myFilesSet == null) initFilesSet();
      final FileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
      for (final VirtualFile file : myFilesSet) {
        if (!myIncludeTestSource && index.isInTestSourceContent(file)) continue;
        if (!processor.process(file)) return false;
      }
      return true;
    }
    if (myScope instanceof GlobalSearchScope) {
      final FileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
      final ContentIterator contentIterator = new ContentIterator() {
        @Override
        public boolean processFile(@Nonnull final VirtualFile fileOrDir) {
          final boolean isInScope = ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
              if (!myIncludeTestSource && projectFileIndex.isInTestSourceContent(fileOrDir)) return false;
              if (isInGeneratedSources(fileOrDir, myProject)) return false;
              return ((GlobalSearchScope)myScope).contains(fileOrDir);
            }
          }).booleanValue();
          return !isInScope || processor.process(fileOrDir);
        }
      };
      if (!projectFileIndex.iterateContent(contentIterator)) return false;
      if (mySearchInLibraries) {
        final VirtualFile[] libraryRoots = LibraryUtil.getLibraryRoots(myProject, false, false);
        for (VirtualFile libraryRoot : libraryRoots) {
          if (!VfsUtilCore.iterateChildrenRecursively(libraryRoot, VirtualFileFilter.ALL, contentIterator)) return false;
        }
      }
      return true;
    }
    if (myScope instanceof LocalSearchScope) {
      final PsiElement[] psiElements = ((LocalSearchScope)myScope).getScope();
      final Set<VirtualFile> files = new HashSet<VirtualFile>();
      for (final PsiElement element : psiElements) {
        VirtualFile file = ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
          @Override
          public VirtualFile compute() {
            return PsiUtilCore.getVirtualFile(element);
          }
        });
        if (file != null && files.add(file)) {
          if (!processor.process(file)) return false;
        }
      }
      return true;
    }
    List<Module> modules = myModule != null ? Collections.<Module>singletonList(myModule) : myModules;
    if (modules != null) {
      for (final Module module : modules) {
        final FileIndex moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        if (!moduleFileIndex.iterateContent(new ContentIterator() {
          @Override
          public boolean processFile(@Nonnull VirtualFile fileOrDir) {
            return processor.process(fileOrDir);
          }
        })) return false;
      }
      return true;
    }

    if (myElement instanceof PsiDirectory) {
      return accept((PsiDirectory)myElement, processor);
    }
    if (myElement != null) {
      VirtualFile file = ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
        @Override
        public VirtualFile compute() {
          return PsiUtilCore.getVirtualFile(myElement);
        }
      });
      return file == null || processor.process(file);
    }
    final FileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    return projectFileIndex.iterateContent(new ContentIterator() {
      @Override
      public boolean processFile(@Nonnull final VirtualFile fileOrDir) {
        return processor.process(fileOrDir);
      }
    });
  }

  private static boolean isInGeneratedSources(@Nonnull VirtualFile file, @Nonnull Project project) {
    for (GeneratedSourcesFilter filter : GeneratedSourcesFilter.EP_NAME.getExtensionList()) {
      if (filter.isGeneratedSource(file, project)) {
        return true;
      }
    }
    return false;
  }

  private static boolean processFile(@Nonnull final VirtualFile vFile,
                                     @Nonnull final PsiElementVisitor visitor,
                                     @Nonnull final PsiManager psiManager,
                                     final boolean needReadAction,
                                     final boolean clearResolveCache) {
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        doProcessFile(visitor, psiManager, vFile, clearResolveCache);
      }
    };
    if (needReadAction && !ApplicationManager.getApplication().isDispatchThread()) {
      commitAndRunInSmartMode(runnable, psiManager.getProject());
    }
    else {
      runnable.run();
    }
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    return indicator == null || !indicator.isCanceled();
  }

  private static void commitAndRunInSmartMode(final Runnable runnable, final Project project) {
    while (true) {
      final DumbService dumbService = DumbService.getInstance(project);
      dumbService.waitForSmartMode();
      boolean passed = PsiDocumentManager.getInstance(project).commitAndRunReadAction(new Computable<Boolean>() {
        @Override
        public Boolean compute() {
          if (dumbService.isDumb()) return false;
          runnable.run();
          return true;
        }
      });
      if (passed) {
        break;
      }
    }
  }

  protected static boolean shouldHighlightFile(@Nonnull PsiFile file) {
    return ProblemHighlightFilter.shouldProcessFileInBatch(file);
  }

  public boolean containsModule(@Nonnull Module module) {
    switch (myType) {
      case PROJECT:
        return true;
      case MODULE:
        return myModule == module;
      case MODULES:
        return myModules.contains(module);
      default:
        return false;
    }
  }

  private static void doProcessFile(@Nonnull PsiElementVisitor visitor, @Nonnull PsiManager psiManager, @Nonnull VirtualFile vFile,
                                    boolean clearResolveCache) {
    if (!vFile.isValid()) return;

    PsiFile psiFile = psiManager.findFile(vFile);
    if (psiFile == null || !shouldHighlightFile(psiFile)) return;

    psiFile.accept(visitor);
    if (clearResolveCache) {
      psiManager.dropResolveCaches();
      InjectedLanguageManager.getInstance(psiManager.getProject()).dropFileCaches(psiFile);
    }
  }

  protected boolean accept(@Nonnull final PsiDirectory dir, @Nonnull final Processor<VirtualFile> processor) {
    final Project project = dir.getProject();
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    //we should analyze generated source files only if the action is explicitly invoked for a directory located under generated roots
    final boolean processGeneratedFiles = isInGeneratedSources(dir.getVirtualFile(), project);
    return VfsUtilCore.iterateChildrenRecursively(dir.getVirtualFile(), VirtualFileFilter.ALL, new ContentIterator() {
      @Override
      @SuppressWarnings({"SimplifiableIfStatement"})
      public boolean processFile(@Nonnull final VirtualFile fileOrDir) {
        if (!myIncludeTestSource && index.isInTestSourceContent(fileOrDir)) return true;
        if (!processGeneratedFiles && isInGeneratedSources(fileOrDir, project)) return true;
        if (!fileOrDir.isDirectory()) {
          return processor.process(fileOrDir);
        }
        return true;
      }
    });
  }

  public boolean isValid() {
    if (myModules != null){
      for (Module module : myModules) {
        if (module.isDisposed()) return false;
      }
      return true;
    }
    if (myModule != null) return !myModule.isDisposed();
    if (myElement != null) {
      return myElement.isValid();
    }
    return myType == VIRTUAL_FILES || myType == CUSTOM || myType == PROJECT;
  }

  @Type
  public int getScopeType() {
    return myType;
  }

  @Nonnull
  public String getDisplayName() {
    switch (myType) {
      case CUSTOM:
        return myScope.getDisplayName();

      case MODULE:
        return AnalysisScopeBundle.message("scope.option.module", pathToName(myModule.getModuleDirPath()));

      case MODULES:
        String modules = StringUtil.join(myModules, new Function<Module, String>() {
          @Override
          public String fun(@Nonnull final Module module) {
            return pathToName(module.getModuleDirPath());
          }
        }, ", ");

        return AnalysisScopeBundle.message("scope.module.list", modules, Integer.valueOf(myModules.size()));

      case PROJECT:
        return AnalysisScopeBundle.message("scope.project", myProject.getName());

      case FILE:
        return AnalysisScopeBundle.message("scope.file", getPresentableUrl((PsiFileSystemItem)myElement));

      case DIRECTORY:
        return AnalysisScopeBundle.message("scope.directory", getPresentableUrl((PsiFileSystemItem)myElement));

      case VIRTUAL_FILES:
        return AnalysisScopeBundle.message("scope.virtual.files");
    }

    return "";
  }

  @Nonnull
  private static String getPresentableUrl(@Nonnull final PsiFileSystemItem element) {
    final VirtualFile virtualFile = element.getVirtualFile();
    assert virtualFile != null : element;
    return virtualFile.getPresentableUrl();
  }

  @Nonnull
  public String getShortenName(){
    switch (myType) {
      case CUSTOM:
        return myScope.getDisplayName();

      case MODULE:
        return AnalysisScopeBundle.message("scope.option.module", myModule.getName());

      case MODULES:
        String modules = StringUtil.join(myModules, new Function<Module, String>() {
          @Override
          @Nonnull
          public String fun(@Nonnull final Module module) {
            return module.getName();
          }
        }, ", ");
        return AnalysisScopeBundle.message("scope.module.list", modules, Integer.valueOf(myModules.size()));

      case PROJECT:
        return AnalysisScopeBundle.message("scope.project", myProject.getName());

      case FILE:
        final String relativePath = getRelativePath();
        return relativePath != null ? AnalysisScopeBundle.message("scope.file", relativePath) : "Current File";

      case DIRECTORY:
        final String relativeDirPath = getRelativePath();
        return relativeDirPath != null ? AnalysisScopeBundle.message("scope.directory", relativeDirPath) : "Current Directory";


      case VIRTUAL_FILES:
        return AnalysisScopeBundle.message("scope.selected.files");
    }

    return "";
  }

  @javax.annotation.Nullable
  private String getRelativePath() {
    final String relativePath = displayProjectRelativePath(((PsiFileSystemItem)myElement).getVirtualFile(), myElement.getProject());
    if (relativePath.length() > 100) {
      return null;
    }
    return relativePath;
  }

  @Nonnull
  private static String pathToName(@Nonnull String path) {
    File file = new File(path);
    return FileUtil.getNameWithoutExtension(file);
  }

  public int getFileCount() {
    if (myFilesSet == null) initFilesSet();
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) { //clear text after building analysis scope set
      indicator.setText("");
      indicator.setText2("");
    }
    return myFilesSet.size();
  }

  /**
   * scope elements should be checked only when needed
   */
  @Deprecated
  public boolean checkScopeWritable(@Nonnull Project project) {
    if (myFilesSet == null) initFilesSet();
    return !FileModificationService.getInstance().prepareVirtualFilesForWrite(project, myFilesSet);
  }

  public void invalidate(){
    if (myType == VIRTUAL_FILES) {
      for (Iterator<VirtualFile> i = myVFiles.iterator(); i.hasNext();) {
        final VirtualFile virtualFile = i.next();
        if (virtualFile == null || !virtualFile.isValid()) {
          i.remove();
        }
      }
    }
    else {
      myFilesSet = null;
    }
  }

  public boolean containsSources(boolean isTest) {
    if (myElement != null) {
      final Project project = myElement.getProject();
      final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
      if (myElement instanceof PsiDirectory) {
        final VirtualFile directory = ((PsiFileSystemItem)myElement).getVirtualFile();
        if (index.isInSourceContent(directory)) {
          return isTest ? index.isInTestSourceContent(directory) : !index.isInTestSourceContent(directory);
        }
      } else if (myElement instanceof PsiFile) {
        final VirtualFile file = ((PsiFileSystemItem)myElement).getVirtualFile();
        if (file != null) {
          return isTest ? index.isInTestSourceContent(file) : !index.isInTestSourceContent(file);
        }
      }
    }
    return true;
  }

  @Nonnull
  public AnalysisScope getNarrowedComplementaryScope(@Nonnull Project defaultProject) {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(defaultProject).getFileIndex();
    final HashSet<Module> modules = new HashSet<Module>();
    if (myType == FILE || myType == DIRECTORY) {
      final VirtualFile vFile = ((PsiFileSystemItem)myElement).getVirtualFile();
      modules.addAll(getAllInterestingModules(fileIndex, vFile));
    }
    else if (myType == MODULE) {
      modules.add(myModule);
    }
    else if (myType == MODULES) {
      modules.addAll(myModules);
    }
    return collectScopes(defaultProject, modules);
  }

  @Nonnull
  protected static AnalysisScope collectScopes(@Nonnull final Project defaultProject, @Nonnull final HashSet<Module> modules) {
    if (modules.isEmpty()) {
      return new AnalysisScope(defaultProject);
    }
    final Module[] allModules = ModuleManager.getInstance(defaultProject).getModules();
    Set<Module> modulesToAnalyze = new HashSet<Module>();
    for (final Module module : modules) {
      modulesToAnalyze.addAll(getDirectBackwardDependencies(module, allModules));
      modulesToAnalyze.addAll(getExportBackwardDependencies(module, allModules));
      modulesToAnalyze.add(module);
    }
    return new AnalysisScope(modulesToAnalyze.toArray(new Module[modulesToAnalyze.size()]));
  }

  @Nonnull
  private static Set<Module> getExportBackwardDependencies(@Nonnull Module fromModule, @Nonnull Module[] allModules) {
    Set<Module> result = new HashSet<Module>();
    for (Module module : allModules) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
      for (OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof ModuleOrderEntry && ((ExportableOrderEntry)orderEntry).isExported() &&
            fromModule == ((ModuleOrderEntry)orderEntry).getModule()) {
          result.addAll(getDirectBackwardDependencies(module, allModules));
        }
      }
    }
    return result;
  }

  @Nonnull
  private static Set<Module> getDirectBackwardDependencies(@Nonnull Module module, @Nonnull Module[] allModules) {
    Set<Module> result = new HashSet<Module>();
    for (Module dependency : allModules) {
      if (ArrayUtil.find(ModuleRootManager.getInstance(dependency).getDependencies(), module) > -1) {
        result.add(dependency);
      }
    }
    return result;
  }

  @Nonnull
  protected static HashSet<Module> getAllInterestingModules(@Nonnull final ProjectFileIndex fileIndex, @Nonnull final VirtualFile vFile) {
    final HashSet<Module> modules = new HashSet<Module>();
    if (fileIndex.isInLibrarySource(vFile) || fileIndex.isInLibraryClasses(vFile)) {
      for (OrderEntry orderEntry : fileIndex.getOrderEntriesForFile(vFile)) {
        modules.add(orderEntry.getOwnerModule());
      }
    }
    else {
      modules.add(fileIndex.getModuleForFile(vFile));
    }
    return modules;
  }

  @Nonnull
  public SearchScope toSearchScope() {
    switch (myType) {
      case CUSTOM:
        return myScope;
      case DIRECTORY:
        return GlobalSearchScopesCore.directoryScope((PsiDirectory)myElement, true);
      case FILE:
        return new LocalSearchScope(myElement);
      case INVALID:
        return LocalSearchScope.EMPTY;
      case MODULE:
        GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(myModule);
        return myIncludeTestSource ? moduleScope : GlobalSearchScope.notScope(GlobalSearchScopesCore.projectTestScope(myModule.getProject())).intersectWith(moduleScope);
      case MODULES:
        SearchScope scope = GlobalSearchScope.EMPTY_SCOPE;
        for (Module module : myModules) {
          scope = scope.union(GlobalSearchScope.moduleScope(module));
        }
        return scope;
      case PROJECT:
        return myIncludeTestSource ? GlobalSearchScope.projectScope(myProject) : GlobalSearchScopesCore.projectProductionScope(myProject);
      case VIRTUAL_FILES:
        return new GlobalSearchScope() {
          @Override
          public boolean contains(@Nonnull VirtualFile file) {
            return myFilesSet.contains(file);
          }

          @Override
          public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
            return 0;
          }

          @Override
          public boolean isSearchInModuleContent(@Nonnull Module aModule) {
            return false;
          }

          @Override
          public boolean isSearchInLibraries() {
            return false;
          }
        };
      default:
        LOG.error("invalid type " + myType);
        return GlobalSearchScope.EMPTY_SCOPE;
    }
  }

  public boolean isAnalyzeTestsByDefault() {
    switch (myType) {
      case DIRECTORY:
        return ProjectRootManager.getInstance(myElement.getProject()).getFileIndex()
                .isInTestSourceContent(((PsiDirectory)myElement).getVirtualFile());
      case FILE:
        final PsiFile containingFile = myElement.getContainingFile();
        return ProjectRootManager.getInstance(myElement.getProject()).getFileIndex().isInTestSourceContent(containingFile.getVirtualFile());
      case MODULE:
        return isTestOnly(myModule);
      case MODULES:
        for (Module module : myModules) {
          if (!isTestOnly(module)) return false;
        }
        return true;

    }
    return false;
  }

  private static boolean isTestOnly(@Nonnull Module module) {
    return ModuleRootManager.getInstance(module).getSourceRootUrls(false).length == 0;
  }

  public boolean isIncludeTestSource() {
    return myIncludeTestSource;
  }
}
