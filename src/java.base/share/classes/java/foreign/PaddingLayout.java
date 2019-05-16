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
import java.util.OptionalLong;

/**
 * A padding layout specifies the size of extra space used to align struct fields around word boundaries.
 */
public class PaddingLayout extends AbstractLayout<PaddingLayout> implements Layout {
    private final long size;

    PaddingLayout(long size, OptionalLong alignment, Optional<String> name) {
        super(alignment, name);
        this.size = size;
    }

    /**
     * Create a new selector layout from given path expression.
     * @param size the padding size in bits.
     * @return the new selector layout.
     */
    public static PaddingLayout of(long size) {
        return new PaddingLayout(size, OptionalLong.empty(), Optional.empty());
    }

    @Override
    public long bitsSize() {
        return size;
    }

    @Override
    long naturalAlignmentBits() {
        return size;
    }

    @Override
    public String toString() {
        return decorateLayoutString("x" + size);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        if (!(other instanceof PaddingLayout)) {
            return false;
        }
        PaddingLayout p = (PaddingLayout)other;
        return size == p.size;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ Long.hashCode(size);
    }

    @Override
    PaddingLayout dup(OptionalLong alignment, Optional<String> name) {
        return new PaddingLayout(size, alignment, name);
    }
}