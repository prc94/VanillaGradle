/*
 * This file is part of VanillaGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.gradle.vanilla.internal.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.spongepowered.gradle.vanilla.internal.Constants;

public class ParchmentLVNamer extends MethodVisitor {
    private final MappingDataContainer.MethodData methodData;

    public ParchmentLVNamer(MethodVisitor methodVisitor, MappingDataContainer.MethodData methodData) {
        super(Constants.ASM_VERSION, methodVisitor);
        this.methodData = methodData;
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        final MappingDataContainer.ParameterData parameterData = methodData.getParameter((byte) (index));
        final String mappedName = parameterData != null ? parameterData.getName() : name;

        super.visitLocalVariable(mappedName, descriptor, signature, start, end, index);
    }
}
