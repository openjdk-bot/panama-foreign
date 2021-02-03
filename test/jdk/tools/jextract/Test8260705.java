/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.nio.file.Path;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @library /test/lib
 * @modules jdk.incubator.jextract
 * @build JextractToolRunner
 * @bug 8260705
 * @summary jextract crash with libbart's types.h
 * @run testng/othervm -Dforeign.restricted=permit Test8260705
 */
public class Test8260705 extends JextractToolRunner {
    @Test
    public void test() {
        Path outputPath = getOutputFilePath("output");
        Path headerFile = getInputFilePath("test8260705.h");
        run("-d", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(Loader loader = classLoader(outputPath)) {
            Class<?> FooClass = loader.loadClass("test8260705_h$Foo");
            checkMethod(FooClass, "c$get", byte.class, MemorySegment.class);
            checkMethod(FooClass, "c$get", byte.class, MemorySegment.class, long.class);
            checkMethod(FooClass, "c$set", void.class, MemorySegment.class, byte.class);
            checkMethod(FooClass, "c$set", void.class, MemorySegment.class, long.class, byte.class);

            Class<?> Foo2Class = loader.loadClass("test8260705_h$Foo2");
            checkMethod(Foo2Class, "z$get", int.class, MemorySegment.class);
            checkMethod(Foo2Class, "z$get", int.class, MemorySegment.class, long.class);
            checkMethod(Foo2Class, "z$set", void.class, MemorySegment.class, int.class);
            checkMethod(Foo2Class, "z$set", void.class, MemorySegment.class, long.class, int.class);
            checkMethod(Foo2Class, "w$get", int.class, MemorySegment.class);
            checkMethod(Foo2Class, "w$get", int.class, MemorySegment.class, long.class);
            checkMethod(Foo2Class, "w$set", void.class, MemorySegment.class, int.class);
            checkMethod(Foo2Class, "w$set", void.class, MemorySegment.class, long.class, int.class);

            assertNotNull(loader.loadClass("test8260705_h$Foo3"));

            Class<?> Foo4Class = loader.loadClass("test8260705_h$Foo4");
            assertTrue(sizeof(Foo4Class) == 8L);

            Class<?> Foo5Class = loader.loadClass("test8260705_h$Foo5");
            assertTrue(sizeof(Foo5Class) == 4L);

        } finally {
            deleteDir(outputPath);
        }
    }

    private long sizeof(Class<?> cls) {
        Method m = findMethod(cls, "sizeof");
        try {
            return (long)m.invoke(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

