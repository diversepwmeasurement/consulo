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

/*
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 13-Jul-2006
 * Time: 12:07:39
 */
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.ArrayUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.Rectangle2D;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ex.ToolWindowInternalDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.beans.PropertyChangeListener;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class ToolWindowHeadlessManagerImpl extends ToolWindowManagerEx {
  private final Map<String, ToolWindow> myToolWindows = new HashMap<>();
  private final Project myProject;

  public ToolWindowHeadlessManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public boolean canShowNotification(@NotNull String toolWindowId) {
    return false;
  }

  @Override
  public void notifyByBalloon(@NotNull final String toolWindowId, @NotNull final MessageType type, @NotNull final String htmlBody) {
  }

  private ToolWindow doRegisterToolWindow(final String id, @Nullable Disposable parentDisposable) {
    MockToolWindow tw = new MockToolWindow(myProject);
    myToolWindows.put(id, tw);
    if (parentDisposable != null)  {
      Disposer.register(parentDisposable, () -> unregisterToolWindow(id));
    }
    return tw;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, @NotNull JComponent component, @NotNull ToolWindowAnchor anchor) {
    return doRegisterToolWindow(id, null);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id,
                                       @NotNull JComponent component,
                                       @NotNull ToolWindowAnchor anchor,
                                       @NotNull Disposable parentDisposable,
                                       boolean canWorkInDumbMode,
                                       boolean canCloseContents) {
    return doRegisterToolWindow(id, parentDisposable);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id, final boolean canCloseContent, @NotNull final ToolWindowAnchor anchor) {
    return doRegisterToolWindow(id, null);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id,
                                       final boolean canCloseContent,
                                       @NotNull final ToolWindowAnchor anchor,
                                       final boolean secondary) {
    return doRegisterToolWindow(id, null);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id, final boolean canCloseContent, @NotNull final ToolWindowAnchor anchor,
                                       @NotNull final Disposable parentDisposable, final boolean dumbAware) {
    return doRegisterToolWindow(id, parentDisposable);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id,
                                       boolean canCloseContent,
                                       @NotNull ToolWindowAnchor anchor,
                                       @NotNull Disposable parentDisposable,
                                       boolean canWorkInDumbMode,
                                       boolean secondary) {
    return doRegisterToolWindow(id, parentDisposable);
  }

  @RequiredUIAccess
  @Override
  public void unregisterToolWindow(@NotNull String id) {
    myToolWindows.remove(id);
  }

  @Override
  public void activateEditorComponent() {
  }

  @Override
  public boolean isEditorComponentActive() {
    return false;
  }

  @NotNull
  @Override
  public String[] getToolWindowIds() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @RequiredUIAccess
  @Override
  public String getActiveToolWindowId() {
    return null;
  }

  @Override
  public ToolWindow getToolWindow(String id) {
    return myToolWindows.get(id);
  }

  @Override
  public void invokeLater(@NotNull Runnable runnable) {
  }

  @NotNull
  @Override
  public IdeFocusManager getFocusManager() {
    return IdeFocusManagerHeadless.INSTANCE;
  }

  @Override
  public void notifyByBalloon(@NotNull final String toolWindowId,
                              @NotNull final MessageType type,
                              @NotNull final String text,
                              @Nullable final Icon icon,
                              @Nullable final HyperlinkListener listener) {
  }

  @Override
  public Balloon getToolWindowBalloon(String id) {
    return null;
  }

  @Override
  public boolean isMaximized(@NotNull ToolWindow wnd) {
    return false;
  }

  @Override
  public void setMaximized(@NotNull ToolWindow wnd, boolean maximized) {
  }

  @RequiredUIAccess
  @Override
  public void initToolWindow(@NotNull ToolWindowEP bean) {

  }

  @Override
  public void addToolWindowManagerListener(@NotNull ToolWindowManagerListener listener) {

  }

  @Override
  public void addToolWindowManagerListener(@NotNull ToolWindowManagerListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removeToolWindowManagerListener(@NotNull ToolWindowManagerListener listener) {
  }

  @NotNull
  @RequiredUIAccess
  @Override
  public ToolWindowLayout getLayout() {
    return new ToolWindowLayout();
  }

  @Override
  public void setLayoutToRestoreLater(ToolWindowLayout layout) {
  }

  @Override
  public ToolWindowLayout getLayoutToRestoreLater() {
    return new ToolWindowLayout();
  }

  @RequiredUIAccess
  @Override
  public void setLayout(@NotNull ToolWindowLayout layout) {
  }

  @Override
  public void clearSideStack() {
  }

  @RequiredUIAccess
  @Override
  public void hideToolWindow(@NotNull final String id, final boolean hideSide) {
  }

  @NotNull
  @Override
  public List<String> getIdsOn(@NotNull final ToolWindowAnchor anchor) {
    return new ArrayList<>();
  }

  public static class MockToolWindow implements ToolWindowEx {
    ContentManager myContentManager = new MockContentManager();

    public MockToolWindow(@NotNull Project project) {
      Disposer.register(project, myContentManager);
    }

    @NotNull
    @Override
    public String getId() {
      return null;
    }

    @RequiredUIAccess
    @Override
    public boolean isActive() {
      return false;
    }

    @RequiredUIAccess
    @Override
    public void activate(@Nullable Runnable runnable) {
    }

    @Override
    public boolean isDisposed() {
      return false;
    }

    @Override
    public boolean isVisible() {
      return false;
    }

    @Override
    public void setShowStripeButton(boolean show) {
    }

    @Override
    public boolean isShowStripeButton() {
      return false;
    }

    @NotNull
    @Override
    public ActionCallback getReady(@NotNull Object requestor) {
      return ActionCallback.DONE;
    }

    @RequiredUIAccess
    @Override
    public void show(@Nullable Runnable runnable) {
    }

    @RequiredUIAccess
    @Override
    public void hide(@Nullable Runnable runnable) {
    }

    @Override
    public ToolWindowAnchor getAnchor() {
      return ToolWindowAnchor.BOTTOM;
    }

    @RequiredUIAccess
    @Override
    public void setAnchor(@NotNull ToolWindowAnchor anchor, @Nullable Runnable runnable) {
    }

    @RequiredUIAccess
    @Override
    public boolean isSplitMode() {
      return false;
    }

    @RequiredUIAccess
    @Override
    public void setSplitMode(final boolean isSideTool, @Nullable final Runnable runnable) {

    }

    @RequiredUIAccess
    @Override
    public boolean isAutoHide() {
      return false;
    }

    @RequiredUIAccess
    @Override
    public void setAutoHide(boolean state) {
    }

    @Override
    public void setToHideOnEmptyContent(final boolean hideOnEmpty) {
    }

    @Override
    public boolean isToHideOnEmptyContent() {
      return false;
    }

    @Override
    public ToolWindowType getType() {
      return ToolWindowType.SLIDING;
    }

    @RequiredUIAccess
    @Override
    public void setType(@NotNull ToolWindowType type, @Nullable Runnable runnable) {
    }

    @RequiredUIAccess
    @Override
    public Icon getIcon() {
      return null;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(Icon icon) {
    }

    @RequiredUIAccess
    @Override
    public String getTitle() {
      return "";
    }

    @RequiredUIAccess
    @Override
    public void setTitle(String title) {
    }

    @RequiredUIAccess
    @NotNull
    @Override
    public String getStripeTitle() {
      return "";
    }

    @RequiredUIAccess
    @Override
    public void setStripeTitle(@NotNull String title) {
    }

    @Override
    public boolean isAvailable() {
      return false;
    }

    @RequiredUIAccess
    @Override
    public void setContentUiType(@NotNull ToolWindowContentUiType type, @Nullable Runnable runnable) {
    }

    @Override
    public void setDefaultContentUiType(@NotNull ToolWindowContentUiType type) {
    }

    @RequiredUIAccess
    @NotNull
    @Override
    public ToolWindowContentUiType getContentUiType() {
      return ToolWindowContentUiType.TABBED;
    }

    @RequiredUIAccess
    @Override
    public void setAvailable(boolean available, @Nullable Runnable runnable) {
    }

    @Override
    public void installWatcher(@NotNull ContentManager contentManager) {
    }

    @Override
    public ContentManager getContentManager() {
      return myContentManager;
    }

    @Override
    public void setDefaultState(@Nullable ToolWindowAnchor anchor, @Nullable ToolWindowType type, @Nullable Rectangle2D floatingBounds) {

    }

    @RequiredUIAccess
    @Override
    public void activate(@Nullable final Runnable runnable, final boolean autoFocusContents) {
    }

    @RequiredUIAccess
    @Override
    public void activate(@Nullable Runnable runnable, boolean autoFocusContents, boolean forced) {
    }

    @Override
    public ActionCallback getActivation() {
      return ActionCallback.DONE;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

    @RequiredUIAccess
    @Override
    public ToolWindowType getInternalType() {
      return ToolWindowType.DOCKED;
    }

    @Override
    public void stretchWidth(int value) {
    }

    @Override
    public void stretchHeight(int value) {
    }

    @Override
    public ToolWindowInternalDecorator getDecorator() {
      return null;
    }

    @Override
    public void setAdditionalGearActions(ActionGroup additionalGearActions) {
    }

    @Override
    public void setTitleActions(AnAction... actions) {
    }

    @Override
    public void setUseLastFocusedOnActivation(boolean focus) {
    }

    @Override
    public boolean isUseLastFocusedOnActivation() {
      return false;
    }
  }

  private static class MockContentManager implements ContentManager {
    private final EventDispatcher<ContentManagerListener> myDispatcher = EventDispatcher.create(ContentManagerListener.class);
    private final List<Content> myContents = new ArrayList<>();
    private Content mySelected;

    @NotNull
    @Override
    public ActionCallback getReady(@NotNull Object requestor) {
      return ActionCallback.DONE;
    }

    @Override
    public void addContent(@NotNull final Content content) {
      myContents.add(content);
      Disposer.register(this, content);
      ContentManagerEvent e = new ContentManagerEvent(this, content, myContents.indexOf(content), ContentManagerEvent.ContentOperation.add);
      myDispatcher.getMulticaster().contentAdded(e);
      if (mySelected == null) setSelectedContent(content);
    }

    @Override
    public void addContent(@NotNull Content content, int order) {
      myContents.add(order, content);
      Disposer.register(this, content);
      ContentManagerEvent e = new ContentManagerEvent(this, content, myContents.indexOf(content), ContentManagerEvent.ContentOperation.add);
      myDispatcher.getMulticaster().contentAdded(e);
      if (mySelected == null) setSelectedContent(content);
    }

    @Override
    public void addContent(@NotNull final Content content, final Object constraints) {
      addContent(content);
    }

    @Override
    public void addSelectedContent(@NotNull final Content content) {
      addContent(content);
      setSelectedContent(content);
    }

    @Override
    public void addContentManagerListener(@NotNull final ContentManagerListener l) {
      myDispatcher.getListeners().add(0, l);
    }

    @Override
    public void addDataProvider(@NotNull final DataProvider provider) {
    }

    @Override
    public boolean canCloseAllContents() {
      return false;
    }

    @Override
    public boolean canCloseContents() {
      return false;
    }

    @Override
    public Content findContent(final String displayName) {
      for (Content each : myContents) {
        if (each.getDisplayName().equals(displayName)) return each;
      }
      return null;
    }

    @Override
    public List<AnAction> getAdditionalPopupActions(@NotNull final Content content) {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public String getCloseActionName() {
      return "close";
    }

    @NotNull
    @Override
    public String getCloseAllButThisActionName() {
      return "closeallbutthis";
    }

    @NotNull
    @Override
    public String getPreviousContentActionName() {
      return "previous";
    }

    @NotNull
    @Override
    public String getNextContentActionName() {
      return "next";
    }

    @NotNull
    @Override
    public JComponent getComponent() {
      return new JLabel();
    }

    @Override
    public Content getContent(final JComponent component) {
      Content[] contents = getContents();
      for (Content content : contents) {
        if (Comparing.equal(component, content.getComponent())) {
          return content;
        }
      }
      return null;
    }

    @Override
    @Nullable
    public Content getContent(final int index) {
      return myContents.get(index);
    }

    @Override
    public int getContentCount() {
      return myContents.size();
    }

    @Override
    @NotNull
    public Content[] getContents() {
      return myContents.toArray(new Content[myContents.size()]);
    }

    @Override
    public int getIndexOfContent(final Content content) {
      return myContents.indexOf(content);
    }

    @Override
    @Nullable
    public Content getSelectedContent() {
      return mySelected;
    }

    @Override
    @NotNull
    public Content[] getSelectedContents() {
      return mySelected != null ? new Content[]{mySelected} : new Content[0];
    }

    @Override
    public boolean isSelected(@NotNull final Content content) {
      return content == mySelected;
    }

    @Override
    public void removeAllContents(final boolean dispose) {
      for (Content content : getContents()) {
        removeContent(content, dispose);
      }
    }

    @Override
    public boolean removeContent(@NotNull final Content content, final boolean dispose) {
      boolean wasSelected = mySelected == content;
      int oldIndex = myContents.indexOf(content);
      if (wasSelected) {
        removeFromSelection(content);
      }
      boolean result = myContents.remove(content);
      if (dispose) Disposer.dispose(content);
      ContentManagerEvent e = new ContentManagerEvent(this, content, oldIndex, ContentManagerEvent.ContentOperation.remove);
      myDispatcher.getMulticaster().contentRemoved(e);
      Content item = ContainerUtil.getFirstItem(myContents);
      if (item != null) setSelectedContent(item);
      return result;
    }

    @NotNull
    @Override
    public ActionCallback removeContent(@NotNull Content content, boolean dispose, boolean trackFocus, boolean implicitFocus) {
      removeContent(content, dispose);
      return ActionCallback.DONE;
    }

    @Override
    public void removeContentManagerListener(@NotNull final ContentManagerListener l) {
      myDispatcher.removeListener(l);
    }

    @Override
    public void removeFromSelection(@NotNull final Content content) {
      ContentManagerEvent e = new ContentManagerEvent(this, content, myContents.indexOf(mySelected), ContentManagerEvent.ContentOperation.remove);
      myDispatcher.getMulticaster().selectionChanged(e);
    }

    @Override
    public ActionCallback selectNextContent() {
      return ActionCallback.DONE;
    }

    @Override
    public ActionCallback selectPreviousContent() {
      return ActionCallback.DONE;
    }

    @Override
    public void setSelectedContent(@NotNull final Content content) {
      if (mySelected != null) {
        removeFromSelection(mySelected);
      }
      mySelected = content;
      ContentManagerEvent e = new ContentManagerEvent(this, content, myContents.indexOf(content), ContentManagerEvent.ContentOperation.add);
      myDispatcher.getMulticaster().selectionChanged(e);
    }

    @NotNull
    @Override
    public ActionCallback setSelectedContentCB(@NotNull Content content) {
      setSelectedContent(content);
      return ActionCallback.DONE;
    }

    @Override
    public void setSelectedContent(@NotNull final Content content, final boolean requestFocus) {
      setSelectedContent(content);
    }

    @NotNull
    @Override
    public ActionCallback setSelectedContentCB(@NotNull final Content content, final boolean requestFocus) {
      return setSelectedContentCB(content);
    }

    @Override
    public void setSelectedContent(@NotNull Content content, boolean requestFocus, boolean forcedFocus) {
      setSelectedContent(content);
    }

    @NotNull
    @Override
    public ActionCallback setSelectedContentCB(@NotNull final Content content, final boolean requestFocus, final boolean forcedFocus) {
      return setSelectedContentCB(content);
    }

    @NotNull
    @Override
    public ActionCallback setSelectedContent(@NotNull Content content, boolean requestFocus, boolean forcedFocus, boolean implicit) {
      return setSelectedContentCB(content);
    }

    @NotNull
    @Override
    public ActionCallback requestFocus(@Nullable final Content content, final boolean forced) {
      return ActionCallback.DONE;
    }

    @Override
    public void dispose() {
      myContents.clear();
      mySelected = null;
      myDispatcher.getListeners().clear();
    }

    @Override
    public boolean isDisposed() {
      return false;
    }

    @Override
    public boolean isSingleSelection() {
      return true;
    }
  }}
