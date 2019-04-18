/*
 * Copyright (c) 2015, 2019 Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.foreign.abi;

import java.foreign.layout.Layout;

public abstract class Argument {
    private final int argumentIndex; // index of argument (in argument list)
    private final Layout layout;
    private final String debugName; //optional debug name

    protected Argument(Layout layout, int argumentIndex, String debugName) {
        this.argumentIndex = argumentIndex;
        this.layout = layout;
        this.debugName = debugName;
    }

    public int argumentIndex() {
        return argumentIndex;
    }

    public Layout layout() {
        return layout;
    }

    public String name() {
        return debugName != null ? debugName : "<anonymous>";
    }

    public abstract boolean inMemory();

    @Override
    public String toString() {
        return "[" + layout.toString() + " " + name() + "]";
    }
}
