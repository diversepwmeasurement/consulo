/*
 * Copyright 2013-2017 consulo.io
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
package consulo.platform.impl;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

/**
 * @author VISTALL
 * @since 15-Sep-17
 * <p>
 * static fields from SystemInfo from IDEA
 */
public abstract class PlatformBase implements Platform {
  private static final String OS_NAME = System.getProperty("os.name");
  private static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.ROOT);

  private static final String _OS_NAME = OS_NAME.toLowerCase(Locale.ROOT);
  private static final boolean isWindows = _OS_NAME.startsWith("windows");
  private static final boolean isOS2 = _OS_NAME.startsWith("os/2") || _OS_NAME.startsWith("os2");
  private static final boolean isMac = _OS_NAME.startsWith("mac");
  private static final boolean isLinux = _OS_NAME.startsWith("linux");
  private static final boolean isUnix = !isWindows && !isOS2;

  public static final boolean isMacOSMojave = isMac && isOsVersionAtLeast("10.14");

  public static final boolean isXWindow = isUnix && !isMac;
  /* http://askubuntu.com/questions/72549/how-to-determine-which-window-manager-is-running/227669#227669 */
  public static final boolean isGNOME = isXWindow &&
                                        (StringUtil.notNullize(System.getenv("GDMSESSION")).startsWith("gnome") ||
                                         StringUtil.notNullize(System.getenv("XDG_CURRENT_DESKTOP")).toLowerCase(Locale.ROOT).endsWith("gnome"));
  /* https://userbase.kde.org/KDE_System_Administration/Environment_Variables#KDE_FULL_SESSION */
  public static final boolean isKDE = isXWindow && !StringUtil.isEmpty(System.getenv("KDE_FULL_SESSION"));

  // version numbers from http://msdn.microsoft.com/en-us/library/windows/desktop/ms724832.aspx
  private static final boolean isWin2kOrNewer = isWindows && isOsVersionAtLeast("5.0");
  private static final boolean isWinXpOrNewer = isWindows && isOsVersionAtLeast("5.1");
  private static final boolean isWinVistaOrNewer = isWindows && isOsVersionAtLeast("6.0");
  private static final boolean isWin7OrNewer = isWindows && isOsVersionAtLeast("6.1");
  private static final boolean isWin8OrNewer = isWindows && isOsVersionAtLeast("6.2");
  private static final boolean isWin10OrNewer = isWindows && isOsVersionAtLeast("10.0");

  private static final boolean isFileSystemCaseSensitive = isUnix && !isMac || "true".equalsIgnoreCase(System.getProperty("idea.case.sensitive.fs"));
  private static final boolean areSymLinksSupported = isUnix || isWinVistaOrNewer;

  public static boolean isOsVersionAtLeast(@Nonnull String version) {
    return StringUtil.compareVersionNumbers(OS_VERSION, version) >= 0;
  }

  protected static class FileSystemImpl implements FileSystem {

    @Override
    public boolean isCaseSensitive() {
      return isFileSystemCaseSensitive;
    }

    @Override
    public boolean areSymLinksSupported() {
      return areSymLinksSupported;
    }
  }

  protected static class OperatingSystemImpl implements OperatingSystem {
    private final String myOSArch = StringUtil.notNullize(System.getProperty("os.arch"));
    private Boolean myWindows11OrLater;

    public OperatingSystemImpl() {
    }

    @Override
    public boolean isWindows() {
      return isWindows;
    }

    @Override
    public boolean isWindowsVistaOrNewer() {
      return isWinVistaOrNewer;
    }

    @Override
    public boolean isWindows7OrNewer() {
      return isWin7OrNewer;
    }

    @Override
    public boolean isWindows8OrNewer() {
      return isWin8OrNewer;
    }

    @Override
    public boolean isWindows10OrNewer() {
      return isWin10OrNewer;
    }

    @Override
    public boolean isWindows11OrNewer() {
      if (myWindows11OrLater == null) {
        myWindows11OrLater = isWindows11OrNewerImpl();
      }
      return myWindows11OrLater;
    }

    private boolean isWindows11OrNewerImpl() {
      // at jdk 17 windows 11 will return in os name, but at old versions of jdk that will be Windows 10
      try {
        boolean windows11OrLater = OS_NAME.contains("Windows 11");
        if (isWindows10OrNewer() && !windows11OrLater) {
          WinNT.OSVERSIONINFO osversioninfo = new WinNT.OSVERSIONINFO();
          if (Kernel32.INSTANCE.GetVersionEx(osversioninfo)) {
            int dwBuildNumber = osversioninfo.dwBuildNumber.intValue();

            if (dwBuildNumber >= 22_000) {
              return true;
            }
          }
        }
      }
      catch (Throwable ignored) {
      }

      return false;
    }

    @Override
    public boolean isMac() {
      return isMac;
    }

    @Override
    public boolean isMacMojave() {
      return isMacOSMojave;
    }

    @Override
    public boolean isLinux() {
      return isLinux;
    }

    @Override
    public boolean isKDE() {
      return isKDE;
    }

    @Override
    public boolean isGNOME() {
      return isGNOME;
    }

    @Nonnull
    @Override
    public String name() {
      return OS_NAME;
    }

    @Nonnull
    @Override
    public String version() {
      return OS_VERSION;
    }

    @Nullable
    @Override
    public String getEnvironmentVariable(@Nonnull String key) {
      return System.getenv(key);
    }

    @Nonnull
    @Override
    public Map<String, String> environmentVariables() {
      return Collections.unmodifiableMap(System.getenv());
    }

    @Nonnull
    @Override
    public String arch() {
      return myOSArch;
    }
  }

  protected static class JvmImpl implements Jvm {
    private final String myJavaVersion;
    private final String myJavaRuntimeVersion;
    private final String myJavaVendor;
    private final String myJavaName;
    private final CpuArchitecture myCpuArchitecture;

    protected JvmImpl(Map<String, String> jvmProperties) {
      myJavaVersion = jvmProperties.getOrDefault("java.version", "n/a");
      myJavaRuntimeVersion = jvmProperties.getOrDefault("java.runtime.version", "n/a");
      myJavaVendor = jvmProperties.getOrDefault("java.vendor", "n/a");
      myJavaName = jvmProperties.getOrDefault("java.vm.name", "n/a");

      String osArch = jvmProperties.getOrDefault("os.arch", "");
      switch (osArch) {
        case "x86_64":
        case "amd64":
          myCpuArchitecture = CpuArchitecture.X86_64;
          break;
        case "i386":
        case "x86":
          myCpuArchitecture = CpuArchitecture.X86;
          break;
        case "arm64":
        case "aarch64":
          myCpuArchitecture = CpuArchitecture.AARCH64;
          break;
        default:
          String name = osArch.toUpperCase(Locale.ROOT);
          int width = 0;
          String sunArchModel = jvmProperties.get("sun.arch.data.model");
          if (sunArchModel != null) {
            width = StringUtil.parseInt(sunArchModel, 0);
          }

          myCpuArchitecture = new CpuArchitecture(name, width);
          break;
      }
    }

    @Nonnull
    @Override
    public String version() {
      return myJavaVersion;
    }

    @Nonnull
    @Override
    public String runtimeVersion() {
      return myJavaRuntimeVersion;
    }

    @Nonnull
    @Override
    public String vendor() {
      return myJavaVendor;
    }

    @Nonnull
    @Override
    public String name() {
      return myJavaName;
    }

    @Nullable
    @Override
    public String getRuntimeProperty(@Nonnull String key) {
      return System.getProperty(key);
    }

    @Nonnull
    @Override
    public Map<String, String> getRuntimeProperties() {
      Properties properties = System.getProperties();
      Map<String, String> map = new LinkedHashMap<>();
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }
      return map;
    }

    @Override
    @Nonnull
    public CpuArchitecture arch() {
      return myCpuArchitecture;
    }
  }

  protected static class UserImpl implements User {
    private final String myUserName = System.getProperty("user.name");
    private final Path myUserPath = Path.of(System.getProperty("user.home"));

    @Override
    public boolean superUser() {
      return false;
    }

    @Nonnull
    @Override
    public String name() {
      return myUserName;
    }

    @Nonnull
    @Override
    public Path homePath() {
      return myUserPath;
    }
  }

  private final FileSystem myFileSystem;
  private final OperatingSystem myOperatingSystem;
  private final Jvm myJvm;
  private final User myUser;

  protected PlatformBase(@Nonnull Map<String, String> jvmProperties) {
    myFileSystem = createFS();
    myOperatingSystem = createOS();
    myJvm = createJVM(jvmProperties);
    myUser = createUser();
  }

  protected static Map<String, String> getSystemJvmProperties() {
    Properties properties = System.getProperties();
    Map<String, String> map = new LinkedHashMap<>();
    for (Object key : properties.keySet()) {
      if (key instanceof String keyString) {
        map.put(keyString, properties.getProperty(keyString, ""));
      }
    }
    return map;
  }

  @Nonnull
  protected FileSystem createFS() {
    return new FileSystemImpl();
  }

  @Nonnull
  protected OperatingSystem createOS() {
    return new OperatingSystemImpl();
  }

  @Nonnull
  protected Jvm createJVM(@Nonnull Map<String, String> jvmProperties) {
    return new JvmImpl(jvmProperties);
  }

  @Nonnull
  protected User createUser() {
    return new UserImpl();
  }

  @Nonnull
  @Override
  public Jvm jvm() {
    return myJvm;
  }

  @Nonnull
  @Override
  public FileSystem fs() {
    return myFileSystem;
  }

  @Nonnull
  @Override
  public OperatingSystem os() {
    return myOperatingSystem;
  }

  @Nonnull
  @Override
  public User user() {
    return myUser;
  }
}
