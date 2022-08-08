/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.ide.impl.idea.notification.impl.NotificationsManagerImpl;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.ex.action.AnAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.impl.DocumentImpl;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.*;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.popup.Balloon;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.ui.BalloonLayoutData;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.content.Content;
import consulo.ide.impl.idea.util.*;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.document.util.TextRange;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import consulo.util.lang.ref.Ref;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author peter
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class EventLog {
  public static final String LOG_REQUESTOR = "Internal log requestor";
  public static final String LOG_TOOL_WINDOW_ID = "Event Log";
  public static final String HELP_ID = "reference.toolwindows.event.log";
  private static final String A_CLOSING = "</a>";
  private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
  private static final Pattern A_PATTERN = Pattern.compile("<a ([^>]* )?href=[\"\']([^>]*)[\"\'][^>]*>");
  private static final Set<String> NEW_LINES = ContainerUtil.newHashSet("<br>", "</br>", "<br/>", "<p>", "</p>", "<p/>");
  protected static final String DEFAULT_CATEGORY = "";

  protected final LogModel myModel;

  @Inject
  public EventLog(Application application) {
    myModel = new LogModel(null, application);

    application.getMessageBus().connect().subscribe(Notifications.class, new Notifications() {
      @Override
      public void notify(@Nonnull Notification notification) {
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) {
          myModel.addNotification(notification);
        }
        for (Project p : openProjects) {
          getProjectComponent(p).printNotification(notification);
        }
      }
    });
  }

  public static void expireNotification(@Nonnull Notification notification) {
    getApplicationComponent().myModel.removeNotification(notification);
    for (Project p : ProjectManager.getInstance().getOpenProjects()) {
      getProjectComponent(p).myProjectModel.removeNotification(notification);
    }
  }

  public static void showNotification(@Nonnull Project project, @Nonnull String groupId, @Nonnull List<String> ids) {
    getProjectComponent(project).showNotification(groupId, ids);
  }

  private static EventLog getApplicationComponent() {
    return ApplicationManager.getApplication().getComponent(EventLog.class);
  }

  @Nonnull
  public static LogModel getLogModel(@Nullable Project project) {
    return project != null ? getProjectComponent(project).myProjectModel : getApplicationComponent().myModel;
  }

  public static void markAllAsRead(@Nullable Project project) {
    LogModel model = getLogModel(project);
    Set<String> groups = new HashSet<String>();
    for (Notification notification : model.getNotifications()) {
      groups.add(notification.getGroupId());
      model.removeNotification(notification);
      notification.expire();
    }

    if (project != null && !groups.isEmpty()) {
      clearNMore(project, groups);
    }
  }

  public static void clearNMore(@Nonnull Project project, @Nonnull Collection<String> groups) {
    getProjectComponent(project).clearNMore(groups);
  }

  @Nullable
  public static Trinity<Notification, String, Long> getStatusMessage(@Nullable Project project) {
    return getLogModel(project).getStatusMessage();
  }

  public static LogEntry formatForLog(@Nonnull final Notification notification, final String indent) {
    DocumentImpl logDoc = new DocumentImpl("", true);
    AtomicBoolean showMore = new AtomicBoolean(false);
    Map<RangeMarker, HyperlinkInfo> links = new LinkedHashMap<>();
    List<RangeMarker> lineSeparators = new ArrayList<RangeMarker>();

    String title = notification.getTitle();
    String subtitle = notification.getSubtitle();
    if (StringUtil.isNotEmpty(title) && StringUtil.isNotEmpty(subtitle)) {
      title += " (" + subtitle + ")";
    }
    title = truncateLongString(showMore, title);
    String content = truncateLongString(showMore, notification.getContent());

    RangeMarker afterTitle = null;
    boolean hasHtml = parseHtmlContent(addIndents(title, indent), notification, logDoc, showMore, links, lineSeparators);
    if (StringUtil.isNotEmpty(title)) {
      if (StringUtil.isNotEmpty(content)) {
        appendText(logDoc, ": ");
        afterTitle = logDoc.createRangeMarker(logDoc.getTextLength() - 2, logDoc.getTextLength());
      }
    }
    int titleLength = logDoc.getTextLength();

    hasHtml |= parseHtmlContent(addIndents(content, indent), notification, logDoc, showMore, links, lineSeparators);

    List<AnAction> actions = notification.getActions();
    if (!actions.isEmpty()) {
      String text = "<p>" + StringUtil.join(actions, new Function<AnAction, String>() {
        private int index;

        @Override
        public String fun(AnAction action) {
          return "<a href=\"" + index++ + "\">" + action.getTemplatePresentation().getText() + "</a>";
        }
      }, isLongLine(actions) ? "<br>" : "&nbsp;") + "</p>";
      Notification n = new Notification("", "", ".", NotificationType.INFORMATION, new NotificationListener() {
        @Override
        public void hyperlinkUpdate(@Nonnull Notification n, @Nonnull HyperlinkEvent event) {
          Notification.fire(notification, notification.getActions().get(Integer.parseInt(event.getDescription())));
        }
      });
      if (title.length() > 0 || content.length() > 0) {
        lineSeparators.add(logDoc.createRangeMarker(TextRange.from(logDoc.getTextLength(), 0)));
      }
      hasHtml |= parseHtmlContent(text, n, logDoc, showMore, links, lineSeparators);
    }

    String status = getStatusText(logDoc, showMore, lineSeparators, indent, hasHtml);

    indentNewLines(logDoc, lineSeparators, afterTitle, hasHtml, indent);

    ArrayList<Pair<TextRange, HyperlinkInfo>> list = new ArrayList<Pair<TextRange, HyperlinkInfo>>();
    for (RangeMarker marker : links.keySet()) {
      if (!marker.isValid()) {
        showMore.set(true);
        continue;
      }
      list.add(Pair.create(new TextRange(marker.getStartOffset(), marker.getEndOffset()), links.get(marker)));
    }

    if (showMore.get()) {
      String sb = "show balloon";
      if (!logDoc.getText().endsWith(" ")) {
        appendText(logDoc, " ");
      }
      appendText(logDoc, "(" + sb + ")");
      list.add(new Pair<TextRange, HyperlinkInfo>(TextRange.from(logDoc.getTextLength() - 1 - sb.length(), sb.length()), new ShowBalloon(notification)));
    }

    return new LogEntry(logDoc.getText(), status, list, titleLength);
  }

  @Nonnull
  private static String addIndents(@Nonnull String text, @Nonnull String indent) {
    return StringUtil.replace(text, "\n", "\n" + indent);
  }

  private static boolean isLongLine(@Nonnull List<AnAction> actions) {
    int size = actions.size();
    if (size > 3) {
      return true;
    }
    if (size > 1) {
      int length = 0;
      for (AnAction action : actions) {
        length += StringUtil.length(action.getTemplatePresentation().getText());
      }
      return length > 30;
    }
    return false;
  }

  @Nonnull
  private static String truncateLongString(AtomicBoolean showMore, String title) {
    if (title.length() > 1000) {
      showMore.set(true);
      return title.substring(0, 1000) + "...";
    }
    return title;
  }

  private static void indentNewLines(DocumentImpl logDoc, List<RangeMarker> lineSeparators, RangeMarker afterTitle, boolean hasHtml, String indent) {
    if (!hasHtml) {
      int i = -1;
      while (true) {
        i = StringUtil.indexOf(logDoc.getText(), '\n', i + 1);
        if (i < 0) {
          break;
        }
        lineSeparators.add(logDoc.createRangeMarker(i, i + 1));
      }
    }
    if (!lineSeparators.isEmpty() && afterTitle != null && afterTitle.isValid()) {
      lineSeparators.add(afterTitle);
    }
    int nextLineStart = -1;
    for (RangeMarker separator : lineSeparators) {
      if (separator.isValid()) {
        int start = separator.getStartOffset();
        if (start == nextLineStart) {
          continue;
        }

        logDoc.replaceString(start, separator.getEndOffset(), "\n" + indent);
        nextLineStart = start + 1 + indent.length();
        while (nextLineStart < logDoc.getTextLength() && Character.isWhitespace(logDoc.getCharsSequence().charAt(nextLineStart))) {
          logDoc.deleteString(nextLineStart, nextLineStart + 1);
        }
      }
    }
  }

  private static String getStatusText(DocumentImpl logDoc, AtomicBoolean showMore, List<RangeMarker> lineSeparators, String indent, boolean hasHtml) {
    DocumentImpl statusDoc = new DocumentImpl(logDoc.getImmutableCharSequence(), true);
    List<RangeMarker> statusSeparators = new ArrayList<RangeMarker>();
    for (RangeMarker separator : lineSeparators) {
      if (separator.isValid()) {
        statusSeparators.add(statusDoc.createRangeMarker(separator.getStartOffset(), separator.getEndOffset()));
      }
    }
    removeJavaNewLines(statusDoc, statusSeparators, indent, hasHtml);
    insertNewLineSubstitutors(statusDoc, showMore, statusSeparators);

    return statusDoc.getText();
  }

  private static boolean parseHtmlContent(String text,
                                          Notification notification,
                                          Document document,
                                          AtomicBoolean showMore,
                                          Map<RangeMarker, HyperlinkInfo> links,
                                          List<RangeMarker> lineSeparators) {
    String content = StringUtil.convertLineSeparators(text);

    int initialLen = document.getTextLength();
    boolean hasHtml = false;
    while (true) {
      Matcher tagMatcher = TAG_PATTERN.matcher(content);
      if (!tagMatcher.find()) {
        appendText(document, content);
        break;
      }

      String tagStart = tagMatcher.group();
      appendText(document, content.substring(0, tagMatcher.start()));
      Matcher aMatcher = A_PATTERN.matcher(tagStart);
      if (aMatcher.matches()) {
        final String href = aMatcher.group(2);
        int linkEnd = content.indexOf(A_CLOSING, tagMatcher.end());
        if (linkEnd > 0) {
          String linkText = content.substring(tagMatcher.end(), linkEnd).replaceAll(TAG_PATTERN.pattern(), "");
          int linkStart = document.getTextLength();
          appendText(document, linkText);
          links.put(document.createRangeMarker(new TextRange(linkStart, document.getTextLength())), new NotificationHyperlinkInfo(notification, href));
          content = content.substring(linkEnd + A_CLOSING.length());
          continue;
        }
      }

      if (isTag(HTML_TAGS, tagStart)) {
        hasHtml = true;
        if (NEW_LINES.contains(tagStart)) {
          if (initialLen != document.getTextLength()) {
            lineSeparators.add(document.createRangeMarker(TextRange.from(document.getTextLength(), 0)));
          }
        }
        else if (!isTag(SKIP_TAGS, tagStart)) {
          showMore.set(true);
        }
      }
      else {
        appendText(document, content.substring(tagMatcher.start(), tagMatcher.end()));
      }
      content = content.substring(tagMatcher.end());
    }
    for (Iterator<RangeMarker> iterator = lineSeparators.iterator(); iterator.hasNext(); ) {
      RangeMarker next = iterator.next();
      if (next.getEndOffset() == document.getTextLength()) {
        iterator.remove();
      }
    }
    return hasHtml;
  }

  private static final String[] HTML_TAGS =
          {"a", "abbr", "acronym", "address", "applet", "area", "article", "aside", "audio", "b", "base", "basefont", "bdi", "bdo", "big", "blockquote", "body",
                  "br", "button", "canvas", "caption", "center", "cite", "code", "col", "colgroup", "command", "datalist", "dd", "del", "details", "dfn", "dir",
                  "div", "dl", "dt", "em", "embed", "fieldset", "figcaption", "figure", "font", "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4",
                  "h5", "h6", "head", "header", "hgroup", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "keygen", "label", "legend", "li", "link",
                  "map", "mark", "menu", "meta", "meter", "nav", "noframes", "noscript", "object", "ol", "optgroup", "option", "output", "p", "param", "pre",
                  "progress", "q", "rp", "rt", "ruby", "s", "samp", "script", "section", "select", "small", "source", "span", "strike", "strong", "style",
                  "sub", "summary", "sup", "table", "tbody", "td", "textarea", "tfoot", "th", "thead", "time", "title", "tr", "track", "tt", "u", "ul", "var",
                  "video", "wbr"};

  private static final String[] SKIP_TAGS = {"html", "body", "b", "i", "font"};

  private static boolean isTag(@Nonnull String[] tags, @Nonnull String tag) {
    tag = tag.substring(1, tag.length() - 1); // skip <>
    tag = StringUtil.trimEnd(StringUtil.trimStart(tag, "/"), "/"); // skip /
    int index = tag.indexOf(' ');
    if (index != -1) {
      tag = tag.substring(0, index);
    }
    return ArrayUtil.indexOf(tags, tag) != -1;
  }

  private static void insertNewLineSubstitutors(Document document, AtomicBoolean showMore, List<RangeMarker> lineSeparators) {
    for (RangeMarker marker : lineSeparators) {
      if (!marker.isValid()) {
        showMore.set(true);
        continue;
      }

      int offset = marker.getStartOffset();
      if (offset == 0 || offset == document.getTextLength()) {
        continue;
      }
      boolean spaceBefore = offset > 0 && Character.isWhitespace(document.getCharsSequence().charAt(offset - 1));
      if (offset < document.getTextLength()) {
        boolean spaceAfter = Character.isWhitespace(document.getCharsSequence().charAt(offset));
        int next = CharArrayUtil.shiftForward(document.getCharsSequence(), offset, " \t");
        if (next < document.getTextLength() && !Character.isLowerCase(document.getCharsSequence().charAt(next))) {
          document.insertString(offset, (spaceBefore ? "" : " ") + "//" + (spaceAfter ? "" : " "));
          continue;
        }
        if (spaceAfter) {
          continue;
        }
      }
      if (spaceBefore) {
        continue;
      }

      document.insertString(offset, " ");
    }
  }

  private static void removeJavaNewLines(Document document, List<RangeMarker> lineSeparators, String indent, boolean hasHtml) {
    CharSequence text = document.getCharsSequence();
    int i = 0;
    while (true) {
      i = StringUtil.indexOf(text, '\n', i);
      if (i < 0) break;
      int j = i + 1;
      if (StringUtil.startsWith(text, j, indent)) {
        j += indent.length();
      }
      document.deleteString(i, j);
      if (!hasHtml) {
        lineSeparators.add(document.createRangeMarker(TextRange.from(i, 0)));
      }
    }
  }

  private static void appendText(Document document, String text) {
    text = StringUtil.replace(text, "&nbsp;", " ");
    text = StringUtil.replace(text, "&raquo;", ">>");
    text = StringUtil.replace(text, "&laquo;", "<<");
    text = StringUtil.replace(text, "&hellip;", "...");
    document.insertString(document.getTextLength(), StringUtil.unescapeXml(text));
  }

  public static class LogEntry {
    public final String message;
    public final String status;
    public final List<Pair<TextRange, HyperlinkInfo>> links;
    public final int titleLength;

    public LogEntry(@Nonnull String message, @Nonnull String status, @Nonnull List<Pair<TextRange, HyperlinkInfo>> links, int titleLength) {
      this.message = message;
      this.status = status;
      this.links = links;
      this.titleLength = titleLength;
    }
  }

  @Nullable
  public static ToolWindow getEventLog(Project project) {
    return project == null ? null : ToolWindowManager.getInstance(project).getToolWindow(LOG_TOOL_WINDOW_ID);
  }

  public static void toggleLog(@Nullable final Project project, @Nullable final Notification notification) {
    final ToolWindow eventLog = getEventLog(project);
    if (eventLog != null) {
      if (!eventLog.isVisible()) {
        activate(eventLog, notification == null ? null : notification.getGroupId(), null);
      }
      else {
        eventLog.hide(null);
      }
    }
  }

  protected static void activate(@Nonnull final ToolWindow eventLog, @Nullable final String groupId, @Nullable final Runnable r) {
    eventLog.activate(new Runnable() {
      @Override
      public void run() {
        if (groupId == null) return;
        String contentName = getContentName(groupId);
        Content content = eventLog.getContentManager().findContent(contentName);
        if (content != null) {
          eventLog.getContentManager().setSelectedContent(content);
        }
        if (r != null) {
          r.run();
        }
      }
    }, true);
  }

  @Nonnull
  protected static String getContentName(String groupId) {
    for (EventLogCategory category : EventLogCategory.EP_NAME.getExtensionList()) {
      if (category.acceptsNotification(groupId)) {
        return category.getDisplayName();
      }
    }
    return DEFAULT_CATEGORY;
  }

  static NotificationProjectTracker getProjectComponent(Project project) {
    return project.getComponent(NotificationProjectTracker.class);
  }

  private static class NotificationHyperlinkInfo implements HyperlinkInfo {
    private final Notification myNotification;
    private final String myHref;

    public NotificationHyperlinkInfo(Notification notification, String href) {
      myNotification = notification;
      myHref = href;
    }

    @Override
    public void navigate(Project project) {
      NotificationListener listener = myNotification.getListener();
      if (listener != null) {
        EventLogConsole console = ObjectUtils.assertNotNull(getProjectComponent(project).getConsole(myNotification));
        JComponent component = console.getConsoleEditor().getContentComponent();
        listener.hyperlinkUpdate(myNotification, IJSwingUtilities.createHyperlinkEvent(myHref, component));
      }
    }
  }

  static class ShowBalloon implements HyperlinkInfo {
    private final Notification myNotification;
    private RangeHighlighter myRangeHighlighter;

    public ShowBalloon(Notification notification) {
      myNotification = notification;
    }

    public void setRangeHighlighter(RangeHighlighter rangeHighlighter) {
      myRangeHighlighter = rangeHighlighter;
    }

    @Override
    public void navigate(Project project) {
      hideBalloon(myNotification);

      for (Notification notification : getLogModel(project).getNotifications()) {
        hideBalloon(notification);
      }

      EventLogConsole console = ObjectUtil.assertNotNull(getProjectComponent(project).getConsole(myNotification));
      if (myRangeHighlighter == null || !myRangeHighlighter.isValid()) {
        return;
      }
      RelativePoint target = console.getRangeHighlighterLocation(myRangeHighlighter);
      if (target != null) {
        IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
        assert frame != null;
        Ref<BalloonLayoutData> layoutDataRef = null;
        BalloonLayoutData layoutData = new BalloonLayoutData();
        layoutData.groupId = "";
        layoutData.showFullContent = true;
        layoutData.showSettingButton = false;
        layoutDataRef = Ref.create(layoutData);

        Balloon balloon = NotificationsManagerImpl.createBalloon(frame, myNotification, true, true, layoutDataRef, project);
        balloon.show(target, Balloon.Position.above);
      }
    }

    private static void hideBalloon(Notification notification) {
      Balloon balloon = notification.getBalloon();
      if (balloon != null) {
        balloon.hide(true);
      }
    }
  }
}
