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

import java.nio.file.Path;
import org.testng.annotations.Test;
import jdk.incubator.foreign.MemorySegment;
import static org.testng.Assert.assertNotNull;

/*
 * @test
 * @library .. /test/lib
 * @modules jdk.incubator.jextract
 * @build JextractToolRunner
 * @bug 8260929
 * @summary jextract crashes with Crossing storage unit boundaries
 * @run testng/othervm --enable-native-access=jdk.incubator.jextract,ALL-UNNAMED Test8261578
 */
public class Test8261578 extends JextractToolRunner {
    @Test
    public void test1() {
        Path outputPath = getOutputFilePath("output_1");
        Path headerFile = getInputFilePath("test8261578_1.h");
        run("-d", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(Loader loader = classLoader(outputPath)) {
            Class<?> ndpi_class = loader.loadClass("test8261578_1_h$ndpi_flow_tcp_struct");
            assertNotNull(ndpi_class);

            checkMethod(ndpi_class, "gnutella_msg_id$slice", MemorySegment.class, MemorySegment.class);
        } finally {
            deleteDir(outputPath);
        }
    }

    @Test
    public void test2() {
        Path outputPath = getOutputFilePath("output_2");
        Path headerFile = getInputFilePath("test8261578_2.h");
        run("-d", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(Loader loader = classLoader(outputPath)) {
            Class<?> foo_class = loader.loadClass("test8261578_2_h$foo");
            assertNotNull(foo_class);

            checkMethod(foo_class, "clear_color$slice", MemorySegment.class, MemorySegment.class);
            checkMethod(foo_class, "clear_z$get", int.class, MemorySegment.class);
            checkMethod(foo_class, "clear_z$set", void.class, MemorySegment.class, int.class);
            checkMethod(foo_class, "clear_s$get", byte.class, MemorySegment.class);
            checkMethod(foo_class, "clear_s$set", void.class, MemorySegment.class, byte.class);
        } finally {
            deleteDir(outputPath);
        }
    }

    @Test
    public void test3() {
        Path outputPath = getOutputFilePath("output_3");
        Path headerFile = getInputFilePath("test8261578_3.h");
        run("-d", outputPath.toString(), headerFile.toString()).checkSuccess();
        try(Loader loader = classLoader(outputPath)) {
            Class<?> plugin_class = loader.loadClass("test8261578_3_h$PluginCodec_H323AudioG7231AnnexC");
            assertNotNull(plugin_class);

            checkMethod(plugin_class, "maxAl_sduAudioFrames$get", byte.class, MemorySegment.class);
            checkMethod(plugin_class, "maxAl_sduAudioFrames$set", void.class, MemorySegment.class, byte.class);
        } finally {
            deleteDir(outputPath);
        }
    }
}
