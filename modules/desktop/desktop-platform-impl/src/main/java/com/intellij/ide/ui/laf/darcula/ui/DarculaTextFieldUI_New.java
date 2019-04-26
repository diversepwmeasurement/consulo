// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui.laf.darcula.ui;

import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.MacUIUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

import static com.intellij.ide.ui.laf.darcula.DarculaUIUtil_New.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaTextFieldUI_New extends TextFieldWithPopupHandlerUI_New {
  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new DarculaTextFieldUI_New();
  }

  @Override
  protected int getMinimumHeight(int textHeight) {
    Insets i = getComponent().getInsets();
    JComponent c = getComponent();
    int minHeight = (isCompact(c) ? COMPACT_HEIGHT.get() : MINIMUM_HEIGHT.get()) + i.top + i.bottom;
    return isComboBoxEditor(c) || UIUtil.getParentOfType(JSpinner.class, c) != null ? textHeight : minHeight;
  }

  @Override
  protected int getClearIconPreferredSpace() {
    return super.getClearIconPreferredSpace() - getClearIconGap();
  }

  @Override
  protected void paintBackground(Graphics g) {
    JTextComponent component = getComponent();
    if (component != null) {
      Container parent = component.getParent();
      if (parent != null && component.isOpaque()) {
        g.setColor(parent.getBackground());
        g.fillRect(0, 0, component.getWidth(), component.getHeight());
      }

      if (component.getBorder() instanceof DarculaTextBorder && !isTableCellEditor(component)) {
        paintDarculaBackground(g, component);
      }
      else if (component.isOpaque()) {
        super.paintBackground(g);
      }
    }
  }

  protected void paintDarculaBackground(Graphics g, JTextComponent component) {
    Graphics2D g2 = (Graphics2D)g.create();
    Rectangle r = new Rectangle(component.getSize());
    JBInsets.removeFrom(r, paddings());

    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

      g2.translate(r.x, r.y);

      if (component.isEnabled() && component.isEditable()) {
        float arc = isSearchField(component) ? COMPONENT_ARC.getFloat() : 0.0f;
        float bw = bw();

        g2.setColor(component.getBackground());
        g2.fill(new RoundRectangle2D.Float(bw, bw, r.width - bw * 2, r.height - bw * 2, arc, arc));
      }
    }
    finally {
      g2.dispose();
    }
  }

  @Override
  protected Insets getDefaultMargins() {
    Component c = getComponent();
    return isCompact(c) || isTableCellEditor(c) ? JBUI.insets(0, 3) : JBUI.insets(2, 6);
  }

  protected float bw() {
    return BW.getFloat();
  }
}
