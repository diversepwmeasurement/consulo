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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.application.ui.UISettings;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.application.dumb.DumbAware;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public abstract class TabsPlacementAction extends ToggleAction implements DumbAware {
  abstract int getPlace();

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    return UISettings.getInstance().EDITOR_TAB_PLACEMENT == getPlace();
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    UISettings.getInstance().EDITOR_TAB_PLACEMENT = getPlace();
    LafManager.getInstance().repaintUI();
    UISettings.getInstance().fireUISettingsChanged();
  }

  public static class Top extends TabsPlacementAction {
    @Override
    int getPlace() {
      return UISettings.PLACEMENT_EDITOR_TAB_TOP;
    }
  }

  public static class Left extends TabsPlacementAction {
    @Override
    int getPlace() {
      return UISettings.PLACEMENT_EDITOR_TAB_LEFT;
    }
  }

  public static class Bottom extends TabsPlacementAction {
    @Override
    int getPlace() {
      return UISettings.PLACEMENT_EDITOR_TAB_BOTTOM;
    }
  }

  public static class Right extends TabsPlacementAction {
    @Override
    int getPlace() {
      return UISettings.PLACEMENT_EDITOR_TAB_RIGHT;
    }
  }

  public static class None extends TabsPlacementAction {
    @Override
    int getPlace() {
      return UISettings.PLACEMENT_EDITOR_TAB_NONE;
    }
  }
}
