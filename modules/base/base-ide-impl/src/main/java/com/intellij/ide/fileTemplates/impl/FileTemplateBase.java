/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import consulo.virtualFileSystem.fileType.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.velocity.runtime.parser.ParseException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * @author Eugene Zhuravlev
 *         Date: 4/6/11
 */
public abstract class FileTemplateBase implements FileTemplate {
  public static final boolean DEFAULT_REFORMAT_CODE_VALUE = true;
  public static final boolean DEFAULT_ENABLED_VALUE = true;
  @Nullable
  private String myText;
  private boolean myShouldReformatCode = DEFAULT_REFORMAT_CODE_VALUE;
  private boolean myLiveTemplateEnabled;

  @Override
  public final boolean isReformatCode() {
    return myShouldReformatCode;
  }

  @Override
  public final void setReformatCode(boolean reformat) {
    myShouldReformatCode = reformat;
  }

  public final String getQualifiedName() {
    return getQualifiedName(getName(), getExtension());
  }

  public static String getQualifiedName(final String name, final String extension) {
    return FTManager.encodeFileName(name, extension);
  }

  @Override
  @Nonnull
  public final String getText() {
    final String text = myText;
    return text != null? text : getDefaultText();
  }

  @Override
  public final void setText(@Nullable String text) {
    if (text == null) {
      myText = null;
    }
    else {
      final String converted = StringUtil.convertLineSeparators(text);
      myText = converted.equals(getDefaultText())? null : converted;
    }
  }

  @Nonnull
  protected String getDefaultText() {
    return "";
  }

  @Override
  @Nonnull
  public final String getText(Map attributes) throws IOException{
    return FileTemplateUtil.mergeTemplate(attributes, getText(), false);
  }

  @Override
  @Nonnull
  public final String getText(Properties attributes) throws IOException{
    return FileTemplateUtil.mergeTemplate(attributes, getText(), false);
  }

  @Override
  @Nonnull
  public final String[] getUnsetAttributes(@Nonnull Properties properties, Project project) throws ParseException {
    return FileTemplateUtil.calculateAttributes(getText(), properties, false, project);
  }

  @Override
  @Nonnull
  public final String[] getUnsetAttributes(@Nonnull Map<String, Object> properties, Project project) throws ParseException {
    return FileTemplateUtil.calculateAttributes(getText(), properties, false, project);
  }

  @Override
  public FileTemplateBase clone() {
    try {
      return (FileTemplateBase)super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isTemplateOfType(@Nonnull final FileType fType) {
    return fType.equals(FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(getExtension()));
  }

  @Override
  public boolean isLiveTemplateEnabled() {
    return myLiveTemplateEnabled;
  }

  @Override
  public void setLiveTemplateEnabled(boolean value) {
    myLiveTemplateEnabled = value;
  }

  public boolean isLiveTemplateEnabledByDefault() { return false; }
}
