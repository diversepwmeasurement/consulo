/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package consulo.task.impl.internal.action;

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.NameUtil;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Condition;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Dmitry Avdeev
 */
public class TaskSearchSupport {
  private TaskSearchSupport() {
  }

  public static List<Task> getLocalAndCachedTasks(final TaskManager myManager, String pattern, final boolean withClosed) {
    List<Task> tasks = new ArrayList<Task>();
    ContainerUtil.addAll(tasks, myManager.getLocalTasks(withClosed));
    ContainerUtil.addAll(tasks, ContainerUtil.filter(myManager.getCachedIssues(withClosed), new Condition<Task>() {
      @Override
      public boolean value(final Task task) {
        return myManager.findTask(task.getId()) == null;
      }
    }));
    List<Task> filteredTasks = filterTasks(pattern, tasks);
    ContainerUtil.sort(filteredTasks, TaskManagerImpl.TASK_UPDATE_COMPARATOR);
    return filteredTasks;
  }

  public static List<Task> filterTasks(final String pattern, final List<Task> tasks) {
    final Matcher matcher = getMatcher(pattern);
    return ContainerUtil.mapNotNull(tasks, task -> matcher.matches(task.getId()) || matcher.matches(task.getSummary()) ? task : null);
  }

  public static List<Task> getRepositoriesTasks(final TaskManager myManager,
                                                String pattern,
                                                int max,
                                                int since,
                                                boolean forceRequest,
                                                final boolean withClosed,
                                                @Nonnull final ProgressIndicator cancelled) {
    List<Task> tasks = myManager.getIssues(pattern, since, max, withClosed, cancelled, forceRequest);
    ContainerUtil.sort(tasks, TaskManagerImpl.TASK_UPDATE_COMPARATOR);
    return tasks;
  }

  public static List<Task> getItems(final TaskManager myManager, String pattern, boolean cached, boolean autopopup) {
    return filterTasks(pattern, getTasks(pattern, cached, autopopup, myManager));
  }


  private static Matcher getMatcher(String pattern) {
    StringTokenizer tokenizer = new StringTokenizer(pattern, " ");
    StringBuilder builder = new StringBuilder();
    while (tokenizer.hasMoreTokens()) {
      String word = tokenizer.nextToken();
      builder.append('*');
      builder.append(word);
      builder.append("* ");
    }

    return NameUtil.buildMatcher(builder.toString(), NameUtil.MatchingCaseSensitivity.NONE);
  }

  private static List<Task> getTasks(String pattern, boolean cached, boolean autopopup, final TaskManager myManager) {
    return cached ? myManager.getCachedIssues() : myManager.getIssues(pattern, !autopopup);
  }
}
