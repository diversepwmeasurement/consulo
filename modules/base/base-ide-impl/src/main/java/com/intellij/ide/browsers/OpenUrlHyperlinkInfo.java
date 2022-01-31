/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.browsers;

import com.intellij.execution.filters.HyperlinkWithPopupMenuInfo;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import consulo.project.Project;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import consulo.ide.browsers.WebBrowserManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;

public final class OpenUrlHyperlinkInfo implements HyperlinkWithPopupMenuInfo {
  private final String url;
  private final WebBrowser browser;
  private final Condition<WebBrowser> browserCondition;

  public OpenUrlHyperlinkInfo(@Nonnull String url) {
    this(url, Conditions.<WebBrowser>alwaysTrue(), null);
  }

  public OpenUrlHyperlinkInfo(@Nonnull String url, @Nullable WebBrowser browser) {
    this(url, null, browser);
  }

  public OpenUrlHyperlinkInfo(@Nonnull String url, @Nonnull Condition<WebBrowser> browserCondition) {
    this(url, browserCondition, null);
  }

  private OpenUrlHyperlinkInfo(@Nonnull String url, @Nullable Condition<WebBrowser> browserCondition, @Nullable WebBrowser browser) {
    this.url = url;
    this.browserCondition = browserCondition;
    this.browser = browser;
  }

  @Override
  public ActionGroup getPopupMenuGroup(@Nonnull MouseEvent event) {
    DefaultActionGroup group = new DefaultActionGroup();
    for (final WebBrowser browser : WebBrowserManager.getInstance().getActiveBrowsers()) {
      if (browserCondition == null ? (this.browser == null || browser.equals(this.browser)) : browserCondition.value(browser)) {
        group.add(new DumbAwareAction("Open in " + browser.getName(), "Open URL in " + browser.getName(), browser.getIcon()) {
          @Override
          public void actionPerformed(AnActionEvent e) {
            BrowserLauncher.getInstance().browse(url, browser, e.getData(CommonDataKeys.PROJECT));
          }
        });
      }
    }

    group.add(new AnAction("Copy URL", "Copy URL to clipboard", PlatformIconGroup.actionsCopy()) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        CopyPasteManager.getInstance().setContents(new StringSelection(url));
      }
    });
    return group;
  }

  @Override
  public void navigate(Project project) {
    BrowserLauncher.getInstance().browse(url, browser, project);
  }
}
