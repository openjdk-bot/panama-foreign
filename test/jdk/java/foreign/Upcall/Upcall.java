/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @run main/othervm Upcall
 */

import java.foreign.Scope;
import java.foreign.annotations.NativeFunction;
import java.foreign.memory.Pointer;
import java.lang.invoke.MethodHandles;
import java.foreign.Libraries;
import java.foreign.annotations.NativeCallback;
import java.foreign.annotations.NativeHeader;
import java.foreign.annotations.NativeLocation;
import java.foreign.memory.Callback;

public class Upcall {
    private static final boolean DEBUG = false;

    private static final int MAGIC_INTEGER = 4711;

    @NativeHeader
    public static interface upcall {
        @NativeCallback("(i32)v")
        @FunctionalInterface
        static interface visitor {
            @NativeLocation(file="dummy", line=47, column=11)
            public void fn(int i);
        }

        @NativeLocation(file="dummy", line=47, column=11)
        @NativeFunction("(u64:(i32)vi32)v")
        public abstract void do_upcall(Callback<visitor> v, int i);

        @NativeLocation(file="dummy", line=67, column=1)
        @NativeFunction("(u64:(i32)vi32)i32")
        public abstract int no_upcall(Callback<visitor> v, int i);
    }

    public static class visitorImpl implements upcall.visitor {
        boolean called = false;

        @Override
        public void fn(int i) {
            called = true;

            if (DEBUG) {
                System.err.println("visit(" + i + ")");
            }

            // value passed up from native code
            assertEquals(i, MAGIC_INTEGER);
        }
    }

    public void test() {
        upcall i = Libraries.bind(upcall.class, Libraries.loadLibrary(MethodHandles.lookup(), "Upcall"));

        try (Scope sc = Scope.globalScope().fork()) {
            visitorImpl v = new visitorImpl();

            i.do_upcall(sc.allocateCallback(v), MAGIC_INTEGER);

            assertTrue(v.called);

            if (DEBUG) {
                System.err.println("back in test()\n");
            }

            assertTrue(i.no_upcall(Callback.ofNull(), 0) == 0);
            assertTrue(i.no_upcall(sc.allocateCallback(upcall.visitor.class, x -> {}), 0) == 1);

            boolean gotNPE = true;
            try {
                Callback.ofNull().asFunction();
            } catch (NullPointerException npe) {
                gotNPE = true;
            }
            assertTrue(gotNPE);

            assertTrue(Callback.ofNull().entryPoint().equals(Pointer.ofNull()));
        }
    }

    static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new RuntimeException("expected: " + expected + " does not match actual: " + actual);
        }
    }

    static void assertTrue(boolean actual) {
        if (!actual) {
            throw new RuntimeException("expected: true does not match actual: " + actual);
        }
    }

    public static void main(String[] args) {
        new Upcall().test();
    }
}