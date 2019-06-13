/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Arm Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.internal.foreign.abi.aarch64;

import jdk.internal.foreign.abi.Storage;

import java.foreign.layout.Address;
import java.foreign.layout.Group;
import java.foreign.layout.Layout;
import java.foreign.layout.Padding;
import java.foreign.layout.Sequence;
import java.foreign.layout.Value;

public class SharedUtils {
    public static final int INTEGER_REGISTER_SIZE = 8;
    public static final int VECTOR_REGISTER_SIZE = 16;

    public static final int STACK_SLOT_SIZE = 8;

    /**
     * Align the specified type from a given address
     * @return The address the data should be at based on alignment requirement
     */
    public static long align(Layout t, boolean isVar, long addr) {
        return alignUp(addr, alignment(t, isVar));
    }

    public static long alignUp(long addr, long alignment) {
        return ((addr - 1) | (alignment - 1)) + 1;
    }

    public static long alignDown(long addr, long alignment) {
        return addr & ~(alignment - 1);
    }

    /**
     * The alignment requirement for a given type
     * @param isVar indicate if the type is a standalone variable. This change how
     * array is aligned. for example.
     */
    public static long alignment(Layout t, boolean isVar) {
        if (t instanceof Value) {
            return alignmentOfScalar((Value) t);
        } else if (t instanceof Sequence) {
            // when array is used alone
            return alignmentOfArray((Sequence) t, isVar);
        } else if (t instanceof Group) {
            return alignmentOfContainer((Group) t);
        } else if (t instanceof Address) {
            return 8;
        } else if (t instanceof Padding) {
            return 1;
        } else {
            throw new IllegalArgumentException("Invalid type: " + t);
        }
    }

    private static long alignmentOfScalar(Value st) {
        return st.bitsSize() / 8;
    }

    private static long alignmentOfArray(Sequence ar, boolean isVar) {
        if (ar.elementsSize() == 0) {
            // VLA or incomplete
            return 16;
        } else if ((ar.bitsSize() / 8) >= 16 && isVar) {
            return 16;
        } else {
            // align as element type
            Layout elementType = ar.element();
            return alignment(elementType, false);
        }
    }

    private static long alignmentOfContainer(Group ct) {
        // Most strict member
        return ct.elements().stream().mapToLong(t -> alignment(t, false)).max().orElse(1);
    }

    public static class StorageDebugHelper {

        private final int maxIntegerArgRegisters;
        private final int maxIntegerReturnRegisters;
        private final int maxVectorArgRegisters;
        private final int maxVectorReturnRegisters;

        public StorageDebugHelper(int maxIntegerArgRegisters,
                                  int maxIntegerReturnRegisters,
                                  int maxVectorArgRegisters,
                                  int maxVectorReturnRegisters) {
            this.maxIntegerArgRegisters = maxIntegerArgRegisters;
            this.maxIntegerReturnRegisters = maxIntegerReturnRegisters;
            this.maxVectorArgRegisters = maxVectorArgRegisters;
            this.maxVectorReturnRegisters = maxVectorReturnRegisters;
        }

        private static String getVectorRegisterName(long index, long size) {
            return "v" + index;
        }

        private static String getIntegerRegisterName(long index) {
            return "r" + index;
        }

        public String getStorageName(Storage storage) {
            switch (storage.getStorageClass()) {
                case INTEGER_ARGUMENT_REGISTER:
                    if (storage.getStorageIndex() > maxIntegerArgRegisters) {
                        throw new IllegalArgumentException("Illegal storage: " + storage);
                    }
                    return getIntegerRegisterName(storage.getStorageIndex());

                case VECTOR_ARGUMENT_REGISTER:
                    if (storage.getStorageIndex() > maxVectorArgRegisters) {
                        throw new IllegalArgumentException("Illegal storage: " + storage);
                    }
                    return getVectorRegisterName(storage.getStorageIndex(), storage.getSize());

                case INTEGER_RETURN_REGISTER:
                    if (storage.getStorageIndex() > maxIntegerReturnRegisters) {
                        throw new IllegalArgumentException("Illegal storage: " + storage);
                    }
                    return getIntegerRegisterName(storage.getStorageIndex());

                case VECTOR_RETURN_REGISTER:
                    if (storage.getStorageIndex() > maxVectorReturnRegisters) {
                        throw new IllegalArgumentException("Illegal storage: " + storage);
                    }
                    return getVectorRegisterName(storage.getStorageIndex(), storage.getSize());

                case STACK_ARGUMENT_SLOT:
                    return "[sp + " + Long.toHexString(8 * storage.getStorageIndex()) + "]";
            }

            throw new IllegalArgumentException("Unhandled storage type: " + storage.getStorageClass());
        }
    }
}