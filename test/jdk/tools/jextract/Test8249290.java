/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.nio.file.Path;

import jdk.incubator.foreign.Addressable;
import org.testng.annotations.Test;

/*
 * @test
 * @library /test/lib
 * @modules jdk.incubator.jextract
 * @build JextractToolRunner
 * @bug 8249290
 * @summary jextract does not handle void typedef in function pointer argument
 * @run testng/othervm --enable-native-access=jdk.incubator.jextract Test8249290
 */
public class Test8249290 extends JextractToolRunner {
    @Test
    public void testVoidTypedef() {
        Path outputPath = getOutputFilePath("output8249290");
        Path headerFile = getInputFilePath("test8249290.h");
        run("-d", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(Loader loader = classLoader(outputPath)) {
            Class<?> headerClass = loader.loadClass("test8249290_h");
            checkMethod(headerClass, "func", void.class, Addressable.class);
            Class<?> fiClass = loader.loadClass("test8249290_h$func$f");
            checkMethod(fiClass, "apply", void.class);
        } finally {
            deleteDir(outputPath);
        }
    }
}
