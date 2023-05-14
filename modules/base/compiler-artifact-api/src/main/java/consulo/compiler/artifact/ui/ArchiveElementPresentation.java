/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact.ui;

import consulo.application.AllIcons;
import consulo.compiler.artifact.element.ArchivePackagingElement;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentationData;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class ArchiveElementPresentation extends PackagingElementPresentation {
  private final ArchivePackagingElement myElement;

  public ArchiveElementPresentation(ArchivePackagingElement element) {
    myElement = element;
  }

  @Override
  public String getPresentableName() {
    return myElement.getArchiveFileName();
  }

  @Override
  public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    presentationData.setIcon(AllIcons.Nodes.PpJar);
    presentationData.addText(myElement.getArchiveFileName(), mainAttributes);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.FILE_COPY;
  }
}
