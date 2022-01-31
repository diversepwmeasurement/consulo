// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.beg;

import consulo.application.ui.UISettings;
import consulo.desktop.awt.ui.plaf.intellij.IdeaPopupMenuUI;
import consulo.application.ui.awt.GraphicsConfig;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import com.intellij.openapi.wm.impl.IdeFrameDecorator;
import consulo.application.ui.awt.LinePainter2D;
import consulo.application.ui.awt.JBUIScale;
import consulo.application.ui.awt.GraphicsUtil;
import consulo.application.ui.awt.JBInsets;
import consulo.application.ui.awt.UIUtil;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicMenuUI;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
public class IdeaMenuUI extends BasicMenuUI {
  private static final Rectangle ourZeroRect = new Rectangle(0, 0, 0, 0);
  private static final Rectangle ourTextRect = new Rectangle();
  private static final Rectangle ourArrowIconRect = new Rectangle();
  private int myMaxGutterIconWidth;
  private int myMaxGutterIconWidth2;
  private int a;
  private static Rectangle ourPreferredSizeRect = new Rectangle();
  private int k;
  private int e;
  private static final Rectangle ourAcceleratorRect = new Rectangle();
  private static final Rectangle ourCheckIconRect = new Rectangle();
  private static final Rectangle ourIconRect = new Rectangle();
  private static final Rectangle ourViewRect = new Rectangle(32767, 32767);

  /**
   * invoked by reflection
   */
  public static ComponentUI createUI(JComponent component) {
    return new IdeaMenuUI();
  }

  public IdeaMenuUI() {
    myMaxGutterIconWidth = JBUIScale.scale(18);
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();
    Integer integer = UIUtil.getPropertyMaxGutterIconWidth(getPropertyPrefix());
    if (integer != null) {
      myMaxGutterIconWidth2 = myMaxGutterIconWidth = integer.intValue();
    }

    selectionBackground = UIUtil.getListSelectionBackground(true);
  }

  private void checkEmptyIcon(JComponent comp) {
    myMaxGutterIconWidth = getAllowedIcon() == null && IdeaPopupMenuUI.hideEmptyIcon(comp) ? 0 : myMaxGutterIconWidth2;
  }

  @Override
  public void paint(Graphics g, JComponent comp) {
    UISettings.setupAntialiasing(g);
    JMenu jMenu = (JMenu)comp;
    ButtonModel buttonmodel = jMenu.getModel();
    int mnemonicIndex = jMenu.getDisplayedMnemonicIndex();
    Icon icon = getIcon();
    Icon allowedIcon = getAllowedIcon();
    checkEmptyIcon(comp);
    Insets insets = comp.getInsets();
    resetRects();

    ourViewRect.setBounds(0, 0, jMenu.getWidth(), jMenu.getHeight());
    JBInsets.removeFrom(ourViewRect, insets);

    Font font = g.getFont();
    Font font1 = comp.getFont();
    g.setFont(font1);
    FontMetrics fontmetrics = g.getFontMetrics(font1);
    String s1 = layoutMenuItem(fontmetrics, jMenu.getText(), icon, allowedIcon, arrowIcon, jMenu.getVerticalAlignment(), jMenu.getHorizontalAlignment(), jMenu.getVerticalTextPosition(), jMenu.getHorizontalTextPosition(),
                               ourViewRect, ourIconRect, ourTextRect, ourAcceleratorRect, ourCheckIconRect, ourArrowIconRect, jMenu.getText() != null ? defaultTextIconGap : 0, defaultTextIconGap);
    Color mainColor = g.getColor();
    if (comp.isOpaque()) {
      fillOpaque(g, comp, jMenu, buttonmodel, allowedIcon, mainColor);
    }
    else {
      fillOpaqueFalse(g, comp, jMenu, buttonmodel, allowedIcon, mainColor);
    }
    if (allowedIcon != null) {
      if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
        g.setColor(selectionForeground);
      }
      else {
        g.setColor(jMenu.getForeground());
      }
      if (useCheckAndArrow()) {
        allowedIcon.paintIcon(comp, g, ourCheckIconRect.x, ourCheckIconRect.y);
      }
      g.setColor(mainColor);
      if (menuItem.isArmed()) {
        drawIconBorder(g);
      }
    }
    if (icon != null) {
      if (!buttonmodel.isEnabled()) {
        icon = jMenu.getDisabledIcon();
      }
      else if (buttonmodel.isPressed() && buttonmodel.isArmed()) {
        icon = jMenu.getPressedIcon();
        if (icon == null) {
          icon = jMenu.getIcon();
        }
      }
      if (icon != null) {
        icon.paintIcon(comp, g, ourIconRect.x, ourIconRect.y);
      }
    }
    if (s1 != null && s1.length() > 0) {
      if (buttonmodel.isEnabled()) {
        if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
          g.setColor(selectionForeground);
        }
        else {
          g.setColor(jMenu.getForeground());
        }
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x, ourTextRect.y + fontmetrics.getAscent());
      }
      else {
        final Color disabledForeground = UIUtil.getMenuItemDisabledForeground();
        if (disabledForeground != null) {
          g.setColor(disabledForeground);
          BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x, ourTextRect.y + fontmetrics.getAscent());
        }
        else {
          g.setColor(jMenu.getBackground().brighter());
          BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x, ourTextRect.y + fontmetrics.getAscent());
          g.setColor(jMenu.getBackground().darker());
          BasicGraphicsUtils.drawStringUnderlineCharAt(g, s1, mnemonicIndex, ourTextRect.x - 1, (ourTextRect.y + fontmetrics.getAscent()) - 1);
        }
      }
    }
    if (arrowIcon != null) {
      if (SystemInfo.isMac) {
        ourArrowIconRect.y += JBUIScale.scale(1);
      }

      if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
        g.setColor(selectionForeground);
      }

      if (useCheckAndArrow()) {
        arrowIcon.paintIcon(comp, g, ourArrowIconRect.x, ourArrowIconRect.y);
      }
    }
    g.setColor(mainColor);
    g.setFont(font);
  }

  protected void fillOpaque(Graphics g, JComponent comp, JMenu jMenu, ButtonModel buttonmodel, Icon allowedIcon, Color mainColor) {
    g.setColor(jMenu.getBackground());
    g.fillRect(0, 0, jMenu.getWidth(), jMenu.getHeight());
    if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
      paintHover(g, comp, jMenu, allowedIcon);
    }
    g.setColor(mainColor);
  }

  protected void fillOpaqueFalse(Graphics g, JComponent comp, JMenu jMenu, ButtonModel buttonmodel, Icon allowedIcon, Color mainColor) {
    if (IdeFrameDecorator.isCustomDecorationActive()) {
      if (buttonmodel.isArmed() || buttonmodel.isSelected()) {
        paintHover(g, comp, jMenu, allowedIcon);
      }
      g.setColor(mainColor);
    }
  }

  protected final void paintHover(Graphics g, JComponent comp, JMenu jMenu, Icon allowedIcon) {
    g.setColor(selectionBackground);
    if (allowedIcon != null && !(UIUtil.isUnderIntelliJLaF() || UIUtil.isUnderDarcula())) {
      g.fillRect(k, 0, jMenu.getWidth() - k, jMenu.getHeight());
    }
    else if (IdeaPopupMenuUI.isPartOfPopupMenu(comp) && Registry.is("popup.menu.roundSelection.enabled", false)) {
      GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
      g.fillRoundRect(4, 1, jMenu.getWidth() - 8, jMenu.getHeight() - 2, 8, 8);
      config.restore();
    }
    else {
      g.fillRect(0, 0, jMenu.getWidth(), jMenu.getHeight());
    }
  }

  private boolean useCheckAndArrow() {
    return !((JMenu)menuItem).isTopLevelMenu();
  }

  @Override
  public MenuElement[] getPath() {
    MenuSelectionManager menuselectionmanager = MenuSelectionManager.defaultManager();
    MenuElement[] amenuelement = menuselectionmanager.getSelectedPath();
    int i1 = amenuelement.length;
    if (i1 == 0) {
      return new MenuElement[0];
    }
    Container container = menuItem.getParent();
    MenuElement[] amenuelement1;
    if (amenuelement[i1 - 1].getComponent() == container) {
      amenuelement1 = new MenuElement[i1 + 1];
      System.arraycopy(amenuelement, 0, amenuelement1, 0, i1);
      amenuelement1[i1] = menuItem;
    }
    else {
      int j1;
      for (j1 = amenuelement.length - 1; j1 >= 0; j1--) {
        if (amenuelement[j1].getComponent() == container) {
          break;
        }
      }
      amenuelement1 = new MenuElement[j1 + 2];
      System.arraycopy(amenuelement, 0, amenuelement1, 0, j1 + 1);
      amenuelement1[j1 + 1] = menuItem;
    }
    return amenuelement1;
  }

  private String layoutMenuItem(FontMetrics fontmetrics,
                                @Nls String text,
                                Icon icon,
                                Icon checkIcon,
                                Icon arrowIcon,
                                int verticalAlignment,
                                int horizontalAlignment,
                                int verticalTextPosition,
                                int horizontalTextPosition,
                                Rectangle viewRect,
                                Rectangle iconRect,
                                Rectangle textRect,
                                Rectangle acceleratorRect,
                                Rectangle checkIconRect,
                                Rectangle arrowIconRect,
                                int textIconGap,
                                int menuItemGap) {
    SwingUtilities.layoutCompoundLabel(menuItem, fontmetrics, text, icon, verticalAlignment, horizontalAlignment, verticalTextPosition, horizontalTextPosition, viewRect, iconRect, textRect, textIconGap);
    acceleratorRect.width = acceleratorRect.height = 0;

    /* Initialize the checkIcon bounds rectangle's width & height.
     */
    if (useCheckAndArrow()) {
      if (checkIcon != null) {
        checkIconRect.width = checkIcon.getIconWidth();
        checkIconRect.height = checkIcon.getIconHeight();
      }
      else {
        checkIconRect.width = checkIconRect.height = 0;
      }

      /* Initialize the arrowIcon bounds rectangle width & height.
       */
      if (arrowIcon != null) {
        arrowIconRect.width = arrowIcon.getIconWidth();
        arrowIconRect.height = arrowIcon.getIconHeight();
      }
      else {
        arrowIconRect.width = arrowIconRect.height = 0;
      }
      textRect.x += myMaxGutterIconWidth;
      iconRect.x += myMaxGutterIconWidth;
    }
    textRect.x += menuItemGap;
    iconRect.x += menuItemGap;
    Rectangle labelRect = iconRect.union(textRect);

    // Position the Accelerator text rect

    acceleratorRect.x += viewRect.width - arrowIconRect.width - menuItemGap - acceleratorRect.width;
    acceleratorRect.y = (viewRect.y + viewRect.height / 2) - acceleratorRect.height / 2;

    // Position the Check and Arrow Icons

    if (useCheckAndArrow()) {
      arrowIconRect.x += viewRect.width - arrowIconRect.width;
      arrowIconRect.y = (viewRect.y + labelRect.height / 2) - arrowIconRect.height / 2;
      if (checkIcon != null) {
        checkIconRect.y = (viewRect.y + labelRect.height / 2) - checkIconRect.height / 2;
        checkIconRect.x += (viewRect.x + myMaxGutterIconWidth / 2) - checkIcon.getIconWidth() / 2;
        a = viewRect.x;
        e = (viewRect.y + labelRect.height / 2) - myMaxGutterIconWidth / 2;
        k = viewRect.x + myMaxGutterIconWidth + 2;
      }
      else {
        checkIconRect.x = checkIconRect.y = 0;
      }
    }
    return text;
  }

  private Icon getIcon() {
    Icon icon = menuItem.getIcon();
    if (icon != null && getAllowedIcon() != null) {
      icon = null;
    }
    return icon;
  }

  @Override
  protected Dimension getPreferredMenuItemSize(JComponent comp, Icon checkIcon, Icon arrowIcon, int defaultTextIconGap) {
    JMenu jMenu = (JMenu)comp;
    Icon icon1 = getIcon();
    Icon icon2 = getAllowedIcon();
    checkEmptyIcon(comp);
    String text = jMenu.getText();
    Font font = jMenu.getFont();
    FontMetrics fontmetrics = jMenu.getToolkit().getFontMetrics(font);
    resetRects();
    layoutMenuItem(fontmetrics, text, icon1, icon2, arrowIcon, jMenu.getVerticalAlignment(), jMenu.getHorizontalAlignment(), jMenu.getVerticalTextPosition(), jMenu.getHorizontalTextPosition(),
                   ourViewRect, ourIconRect, ourTextRect, ourAcceleratorRect, ourCheckIconRect, ourArrowIconRect, text != null ? defaultTextIconGap : 0, defaultTextIconGap);
    ourPreferredSizeRect.setBounds(ourTextRect);
    ourPreferredSizeRect = SwingUtilities.computeUnion(ourIconRect.x, ourIconRect.y, ourIconRect.width, ourIconRect.height, ourPreferredSizeRect);
    if (useCheckAndArrow()) {
      ourPreferredSizeRect.width += myMaxGutterIconWidth;
      ourPreferredSizeRect.width += defaultTextIconGap;
      ourPreferredSizeRect.width += defaultTextIconGap;
      ourPreferredSizeRect.width += ourArrowIconRect.width;
    }
    ourPreferredSizeRect.width += 2 * defaultTextIconGap;
    Insets insets = jMenu.getInsets();
    if (insets != null) {
      ourPreferredSizeRect.width += insets.left + insets.right;
      ourPreferredSizeRect.height += insets.top + insets.bottom;
    }
    if (ourPreferredSizeRect.width % 2 == 0) {
      ourPreferredSizeRect.width++;
    }
    if (ourPreferredSizeRect.height % 2 == 0) {
      ourPreferredSizeRect.height++;
    }
    return ourPreferredSizeRect.getSize();
  }

  private void drawIconBorder(Graphics g) {
    int i1 = a - 1;
    int j1 = e - 2;
    int k1 = i1 + myMaxGutterIconWidth + 1;
    int l1 = j1 + myMaxGutterIconWidth + 4;
    g.setColor(BegResources.m);
    LinePainter2D.paint((Graphics2D)g, i1, j1, i1, l1);
    LinePainter2D.paint((Graphics2D)g, i1, j1, k1, j1);
    g.setColor(BegResources.j);
    LinePainter2D.paint((Graphics2D)g, k1, j1, k1, l1);
    LinePainter2D.paint((Graphics2D)g, i1, l1, k1, l1);
  }

  private void resetRects() {
    ourIconRect.setBounds(ourZeroRect);
    ourTextRect.setBounds(ourZeroRect);
    ourAcceleratorRect.setBounds(ourZeroRect);
    ourCheckIconRect.setBounds(ourZeroRect);
    ourArrowIconRect.setBounds(ourZeroRect);
    ourViewRect.setBounds(0, 0, Short.MAX_VALUE, Short.MAX_VALUE);
    ourPreferredSizeRect.setBounds(ourZeroRect);
  }

  private Icon getAllowedIcon() {
    Icon icon = menuItem.isEnabled() ? menuItem.getIcon() : menuItem.getDisabledIcon();
    if (icon != null && icon.getIconWidth() > myMaxGutterIconWidth) {
      icon = null;
    }
    return icon;
  }

  @Override
  public void update(Graphics g, JComponent comp) {
    paint(g, comp);
  }
}