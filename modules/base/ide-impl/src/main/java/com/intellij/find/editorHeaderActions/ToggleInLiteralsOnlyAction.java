package com.intellij.find.editorHeaderActions;

import consulo.find.FindModel;

public class ToggleInLiteralsOnlyAction extends EditorHeaderSetSearchContextAction {
  public ToggleInLiteralsOnlyAction() {
    super("In &Literals Only", FindModel.SearchContext.IN_STRING_LITERALS);
  }
}
