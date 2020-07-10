/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @modules jdk.incubator.foreign/jdk.incubator.foreign.unsafe
 *          jdk.incubator.foreign/jdk.internal.foreign
 *          jdk.incubator.foreign/jdk.internal.foreign.abi
 *          java.base/sun.security.action
 * @run testng/othervm -Dforeign.restricted=permit TestVarArgs
 */

import jdk.incubator.foreign.CSupport;
import jdk.incubator.foreign.ForeignLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ValueLayout;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

import static jdk.incubator.foreign.CSupport.*;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;
import static org.testng.Assert.assertEquals;

public class TestVarArgs extends NativeTestHelper {

    static final MemoryLayout ML_CallInfo = MemoryLayout.ofStruct(
            C_POINTER.withName("writeback"), // writeback
            C_POINTER.withName("argIDs")); // arg ids

    static final VarHandle VH_CallInfo_writeback = ML_CallInfo.varHandle(long.class, groupElement("writeback"));
    static final VarHandle VH_CallInfo_argIDs = ML_CallInfo.varHandle(long.class, groupElement("argIDs"));

    static final VarHandle VH_IntArray = MemoryLayout.ofSequence(C_INT).varHandle(int.class, sequenceElement());

    static final ForeignLinker abi = CSupport.getSystemLinker();
    static final MemoryAddress varargsAddr;

    static {
        try {
            varargsAddr = LibraryLookup.ofLibrary("VarArgs").lookup("varargs");
        } catch (NoSuchMethodException e) {
            throw new BootstrapMethodError(e);
        }
    }

    static final int WRITEBACK_BYTES_PER_ARG = 8;

    @Test(dataProvider = "args")
    public void testVarArgs(List<VarArg> args) throws Throwable {
        try (MemorySegment writeBack = MemorySegment.allocateNative(args.size() * WRITEBACK_BYTES_PER_ARG);
            MemorySegment callInfo = MemorySegment.allocateNative(ML_CallInfo);
            MemorySegment argIDs = MemorySegment.allocateNative(MemoryLayout.ofSequence(args.size(), C_INT))) {

            MemoryAddress callInfoPtr = callInfo.baseAddress();

            VH_CallInfo_writeback.set(callInfoPtr, writeBack.baseAddress().toRawLongValue());
            VH_CallInfo_argIDs.set(callInfoPtr, argIDs.baseAddress().toRawLongValue());

            for (int i = 0; i < args.size(); i++) {
                VH_IntArray.set(argIDs.baseAddress(), (long) i, args.get(i).id.ordinal());
            }

            List<MemoryLayout> argLayouts = new ArrayList<>();
            argLayouts.add(C_POINTER); // call info
            argLayouts.add(C_INT); // size
            args.forEach(a -> argLayouts.add(asVarArg(a.layout)));

            FunctionDescriptor desc = FunctionDescriptor.ofVoid(argLayouts.toArray(MemoryLayout[]::new));

            List<Class<?>> carriers = new ArrayList<>();
            carriers.add(MemoryAddress.class); // call info
            carriers.add(int.class); // size
            args.forEach(a -> carriers.add(a.carrier));

            MethodType mt = MethodType.methodType(void.class, carriers);

            MethodHandle downcallHandle = abi.downcallHandle(varargsAddr, mt, desc);

            List<Object> argValues = new ArrayList<>();
            argValues.add(callInfoPtr); // call info
            argValues.add(args.size());  // size
            args.forEach(a -> argValues.add(a.value));

            downcallHandle.invokeWithArguments(argValues);

            for (int i = 0; i < args.size(); i++) {
                VarArg a = args.get(i);
                MemoryAddress writtenPtr = writeBack.baseAddress().addOffset(i * WRITEBACK_BYTES_PER_ARG);
                Object written = a.vh.get(writtenPtr);
                assertEquals(written, a.value);
            }
        }
    }

    @DataProvider
    public static Object[][] args() {
        return new Object[][] {
            new Object[] { List.of(VarArg.intArg(5), VarArg.intArg(10), VarArg.intArg(15)) },
            new Object[] { List.of(VarArg.doubleArg(5), VarArg.doubleArg(10), VarArg.doubleArg(15)) },
            new Object[] { List.of(VarArg.intArg(5), VarArg.doubleArg(10), VarArg.intArg(15)) },
        };
    }

    private static final class VarArg {
        final NativeType id;
        final Object value;
        final ValueLayout layout;
        final Class<?> carrier;
        final VarHandle vh;

        private VarArg(NativeType id, ValueLayout layout, Class<?> carrier, Object value) {
            this.id = id;
            this.value = value;
            this.layout = layout;
            this.carrier = carrier;
            this.vh = layout.varHandle(carrier);
        }

        static VarArg intArg(int value) {
            return new VarArg(VarArg.NativeType.INT, C_INT, int.class, value);
        }

        static VarArg doubleArg(double value) {
            return new VarArg(VarArg.NativeType.DOUBLE, C_DOUBLE, double.class, value);
        }

        enum NativeType {
            INT,
            DOUBLE
        }
    }

}
