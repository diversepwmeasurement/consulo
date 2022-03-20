/**
 * @author VISTALL
 * @since 17/01/2022
 */
module consulo.document.api {
  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;

  requires transitive kava.beans;

  exports consulo.document;
  exports consulo.document.event;
  exports consulo.document.util;

  exports consulo.document.internal to consulo.document.impl, consulo.ide.impl, consulo.language.editor.api, consulo.code.editor.api, consulo.code.editor.impl, consulo.language.impl;
}