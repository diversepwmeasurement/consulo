/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.passwordSafe;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;

/**
 * The password safe component. It allows storing, removing, and retrieving the passwords.
 * Note that on the first access to the password safe functions, the dialog asking for the
 * master password might have to be shown. So the access should be either done from UI thread,
 * or it should be possible to invoke {@link java.awt.EventQueue#invokeAndWait(Runnable)}
 * method when password access methods are invoked.
 *
 * @see consulo.ide.impl.idea.ide.passwordSafe.ui.PasswordSafePromptDialog
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PasswordSafe implements PasswordStorage {

  /**
   * @return the instance of password safe service
   */
  public static PasswordSafe getInstance() {
    return ServiceManager.getService(PasswordSafe.class);
  }
}
