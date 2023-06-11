// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.process;

import consulo.process.BaseProcessHandler;

/**
 * @author Vladislav.Soroka
 */
public abstract class BuildProcessHandler extends BaseProcessHandler {
  public abstract String getExecutionName();

  public void forceProcessDetach() {
    notifyProcessDetached();
  }
}
