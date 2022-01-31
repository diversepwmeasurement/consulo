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
package com.intellij.openapi.vcs.roots;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.util.lang.function.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import consulo.application.ui.awt.UIUtil;
import javax.annotation.Nonnull;

import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.pluralize;

public abstract class VcsIntegrationEnabler<VcsT extends AbstractVcs> {

  protected final @Nonnull
  Project myProject;
  protected final @Nonnull
  VcsT myVcs;


  protected VcsIntegrationEnabler(@Nonnull VcsT vcs) {
    myProject = vcs.getProject();
    myVcs = vcs;
  }

  public void enable(@Nonnull Collection<VcsRoot> vcsRoots) {
    Collection<VcsRoot> vcsFilterRoots = ContainerUtil.filter(vcsRoots, new Condition<VcsRoot>() {
      @Override
      public boolean value(VcsRoot root) {
        AbstractVcs vcs = root.getVcs();
        return vcs != null && vcs.getName().equals(myVcs.getName());
      }
    });
    Collection<VirtualFile> roots = VcsRootErrorsFinder.vcsRootsToVirtualFiles(vcsFilterRoots);
    VirtualFile projectDir = myProject.getBaseDir();
    assert projectDir != null : "Base dir is unexpectedly null for project: " + myProject;

    if (vcsFilterRoots.isEmpty()) {
      boolean succeeded = initOrNotifyError(projectDir);
      if (succeeded) {
        addVcsRoots(Collections.singleton(projectDir));
      }
    }
    else {
      assert !roots.isEmpty();
      if (roots.size() > 1 || isProjectBelowVcs(roots)) {
        notifyAddedRoots(roots);
      }
      addVcsRoots(roots);
    }
  }

  private boolean isProjectBelowVcs(@Nonnull Collection<VirtualFile> vcsRoots) {
    //check if there are vcs roots strictly above the project dir
    VirtualFile baseDir = myProject.getBaseDir();
    for (VirtualFile root : vcsRoots) {
      if (VfsUtilCore.isAncestor(root, baseDir, true)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public static String joinRootsPaths(@Nonnull Collection<VirtualFile> roots) {
    return StringUtil.join(roots, new Function<VirtualFile, String>() {
      @Override
      public String fun(VirtualFile virtualFile) {
        return virtualFile.getPresentableUrl();
      }
    }, ", ");
  }

  protected abstract boolean initOrNotifyError(@Nonnull final VirtualFile projectDir);

  protected void notifyAddedRoots(Collection<VirtualFile> roots) {
    VcsNotifier notifier = VcsNotifier.getInstance(myProject);
    notifier
            .notifySuccess("", String.format("Added %s %s: %s", myVcs.getName(), pluralize("root", roots.size()), joinRootsPaths(roots)));
  }

  private void addVcsRoots(@Nonnull Collection<VirtualFile> roots) {
    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
    List<VirtualFile> currentVcsRoots = Arrays.asList(vcsManager.getRootsUnderVcs(myVcs));

    List<VcsDirectoryMapping> mappings = new ArrayList<VcsDirectoryMapping>(vcsManager.getDirectoryMappings(myVcs));

    for (VirtualFile root : roots) {
      if (!currentVcsRoots.contains(root)) {
        mappings.add(new VcsDirectoryMapping(root.getPath(), myVcs.getName()));
      }
    }
    vcsManager.setDirectoryMappings(mappings);
  }

  protected static void refreshVcsDir(@Nonnull final VirtualFile projectDir, @Nonnull final String vcsDirName) {
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            LocalFileSystem.getInstance().refreshAndFindFileByPath(projectDir.getPath() + "/" + vcsDirName);
          }
        });
      }
    });
  }

}
