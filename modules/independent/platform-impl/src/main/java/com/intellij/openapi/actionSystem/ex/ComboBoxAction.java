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
package com.intellij.openapi.actionSystem.ex;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Condition;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.actionSystem.ex.ComboBoxButton;
import consulo.actionSystem.ex.ComboBoxButtonImpl;
import consulo.annotations.RequiredDispatchThread;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

public abstract class ComboBoxAction extends AnAction implements CustomComponentAction {
  private String myPopupTitle;

  protected ComboBoxAction() {
  }

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ComboBoxButton button = (ComboBoxButton)e.getPresentation().getClientProperty(CUSTOM_COMPONENT_PROPERTY);
    if (button == null) {
      Component contextComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      JRootPane rootPane = UIUtil.getParentOfType(JRootPane.class, contextComponent);
      if (rootPane != null) {
        button = (ComboBoxButton)UIUtil.uiTraverser().withRoot(rootPane).bfsTraversal()
                .filter(component -> component instanceof ComboBoxButton && ((ComboBoxButton)component).getComboBoxAction() == ComboBoxAction.this).first();
      }
      if (button == null) return;
    }

    button.showPopup();
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(Presentation presentation) {
    JPanel panel = new JPanel(new GridBagLayout());
    ComboBoxButton button = createComboBoxButton(presentation);
    panel.add(button.getComponent(), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insets(0, 3, 0, 3), 0, 0));
    return panel;
  }

  @Nonnull
  protected ComboBoxButton createComboBoxButton(Presentation presentation) {
    return new ComboBoxButtonImpl(this, presentation);
  }

  public void setPopupTitle(String popupTitle) {
    myPopupTitle = popupTitle;
  }

  public String getPopupTitle() {
    return myPopupTitle;
  }

  @RequiredDispatchThread
  @Override
  public void update(@Nonnull AnActionEvent e) {
  }

  public boolean shouldShowDisabledActions() {
    return false;
  }

  @Nonnull
  public abstract DefaultActionGroup createPopupActionGroup(JComponent button);

  public int getMaxRows() {
    return 30;
  }

  public int getMinHeight() {
    return 1;
  }

  public int getMinWidth() {
    return 1;
  }

  public Condition<AnAction> getPreselectCondition() {
    return null;
  }
}
