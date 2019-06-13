/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.foreign.memory;

import jdk.internal.foreign.memory.MemoryBoundInfo;
import jdk.internal.foreign.memory.BoundedPointer;

import java.foreign.NativeTypes;
import java.foreign.Scope;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This interface models a native pointer.
 * @param <X> the carrier type associated with the pointee.
 */
public interface Pointer<X> {

    /**
     * Obtains the {@code NULL} pointer.
     * @param <Z> the carrier type of the pointer.
     * @return the {@code NULL} pointer.
     */
    static <Z> Pointer<Z> ofNull() {
        return BoundedPointer.ofNull();
    }

    /**
     * Add a given offset to this pointer.
     * @param nElements offset (expressed in number of elements).
     * @return a new pointer with the added offset.
     * @throws IllegalArgumentException if the size of the layout of this pointer is zero.
     * @throws IndexOutOfBoundsException if offset exceeds the boundaries of the memory region of this pointer.
     */
    Pointer<X> offset(long nElements) throws IllegalArgumentException, IndexOutOfBoundsException;

    /**
     * Returns a stream of pointers starting at this pointer and incrementing the pointer by 1 until
     * the {@code hasNext} predicate returns {@code false}. This is effectively the same as:
     * <p>
     *     <code>
     *         Stream.iterate(pointer, hasNext, p -&gt; p.offset(1))
     *     </code>
     * </p>
     *
     * @param hasNext a predicate which should return {@code true} as long as the stream should continue
     * @return a stream limited by the {@code hasNext} predicate.
     * @throws IllegalArgumentException if the size of the layout of this pointer is zero.
     * @throws IndexOutOfBoundsException if offset exceeds the boundaries of the memory region of this pointer.
     */
    default Stream<Pointer<X>> iterate(Predicate<? super Pointer<X>> hasNext) throws IllegalArgumentException, IndexOutOfBoundsException {
        return Stream.iterate(this, hasNext, p -> p.offset(1));
    }

    /**
     * Returns a stream of pointers starting at this pointer and incrementing the pointer by 1 until
     * the pointer is equal to {@code end}. This is effectively the same as:
     * <p>
     *     <code>
     *         Stream.iterate(pointer, p -&gt; !p.equals(end), p -&gt; p.offset(1))
     *     </code>
     * </p>
     *
     * @param end a pointer which is used as the end-point of the iteration
     * @return a stream from this pointer until {@code end}
     * @throws IllegalArgumentException if the size of the layout of this pointer is zero.
     * @throws IndexOutOfBoundsException if offset exceeds the boundaries of the memory region of this pointer.
     */
    default Stream<Pointer<X>> iterate(Pointer<X> end) throws IllegalArgumentException, IndexOutOfBoundsException {
        return Stream.iterate(this, p -> !p.equals(end), p -> p.offset(1));
    }

    /**
     * Retrieves the {@link LayoutType} associated with this pointer.
     * @return the pointer's {@link LayoutType}.
     */
    LayoutType<X> type();

    /**
     * Checks if this pointer is {@code NULL}.
     * @return {@code true} if pointer is {@code NULL}.
     */
    boolean isNull();

    /**
     * Is the memory this pointer points to accessible for the given mode.
     *
     * @param mode the access mode
     * @return {@code true} if accessible, otherwise {@code false}
     */
    boolean isAccessibleFor(AccessMode mode);

    /**
     * Creates a new, read-only pointer with the same offset as this pointer.
     *
     * @return The created Pointer
     * @throws AccessControlException If this pointer does not have read access
     */
    Pointer<X> asReadOnly() throws AccessControlException;

    /**
     * Creates a new, write-only pointer with the same offset as this pointer.
     *
     * @return The created Pointer
     * @throws AccessControlException If this pointer does not have write access
     */
    Pointer<X> asWriteOnly() throws AccessControlException;

    /**
     * Returns the underlying memory address associated with this pointer, if available.
     * @return the memory address.
     * @throws UnsupportedOperationException if the memory address is not a native address.
     * @throws IllegalStateException if the pointer is not alive, or out of bounds.
     * @throws AccessControlException if the pointer does not support the {@link AccessMode#READ_WRITE} access mode.
     */
    long addr() throws UnsupportedOperationException, IllegalStateException, AccessControlException;

    /**
     * Construct an array out of an element pointer, with given size.
     * @param size the size of the resulting array.
     * @return an array.
     */
    Array<X> withSize(long size);

    /**
     * Cast the pointer to given {@code LayoutType}.
     * @param <Y> the target pointer type.
     * @param type the new {@code LayoutType} associated with the pointer.
     * @return a new pointer with desired type info.
     */
    <Y> Pointer<Y> cast(LayoutType<Y> type);

    /**
     * Load the value associated with this pointer.
     * @return the pointer's value.
     */
    @SuppressWarnings("unchecked")
    default X get() {
        try {
            return (X)type().getter().invoke(this);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Stores the value associated with this pointer.
     * @param x the value to be stored.
     */
    default void set(X x) {
        try {
            type().setter().invoke(this, x);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * The scope this pointer belongs to.
     * @return the owning scope.
     */
    Scope scope();

    /**
     * Returns a pointer to the memory region covered by the given byte
     * buffer. The region starts relative to the buffer's position (inclusive)
     * and ends relative to the buffer's limit (exclusive).
     * <p>
     * The pointer keeps a reference to the buffer to ensure the buffer is kept
     * live for the life-time of the pointer.
     * <p>
     * For a direct ByteBuffer the address is accessible via {@link #addr()},
     * where as for a heap ByteBuffer this method throws an
     * {@link UnsupportedOperationException}.
     *
     * @param bb the byte buffer
     * @return the created pointer
     */
    static Pointer<Byte> fromByteBuffer(ByteBuffer bb) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("java.foreign.Pointer.fromByteBuffer"));
        }
        return new BoundedPointer<>(NativeTypes.UINT8, null, AccessMode.READ_WRITE, MemoryBoundInfo.ofByteBuffer(bb));
    }

    /**
     * Wraps the this pointer in a direct {@link ByteBuffer}
     *
     * This method performs the same access checks as {@link Pointer#addr()}.
     *
     * @param bytes the size of the buffer in bytes
     * @return the created {@link ByteBuffer}
     * @throws UnsupportedOperationException if the memory address is not a native address.
     * @throws IllegalStateException if the pointer is not alive, or out of bounds.
     * @throws AccessControlException if the pointer does not support the {@link AccessMode#READ_WRITE} access mode.
     */
    ByteBuffer asDirectByteBuffer(int bytes) throws UnsupportedOperationException, IllegalStateException, AccessControlException;

    static void copy(Pointer<?> src, Pointer<?> dst, long bytes) {
        BoundedPointer<?> bsrc = (BoundedPointer<?>) Objects.requireNonNull(src);
        BoundedPointer<?> bdst = (BoundedPointer<?>) Objects.requireNonNull(dst);
        bsrc.copyTo(bdst, bytes);
    }

    /**
     * Copies the contents of one pointer to another, given that they have the same LayoutType.
     *
     * @param src the source pointer
     * @param dst the destination pointer.
     */
    static void copy(Pointer<?> src, Pointer<?> dst) {
        if (!src.type().equals(dst.type())) {
            throw new IllegalArgumentException("Incompatible types: " + src.type() + ", and: " + dst.type());
        }
        assert src.type().bytesSize() == dst.type().bytesSize() : "byteSize should be equal after type check";

        copy(src, dst, dst.type().bytesSize());
    }

    static String toString(Pointer<Byte> cstr) {
        if (cstr == null || cstr.isNull()) {
            return null;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte b;
        for (int i = 0; (b = cstr.offset(i).get()) != 0; i++) {
            os.write(b);
        }
        return os.toString();
    }

    /**
     * Defines a set of memory access modes
     */
    enum AccessMode {
        /**
         * An access mode that allows no access
         */
        NONE(0),
        /**
         * A read-only access mode
         */
        READ(1 << 0),
        /**
         * A write-only access mode
         */
        WRITE(1 << 1),
        /**
         * A read and write access mode
         */
        READ_WRITE(READ.value | WRITE.value);

        private final int value;

        AccessMode(int value) {
            this.value = value;
        }

        /**
         * Compare this access mode to the given access mode
         * to see if the given access mode is available as
         * a part of this access mode.
         *
         * @param mode the mode to check
         * @return {@code true} if the given mode is available
         */
        public boolean isAvailable(AccessMode mode) {
            return (this.value & mode.value) == mode.value;
        }
    }
}