/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import consulo.content.OrderRootType;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import consulo.content.library.LibraryType;
import consulo.content.library.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor;
import consulo.content.library.ui.LibraryEditor;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import consulo.util.lang.function.Condition;
import com.intellij.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.logging.Logger;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public class LibrariesContainerFactory {
  private static final Logger LOG = Logger.getInstance(LibrariesContainerFactory.class);
  private static final Library[] EMPTY_LIBRARIES_ARRAY = new Library[0];

  private LibrariesContainerFactory() {
  }

  @Nonnull
  public static LibrariesContainer createContainer(@Nullable Project project) {
    return new LibrariesContainerImpl(project, null, null);
  }

  @Nonnull
  public static LibrariesContainer createContainer(@Nonnull Module module) {
    return new LibrariesContainerImpl(module.getProject(), module, null);
  }

  @Nonnull
  public static LibrariesContainer createContainer(@Nonnull ModifiableRootModel rootModel) {
    Module module = rootModel.getModule();
    return new LibrariesContainerImpl(module.getProject(), module, rootModel);
  }

  public static LibrariesContainer createContainer(Project project, LibrariesConfigurator context) {
    return new StructureConfigurableLibrariesContainer(project, context);
  }

  public static Library createLibrary(@Nullable LibrariesContainer container1,
                                      @Nonnull LibrariesContainer container2,
                                      @Nonnull final NewLibraryEditor editor,
                                      @Nonnull final LibrariesContainer.LibraryLevel level) {
    if (container1 != null && container1.canCreateLibrary(level)) {
      return container1.createLibrary(editor, level);
    }
    else {
      return container2.createLibrary(editor, level);
    }
  }

  @Nonnull
  private static Library createLibraryInTable(final @Nonnull NewLibraryEditor editor, final LibraryTable table) {
    LibraryTableBase.ModifiableModelEx modifiableModel = (LibraryTableBase.ModifiableModelEx)table.getModifiableModel();
    final String name = StringUtil.isEmpty(editor.getName()) ? null : getUniqueLibraryName(editor.getName(), modifiableModel);
    final LibraryType<?> type = editor.getType();
    Library library = modifiableModel.createLibrary(name, type == null ? null : type.getKind());
    final LibraryEx.ModifiableModelEx model = (LibraryEx.ModifiableModelEx)library.getModifiableModel();
    editor.applyTo(model);
    model.commit();
    modifiableModel.commit();
    return library;
  }

  private static String getUniqueLibraryName(final String baseName, final LibraryTable.ModifiableModel model) {
    return UniqueNameGenerator.generateUniqueName(baseName, "", "", " (", ")", new Condition<String>() {
      @Override
      public boolean value(String s) {
        return model.getLibraryByName(s) == null;
      }
    });
  }

  private abstract static class LibrariesContainerBase implements LibrariesContainer {
    private UniqueNameGenerator myNameGenerator;

    @Override
    public Library createLibrary(@Nonnull @NonNls String name, @Nonnull LibraryLevel level, @Nonnull VirtualFile[] classRoots, @Nonnull VirtualFile[] sourceRoots) {
      NewLibraryEditor editor = new NewLibraryEditor();
      editor.setName(name);
      for (VirtualFile classRoot : classRoots) {
        editor.addRoot(classRoot, BinariesOrderRootType.getInstance());
      }
      for (VirtualFile sourceRoot : sourceRoots) {
        editor.addRoot(sourceRoot, SourcesOrderRootType.getInstance());
      }
      return createLibrary(editor, level);
    }

    @Override
    public Library createLibrary(@Nonnull @NonNls String name, @Nonnull LibraryLevel level, @Nonnull Collection<? extends OrderRoot> roots) {
      final NewLibraryEditor editor = new NewLibraryEditor();
      editor.setName(name);
      editor.addRoots(roots);
      return createLibrary(editor, level);
    }

    @Override
    @Nonnull
    public Library[] getAllLibraries() {
      Library[] libraries = getLibraries(LibraryLevel.GLOBAL);
      Library[] projectLibraries = getLibraries(LibraryLevel.PROJECT);
      if (projectLibraries.length > 0) {
        libraries = ArrayUtil.mergeArrays(libraries, projectLibraries);
      }
      Library[] moduleLibraries = getLibraries(LibraryLevel.MODULE);
      if (moduleLibraries.length > 0) {
        libraries = ArrayUtil.mergeArrays(libraries, moduleLibraries);
      }
      return libraries;
    }

    @Nonnull
    @Override
    public List<LibraryLevel> getAvailableLevels() {
      final List<LibraryLevel> levels = new ArrayList<LibraryLevel>();
      for (LibraryLevel level : LibraryLevel.values()) {
        if (canCreateLibrary(level)) {
          levels.add(level);
        }
      }
      return levels;
    }

    @Nonnull
    @Override
    public String suggestUniqueLibraryName(@Nonnull String baseName) {
      if (myNameGenerator == null) {
        myNameGenerator = new UniqueNameGenerator(Arrays.asList(getAllLibraries()), new Function<Library, String>() {
          @Override
          public String fun(Library o) {
            return o.getName();
          }
        });
      }
      return myNameGenerator.generateUniqueName(baseName, "", "", " (", ")");
    }
  }


  private static class LibrariesContainerImpl extends LibrariesContainerBase {
    private
    @Nullable
    final Project myProject;
    @Nullable
    private final Module myModule;
    @Nullable
    private final ModifiableRootModel myRootModel;

    private LibrariesContainerImpl(final @Nullable Project project, final @Nullable Module module, final @Nullable ModifiableRootModel rootModel) {
      myProject = project;
      myModule = module;
      myRootModel = rootModel;
    }

    @Override
    @Nullable
    public Project getProject() {
      return myProject;
    }

    @Override
    @Nonnull
    public Library[] getLibraries(@Nonnull final LibraryLevel libraryLevel) {
      if (libraryLevel == LibraryLevel.MODULE && myModule != null) {
        return getModuleLibraries();
      }

      LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
      if (libraryLevel == LibraryLevel.GLOBAL) {
        return registrar.getLibraryTable().getLibraries();
      }

      if (libraryLevel == LibraryLevel.PROJECT && myProject != null) {
        return registrar.getLibraryTable(myProject).getLibraries();
      }

      return EMPTY_LIBRARIES_ARRAY;
    }

    private Library[] getModuleLibraries() {
      if (myRootModel != null) {
        return myRootModel.getModuleLibraryTable().getLibraries();
      }
      OrderEntry[] orderEntries = ModuleRootManager.getInstance(myModule).getOrderEntries();
      List<Library> libraries = new ArrayList<Library>();
      for (OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof LibraryOrderEntry) {
          final LibraryOrderEntry entry = (LibraryOrderEntry)orderEntry;
          if (entry.isModuleLevel()) {
            libraries.add(entry.getLibrary());
          }
        }
      }
      return libraries.toArray(new Library[libraries.size()]);
    }

    @Override
    @Nonnull
    public VirtualFile[] getLibraryFiles(@Nonnull final Library library, @Nonnull final OrderRootType rootType) {
      return library.getFiles(rootType);
    }

    @Override
    public boolean canCreateLibrary(@Nonnull final LibraryLevel level) {
      if (level == LibraryLevel.MODULE) {
        return myRootModel != null;
      }
      return level == LibraryLevel.GLOBAL || myProject != null;
    }

    @Override
    public Library createLibrary(@Nonnull NewLibraryEditor libraryEditor, @Nonnull LibraryLevel level) {
      if (level == LibraryLevel.MODULE && myRootModel != null) {
        return createLibraryInTable(libraryEditor, myRootModel.getModuleLibraryTable());
      }

      LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
      LibraryTable table;
      if (level == LibraryLevel.GLOBAL) {
        table = registrar.getLibraryTable();
      }
      else if (level == LibraryLevel.PROJECT && myProject != null) {
        table = registrar.getLibraryTable(myProject);
      }
      else {
        return null;
      }
      return createLibraryInTable(libraryEditor, table);
    }

    @Override
    public ExistingLibraryEditor getLibraryEditor(@Nonnull Library library) {
      return null;
    }
  }

  private static class StructureConfigurableLibrariesContainer extends LibrariesContainerBase {
    private final Project myProject;
    private final LibrariesConfigurator myLibrariesConfigurator;

    public StructureConfigurableLibrariesContainer(Project project, LibrariesConfigurator librariesConfigurator) {
      myProject = project;
      myLibrariesConfigurator = librariesConfigurator;
    }

    @Override
    public Library createLibrary(@Nonnull NewLibraryEditor libraryEditor, @Nonnull LibraryLevel level) {
      LibraryTableModifiableModelProvider provider = getProvider(level);
      if (provider == null) {
        LOG.error("cannot create module library in this context");
      }

      LibraryTableBase.ModifiableModelEx model = (LibraryTableBase.ModifiableModelEx)provider.getModifiableModel();
      final LibraryType<?> type = libraryEditor.getType();
      Library library = model.createLibrary(getUniqueLibraryName(libraryEditor.getName(), model), type == null ? null : type.getKind());
      ExistingLibraryEditor createdLibraryEditor = ((LibrariesModifiableModel)model).getLibraryEditor(library);
      createdLibraryEditor.setProperties(libraryEditor.getProperties());
      libraryEditor.applyTo(createdLibraryEditor);
      return library;
    }

    @Override
    public ExistingLibraryEditor getLibraryEditor(@Nonnull Library library) {
      final LibraryTable table = library.getTable();
      if (table == null) return null;

      final LibraryTable.ModifiableModel model = myLibrariesConfigurator.getModifiableLibraryTable(table);
      if (model instanceof LibrariesModifiableModel) {
        return ((LibrariesModifiableModel)model).getLibraryEditor(library);
      }
      return null;
    }

    @Override
    @Nullable
    public Project getProject() {
      return myProject;
    }

    @Override
    @Nonnull
    public Library[] getLibraries(@Nonnull final LibraryLevel libraryLevel) {
      LibraryTableModifiableModelProvider provider = getProvider(libraryLevel);
      return provider != null ? provider.getModifiableModel().getLibraries() : EMPTY_LIBRARIES_ARRAY;
    }

    @Nullable
    private LibraryTableModifiableModelProvider getProvider(LibraryLevel libraryLevel) {
      if (libraryLevel == LibraryLevel.PROJECT) {
        return myLibrariesConfigurator.getProjectLibrariesProvider();
      }
      else {
        return null;
      }
    }

    @Override
    public boolean canCreateLibrary(@Nonnull final LibraryLevel level) {
      return level == LibraryLevel.GLOBAL || level == LibraryLevel.PROJECT;
    }

    @Override
    @Nonnull
    public VirtualFile[] getLibraryFiles(@Nonnull final Library library, @Nonnull final OrderRootType rootType) {
      LibrariesModifiableModel projectLibrariesModel = (LibrariesModifiableModel)myLibrariesConfigurator.getProjectLibrariesProvider().getModifiableModel();
      if (projectLibrariesModel.hasLibraryEditor(library)) {
        LibraryEditor libraryEditor = projectLibrariesModel.getLibraryEditor(library);
        return libraryEditor.getFiles(rootType);
      }
      return library.getFiles(rootType);
    }
  }
}
