package com.intellij.find.editorHeaderActions;

import consulo.find.FindModel;

public class ToggleInCommentsAction extends EditorHeaderSetSearchContextAction {
  public ToggleInCommentsAction() {
    super("In &Comments Only", FindModel.SearchContext.IN_COMMENTS);
  }
}
