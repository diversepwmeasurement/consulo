/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs;

import consulo.process.ProcessOutputTypes;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.ProcessEventListener;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 1/25/12
 * Time: 5:08 PM
 */
public interface LineProcessEventListener extends ProcessEventListener {
  /**
   * This method is invoked when line (as separated by \n or \r) becomes available.
   *
   * @param line       a line of the text
   * @param outputType a type of output (one of constants from {@link ProcessOutputTypes})
   */
  @SuppressWarnings({"UnusedParameters", "UnnecessaryFullyQualifiedName"})
  void onLineAvailable(String line, Key outputType);
}
