/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.image;

import consulo.ui.UIInternal;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.shared.ColorValue;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2018-05-06
 */
public final class ImageEffects {
  @Nonnull
  public static Image layered(@Nonnull Image... images) {
    if (images.length == 0) {
      throw new IllegalArgumentException("empty array");
    }
    return UIInternal.get()._ImageEffects_layered(images);
  }

  @Nonnull
  public static Image transparent(@Nonnull Image original) {
    return transparent(original, .5f);
  }

  @Nonnull
  public static Image transparent(@Nonnull Image original, float alpha) {
    return UIInternal.get()._ImageEffects_transparent(original, alpha);
  }

  @Nonnull
  public static Image grayed(@Nonnull Image original) {
    return UIInternal.get()._ImageEffects_grayed(original);
  }

  /**
   * Return composize image, where height is max of i0&i1, and width is sum of both
   * Return will be displayed like [i0][i1]
   */
  @Nonnull
  public static Image appendRight(@Nonnull Image i0, @Nonnull Image i1) {
    return UIInternal.get()._ImageEffects_appendRight(i0, i1);
  }

  @Nonnull
  public static Image empty(int width, int height) {
    return UIInternal.get()._ImageEffects_empty(width, height);
  }

  @Nonnull
  public static Image canvas(int width, int height, @Nonnull Consumer<Canvas2D> consumer) {
    return UIInternal.get()._ImageEffects_canvas(width, height, consumer);
  }

  @Nonnull
  public static Image colorFilled(int width, int heght, @Nonnull ColorValue colorValue) {
    return canvas(width, heght, ctx -> {
      ctx.setFillStyle(colorValue);
      ctx.fillRect(0, 0, width, heght);
    });
  }

  @Nonnull
  public static Image twoColorFilled(int width, int heght, @Nonnull ColorValue colorValue1, @Nonnull ColorValue colorValue2) {
    return canvas(width, heght, ctx -> {
      ctx.beginPath();
      ctx.setFillStyle(colorValue1);
      ctx.moveTo(0, 0);
      ctx.lineTo(12, 0);
      ctx.lineTo(0, 12);
      ctx.fill();

      ctx.beginPath();
      ctx.setFillStyle(colorValue2);
      ctx.moveTo(12, 12);
      ctx.lineTo(0, 12);
      ctx.lineTo(12, 0);
      ctx.fill();
    });
  }
}
