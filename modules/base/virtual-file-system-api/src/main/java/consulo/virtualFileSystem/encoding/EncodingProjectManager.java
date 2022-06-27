// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class EncodingProjectManager extends EncodingManager {
  public static EncodingProjectManager getInstance(@Nonnull ComponentManager project) {
    return project.getInstance(EncodingProjectManager.class);
  }

  /**
   * @return Project encoding name (configured in Settings|File Encodings|Project Encoding) or empty string if it's configured to "System Default"
   */
  @Nonnull
  @Override
  public abstract String getDefaultCharsetName();

  /**
   * @return Project encoding (configured in Settings|File Encodings|Project Encoding)
   */
  @Nonnull
  @Override
  public abstract Charset getDefaultCharset();

  /**
   * Sets Project encoding (configured in Settings|File Encodings|Project Encoding). Use empty string to specify "System Default"
   */
  @Override
  public abstract void setDefaultCharsetName(@Nonnull String name);
}
