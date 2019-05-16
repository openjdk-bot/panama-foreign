/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.foreign;

import java.util.Optional;

/**
 * This interface models the layout of a contiguous memory region. Layouts have a size (where possible), see {@link Layout#bitsSize()},
 * an optional name (which is useful to refer to them in other context), see {@link Layout#name()} and alignment
 * constraints, see {@link Layout#alignmentBits()}.
 */
public interface Layout {

    /**
     * Computes the layout size, in bits
     * @return the layout size.
     * @throws UnsupportedOperationException if the layout has unbounded size (see {@link SequenceLayout}).
     */
    long bitsSize() throws UnsupportedOperationException;

    /**
     * Return the value of the 'name' attribute (if any) associated with this layout.
     * @return the layout 'name' attribute (if any).
     */
    Optional<String> name();

    /**
     * Attach name annotation to the current layout.
     * @param name name annotation.
     * @return a new layout with desired name annotation.
     */
    Layout withName(String name);

    /**
     * Returns the alignment constraints assocciated with this layout. Layout alignment defines a power of two A which is the
     * bitwise alignment of the layout. If A&gt;=8 then A/8 is the number of bytes that must be aligned for any pointer that
     * correctly points to this layout. Thus:
     *
     * <ul>
     * <li>A=8 means unaligned (in the usual sense), which is common in packets.</li>
     * <li>A=64 means word aligned (on LP64), A=32 int aligned, A=16 short aligned, etc.</li>
     * <li>A=512 is the most strict alignment required by the x86/SV ABI (for AVX-512 data).</li>
     * </ul>
     *
     * @return bit alignment.
     */
    long alignmentBits();

    /**
     * Creates a new layout which features the desired alignment.
     *
     * @param alignmentBits the required alignment requirements.
     * @return a new layout with given alignment requirements.
     * @throws IllegalArgumentException if the supplied alignment is not a power of two, or if it's lower than 8.
     */
    Layout alignTo(long alignmentBits) throws IllegalArgumentException;

    @Override
    String toString();

    /**
     * Obtain layout path rooted at this layout.
     * @return a new layout path rooted at this layout.
     */
    LayoutPath toPath();
}