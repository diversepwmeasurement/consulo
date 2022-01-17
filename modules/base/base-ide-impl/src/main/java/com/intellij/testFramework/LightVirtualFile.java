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
package com.intellij.testFramework;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.CharsetUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import com.intellij.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.lang.LocalTimeCounter;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.io.*;
import java.nio.charset.Charset;

/**
 * In-memory implementation of {@link VirtualFile}.
 */
public class LightVirtualFile extends LightVirtualFileBase {
  private CharSequence myContent = "";
  private Language myLanguage;
  private boolean myReadOnly;

  public LightVirtualFile() {
    this("");
  }

  public LightVirtualFile(@NonNls @Nonnull String name) {
    this(name, "");
  }

  public LightVirtualFile(@NonNls @Nonnull String name, @Nonnull CharSequence content) {
    this(name, null, content, LocalTimeCounter.currentTime());
  }

  public LightVirtualFile(@Nonnull String name, final FileType fileType, @Nonnull CharSequence text) {
    this(name, fileType, text, LocalTimeCounter.currentTime());
  }

  public LightVirtualFile(VirtualFile original, @Nonnull CharSequence text, long modificationStamp) {
    this(original.getName(), original.getFileType(), text, modificationStamp);
    setCharset(original.getCharset());
  }

  public LightVirtualFile(@Nonnull String name, final FileType fileType, @Nonnull CharSequence text, final long modificationStamp) {
    this(name, fileType, text, CharsetUtil.extractCharsetFromFileContent(null, null, fileType, text), modificationStamp);
  }

  public LightVirtualFile(@Nonnull String name,
                          final FileType fileType,
                          @Nonnull CharSequence text,
                          Charset charset,
                          final long modificationStamp) {
    super(name, fileType, modificationStamp);
    setContent(text);
    setCharset(charset);
  }

  public LightVirtualFile(@Nonnull String name, final Language language, @Nonnull CharSequence text) {
    super(name, null, LocalTimeCounter.currentTime());
    setContent(text);
    setLanguage(language);
  }

  public Language getLanguage() {
    return myLanguage;
  }

  public void setLanguage(@Nonnull Language language) {
    myLanguage = language;
    FileType type = language.getAssociatedFileType();
    if (type == null) {
      type = FileTypeRegistry.getInstance().getFileTypeByFileName(getName());
    }
    setFileType(type);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return VfsUtilCore.byteStreamSkippingBOM(contentsToByteArray(), this);
  }

  @Override
  @Nonnull
  public OutputStream getOutputStream(Object requestor, final long newModificationStamp, long newTimeStamp) throws IOException {
    return VfsUtilCore.outputStreamAddingBOM(new ByteArrayOutputStream() {
      @Override
      public void close() {
        setModificationStamp(newModificationStamp);

        try {
          String content = toString(getCharset().name());
          setContent(content);
        }
        catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }, this);
  }

  @Override
  @Nonnull
  public byte[] contentsToByteArray() throws IOException {
    final Charset charset = getCharset();
    final String s = getContent().toString();
    return s.getBytes(charset.name());
  }

  public void setContent(Object requestor, @Nonnull CharSequence content, boolean fireEvent) {
    setContent(content);
    setModificationStamp(LocalTimeCounter.currentTime());
  }

  private void setContent(@Nonnull CharSequence content) {
    assert !myReadOnly;
    //StringUtil.assertValidSeparators(content);
    myContent = content;
  }

  @Nonnull
  public CharSequence getContent() {
    return myContent;
  }

  public void markReadOnly() {
    setWritable(false);
    myReadOnly = true;
  }

  @Override
  public String toString() {
    return "LightVirtualFile: " + getPresentableUrl();
  }
}
