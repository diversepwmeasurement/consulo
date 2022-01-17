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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockContainerFactory;
import com.intellij.ui.docking.DockManager;
import com.intellij.ui.docking.DockableContent;
import consulo.disposer.Disposer;
import consulo.util.lang.ref.SimpleReference;
import org.jdom.Element;

public class DockableEditorContainerFactory implements DockContainerFactory.Persistent {

  public static final String TYPE = "file-editors";

  private Project myProject;
  private FileEditorManagerImpl myFileEditorManager;
  private DockManager myDockManager;

  public DockableEditorContainerFactory(Project project, FileEditorManagerImpl fileEditorManager, DockManager dockManager) {
    this.myProject = project;
    myFileEditorManager = fileEditorManager;
    myDockManager = dockManager;
  }

  @Override
  public DockContainer createContainer(DockableContent content) {
    return createContainer(false);
  }

  private DockContainer createContainer(boolean loadingState) {
    final SimpleReference<DesktopDockableEditorTabbedContainer> containerRef = SimpleReference.create();
    DesktopEditorsSplitters splitters = new DesktopEditorsSplitters(myProject, myFileEditorManager, myDockManager, false) {
      @Override
      public void afterFileClosed(VirtualFile file) {
        containerRef.get().fireContentClosed(file);
      }

      @Override
      public void afterFileOpen(VirtualFile file) {
        containerRef.get().fireContentOpen(file);
      }

      @Override
      protected IdeFrame getFrame(Project project) {
        return DockManager.getInstance(project).getIdeFrame(containerRef.get());
      }

      @Override
      public boolean isFloating() {
        return true;
      }
    };
    if (!loadingState) {
      splitters.createCurrentWindow();
    }
    final DesktopDockableEditorTabbedContainer container = new DesktopDockableEditorTabbedContainer(myProject, splitters, true);
    Disposer.register(container, splitters);
    containerRef.set(container);
    container.getSplitters().startListeningFocus();
    return container;
  }

  @Override
  public DockContainer loadContainerFrom(Element element) {
    DesktopDockableEditorTabbedContainer container = (DesktopDockableEditorTabbedContainer)createContainer(true);
    container.getSplitters().readExternal(element.getChild("state"));
    return container;
  }

  @Override
  public void dispose() {
  }
}
