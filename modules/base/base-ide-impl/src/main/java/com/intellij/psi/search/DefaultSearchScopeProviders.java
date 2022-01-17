// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.ide.util.treeView.WeighedItem;
import consulo.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ColoredItem;
import com.intellij.openapi.util.Pair;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.FileColorManager;
import com.intellij.util.TreeItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author gregsh
 */
public class DefaultSearchScopeProviders {
  private DefaultSearchScopeProviders() {
  }

  public static class Favorites implements SearchScopeProvider {
    @Override
    public String getDisplayName() {
      return "Favorites";
    }

    @Nonnull
    @Override
    public List<SearchScope> getSearchScopes(@Nonnull Project project) {
      FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
      if (favoritesManager == null) return Collections.emptyList();
      List<SearchScope> result = new ArrayList<>();
      for (String favorite : favoritesManager.getAvailableFavoritesListNames()) {
        Collection<TreeItem<Pair<AbstractUrl, String>>> rootUrls = favoritesManager.getFavoritesListRootUrls(favorite);
        if (rootUrls.isEmpty()) continue;  // ignore unused root
        result.add(new GlobalSearchScope(project) {
          @Nonnull
          @Override
          public String getDisplayName() {
            return "Favorite \'" + favorite + "\'";
          }

          @Override
          public boolean contains(@Nonnull VirtualFile file) {
            return ReadAction.compute(() -> favoritesManager.contains(favorite, file));
          }

          @Override
          public boolean isSearchInModuleContent(@Nonnull Module aModule) {
            return true;
          }

          @Override
          public boolean isSearchInLibraries() {
            return true;
          }
        });
      }
      return result;
    }
  }

  public static class CustomNamed implements SearchScopeProvider {
    @Override
    public String getDisplayName() {
      return "Other";
    }

    @Nonnull
    @Override
    public List<SearchScope> getSearchScopes(@Nonnull Project project) {
      List<SearchScope> result = new ArrayList<>();
      NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(project);
      for (NamedScopesHolder holder : holders) {
        NamedScope[] scopes = holder.getEditableScopes();  // predefined scopes already included
        for (NamedScope scope : scopes) {
          result.add(wrapNamedScope(project, scope, true));
        }
      }
      return result;
    }
  }

  @Nonnull
  public static GlobalSearchScope wrapNamedScope(@Nonnull Project project, @Nonnull NamedScope namedScope, boolean colored) {
    GlobalSearchScope scope = GlobalSearchScopesCore.filterScope(project, namedScope);
    if (!colored && !(namedScope instanceof WeighedItem)) return scope;
    int weight = namedScope instanceof WeighedItem ? ((WeighedItem)namedScope).getWeight() : -1;
    Color color = !colored ? null :
                  //namedScope instanceof ColoredItem ? ((ColoredItem)namedScope).getColor() :
                  FileColorManager.getInstance(project).getScopeColor(namedScope.getName());
    return new MyWeightedScope(scope, weight, color);
  }

  private static class MyWeightedScope extends DelegatingGlobalSearchScope implements WeighedItem, ColoredItem {
    final int weight;
    final Color color;

    MyWeightedScope(@Nonnull GlobalSearchScope scope, int weight, Color color) {
      super(scope);
      this.weight = weight;
      this.color = color;
    }

    @Override
    public int getWeight() {
      return weight;
    }

    @Nullable
    @Override
    public Color getColor() {
      return color;
    }
  }
}
