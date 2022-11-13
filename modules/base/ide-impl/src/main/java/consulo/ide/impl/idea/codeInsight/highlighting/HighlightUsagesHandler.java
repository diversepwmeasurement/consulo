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

package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.language.editor.IdentifierUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.editor.hint.HintManager;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.highlight.ReadWriteAccessDetector;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerFactory;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.pom.PsiDeclaredTarget;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.usage.UsageTarget;
import consulo.usage.UsageTargetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HighlightUsagesHandler extends HighlightHandlerBase {
  private static final Logger LOG = Logger.getInstance(HighlightUsagesHandler.class);

  public static void invoke(@Nonnull final Project project, @Nonnull final Editor editor, final PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (file == null && !selectionModel.hasSelection()) {
      selectionModel.selectWordAtCaret(false);
    }
    if (file == null || selectionModel.hasSelection()) {
      doRangeHighlighting(editor, project);
      return;
    }

    final HighlightUsagesHandlerBase handler = createCustomHandler(editor, file);
    if (handler != null) {
      final String featureId = handler.getFeatureId();

      if (featureId != null) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(featureId);
      }

      handler.highlightUsages(() -> performHighlighting(editor, handler));
      return;
    }

    DumbService.getInstance(project).withAlternativeResolveEnabled(() -> {
      UsageTarget[] usageTargets = getUsageTargets(editor, file);
      if (usageTargets == null) {
        handleNoUsageTargets(file, editor, selectionModel, project);
        return;
      }

      boolean clearHighlights = isClearHighlights(editor);
      for (UsageTarget target : usageTargets) {
        target.highlightUsages(file, editor, clearHighlights);
      }
    });
  }

  private static void performHighlighting(@Nonnull Editor editor, @Nonnull HighlightUsagesHandlerBase<? extends PsiElement> handlerBase) {
    String hintText = handlerBase.getHintText();
    String statusText = handlerBase.getStatusText();
    List<TextRange> readUsages = handlerBase.getReadUsages();
    List<TextRange> writeUsages = handlerBase.getWriteUsages();

    boolean clearHighlights = HighlightUsagesHandler.isClearHighlights(editor);
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
    HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(editor.getProject()), editor, attributes, clearHighlights, readUsages);
    HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(editor.getProject()), editor, writeAttributes, clearHighlights, writeUsages);
    if (!clearHighlights) {
      WindowManager.getInstance().getStatusBar(editor.getProject()).setInfo(statusText);

      HighlightHandlerBase.setupFindModel(editor.getProject()); // enable f3 navigation
    }
    if (hintText != null) {
      HintManager.getInstance().showInformationHint(editor, hintText);
    }
  }

  @Nullable
  private static UsageTarget[] getUsageTargets(@Nonnull Editor editor, PsiFile file) {
    UsageTarget[] usageTargets = UsageTargetUtil.findUsageTargets(editor, file);

    if (usageTargets == null) {
      PsiElement targetElement = getTargetElement(editor, file);
      if (targetElement != null && targetElement != file) {
        if (!(targetElement instanceof NavigationItem)) {
          targetElement = targetElement.getNavigationElement();
        }
        if (targetElement instanceof NavigationItem) {
          usageTargets = new UsageTarget[]{new PsiElement2UsageTargetAdapter(targetElement)};
        }
      }
    }

    if (usageTargets == null) {
      PsiReference ref = TargetElementUtil.findReference(editor);

      if (ref instanceof PsiPolyVariantReference) {
        ResolveResult[] results = ((PsiPolyVariantReference)ref).multiResolve(false);

        if (results.length > 0) {
          usageTargets = ContainerUtil.mapNotNull(results, result -> {
            PsiElement element = result.getElement();
            return element == null ? null : new PsiElement2UsageTargetAdapter(element);
          }, UsageTarget.EMPTY_ARRAY);
        }
      }
    }
    return usageTargets;
  }

  private static void handleNoUsageTargets(PsiFile file, @Nonnull Editor editor, SelectionModel selectionModel, @Nonnull Project project) {
    if (file.findElementAt(editor.getCaretModel().getOffset()) instanceof PsiWhiteSpace) return;
    selectionModel.selectWordAtCaret(false);
    String selection = selectionModel.getSelectedText();
    LOG.assertTrue(selection != null);
    for (int i = 0; i < selection.length(); i++) {
      if (!Character.isJavaIdentifierPart(selection.charAt(i))) {
        selectionModel.removeSelection();
      }
    }

    doRangeHighlighting(editor, project);
    selectionModel.removeSelection();
  }

  @Nullable
  public static HighlightUsagesHandlerBase createCustomHandler(@Nonnull Editor editor, @Nonnull PsiFile file) {
    for (HighlightUsagesHandlerFactory factory : HighlightUsagesHandlerFactory.EP_NAME.getExtensionList()) {
      final HighlightUsagesHandlerBase handler = factory.createHighlightUsagesHandler(editor, file);
      if (handler != null) {
        return handler;
      }
    }
    return null;
  }

  @Nullable
  @RequiredUIAccess
  private static PsiElement getTargetElement(Editor editor, PsiFile file) {
    PsiElement target = TargetElementUtil.findTargetElement(editor, TargetElementUtil.getReferenceSearchFlags());

    if (target == null) {
      int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
      PsiElement element = file.findElementAt(offset);
      if (element == null) return null;
    }

    return target;
  }

  private static void doRangeHighlighting(Editor editor, Project project) {
    if (!editor.getSelectionModel().hasSelection()) return;

    final String text = editor.getSelectionModel().getSelectedText();
    if (text == null) return;

    if (editor instanceof EditorWindow) {
      // highlight selection in the whole editor, not injected fragment only
      editor = ((EditorWindow)editor).getDelegate();
    }

    EditorSearchSession oldSearch = EditorSearchSession.get(editor);
    if (oldSearch != null) {
      if (oldSearch.hasMatches()) {
        String oldText = oldSearch.getTextInField();
        if (!oldSearch.getFindModel().isRegularExpressions()) {
          oldText = StringUtil.escapeToRegexp(oldText);
          oldSearch.getFindModel().setRegularExpressions(true);
        }

        String newText = oldText + '|' + StringUtil.escapeToRegexp(text);
        oldSearch.setTextInField(newText);
        return;
      }
    }

    EditorSearchSession.start(editor, project).getFindModel().setRegularExpressions(false);
  }

  public static class DoHighlightRunnable implements Runnable {
    private final List<PsiReference> myRefs;
    private final Project myProject;
    private final PsiElement myTarget;
    private final Editor myEditor;
    private final PsiFile myFile;
    private final boolean myClearHighlights;

    public DoHighlightRunnable(@Nonnull List<PsiReference> refs, @Nonnull Project project, @Nonnull PsiElement target, Editor editor, PsiFile file, boolean clearHighlights) {
      myRefs = refs;
      myProject = project;
      myTarget = target;
      myEditor = editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
      myFile = file;
      myClearHighlights = clearHighlights;
    }

    @Override
    public void run() {
      highlightReferences(myProject, myTarget, myRefs, myEditor, myFile, myClearHighlights);
      setStatusText(myProject, getElementName(myTarget), myRefs.size(), myClearHighlights);
    }
  }

  public static void highlightOtherOccurrences(final List<PsiElement> otherOccurrences, Editor editor, boolean clearHighlights) {
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);

    PsiElement[] elements = PsiUtilCore.toPsiElementArray(otherOccurrences);
    doHighlightElements(editor, elements, attributes, clearHighlights);
  }

  public static void highlightReferences(@Nonnull Project project, @Nonnull PsiElement element, @Nonnull List<PsiReference> refs, Editor editor, PsiFile file, boolean clearHighlights) {

    HighlightManager highlightManager = HighlightManager.getInstance(project);
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);

    setupFindModel(project);

    ReadWriteAccessDetector detector = ReadWriteAccessDetector.findDetector(element);

    if (detector != null) {
      List<PsiReference> readRefs = new ArrayList<>();
      List<PsiReference> writeRefs = new ArrayList<>();

      for (PsiReference ref : refs) {
        if (detector.getReferenceAccess(element, ref) == ReadWriteAccessDetector.Access.Read) {
          readRefs.add(ref);
        }
        else {
          writeRefs.add(ref);
        }
      }
      doHighlightRefs(highlightManager, editor, readRefs, attributes, clearHighlights);
      doHighlightRefs(highlightManager, editor, writeRefs, writeAttributes, clearHighlights);
    }
    else {
      doHighlightRefs(highlightManager, editor, refs, attributes, clearHighlights);
    }

    TextRange range = getNameIdentifierRange(file, element);
    if (range != null) {
      TextAttributes nameAttributes = attributes;
      if (detector != null && detector.isDeclarationWriteAccess(element)) {
        nameAttributes = writeAttributes;
      }
      highlightRanges(highlightManager, editor, nameAttributes, clearHighlights, Arrays.asList(range));
    }
  }

  @Nullable
  public static TextRange getNameIdentifierRange(PsiFile file, PsiElement element) {
    final InjectedLanguageManager injectedManager = InjectedLanguageManager.getInstance(element.getProject());
    if (element instanceof PomTargetPsiElement) {
      final PomTarget target = ((PomTargetPsiElement)element).getTarget();
      if (target instanceof PsiDeclaredTarget) {
        final PsiDeclaredTarget declaredTarget = (PsiDeclaredTarget)target;
        final TextRange range = declaredTarget.getNameIdentifierRange();
        if (range != null) {
          if (range.getStartOffset() < 0 || range.getLength() <= 0) {
            return null;
          }
          final PsiElement navElement = declaredTarget.getNavigationElement();
          if (PsiUtilBase.isUnderPsiRoot(file, navElement)) {
            return injectedManager.injectedToHost(navElement, range.shiftRight(navElement.getTextRange().getStartOffset()));
          }
        }
      }
    }

    if (!PsiUtilBase.isUnderPsiRoot(file, element)) {
      return null;
    }

    PsiElement identifier = IdentifierUtil.getNameIdentifier(element);
    if (identifier != null && PsiUtilBase.isUnderPsiRoot(file, identifier)) {
      return injectedManager.injectedToHost(identifier, identifier.getTextRange());
    }
    return null;
  }

  public static void doHighlightElements(Editor editor, PsiElement[] elements, TextAttributes attributes, boolean clearHighlights) {
    HighlightManager highlightManager = HighlightManager.getInstance(editor.getProject());
    List<TextRange> textRanges = new ArrayList<>(elements.length);
    for (PsiElement element : elements) {
      TextRange range = element.getTextRange();
      // injection occurs
      range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
      textRanges.add(range);
    }
    highlightRanges(highlightManager, editor, attributes, clearHighlights, textRanges);
  }

  public static void highlightRanges(HighlightManager highlightManager, Editor editor, TextAttributes attributes, boolean clearHighlights, List<TextRange> textRanges) {
    if (clearHighlights) {
      clearHighlights(editor, highlightManager, textRanges, attributes);
      return;
    }
    ArrayList<RangeHighlighter> highlighters = new ArrayList<>();
    for (TextRange range : textRanges) {
      highlightManager.addRangeHighlight(editor, range.getStartOffset(), range.getEndOffset(), attributes, false, highlighters);
    }
    for (RangeHighlighter highlighter : highlighters) {
      String tooltip = getLineTextErrorStripeTooltip(editor.getDocument(), highlighter.getStartOffset(), true);
      highlighter.setErrorStripeTooltip(tooltip);
    }
  }

  public static boolean isClearHighlights(@Nonnull Editor editor) {
    if (editor instanceof EditorWindow) editor = ((EditorWindow)editor).getDelegate();

    RangeHighlighter[] highlighters = ((HighlightManagerImpl)HighlightManager.getInstance(editor.getProject())).getHighlighters(editor);
    int caretOffset = editor.getCaretModel().getOffset();
    for (RangeHighlighter highlighter : highlighters) {
      if (TextRange.create(highlighter).grown(1).contains(caretOffset)) {
        return true;
      }
    }
    return false;
  }

  private static void clearHighlights(Editor editor, HighlightManager highlightManager, List<TextRange> rangesToHighlight, TextAttributes attributes) {
    if (editor instanceof EditorWindow) editor = ((EditorWindow)editor).getDelegate();
    RangeHighlighter[] highlighters = ((HighlightManagerImpl)highlightManager).getHighlighters(editor);
    Arrays.sort(highlighters, (o1, o2) -> o1.getStartOffset() - o2.getStartOffset());
    Collections.sort(rangesToHighlight, (o1, o2) -> o1.getStartOffset() - o2.getStartOffset());
    int i = 0;
    int j = 0;
    while (i < highlighters.length && j < rangesToHighlight.size()) {
      RangeHighlighter highlighter = highlighters[i];
      TextRange highlighterRange = TextRange.create(highlighter);
      TextRange refRange = rangesToHighlight.get(j);
      if (refRange.equals(highlighterRange) && attributes.equals(highlighter.getTextAttributes()) && highlighter.getLayer() == HighlighterLayer.SELECTION - 1) {
        highlightManager.removeSegmentHighlighter(editor, highlighter);
        i++;
      }
      else if (refRange.getStartOffset() > highlighterRange.getEndOffset()) {
        i++;
      }
      else if (refRange.getEndOffset() < highlighterRange.getStartOffset()) {
        j++;
      }
      else {
        i++;
        j++;
      }
    }
  }

  private static void doHighlightRefs(HighlightManager highlightManager, @Nonnull Editor editor, @Nonnull List<PsiReference> refs, TextAttributes attributes, boolean clearHighlights) {
    List<TextRange> textRanges = new ArrayList<>(refs.size());
    for (PsiReference ref : refs) {
      collectRangesToHighlight(ref, textRanges);
    }
    highlightRanges(highlightManager, editor, attributes, clearHighlights, textRanges);
  }

  /**
   * @deprecated Use {@link #collectRangesToHighlight}
   */
  @Nonnull
  @Deprecated
  public static List<TextRange> getRangesToHighlight(@Nonnull PsiReference ref) {
    return collectRangesToHighlight(ref, new ArrayList<>());
  }

  @Nonnull
  public static List<TextRange> collectRangesToHighlight(@Nonnull PsiReference ref, @Nonnull List<TextRange> result) {
    for (TextRange relativeRange : ReferenceRange.getRanges(ref)) {
      PsiElement element = ref.getElement();
      TextRange range = safeCut(element.getTextRange(), relativeRange);
      // injection occurs
      result.add(InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range));
    }
    return result;
  }

  private static TextRange safeCut(TextRange range, TextRange relative) {
    int start = Math.min(range.getEndOffset(), range.getStartOffset() + relative.getStartOffset());
    int end = Math.min(range.getEndOffset(), range.getStartOffset() + relative.getEndOffset());
    return new TextRange(start, end);
  }

  public static void setStatusText(Project project, final String elementName, int refCount, boolean clearHighlights) {
    String message;
    if (clearHighlights) {
      message = "";
    }
    else if (refCount > 0) {
      message = CodeInsightBundle.message(elementName != null ? "status.bar.highlighted.usages.message" : "status.bar.highlighted.usages.no.target.message", refCount, elementName, getShortcutText());
    }
    else {
      message = CodeInsightBundle.message(elementName != null ? "status.bar.highlighted.usages.not.found.message" : "status.bar.highlighted.usages.not.found.no.target.message", elementName);
    }
    WindowManager.getInstance().getStatusBar(project).setInfo(message);
  }

  private static String getElementName(final PsiElement element) {
    return ElementDescriptionUtil.getElementDescription(element, HighlightUsagesDescriptionLocation.INSTANCE);
  }

  public static String getShortcutText() {
    return HighlightUsagesHandlerBase.getShortcutText();
  }
}
