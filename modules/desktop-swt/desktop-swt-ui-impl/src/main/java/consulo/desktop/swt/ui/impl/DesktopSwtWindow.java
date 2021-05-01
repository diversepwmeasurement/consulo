/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtWindow extends DesktopSwtComponent<Shell> implements Window {
  private DesktopSwtComponent myContent;

  public DesktopSwtWindow(String title) {
    myComponent = new Shell();
    myComponent.setText(title);
    FillLayout layout = new FillLayout();
    layout.type = SWT.VERTICAL;
    myComponent.setLayout(layout);
  }

  @Override
  protected Shell createSWT(Composite parent) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void initialize(Shell component) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Window getParent() {
    return (Window)super.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    myComponent.setSize(size.getWidth(), size.getHeight());
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    myComponent.setText(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    myContent = (DesktopSwtComponent)content;
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {

  }

  @Override
  public void setResizable(boolean value) {
  }

  @Override
  public void setClosable(boolean value) {

  }

  @RequiredUIAccess
  @Override
  public void show() {
    if (myContent != null) {
      myContent.bind(getComposite(), null);
    }
    myComponent.open();
  }

  @RequiredUIAccess
  @Override
  public void close() {
    myComponent.close();
  }
}
