/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInspection.ex;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.InspectionProfileConvertor;
import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ModifiableModel;
import com.intellij.lang.annotation.HighlightSeverity;
import consulo.application.ApplicationManager;
import com.intellij.openapi.options.ExternalInfo;
import com.intellij.openapi.options.ExternalizableScheme;
import consulo.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.profile.DefaultProjectProfileManager;
import com.intellij.profile.ProfileEx;
import com.intellij.profile.ProfileManager;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.SeverityProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Interner;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.DFSTBuilder;
import com.intellij.util.graph.GraphGenerator;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import consulo.application.util.function.Computable;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * @author max
 */
public class InspectionProfileImpl extends ProfileEx implements ModifiableModel, InspectionProfile, ExternalizableScheme {
  @NonNls public static final String INSPECTION_TOOL_TAG = "inspection_tool";
  @NonNls public static final String CLASS_TAG = "class";
  private static final Logger LOG = Logger.getInstance(InspectionProfileImpl.class);
  @NonNls private static final String VALID_VERSION = "1.0";
  @NonNls private static final String VERSION_TAG = "version";
  @NonNls private static final String USED_LEVELS = "used_levels";
  @TestOnly
  public static boolean INIT_INSPECTIONS = false;
  private static Map<String, InspectionElementsMerger> ourMergers = null;
  private final InspectionToolRegistrar myRegistrar;
  @Nonnull
  private final Map<String, Element> myUninstalledInspectionsSettings;
  private final ExternalInfo myExternalInfo = new ExternalInfo();
  protected InspectionProfileImpl mySource;
  private Map<String, ToolsImpl> myTools = new HashMap<String, ToolsImpl>();
  private Map<String, Boolean> myDisplayLevelMap;
  @Attribute("is_locked")
  private boolean myLockedProfile;
  private InspectionProfileImpl myBaseProfile = null;
  private String myEnabledTool = null;
  private String[] myScopesOrder;
  private String myDescription;
  private boolean myModified;
  private volatile boolean myInitialized;

  InspectionProfileImpl(@Nonnull InspectionProfileImpl inspectionProfile) {
    super(inspectionProfile.getName());

    myRegistrar = inspectionProfile.myRegistrar;
    myUninstalledInspectionsSettings = new LinkedHashMap<String, Element>(inspectionProfile.myUninstalledInspectionsSettings);

    myBaseProfile = inspectionProfile.myBaseProfile;
    setProjectLevel(inspectionProfile.isProjectLevel());
    myLockedProfile = inspectionProfile.myLockedProfile;
    mySource = inspectionProfile;
    setProfileManager(inspectionProfile.getProfileManager());
    copyFrom(inspectionProfile);
  }

  public InspectionProfileImpl(@Nonnull final String profileName,
                               @Nonnull InspectionToolRegistrar registrar,
                               @Nonnull final ProfileManager profileManager) {
    super(profileName);
    myRegistrar = registrar;
    myBaseProfile = getDefaultProfile();
    setProfileManager(profileManager);
    myUninstalledInspectionsSettings = new TreeMap<String, Element>();
  }

  public InspectionProfileImpl(@Nonnull @NonNls String profileName) {
    super(profileName);
    myRegistrar = InspectionToolRegistrar.getInstance();
    setProfileManager(InspectionProfileManager.getInstance());
    myUninstalledInspectionsSettings = new TreeMap<String, Element>();
  }

  private static synchronized Map<String, InspectionElementsMerger> getMergers() {
    if (ourMergers == null) {
      ourMergers = new LinkedHashMap<String, InspectionElementsMerger>();
      for (InspectionElementsMerger merger : InspectionElementsMerger.EP_NAME.getExtensionList()) {
        ourMergers.put(merger.getMergedToolName(), merger);
      }
    }
    return ourMergers;
  }

  @Nonnull
  public static InspectionProfileImpl createSimple(@Nonnull String name,
                                                   @Nonnull Project project,
                                                   @Nonnull final InspectionToolWrapper... toolWrappers) {
    InspectionToolRegistrar registrar = new InspectionToolRegistrar() {
      @Nonnull
      @Override
      public List<InspectionToolWrapper> createTools() {
        return Arrays.asList(toolWrappers);
      }
    };
    InspectionProfileImpl profile = new InspectionProfileImpl(name, registrar, InspectionProfileManager.getInstance());
    boolean init = INIT_INSPECTIONS;
    try {
      INIT_INSPECTIONS = true;
      profile.initialize(project);
    }
    finally {
      INIT_INSPECTIONS = init;
    }
    for (InspectionToolWrapper toolWrapper : toolWrappers) {
      profile.enableTool(toolWrapper.getShortName(), project);
    }
    return profile;
  }

  private static boolean toolSettingsAreEqual(@Nonnull String toolName, @Nonnull InspectionProfileImpl profile1, @Nonnull InspectionProfileImpl profile2) {
    final Tools toolList1 = profile1.myTools.get(toolName);
    final Tools toolList2 = profile2.myTools.get(toolName);

    return Comparing.equal(toolList1, toolList2);
  }

  @Nonnull
  private static InspectionToolWrapper copyToolSettings(@Nonnull InspectionToolWrapper toolWrapper)
          throws WriteExternalException, InvalidDataException {
    final InspectionToolWrapper inspectionTool = toolWrapper.createCopy();
    if (toolWrapper.isInitialized()) {
      @NonNls String tempRoot = "config";
      Element config = new Element(tempRoot);
      toolWrapper.getTool().writeSettings(config);
      inspectionTool.getTool().readSettings(config);
    }
    return inspectionTool;
  }

  @Nonnull
  public static InspectionProfileImpl getDefaultProfile() {
    return InspectionProfileImplHolder.DEFAULT_PROFILE;
  }

  @Override
  public void setModified(final boolean modified) {
    myModified = modified;
  }

  @Override
  public InspectionProfile getParentProfile() {
    return mySource;
  }

  @Override
  public String getBaseProfileName() {
    if (myBaseProfile == null) return null;
    return myBaseProfile.getName();
  }

  @Override
  public void setBaseProfile(InspectionProfile profile) {
    myBaseProfile = (InspectionProfileImpl)profile;
  }

  @Override
  @SuppressWarnings({"SimplifiableIfStatement"})
  public boolean isChanged() {
    if (mySource != null && mySource.myLockedProfile != myLockedProfile) return true;
    return myModified;
  }

  @Override
  public boolean isProperSetting(@Nonnull String toolId) {
    if (myBaseProfile != null) {
      final Tools tools = myBaseProfile.getTools(toolId, null);
      final Tools currentTools = myTools.get(toolId);
      return !Comparing.equal(tools, currentTools);
    }
    return false;
  }

  @Override
  public void resetToBase(Project project) {
    initInspectionTools(project);

    copyToolsConfigurations(myBaseProfile, project);
    myDisplayLevelMap = null;
  }

  @Override
  public void resetToEmpty(Project project) {
    initInspectionTools(project);
    final InspectionToolWrapper[] profileEntries = getInspectionTools(null);
    for (InspectionToolWrapper toolWrapper : profileEntries) {
      disableTool(toolWrapper.getShortName(), project);
    }
  }

  @Override
  public HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey inspectionToolKey, PsiElement element) {
    Project project = element == null ? null : element.getProject();
    final ToolsImpl tools = getTools(inspectionToolKey.toString(), project);
    HighlightDisplayLevel level = tools != null ? tools.getLevel(element) : HighlightDisplayLevel.WARNING;
    if (!((SeverityProvider)getProfileManager()).getOwnSeverityRegistrar().isSeverityValid(level.getSeverity().getName())) {
      level = HighlightDisplayLevel.WARNING;
      setErrorLevel(inspectionToolKey, level, project);
    }
    return level;
  }

  @Override
  public void readExternal(@Nonnull Element element) throws InvalidDataException {
    super.readExternal(element);

    if (!ApplicationManager.getApplication().isUnitTestMode() || myBaseProfile == null) {
      // todo remove this strange side effect
      myBaseProfile = getDefaultProfile();
    }
    final String version = element.getAttributeValue(VERSION_TAG);
    if (version == null || !version.equals(VALID_VERSION)) {
      element = InspectionProfileConvertor.convertToNewFormat(element, this);
    }

    final Element highlightElement = element.getChild(USED_LEVELS);
    if (highlightElement != null) {
      // from old profiles
      ((SeverityProvider)getProfileManager()).getOwnSeverityRegistrar().readExternal(highlightElement);
    }

    Interner<String> interner = Interner.createStringInterner();
    for (Element toolElement : element.getChildren(INSPECTION_TOOL_TAG)) {
      // make clone to avoid retaining memory via o.parent pointers
      toolElement = toolElement.clone();
      JDOMUtil.internStringsInElement(toolElement, interner);
      myUninstalledInspectionsSettings.put(toolElement.getAttributeValue(CLASS_TAG), toolElement);
    }
  }

  @Nonnull
  public Set<HighlightSeverity> getUsedSeverities() {
    LOG.assertTrue(myInitialized);
    final Set<HighlightSeverity> result = new HashSet<HighlightSeverity>();
    for (Tools tools : myTools.values()) {
      for (ScopeToolState state : tools.getTools()) {
        result.add(state.getLevel().getSeverity());
      }
    }
    return result;
  }

  @Override
  public void serializeInto(@Nonnull Element element, boolean preserveCompatibility) {
    // must be first - compatibility
    element.setAttribute(VERSION_TAG, VALID_VERSION);

    super.serializeInto(element, preserveCompatibility);

    synchronized (myExternalInfo) {
      if (!myInitialized) {
        for (Element el : myUninstalledInspectionsSettings.values()) {
          element.addContent(el.clone());
        }
        return;
      }
    }

    Map<String, Boolean> diffMap = getDisplayLevelMap();
    if (diffMap != null) {

      diffMap = new TreeMap<String, Boolean>(diffMap);
      for (String toolName : myUninstalledInspectionsSettings.keySet()) {
        diffMap.put(toolName, false);
      }

      for (String toolName : diffMap.keySet()) {
        if (!myLockedProfile && diffMap.get(toolName).booleanValue()) {
          markSettingsMerged(toolName, element);
          continue;
        }

        Element toolElement = myUninstalledInspectionsSettings.get(toolName);
        if (toolElement == null) {
          ToolsImpl toolList = myTools.get(toolName);
          LOG.assertTrue(toolList != null);
          Element inspectionElement = new Element(INSPECTION_TOOL_TAG);
          inspectionElement.setAttribute(CLASS_TAG, toolName);
          try {
            toolList.writeExternal(inspectionElement);
          }
          catch (WriteExternalException e) {
            LOG.error(e);
            continue;
          }

          if (!areSettingsMerged(toolName, inspectionElement)) {
            element.addContent(inspectionElement);
          }
        }
        else {
          element.addContent(toolElement.clone());
        }
      }
    }
  }

  private void markSettingsMerged(String toolName, Element element) {
    //add marker if already merged but result is now default (-> empty node)
    final String mergedName = InspectionElementsMerger.getMergedMarkerName(toolName);
    if (!myUninstalledInspectionsSettings.containsKey(mergedName)) {
      final InspectionElementsMerger merger = getMergers().get(toolName);
      if (merger != null && merger.markSettingsMerged(myUninstalledInspectionsSettings)) {
        element.addContent(new Element(INSPECTION_TOOL_TAG).setAttribute(CLASS_TAG, mergedName));
      }
    }
  }

  private boolean areSettingsMerged(String toolName, Element inspectionElement) {
    //skip merged settings as they could be restored from already provided data
    final InspectionElementsMerger merger = getMergers().get(toolName);
    return merger != null && merger.areSettingsMerged(myUninstalledInspectionsSettings, inspectionElement);
  }

  public void collectDependentInspections(@Nonnull InspectionToolWrapper toolWrapper,
                                          @Nonnull Set<InspectionToolWrapper> dependentEntries,
                                          Project project) {
    String mainToolId = toolWrapper.getMainToolId();

    if (mainToolId != null) {
      InspectionToolWrapper dependentEntryWrapper = getInspectionTool(mainToolId, project);

      if (dependentEntryWrapper == null) {
        LOG.error("Can't find main tool: '" + mainToolId+"' which was specified in "+toolWrapper);
        return;
      }
      if (!dependentEntries.add(dependentEntryWrapper)) {
        collectDependentInspections(dependentEntryWrapper, dependentEntries, project);
      }
    }
  }

  @Override
  @Nullable
  public InspectionToolWrapper getInspectionTool(@Nonnull String shortName, @Nonnull PsiElement element) {
    final Tools toolList = getTools(shortName, element.getProject());
    return toolList == null ? null : toolList.getInspectionTool(element);
  }

  @Nullable
  @Override
  public InspectionProfileEntry getUnwrappedTool(@Nonnull String shortName, @Nonnull PsiElement element) {
    InspectionToolWrapper tool = getInspectionTool(shortName, element);
    return tool == null ? null : tool.getTool();
  }

  @Override
  public <T extends InspectionProfileEntry> T getUnwrappedTool(@Nonnull Key<T> shortNameKey, @Nonnull PsiElement element) {
    //noinspection unchecked
    return (T) getUnwrappedTool(shortNameKey.toString(), element);
  }

  @Override
  public void modifyProfile(@Nonnull Consumer<ModifiableModel> modelConsumer) {
    ModifiableModel model = getModifiableModel();
    modelConsumer.consume(model);
    try {
      model.commit();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Override
  public <T extends InspectionProfileEntry> void modifyToolSettings(@Nonnull final Key<T> shortNameKey,
                                                                    @Nonnull final PsiElement psiElement,
                                                                    @Nonnull final Consumer<T> toolConsumer) {
    modifyProfile(new Consumer<ModifiableModel>() {
      @Override
      public void consume(@Nonnull ModifiableModel model) {
        InspectionProfileEntry tool = model.getUnwrappedTool(shortNameKey.toString(), psiElement);
        //noinspection unchecked
        toolConsumer.consume((T) tool);
      }
    });
  }

  @Override
  @Nullable
  public InspectionToolWrapper getInspectionTool(@Nonnull String shortName, Project project) {
    final ToolsImpl tools = getTools(shortName, project);
    return tools != null? tools.getTool() : null;
  }

  public InspectionToolWrapper getToolById(@Nonnull String id, @Nonnull PsiElement element) {
    initInspectionTools(element.getProject());
    for (Tools toolList : myTools.values()) {
      final InspectionToolWrapper tool = toolList.getInspectionTool(element);
      String toolId =
              tool instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper)tool).getID() : tool.getShortName();
      if (id.equals(toolId)) return tool;
    }
    return null;
  }

  @Override
  public void save() throws IOException {
    InspectionProfileManager.getInstance().fireProfileChanged(this);
  }

  @Override
  public boolean isEditable() {
    return myEnabledTool == null;
  }

  @Override
  public void setEditable(final String displayName) {
    myEnabledTool = displayName;
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return isEditable() ? getName() : myEnabledTool;
  }

  @Override
  public void scopesChanged() {
    for (ScopeToolState toolState : getAllTools(null)) {
      toolState.scopesChanged();
    }
    InspectionProfileManager.getInstance().fireProfileChanged(this);
  }

  @Override
  @Transient
  public boolean isProfileLocked() {
    return myLockedProfile;
  }

  @Override
  public void lockProfile(boolean isLocked) {
    myLockedProfile = isLocked;
  }

  @Override
  @Nonnull
  public InspectionToolWrapper[] getInspectionTools(@Nullable PsiElement element) {
    initInspectionTools(element == null ? null : element.getProject());
    List<InspectionToolWrapper> result = new ArrayList<InspectionToolWrapper>();
    for (Tools toolList : myTools.values()) {
      result.add(toolList.getInspectionTool(element));
    }
    return result.toArray(new InspectionToolWrapper[result.size()]);
  }

  @Override
  @Nonnull
  public List<Tools> getAllEnabledInspectionTools(Project project) {
    initInspectionTools(project);
    List<Tools> result = new ArrayList<Tools>();
    for (final ToolsImpl toolList : myTools.values()) {
      if (toolList.isEnabled()) {
        result.add(toolList);
      }
    }
    return result;
  }

  @Override
  public void disableTool(@Nonnull String toolId, @Nonnull PsiElement element) {
    getTools(toolId, element.getProject()).disableTool(element);
  }

  public void disableToolByDefault(@Nonnull List<String> toolIds, Project project) {
    for (final String toolId : toolIds) {
      getToolDefaultState(toolId, project).setEnabled(false);
    }
  }

  @Nonnull
  public ScopeToolState getToolDefaultState(@Nonnull String toolId, Project project) {
    return getTools(toolId, project).getDefaultState();
  }

  public void enableToolsByDefault(@Nonnull List<String> toolIds, Project project) {
    for (final String toolId : toolIds) {
      getToolDefaultState(toolId, project).setEnabled(true);
    }
  }

  public boolean wasInitialized() {
    return myInitialized;
  }

  public void initInspectionTools(@Nullable Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode() && !INIT_INSPECTIONS) return;
    if (myInitialized) return;
    synchronized (myExternalInfo) {
      if (myInitialized) return;
      myInitialized = initialize(project);
    }
  }

  private boolean initialize(@Nullable Project project) {
    if (myBaseProfile != null) {
      myBaseProfile.initInspectionTools(project);
    }

    final List<InspectionToolWrapper> tools;
    try {
      tools = createTools(project);
    }
    catch (ProcessCanceledException ignored) {
      return false;
    }
    final Map<String, List<String>> dependencies = new HashMap<String, List<String>>();
    for (InspectionToolWrapper toolWrapper : tools) {
      final String shortName = toolWrapper.getShortName();
      HighlightDisplayKey key = HighlightDisplayKey.find(shortName);
      if (key == null) {
        final InspectionEP extension = toolWrapper.getExtension();
        Computable<String> computable = extension == null ? new Computable.PredefinedValueComputable<String>(toolWrapper.getDisplayName()) : new Computable<String>() {
          @Override
          public String compute() {
            return extension.getDisplayName();
          }
        };
        if (toolWrapper instanceof LocalInspectionToolWrapper) {
          key = HighlightDisplayKey.register(shortName, computable, ((LocalInspectionToolWrapper)toolWrapper).getID(),
                                             ((LocalInspectionToolWrapper)toolWrapper).getAlternativeID());
        }
        else {
          key = HighlightDisplayKey.register(shortName, computable);
        }
      }

      LOG.assertTrue(key != null, shortName + " ; number of initialized tools: " + myTools.size());
      HighlightDisplayLevel level = myBaseProfile != null ? myBaseProfile.getErrorLevel(key, project) : toolWrapper.getDefaultLevel();
      boolean enabled = myBaseProfile != null ? myBaseProfile.isToolEnabled(key) : toolWrapper.isEnabledByDefault();
      final ToolsImpl toolsList = new ToolsImpl(toolWrapper, level, !myLockedProfile && enabled, enabled);
      final Element element = myUninstalledInspectionsSettings.remove(shortName);
      try {
        if (element != null) {
          toolsList.readExternal(element, this, dependencies);
        }
        else if (!myUninstalledInspectionsSettings.containsKey(InspectionElementsMerger.getMergedMarkerName(shortName))) {
          final InspectionElementsMerger merger = getMergers().get(shortName);
          if (merger != null) {
            final Element merged = merger.merge(myUninstalledInspectionsSettings);
            if (merged != null) {
              toolsList.readExternal(merged, this, dependencies);
            }
          }
        }
      }
      catch (InvalidDataException e) {
        LOG.error("Can't read settings for " + toolWrapper, e);
      }
      myTools.put(toolWrapper.getShortName(), toolsList);
    }
    final GraphGenerator<String> graphGenerator = GraphGenerator.create(CachingSemiGraph.create(new GraphGenerator.SemiGraph<String>() {
      @Override
      public Collection<String> getNodes() {
        return dependencies.keySet();
      }

      @Override
      public Iterator<String> getIn(String n) {
        return dependencies.get(n).iterator();
      }
    }));

    DFSTBuilder<String> builder = new DFSTBuilder<String>(graphGenerator);
    if (builder.isAcyclic()) {
      final List<String> scopes = builder.getSortedNodes();
      myScopesOrder = ArrayUtil.toStringArray(scopes);
    }

    if (mySource != null) {
      copyToolsConfigurations(mySource, project);
    }
    return true;
  }

  @javax.annotation.Nullable
  @Transient
  public String[] getScopesOrder() {
    return myScopesOrder;
  }

  public void setScopesOrder(String[] scopesOrder) {
    myScopesOrder = scopesOrder;
  }

  @Nonnull
  private List<InspectionToolWrapper> createTools(Project project) {
    if (mySource != null) {
      return ContainerUtil.map(mySource.getDefaultStates(project), new Function<ScopeToolState, InspectionToolWrapper>() {
        @Nonnull
        @Override
        public InspectionToolWrapper fun(@Nonnull ScopeToolState state) {
          return state.getTool();
        }
      });
    }
    //noinspection TestOnlyProblems
    return myRegistrar.createTools();
  }

  private HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey key, Project project) {
    final ToolsImpl tools = getTools(key.toString(), project);
    LOG.assertTrue(tools != null, "profile name: " + myName +  " base profile: " + (myBaseProfile != null ? myBaseProfile.getName() : "-") + " key: " + key);
    return tools.getLevel();
  }

  @Override
  @Nonnull
  public ModifiableModel getModifiableModel() {
    InspectionProfileImpl modifiableModel = new InspectionProfileImpl(this);
    modifiableModel.myExternalInfo.copy(myExternalInfo);
    return modifiableModel;
  }

  @Override
  public void copyFrom(@Nonnull InspectionProfile profile) {
    super.copyFrom(profile);

    myBaseProfile = ((InspectionProfileImpl)profile).myBaseProfile;
  }

  private void copyToolsConfigurations(@Nonnull InspectionProfileImpl profile, @Nullable Project project) {
    try {
      for (ToolsImpl toolList : profile.myTools.values()) {
        final ToolsImpl tools = myTools.get(toolList.getShortName());
        final ScopeToolState defaultState = toolList.getDefaultState();
        tools.setDefaultState(copyToolSettings(defaultState.getTool()), defaultState.isEnabled(), defaultState.getLevel());
        tools.removeAllScopes();
        final List<ScopeToolState> nonDefaultToolStates = toolList.getNonDefaultTools();
        if (nonDefaultToolStates != null) {
          for (ScopeToolState state : nonDefaultToolStates) {
            final InspectionToolWrapper toolWrapper = copyToolSettings(state.getTool());
            final NamedScope scope = state.getScope(project);
            if (scope != null) {
              tools.addTool(scope, toolWrapper, state.isEnabled(), state.getLevel());
            }
            else {
              tools.addTool(state.getScopeName(), toolWrapper, state.isEnabled(), state.getLevel());
            }
          }
        }
        tools.setEnabled(toolList.isEnabled());
      }
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  @Override
  public void cleanup(@Nonnull Project project) {
    for (final ToolsImpl toolList : myTools.values()) {
      if (toolList.isEnabled()) {
        for (InspectionToolWrapper toolWrapper : toolList.getAllTools()) {
          toolWrapper.projectClosed(project);
          toolWrapper.cleanup(project);
        }
      }
    }
  }

  public void enableTool(@Nonnull String toolId, Project project) {
    final ToolsImpl tools = getTools(toolId, project);
    tools.setEnabled(true);
    if (tools.getNonDefaultTools() == null) {
      tools.getDefaultState().setEnabled(true);
    }
  }

  @Override
  public void enableTool(@Nonnull String inspectionTool, NamedScope namedScope, Project project) {
    getTools(inspectionTool, project).enableTool(namedScope, project);
  }

  public void enableTools(@Nonnull List<String> inspectionTools, NamedScope namedScope, Project project) {
    for (String inspectionTool : inspectionTools) {
      enableTool(inspectionTool, namedScope, project);
    }
  }

  @Override
  public void disableTool(@Nonnull String inspectionTool, NamedScope namedScope, @Nonnull Project project) {
    getTools(inspectionTool, project).disableTool(namedScope, project);
  }

  public void disableTools(@Nonnull List<String> inspectionTools, NamedScope namedScope, @Nonnull Project project) {
    for (String inspectionTool : inspectionTools) {
      disableTool(inspectionTool, namedScope, project);
    }
  }

  @Override
  public void disableTool(@Nonnull String inspectionTool, Project project) {
    final ToolsImpl tools = getTools(inspectionTool, project);
    tools.setEnabled(false);
    if (tools.getNonDefaultTools() == null) {
      tools.getDefaultState().setEnabled(false);
    }
  }

  @Override
  public void setErrorLevel(@Nonnull HighlightDisplayKey key, @Nonnull HighlightDisplayLevel level, Project project) {
    getTools(key.toString(), project).setLevel(level);
  }

  @Override
  public boolean isToolEnabled(HighlightDisplayKey key, PsiElement element) {
    if (key == null) {
      return false;
    }
    final Tools toolState = getTools(key.toString(), element == null ? null : element.getProject());
    return toolState != null && toolState.isEnabled(element);
  }

  @Override
  public boolean isToolEnabled(HighlightDisplayKey key) {
    return isToolEnabled(key, null);
  }

  @Override
  public boolean isExecutable(Project project) {
    initInspectionTools(project);
    for (Tools tools : myTools.values()) {
      if (tools.isEnabled()) return true;
    }
    return false;
  }

  //invoke when isChanged() == true
  @Override
  public void commit() throws IOException {
    LOG.assertTrue(mySource != null);
    mySource.commit(this);
    getProfileManager().updateProfile(mySource);
    mySource = null;
  }

  private void commit(@Nonnull InspectionProfileImpl inspectionProfile) {
    setName(inspectionProfile.getName());
    setDescription(inspectionProfile.getDescription());
    setProjectLevel(inspectionProfile.isProjectLevel());
    myLockedProfile = inspectionProfile.myLockedProfile;
    myDisplayLevelMap = inspectionProfile.myDisplayLevelMap;
    myBaseProfile = inspectionProfile.myBaseProfile;
    myTools = inspectionProfile.myTools;
    myProfileManager = inspectionProfile.getProfileManager();

    myExternalInfo.copy(inspectionProfile.getExternalInfo());

    InspectionProfileManager.getInstance().fireProfileChanged(inspectionProfile);
  }

  @Tag
  public String getDescription() {
    return myDescription;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  @Override
  public void convert(@Nonnull Element element, @Nonnull Project project) {
    initInspectionTools(project);
    final Element scopes = element.getChild(DefaultProjectProfileManager.SCOPES);
    if (scopes == null) {
      return;
    }
    final List children = scopes.getChildren(SCOPE);
    for (Object s : children) {
      Element scopeElement = (Element)s;
      final String profile = scopeElement.getAttributeValue(DefaultProjectProfileManager.PROFILE);
      if (profile != null) {
        final InspectionProfileImpl inspectionProfile = (InspectionProfileImpl)getProfileManager().getProfile(profile);
        if (inspectionProfile != null) {
          final NamedScope scope = getProfileManager().getScopesManager().getScope(scopeElement.getAttributeValue(NAME));
          if (scope != null) {
            for (InspectionToolWrapper toolWrapper : inspectionProfile.getInspectionTools(null)) {
              final HighlightDisplayKey key = HighlightDisplayKey.find(toolWrapper.getShortName());
              try {
                InspectionToolWrapper toolWrapperCopy = copyToolSettings(toolWrapper);
                HighlightDisplayLevel errorLevel = inspectionProfile.getErrorLevel(key, null, project);
                getTools(toolWrapper.getShortName(), project).addTool(scope, toolWrapperCopy, inspectionProfile.isToolEnabled(key), errorLevel);
              }
              catch (Exception e) {
                LOG.error(e);
              }
            }
          }
        }
      }
    }
    reduceConvertedScopes();
  }

  private void reduceConvertedScopes() {
    for (ToolsImpl tools : myTools.values()) {
      final ScopeToolState toolState = tools.getDefaultState();
      final List<ScopeToolState> nonDefaultTools = tools.getNonDefaultTools();
      if (nonDefaultTools != null) {
        boolean equal = true;
        boolean isEnabled = toolState.isEnabled();
        for (ScopeToolState state : nonDefaultTools) {
          isEnabled |= state.isEnabled();
          if (!state.equalTo(toolState)) {
            equal = false;
          }
        }
        tools.setEnabled(isEnabled);
        if (equal) {
          tools.removeAllScopes();
        }
      }
    }
  }

  @Override
  @Nonnull
  public ExternalInfo getExternalInfo() {
    return myExternalInfo;
  }

  @Nonnull
  public List<ScopeToolState> getAllTools(Project project) {
    initInspectionTools(project);
    final List<ScopeToolState> result = new ArrayList<ScopeToolState>();
    for (Tools tools : myTools.values()) {
      result.addAll(tools.getTools());
    }
    return result;
  }

  @Nonnull
  public List<ScopeToolState> getDefaultStates(Project project) {
    initInspectionTools(project);
    final List<ScopeToolState> result = new ArrayList<ScopeToolState>();
    for (Tools tools : myTools.values()) {
      result.add(tools.getDefaultState());
    }
    return result;
  }

  @Nonnull
  public List<ScopeToolState> getNonDefaultTools(@Nonnull String shortName, Project project) {
    final List<ScopeToolState> result = new ArrayList<ScopeToolState>();
    final List<ScopeToolState> nonDefaultTools = getTools(shortName, project).getNonDefaultTools();
    if (nonDefaultTools != null) {
      result.addAll(nonDefaultTools);
    }
    return result;
  }

  public boolean isToolEnabled(@Nonnull HighlightDisplayKey key, NamedScope namedScope, Project project) {
    return getTools(key.toString(), project).isEnabled(namedScope,project);
  }

  @Deprecated
  public void removeScope(@Nonnull String toolId, int scopeIdx, Project project) {
    getTools(toolId, project).removeScope(scopeIdx);
  }

  public void removeScope(@Nonnull String toolId, @Nonnull String scopeName, Project project) {
    getTools(toolId, project).removeScope(scopeName);
  }

  public void removeScopes(@Nonnull List<String> toolIds, @Nonnull String scopeName, Project project) {
    for (final String toolId : toolIds) {
      removeScope(toolId, scopeName, project);
    }
  }

  /**
   * @return null if it has no base profile
   */
  @Nullable
  private Map<String, Boolean> getDisplayLevelMap() {
    if (myBaseProfile == null) return null;
    if (myDisplayLevelMap == null) {
      initInspectionTools(null);
      myDisplayLevelMap = new TreeMap<String, Boolean>();
      for (String toolId : myTools.keySet()) {
        myDisplayLevelMap.put(toolId, toolSettingsAreEqual(toolId, myBaseProfile, this));
      }
    }
    return myDisplayLevelMap;
  }

  @Override
  public void profileChanged() {
    myDisplayLevelMap = null;
  }

  @Nonnull
  @Transient
  public HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey key, NamedScope scope, Project project) {
    final ToolsImpl tools = getTools(key.toString(), project);
    return tools != null ? tools.getLevel(scope, project) : HighlightDisplayLevel.WARNING;
  }

  public ScopeToolState addScope(@Nonnull InspectionToolWrapper toolWrapper,
                                 NamedScope scope,
                                 @Nonnull HighlightDisplayLevel level,
                                 boolean enabled,
                                 Project project) {
    return getTools(toolWrapper.getShortName(), project).prependTool(scope, toolWrapper, enabled, level);
  }

  public void setErrorLevel(@Nonnull HighlightDisplayKey key, @Nonnull HighlightDisplayLevel level, String scopeName, Project project) {
    getTools(key.toString(), project).setLevel(level, scopeName, project);
  }

  public void setErrorLevel(@Nonnull List<HighlightDisplayKey> keys, @Nonnull HighlightDisplayLevel level, String scopeName, Project project) {
    for (HighlightDisplayKey key : keys) {
      setErrorLevel(key, level, scopeName, project);
    }
  }

  public ToolsImpl getTools(@Nonnull String toolId, Project project) {
    initInspectionTools(project);
    return myTools.get(toolId);
  }

  public void enableAllTools(Project project) {
    for (InspectionToolWrapper entry : getInspectionTools(null)) {
      enableTool(entry.getShortName(), project);
    }
  }

  public void disableAllTools(Project project) {
    for (InspectionToolWrapper entry : getInspectionTools(null)) {
      disableTool(entry.getShortName(), project);
    }
  }

  @Override
  @Nonnull
  public String toString() {
    return mySource == null ? getName() : getName() + " (copy)";
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && ((InspectionProfileImpl)o).getProfileManager() == getProfileManager();
  }

  private static class InspectionProfileImplHolder {
    private static final InspectionProfileImpl DEFAULT_PROFILE = new InspectionProfileImpl("Default");
  }
}
