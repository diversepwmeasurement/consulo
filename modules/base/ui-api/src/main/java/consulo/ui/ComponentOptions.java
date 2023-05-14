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
package consulo.ui;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 03/05/2021
 */
public abstract class ComponentOptions {
  protected static abstract class ComponentOptionsBuilder<B extends ComponentOptionsBuilder<B>> {
    protected boolean myBackgroundPaint = true;

    @Nonnull
    @SuppressWarnings("unchecked")
    public B backgroundPaint(boolean value) {
      myBackgroundPaint = value;
      return (B)this;
    }

    @Nonnull
    public abstract ComponentOptions build();
  }

  private final boolean myBackgroundPaint;

  protected ComponentOptions(boolean backgroundPaint) {
    myBackgroundPaint = backgroundPaint;
  }

  public boolean isBackgroundPaint() {
    return myBackgroundPaint;
  }
}
