/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.updateSettings;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationProperties;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.container.boot.ContainerPathManager;
import consulo.externalService.update.UpdateChannel;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.updateSettings.impl.PlatformOrPluginUpdateResult;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.io.File;

/**
 * @author VISTALL
 * @since 02-Sep-16
 */
@Singleton
@ServiceImpl
@State(name = "UpdateSettingsImpl", storages = @Storage(value = "updates.xml", roamingType = RoamingType.DISABLED))
public class UpdateSettingsImpl implements PersistentStateComponent<UpdateSettingsImpl.State>, UpdateSettings {
  @Nonnull
  public static UpdateSettingsImpl getInstance() {
    return (UpdateSettingsImpl)UpdateSettings.getInstance();
  }

  static class State {
    public boolean enable = true;
    public long lastTimeCheck = 0;
    public UpdateChannel channel;
    public PlatformOrPluginUpdateResult.Type lastCheckResult = PlatformOrPluginUpdateResult.Type.NO_UPDATE;
  }

  private State myState = new State();

  @Nonnull
  private static UpdateChannel findDefaultChannel() {
    if (ApplicationProperties.isInSandbox()) {
      return UpdateChannel.nightly;
    }

    File file = ContainerPathManager.get().getAppHomeDirectory();
    for (UpdateChannel channel : UpdateChannel.values()) {
      if (new File(file, "." + channel.name()).exists()) {
        return channel;
      }
    }

    return UpdateChannel.release;
  }

  @Override
  @Nonnull
  public UpdateChannel getChannel() {
    UpdateChannel channel = myState.channel;
    if (channel == null) {
      myState.channel = channel = findDefaultChannel();
    }
    return channel;
  }

  public void setLastCheckResult(PlatformOrPluginUpdateResult.Type type) {
    myState.lastCheckResult = type;
  }

  public PlatformOrPluginUpdateResult.Type getLastCheckResult() {
    return ObjectUtil.notNull(myState.lastCheckResult, PlatformOrPluginUpdateResult.Type.NO_UPDATE);
  }

  @Override
  public void setChannel(@Nonnull UpdateChannel channel) {
    myState.channel = channel;
  }

  @Override
  public boolean isEnable() {
    return myState.enable;
  }

  @Override
  public void setEnable(boolean value) {
    myState.enable = value;
  }

  public long getLastTimeCheck() {
    return myState.lastTimeCheck;
  }

  public void setLastTimeCheck(long time) {
    myState.lastTimeCheck = time;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }
}
