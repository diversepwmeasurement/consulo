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
package consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes;

import consulo.execution.debug.frame.XStackFrame;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTree;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class XStackFrameNode extends XValueContainerNode<XStackFrame> {
  public XStackFrameNode(final @Nonnull XDebuggerTree tree, final @Nonnull XStackFrame xStackFrame) {
    super(tree, null, xStackFrame);
    setLeaf(false);
  }
}
