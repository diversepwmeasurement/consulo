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
package com.intellij.openapi.compiler;

import com.intellij.openapi.module.Module;
import consulo.virtualFileSystem.VirtualFile;

/**
 * A base interface for all compilers that generate new files. The generated files may be processed by other compilers.
 * Actual implementation should implement one of its subinterfaces. Currently only {@link SourceGeneratingCompiler} is available.
 */
public interface GeneratingCompiler extends Compiler, ValidityStateFactory, IntermediateOutputCompiler {
  /**
   * Represents a single item generated by the compiler.
   */
  interface GenerationItem {
    /**
     * Returns the path of the generated file.
     *
     * @return path of a generated file, relative to output directory.
     */
    String getPath();

    /**
     * Returns the object describing dependencies of the generated file.
     *
     * @return a serializable object describing dependencies of the generated file.
     */
    ValidityState getValidityState();

    /**
     * Returns the module to which the generated item belongs. This affects the sequence
     * of compiling the generated files.
     *
     * @return the module to which the generated item belongs.
     */
    Module getModule();

    /**
     * @return true if the generated item is supposed to be located in test sources, false otherwise
     */
    boolean isTestSource();
  }

  /**
   * Returns the list of all the files this compiler can generate.
   *
   * @param context the current compile context.
   * @return items describing all the files this compiler can generate.
   */
  GenerationItem[] getGenerationItems(CompileContext context);

  /**
   * Invokes the generation process.
   *
   * @param context             the current compile context.
   * @param items               what items to generate.
   * @param outputRootDirectory the root directory under which the items are generated (the paths
   *                            in {@link GenerationItem#getPath()} are relative to that directory).
   *                            All files generated by the compiler must be placed in that directory or
   *                            its subdirectories, otherwise they will not be compiled properly on
   *                            subsequent build steps.
   * @return successfully generated items
   */
  GenerationItem[] generate(CompileContext context, GenerationItem[] items, VirtualFile outputRootDirectory);
}
