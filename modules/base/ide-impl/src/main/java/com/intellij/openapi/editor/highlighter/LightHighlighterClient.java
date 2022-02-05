/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.editor.highlighter;

import consulo.document.Document;
import consulo.language.editor.HighlighterClient;
import consulo.project.Project;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 9/8/11
 * Time: 12:18 PM
 */
public class LightHighlighterClient implements HighlighterClient {
  private final Project myProject;
  private final Document myDocument;

  public LightHighlighterClient(Document document, Project project) {
    myDocument = document;
    myProject = project;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void repaint(int start, int end) {
  }

  @Override
  public Document getDocument() {
    return myDocument;
  }
}
