/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @run testng TestMemoryAccess
 */

import java.foreign.GroupLayout;
import java.foreign.Layout;
import java.foreign.LayoutPath;
import java.foreign.PaddingLayout;
import java.foreign.SequenceLayout;
import java.foreign.ValueLayout;
import java.foreign.MemoryAddress;
import java.foreign.MemoryScope;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class TestMemoryAccess {

    @Test(dataProvider = "elements")
    public void testAccess(Layout elemLayout, Class<?> carrier, Checker checker) {
        Layout layout = elemLayout.withName("elem");
        testAccessInternal(layout, layout.toPath(), carrier, checker);
    }

    @Test(dataProvider = "elements")
    public void testPaddedAccessByName(Layout elemLayout, Class<?> carrier, Checker checker) {
        Layout layout = GroupLayout.struct(PaddingLayout.of(elemLayout.bitsSize()), elemLayout.withName("elem"));
        testAccessInternal(layout, layout.toPath()
                .elementPath("elem"), carrier, checker);
    }

    @Test(dataProvider = "elements")
    public void testPaddedAccessByIndex(Layout elemLayout, Class<?> carrier, Checker checker) {
        Layout layout = GroupLayout.struct(PaddingLayout.of(elemLayout.bitsSize()), elemLayout.withName("elem"));
        testAccessInternal(layout, layout.toPath()
                .elementPath(1), carrier, checker);
    }

    @Test(dataProvider = "elements")
    public void testPaddedAccessByIndexSeq(Layout elemLayout, Class<?> carrier, Checker checker) {
        Layout layout = SequenceLayout.of(2, elemLayout);
        testAccessInternal(layout, layout.toPath()
                .elementPath(1), carrier, checker);
    }

    @Test(dataProvider = "arrayElements")
    public void testArrayAccess(Layout elemLayout, Class<?> carrier, ArrayChecker checker) {
        SequenceLayout seq = SequenceLayout.of(10, elemLayout.withName("elem"));
        testArrayAccessInternal(seq, seq.toPath()
                .elementPath(), carrier, checker);
    }

    @Test(dataProvider = "arrayElements")
    public void testPaddedArrayAccessByName(Layout elemLayout, Class<?> carrier, ArrayChecker checker) {
        SequenceLayout seq = SequenceLayout.of(10, GroupLayout.struct(PaddingLayout.of(elemLayout.bitsSize()), elemLayout.withName("elem")));
        testArrayAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath("elem"), carrier, checker);
    }

    @Test(dataProvider = "arrayElements")
    public void testPaddedArrayAccessByIndex(Layout elemLayout, Class<?> carrier, ArrayChecker checker) {
        SequenceLayout seq = SequenceLayout.of(10, GroupLayout.struct(PaddingLayout.of(elemLayout.bitsSize()), elemLayout.withName("elem")));
        testArrayAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath(1), carrier, checker);
    }

    @Test(dataProvider = "arrayElements")
    public void testPaddedArrayAccessByIndexSeq(Layout elemLayout, Class<?> carrier, ArrayChecker checker) {
        SequenceLayout seq = SequenceLayout.of(10, SequenceLayout.of(2, elemLayout));
        testArrayAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath(1), carrier, checker);
    }

    private void testAccessInternal(Layout layout, LayoutPath path, Class<?> carrier, Checker checker) {
        VarHandle handle = path.dereferenceHandle(carrier);

        MemoryAddress outer_address;
        try (MemoryScope scope = MemoryScope.globalScope().fork()) {
            MemoryAddress addr = scope.allocate(layout);
            checker.check(handle, addr);
            try {
                checker.check(handle, addr.offset(layout.bitsSize() / 8));
                throw new AssertionError(); //not ok, out of bounds
            } catch (IllegalStateException ex) {
                //ok, should fail (out of bounds)
            }
            outer_address = addr; //leak!
        }
        try {
            checker.check(handle, outer_address);
            throw new AssertionError(); //not ok, scope is closed
        } catch (IllegalStateException ex) {
            //ok, should fail (scope is closed)
        }
    }

    private void testArrayAccessInternal(SequenceLayout seq, LayoutPath path, Class<?> carrier, ArrayChecker checker) {
        VarHandle handle = path.dereferenceHandle(carrier);

        MemoryAddress outer_address;
        try (MemoryScope scope = MemoryScope.globalScope().fork()) {
            MemoryAddress addr = scope.allocate(seq);
            for (int i = 0 ; i < seq.elementsSize().getAsLong() ; i++) {
                checker.check(handle, addr, i);
            }
            try {
                checker.check(handle, addr, seq.elementsSize().getAsLong());
                throw new AssertionError(); //not ok, out of bounds
            } catch (IllegalStateException ex) {
                //ok, should fail (out of bounds)
            }
            outer_address = addr; //leak!
        }
        try {
            checker.check(handle, outer_address, 0);
            throw new AssertionError(); //not ok, scope is closed
        } catch (IllegalStateException ex) {
            //ok, should fail (scope is closed)
        }
    }

    @Test(dataProvider = "matrixElements")
    public void testMatrixAccess(Layout elemLayout, Class<?> carrier, MatrixChecker checker) {
        SequenceLayout seq = SequenceLayout.of(20,
                SequenceLayout.of(10, elemLayout.withName("elem")));
        testMatrixAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath(), carrier, checker);
    }

    @Test(dataProvider = "matrixElements")
    public void testPaddedMatrixAccessByName(Layout elemLayout, Class<?> carrier, MatrixChecker checker) {
        SequenceLayout seq = SequenceLayout.of(20,
                SequenceLayout.of(10, GroupLayout.struct(PaddingLayout.of(elemLayout.bitsSize()), elemLayout.withName("elem"))));
        testMatrixAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath()
                .elementPath("elem"), carrier, checker);
    }

    @Test(dataProvider = "matrixElements")
    public void testPaddedMatrixAccessByIndex(Layout elemLayout, Class<?> carrier, MatrixChecker checker) {
        SequenceLayout seq = SequenceLayout.of(20,
                SequenceLayout.of(10, GroupLayout.struct(PaddingLayout.of(elemLayout.bitsSize()), elemLayout.withName("elem"))));
        testMatrixAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath()
                .elementPath(1), carrier, checker);
    }

    @Test(dataProvider = "matrixElements")
    public void testPaddedMatrixAccessByIndexSeq(Layout elemLayout, Class<?> carrier, MatrixChecker checker) {
        SequenceLayout seq = SequenceLayout.of(20,
                SequenceLayout.of(10, SequenceLayout.of(2, elemLayout)));
        testMatrixAccessInternal(seq, seq.toPath()
                .elementPath()
                .elementPath()
                .elementPath(1), carrier, checker);
    }

    @Test(dataProvider = "badCarriers",
          expectedExceptions = IllegalArgumentException.class)
    public void testBadCarriers(Class<?> carrier) {
        Layout l = ValueLayout.ofUnsignedInt(32).withName("elem");
        l.toPath().dereferenceHandle(carrier);
    }

    private void testMatrixAccessInternal(SequenceLayout seq, LayoutPath path, Class<?> carrier, MatrixChecker checker) {
        VarHandle handle = path.dereferenceHandle(carrier);

        MemoryAddress outer_address;
        try (MemoryScope scope = MemoryScope.globalScope().fork()) {
            MemoryAddress addr = scope.allocate(seq);
            for (int i = 0 ; i < seq.elementsSize().getAsLong() ; i++) {
                for (int j = 0; j < ((SequenceLayout)seq.elementLayout()).elementsSize().getAsLong() ; j++) {
                    checker.check(handle, addr, i, j);
                }
            }
            try {
                checker.check(handle, addr, seq.elementsSize().getAsLong(),
                        ((SequenceLayout)seq.elementLayout()).elementsSize().getAsLong());
                throw new AssertionError(); //not ok, out of bounds
            } catch (IllegalStateException ex) {
                //ok, should fail (out of bounds)
            }
            outer_address = addr; //leak!
        }
        try {
            checker.check(handle, outer_address, 0, 0);
            throw new AssertionError(); //not ok, scope is closed
        } catch (IllegalStateException ex) {
            //ok, should fail (scope is closed)
        }
    }

    @DataProvider(name = "elements")
    public Object[][] createData() {
        return new Object[][] {
                //BE
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 8), byte.class, Checker.BYTE },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 8), byte.class, Checker.BYTE },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 16), short.class, Checker.SHORT },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 16), short.class, Checker.SHORT },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 16), char.class, Checker.CHAR },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 16), char.class, Checker.CHAR },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 32), int.class, Checker.INT },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 32), int.class, Checker.INT },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 64), long.class, Checker.LONG },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 64), long.class, Checker.LONG },
                { ValueLayout.ofFloatingPoint(ByteOrder.BIG_ENDIAN, 32), float.class, Checker.FLOAT },
                { ValueLayout.ofFloatingPoint(ByteOrder.BIG_ENDIAN, 64), double.class, Checker.DOUBLE },
                //LE
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 8), byte.class, Checker.BYTE },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 8), byte.class, Checker.BYTE },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 16), short.class, Checker.SHORT },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 16), short.class, Checker.SHORT },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 16), char.class, Checker.CHAR },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 16), char.class, Checker.CHAR },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 32), int.class, Checker.INT },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 32), int.class, Checker.INT },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 64), long.class, Checker.LONG },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 64), long.class, Checker.LONG },
                { ValueLayout.ofFloatingPoint(ByteOrder.LITTLE_ENDIAN, 32), float.class, Checker.FLOAT },
                { ValueLayout.ofFloatingPoint(ByteOrder.LITTLE_ENDIAN, 64), double.class, Checker.DOUBLE },
        };
    }

    interface Checker {
        void check(VarHandle handle, MemoryAddress addr);

        Checker BYTE = (handle, addr) -> {
            handle.set(addr, (byte)42);
            assertEquals(42, (byte)handle.get(addr));
        };

        Checker SHORT = (handle, addr) -> {
            handle.set(addr, (short)42);
            assertEquals(42, (short)handle.get(addr));
        };

        Checker CHAR = (handle, addr) -> {
            handle.set(addr, (char)42);
            assertEquals(42, (char)handle.get(addr));
        };

        Checker INT = (handle, addr) -> {
            handle.set(addr, 42);
            assertEquals(42, (int)handle.get(addr));
        };

        Checker LONG = (handle, addr) -> {
            handle.set(addr, (long)42);
            assertEquals(42, (long)handle.get(addr));
        };

        Checker FLOAT = (handle, addr) -> {
            handle.set(addr, (float)42);
            assertEquals((float)42, (float)handle.get(addr));
        };

        Checker DOUBLE = (handle, addr) -> {
            handle.set(addr, (double)42);
            assertEquals((double)42, (double)handle.get(addr));
        };
    }

    @DataProvider(name = "arrayElements")
    public Object[][] createArrayData() {
        return new Object[][] {
                //BE
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 8), byte.class, ArrayChecker.BYTE },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 8), byte.class, ArrayChecker.BYTE },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 16), short.class, ArrayChecker.SHORT },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 16), short.class, ArrayChecker.SHORT },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 16), char.class, ArrayChecker.CHAR },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 16), char.class, ArrayChecker.CHAR },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 32), int.class, ArrayChecker.INT },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 32), int.class, ArrayChecker.INT },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 64), long.class, ArrayChecker.LONG },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 64), long.class, ArrayChecker.LONG },
                { ValueLayout.ofFloatingPoint(ByteOrder.BIG_ENDIAN, 32), float.class, ArrayChecker.FLOAT },
                { ValueLayout.ofFloatingPoint(ByteOrder.BIG_ENDIAN, 64), double.class, ArrayChecker.DOUBLE },
                //LE
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 8), byte.class, ArrayChecker.BYTE },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 8), byte.class, ArrayChecker.BYTE },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 16), short.class, ArrayChecker.SHORT },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 16), short.class, ArrayChecker.SHORT },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 16), char.class, ArrayChecker.CHAR },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 16), char.class, ArrayChecker.CHAR },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 32), int.class, ArrayChecker.INT },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 32), int.class, ArrayChecker.INT },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 64), long.class, ArrayChecker.LONG },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 64), long.class, ArrayChecker.LONG },
                { ValueLayout.ofFloatingPoint(ByteOrder.LITTLE_ENDIAN, 32), float.class, ArrayChecker.FLOAT },
                { ValueLayout.ofFloatingPoint(ByteOrder.LITTLE_ENDIAN, 64), double.class, ArrayChecker.DOUBLE },
        };
    }

    interface ArrayChecker {
        void check(VarHandle handle, MemoryAddress addr, long index);

        ArrayChecker BYTE = (handle, addr, i) -> {
            handle.set(addr, i, (byte)i);
            assertEquals(i, (byte)handle.get(addr, i));
        };

        ArrayChecker SHORT = (handle, addr, i) -> {
            handle.set(addr, i, (short)i);
            assertEquals(i, (short)handle.get(addr, i));
        };

        ArrayChecker CHAR = (handle, addr, i) -> {
            handle.set(addr, i, (char)i);
            assertEquals(i, (char)handle.get(addr, i));
        };

        ArrayChecker INT = (handle, addr, i) -> {
            handle.set(addr, i, (int)i);
            assertEquals(i, (int)handle.get(addr, i));
        };

        ArrayChecker LONG = (handle, addr, i) -> {
            handle.set(addr, i, (long)i);
            assertEquals(i, (long)handle.get(addr, i));
        };

        ArrayChecker FLOAT = (handle, addr, i) -> {
            handle.set(addr, i, (float)i);
            assertEquals((float)i, (float)handle.get(addr, i));
        };

        ArrayChecker DOUBLE = (handle, addr, i) -> {
            handle.set(addr, i, (double)i);
            assertEquals((double)i, (double)handle.get(addr, i));
        };
    }

    @DataProvider(name = "matrixElements")
    public Object[][] createMatrixData() {
        return new Object[][] {
                //BE
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 8), byte.class, MatrixChecker.BYTE },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 8), byte.class, MatrixChecker.BYTE },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 16), short.class, MatrixChecker.SHORT },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 16), short.class, MatrixChecker.SHORT },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 16), char.class, MatrixChecker.CHAR },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 16), char.class, MatrixChecker.CHAR },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 32), int.class, MatrixChecker.INT },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 32), int.class, MatrixChecker.INT },
                { ValueLayout.ofUnsignedInt(ByteOrder.BIG_ENDIAN, 64), long.class, MatrixChecker.LONG },
                { ValueLayout.ofSignedInt(ByteOrder.BIG_ENDIAN, 64), long.class, MatrixChecker.LONG },
                { ValueLayout.ofFloatingPoint(ByteOrder.BIG_ENDIAN, 32), float.class, MatrixChecker.FLOAT },
                { ValueLayout.ofFloatingPoint(ByteOrder.BIG_ENDIAN, 64), double.class, MatrixChecker.DOUBLE },
                //LE
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 8), byte.class, MatrixChecker.BYTE },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 8), byte.class, MatrixChecker.BYTE },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 16), short.class, MatrixChecker.SHORT },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 16), short.class, MatrixChecker.SHORT },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 16), char.class, MatrixChecker.CHAR },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 16), char.class, MatrixChecker.CHAR },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 32), int.class, MatrixChecker.INT },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 32), int.class, MatrixChecker.INT },
                { ValueLayout.ofUnsignedInt(ByteOrder.LITTLE_ENDIAN, 64), long.class, MatrixChecker.LONG },
                { ValueLayout.ofSignedInt(ByteOrder.LITTLE_ENDIAN, 64), long.class, MatrixChecker.LONG },
                { ValueLayout.ofFloatingPoint(ByteOrder.LITTLE_ENDIAN, 32), float.class, MatrixChecker.FLOAT },
                { ValueLayout.ofFloatingPoint(ByteOrder.LITTLE_ENDIAN, 64), double.class, MatrixChecker.DOUBLE },
        };
    }

    interface MatrixChecker {
        void check(VarHandle handle, MemoryAddress addr, long row, long col);

        MatrixChecker BYTE = (handle, addr, r, c) -> {
            handle.set(addr, r, c, (byte)(r + c));
            assertEquals(r + c, (byte)handle.get(addr, r, c));
        };

        MatrixChecker SHORT = (handle, addr, r, c) -> {
            handle.set(addr, r, c, (short)(r + c));
            assertEquals(r + c, (short)handle.get(addr, r, c));
        };

        MatrixChecker CHAR = (handle, addr, r, c) -> {
            handle.set(addr, r, c, (char)(r + c));
            assertEquals(r + c, (char)handle.get(addr, r, c));
        };

        MatrixChecker INT = (handle, addr, r, c) -> {
            handle.set(addr, r, c, (int)(r + c));
            assertEquals(r + c, (int)handle.get(addr, r, c));
        };

        MatrixChecker LONG = (handle, addr, r, c) -> {
            handle.set(addr, r, c, r + c);
            assertEquals(r + c, (long)handle.get(addr, r, c));
        };

        MatrixChecker FLOAT = (handle, addr, r, c) -> {
            handle.set(addr, r, c, (float)(r + c));
            assertEquals((float)(r + c), (float)handle.get(addr, r, c));
        };

        MatrixChecker DOUBLE = (handle, addr, r, c) -> {
            handle.set(addr, r, c, (double)(r + c));
            assertEquals((double)(r + c), (double)handle.get(addr, r, c));
        };
    }

    @DataProvider(name = "badCarriers")
    public Object[][] createBadCarriers() {
        return new Object[][] {
                { void.class },
                { boolean.class },
                { Object.class },
                { int[].class }
        };
    }
}