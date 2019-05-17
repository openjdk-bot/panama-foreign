/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @bug 8210911
 * @bug 8216268
 * @summary jextract does not handle redundant forward, backward declarations of struct, union, enum properly
 * @modules jdk.jextract
 * @build JextractToolRunner
 * @run testng RedundantDeclsTest
 */
public class RedundantDeclsTest extends JextractToolRunner {
    @Test
    public void redundantDecls() {
        Path clzPath = getOutputFilePath("RedundentDecls.jar");
        run("-o", clzPath.toString(),
                getInputFilePath("redundantDecls.h").toString()).checkSuccess();
        try(Loader loader = classLoader(clzPath)) {
            Class<?> headerCls = loader.loadClass(headerInterfaceName("redundantDecls.h"));
            Class<?>[] inners = headerCls.getDeclaredClasses();
            assertEquals(inners.length, 4);

            Class<?> pointStruct = loader.loadClass(structInterfaceName("redundantDecls.h", "Point"));
            assertNotNull(findStructFieldGet(pointStruct, "i"));
            assertNotNull(findStructFieldGet(pointStruct, "j"));

            Class<?> point3DStruct = loader.loadClass(structInterfaceName("redundantDecls.h", "Point3D"));
            assertNotNull(findStructFieldGet(point3DStruct, "i"));
            assertNotNull(findStructFieldGet(point3DStruct, "j"));
            assertNotNull(findStructFieldGet(point3DStruct, "k"));

            assertNotNull(findEnumConstGet(headerCls, "R"));
            assertNotNull(findEnumConstGet(headerCls, "G"));
            assertNotNull(findEnumConstGet(headerCls, "B"));

            assertNotNull(findEnumConstGet(headerCls, "C"));
            assertNotNull(findEnumConstGet(headerCls, "M"));
            assertNotNull(findEnumConstGet(headerCls, "Y"));

            Class<?> rgbColor = loader.loadClass(enumInterfaceName("redundantDecls.h", "RGBColor"));
            assertNotNull(rgbColor);
            assertTrue(rgbColor.isAnnotation());

            Class<?> cmyColor = loader.loadClass(enumInterfaceName("redundantDecls.h", "CMYColor"));
            assertNotNull(cmyColor);
            assertTrue(cmyColor.isAnnotation());
        } finally {
            deleteFile(clzPath);
        }
    }

    @Test
    public void repeatedDecls() {
        Path clzPath = getOutputFilePath("RepeatedDecls.jar");
        try(Loader loader = classLoader(clzPath)) {
            run("-o", clzPath.toString(),
                getInputFilePath("repeatedDecls.h").toString()).checkSuccess();
            Class<?> headerCls = loader.loadClass(headerInterfaceName("repeatedDecls.h"));
            Class<?>[] inners = headerCls.getDeclaredClasses();
            assertEquals(inners.length, 1);
            Method funcMethod = findMethod(headerCls, "func", int.class);
            assertNotNull(funcMethod);
            Method iGetMethod = findMethod(headerCls, "i$get");
            assertNotNull(iGetMethod);
        } finally {
            deleteFile(clzPath);
        }
    }
}