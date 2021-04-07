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

import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static test.jextract.test8254983.test8254983_h.*;

/*
 * @test id=classes
 * @library ..
 * @modules jdk.incubator.jextract
 * @bug 8254983
 * @summary jextract fails to hande layout paths nested structs/union
 * @run driver JtregJextract -t test.jextract.test8254983 -- test8254983.h
 * @run testng/othervm -Dforeign.restricted=permit LibTest8254983Test
 */
/*
 * @test id=sources
 * @library ..
 * @modules jdk.incubator.jextract
 * @bug 8254983
 * @summary jextract fails to hande layout paths nested structs/union
 * @run driver JtregJextractSources -t test.jextract.test8254983 -- test8254983.h
 * @run testng/othervm -Dforeign.restricted=permit LibTest8254983Test
 */
public class LibTest8254983Test {
    @Test
    public void testOuterStruct() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            assertEquals(((GroupLayout)Foo._struct.$LAYOUT()).memberLayouts().size(), 1);
            MemorySegment str = Foo._struct.allocate(scope);
            Foo._struct.x$set(str, 42);
            assertEquals(Foo._struct.x$get(str), 42);
        }
    }

    @Test
    public void testInnerStruct() {
        assertEquals(((GroupLayout)Foo._union._struct.$LAYOUT()).memberLayouts().size(), 2);
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment str = Foo._union._struct.allocate(scope);
            Foo._union._struct.x$set(str, 42);
            assertEquals(Foo._union._struct.x$get(str), 42);
        }
    }
}
