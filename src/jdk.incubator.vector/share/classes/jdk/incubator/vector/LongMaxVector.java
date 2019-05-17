/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */
package jdk.incubator.vector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.incubator.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class LongMaxVector extends LongVector {
    private static final VectorSpecies<Long> SPECIES = LongVector.SPECIES_MAX;

    static final LongMaxVector ZERO = new LongMaxVector();

    static final int LENGTH = SPECIES.length();

    // Index vector species
    private static final IntVector.IntSpecies INDEX_SPECIES;

    static {
        int bitSize = Vector.bitSizeForVectorLength(int.class, LENGTH);
        INDEX_SPECIES = (IntVector.IntSpecies) IntVector.species(VectorShape.forBitSize(bitSize));
    }

    private final long[] vec; // Don't access directly, use getElements() instead.

    private long[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    LongMaxVector() {
        vec = new long[SPECIES.length()];
    }

    LongMaxVector(long[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    LongMaxVector uOp(FUnOp f) {
        long[] vec = getElements();
        long[] res = new long[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new LongMaxVector(res);
    }

    @Override
    LongMaxVector uOp(VectorMask<Long> o, FUnOp f) {
        long[] vec = getElements();
        long[] res = new long[length()];
        boolean[] mbits = ((LongMaxMask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new LongMaxVector(res);
    }

    // Binary operator

    @Override
    LongMaxVector bOp(Vector<Long> o, FBinOp f) {
        long[] res = new long[length()];
        long[] vec1 = this.getElements();
        long[] vec2 = ((LongMaxVector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new LongMaxVector(res);
    }

    @Override
    LongMaxVector bOp(Vector<Long> o1, VectorMask<Long> o2, FBinOp f) {
        long[] res = new long[length()];
        long[] vec1 = this.getElements();
        long[] vec2 = ((LongMaxVector)o1).getElements();
        boolean[] mbits = ((LongMaxMask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new LongMaxVector(res);
    }

    // Trinary operator

    @Override
    LongMaxVector tOp(Vector<Long> o1, Vector<Long> o2, FTriOp f) {
        long[] res = new long[length()];
        long[] vec1 = this.getElements();
        long[] vec2 = ((LongMaxVector)o1).getElements();
        long[] vec3 = ((LongMaxVector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new LongMaxVector(res);
    }

    @Override
    LongMaxVector tOp(Vector<Long> o1, Vector<Long> o2, VectorMask<Long> o3, FTriOp f) {
        long[] res = new long[length()];
        long[] vec1 = getElements();
        long[] vec2 = ((LongMaxVector)o1).getElements();
        long[] vec3 = ((LongMaxVector)o2).getElements();
        boolean[] mbits = ((LongMaxMask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new LongMaxVector(res);
    }

    @Override
    long rOp(long v, FBinOp f) {
        long[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    @Override
    @ForceInline
    public <F> Vector<F> cast(VectorSpecies<F> s) {
        Objects.requireNonNull(s);
        if (s.length() != LENGTH)
            throw new IllegalArgumentException("Vector length this species length differ");

        return VectorIntrinsics.cast(
            LongMaxVector.class,
            long.class, LENGTH,
            s.vectorType(),
            s.elementType(), LENGTH,
            this, s,
            (species, vector) -> vector.castDefault(species)
        );
    }

    @SuppressWarnings("unchecked")
    @ForceInline
    private <F> Vector<F> castDefault(VectorSpecies<F> s) {
        int limit = s.length();

        Class<?> stype = s.elementType();
        if (stype == byte.class) {
            byte[] a = new byte[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (byte) this.lane(i);
            }
            return (Vector) ByteVector.fromArray((VectorSpecies<Byte>) s, a, 0);
        } else if (stype == short.class) {
            short[] a = new short[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (short) this.lane(i);
            }
            return (Vector) ShortVector.fromArray((VectorSpecies<Short>) s, a, 0);
        } else if (stype == int.class) {
            int[] a = new int[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (int) this.lane(i);
            }
            return (Vector) IntVector.fromArray((VectorSpecies<Integer>) s, a, 0);
        } else if (stype == long.class) {
            long[] a = new long[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (long) this.lane(i);
            }
            return (Vector) LongVector.fromArray((VectorSpecies<Long>) s, a, 0);
        } else if (stype == float.class) {
            float[] a = new float[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (float) this.lane(i);
            }
            return (Vector) FloatVector.fromArray((VectorSpecies<Float>) s, a, 0);
        } else if (stype == double.class) {
            double[] a = new double[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (double) this.lane(i);
            }
            return (Vector) DoubleVector.fromArray((VectorSpecies<Double>) s, a, 0);
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public <F> Vector<F> reinterpret(VectorSpecies<F> s) {
        Objects.requireNonNull(s);

        if(s.elementType().equals(long.class)) {
            return (Vector<F>) reshape((VectorSpecies<Long>)s);
        }
        if(s.bitSize() == bitSize()) {
            return reinterpretType(s);
        }

        return defaultReinterpret(s);
    }

    @ForceInline
    private <F> Vector<F> reinterpretType(VectorSpecies<F> s) {
        Objects.requireNonNull(s);

        Class<?> stype = s.elementType();
        if (stype == byte.class) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                ByteMaxVector.class,
                byte.class, ByteMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == short.class) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                ShortMaxVector.class,
                short.class, ShortMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == int.class) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                IntMaxVector.class,
                int.class, IntMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == long.class) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                LongMaxVector.class,
                long.class, LongMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == float.class) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                FloatMaxVector.class,
                float.class, FloatMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == double.class) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                DoubleMaxVector.class,
                double.class, DoubleMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    public LongVector reshape(VectorSpecies<Long> s) {
        Objects.requireNonNull(s);
        if (s.bitSize() == 64 && (s.vectorType() == Long64Vector.class)) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                Long64Vector.class,
                long.class, Long64Vector.LENGTH,
                this, s,
                (species, vector) -> (LongVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 128 && (s.vectorType() == Long128Vector.class)) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                Long128Vector.class,
                long.class, Long128Vector.LENGTH,
                this, s,
                (species, vector) -> (LongVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 256 && (s.vectorType() == Long256Vector.class)) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                Long256Vector.class,
                long.class, Long256Vector.LENGTH,
                this, s,
                (species, vector) -> (LongVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 512 && (s.vectorType() == Long512Vector.class)) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                Long512Vector.class,
                long.class, Long512Vector.LENGTH,
                this, s,
                (species, vector) -> (LongVector) vector.defaultReinterpret(species)
            );
        } else if ((s.bitSize() > 0) && (s.bitSize() <= 2048)
                && (s.bitSize() % 128 == 0) && (s.vectorType() == LongMaxVector.class)) {
            return VectorIntrinsics.reinterpret(
                LongMaxVector.class,
                long.class, LENGTH,
                LongMaxVector.class,
                long.class, LongMaxVector.LENGTH,
                this, s,
                (species, vector) -> (LongVector) vector.defaultReinterpret(species)
            );
        } else {
            throw new InternalError("Unimplemented size");
        }
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public LongVector add(long o) {
        return add((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector add(long o, VectorMask<Long> m) {
        return add((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public LongVector sub(long o) {
        return sub((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector sub(long o, VectorMask<Long> m) {
        return sub((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public LongVector mul(long o) {
        return mul((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector mul(long o, VectorMask<Long> m) {
        return mul((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public LongVector min(long o) {
        return min((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector max(long o) {
        return max((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Long> equal(long o) {
        return equal((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Long> notEqual(long o) {
        return notEqual((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Long> lessThan(long o) {
        return lessThan((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Long> lessThanEq(long o) {
        return lessThanEq((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Long> greaterThan(long o) {
        return greaterThan((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Long> greaterThanEq(long o) {
        return greaterThanEq((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector blend(long o, VectorMask<Long> m) {
        return blend((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }


    @Override
    @ForceInline
    public LongVector and(long o) {
        return and((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector and(long o, VectorMask<Long> m) {
        return and((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public LongVector or(long o) {
        return or((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector or(long o, VectorMask<Long> m) {
        return or((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public LongVector xor(long o) {
        return xor((LongMaxVector)LongVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public LongVector xor(long o, VectorMask<Long> m) {
        return xor((LongMaxVector)LongVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public LongMaxVector neg() {
        return (LongMaxVector)zero(SPECIES).sub(this);
    }

    // Unary operations

    @ForceInline
    @Override
    public LongMaxVector neg(VectorMask<Long> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public LongMaxVector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, LongMaxVector.class, long.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (long) Math.abs(a)));
    }

    @ForceInline
    @Override
    public LongMaxVector abs(VectorMask<Long> m) {
        return blend(abs(), m);
    }


    @Override
    @ForceInline
    public LongMaxVector not() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NOT, LongMaxVector.class, long.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (long) ~a));
    }

    @ForceInline
    @Override
    public LongMaxVector not(VectorMask<Long> m) {
        return blend(not(), m);
    }
    // Binary operations

    @Override
    @ForceInline
    public LongMaxVector add(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long)(a + b)));
    }

    @Override
    @ForceInline
    public LongMaxVector add(Vector<Long> v, VectorMask<Long> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector sub(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long)(a - b)));
    }

    @Override
    @ForceInline
    public LongMaxVector sub(Vector<Long> v, VectorMask<Long> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector mul(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long)(a * b)));
    }

    @Override
    @ForceInline
    public LongMaxVector mul(Vector<Long> v, VectorMask<Long> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector min(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return (LongMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public LongMaxVector min(Vector<Long> v, VectorMask<Long> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector max(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long) Math.max(a, b)));
        }

    @Override
    @ForceInline
    public LongMaxVector max(Vector<Long> v, VectorMask<Long> m) {
        return blend(max(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector and(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_AND, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long)(a & b)));
    }

    @Override
    @ForceInline
    public LongMaxVector or(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_OR, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long)(a | b)));
    }

    @Override
    @ForceInline
    public LongMaxVector xor(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_XOR, LongMaxVector.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (long)(a ^ b)));
    }

    @Override
    @ForceInline
    public LongMaxVector and(Vector<Long> v, VectorMask<Long> m) {
        return blend(and(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector or(Vector<Long> v, VectorMask<Long> m) {
        return blend(or(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector xor(Vector<Long> v, VectorMask<Long> m) {
        return blend(xor(v), m);
    }

    @Override
    @ForceInline
    public LongMaxVector shiftLeft(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_LSHIFT, LongMaxVector.class, long.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (long) (a << i)));
    }

    @Override
    @ForceInline
    public LongMaxVector shiftLeft(int s, VectorMask<Long> m) {
        return blend(shiftLeft(s), m);
    }

    @Override
    @ForceInline
    public LongMaxVector shiftRight(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_URSHIFT, LongMaxVector.class, long.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (long) (a >>> i)));
    }

    @Override
    @ForceInline
    public LongMaxVector shiftRight(int s, VectorMask<Long> m) {
        return blend(shiftRight(s), m);
    }

    @Override
    @ForceInline
    public LongMaxVector shiftArithmeticRight(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_RSHIFT, LongMaxVector.class, long.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (long) (a >> i)));
    }

    @Override
    @ForceInline
    public LongMaxVector shiftArithmeticRight(int s, VectorMask<Long> m) {
        return blend(shiftArithmeticRight(s), m);
    }

    @Override
    @ForceInline
    public LongMaxVector shiftLeft(Vector<Long> s) {
        LongMaxVector shiftv = (LongMaxVector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(LongVector.broadcast(SPECIES, 0x3f));
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_LSHIFT, LongMaxVector.class, long.class, LENGTH,
            this, shiftv,
            (v1, v2) -> v1.bOp(v2,(i,a, b) -> (long) (a << b)));
    }

    @Override
    @ForceInline
    public LongMaxVector shiftRight(Vector<Long> s) {
        LongMaxVector shiftv = (LongMaxVector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(LongVector.broadcast(SPECIES, 0x3f));
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_URSHIFT, LongMaxVector.class, long.class, LENGTH,
            this, shiftv,
            (v1, v2) -> v1.bOp(v2,(i,a, b) -> (long) (a >>> b)));
    }

    @Override
    @ForceInline
    public LongMaxVector shiftArithmeticRight(Vector<Long> s) {
        LongMaxVector shiftv = (LongMaxVector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(LongVector.broadcast(SPECIES, 0x3f));
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_RSHIFT, LongMaxVector.class, long.class, LENGTH,
            this, shiftv,
            (v1, v2) -> v1.bOp(v2,(i,a, b) -> (long) (a >> b)));
    }
    // Ternary operations


    // Type specific horizontal reductions

    @Override
    @ForceInline
    public long addLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_ADD, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp((long) 0, (i, a, b) -> (long) (a + b)));
    }

    @Override
    @ForceInline
    public long andLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_AND, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp((long) -1, (i, a, b) -> (long) (a & b)));
    }

    @Override
    @ForceInline
    public long andLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, (long) -1).blend(this, m).andLanes();
    }

    @Override
    @ForceInline
    public long minLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MIN, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp(Long.MAX_VALUE , (i, a, b) -> (long) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public long maxLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MAX, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp(Long.MIN_VALUE , (i, a, b) -> (long) Math.max(a, b)));
    }

    @Override
    @ForceInline
    public long mulLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MUL, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp((long) 1, (i, a, b) -> (long) (a * b)));
    }

    @Override
    @ForceInline
    public long orLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_OR, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp((long) 0, (i, a, b) -> (long) (a | b)));
    }

    @Override
    @ForceInline
    public long orLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, (long) 0).blend(this, m).orLanes();
    }

    @Override
    @ForceInline
    public long xorLanes() {
        return (long) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_XOR, LongMaxVector.class, long.class, LENGTH,
            this,
            v -> (long) v.rOp((long) 0, (i, a, b) -> (long) (a ^ b)));
    }

    @Override
    @ForceInline
    public long xorLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, (long) 0).blend(this, m).xorLanes();
    }


    @Override
    @ForceInline
    public long addLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, (long) 0).blend(this, m).addLanes();
    }


    @Override
    @ForceInline
    public long mulLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, (long) 1).blend(this, m).mulLanes();
    }

    @Override
    @ForceInline
    public long minLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, Long.MAX_VALUE).blend(this, m).minLanes();
    }

    @Override
    @ForceInline
    public long maxLanes(VectorMask<Long> m) {
        return LongVector.broadcast(SPECIES, Long.MIN_VALUE).blend(this, m).maxLanes();
    }

    @Override
    @ForceInline
    public VectorShuffle<Long> toShuffle() {
        long[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(SPECIES, sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT         = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_LONG_INDEX_SCALE);
    private static final int BOOLEAN_ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BOOLEAN_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(long[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(LongMaxVector.class, long.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_LONG_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(long[] a, int ax, VectorMask<Long> m) {
        LongVector oldVal = LongVector.fromArray(SPECIES, a, ax);
        LongVector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }
    @Override
    @ForceInline
    public void intoArray(long[] a, int ix, int[] b, int iy) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
        IntVector vix = IntVector.fromArray(INDEX_SPECIES, b, iy).add(ix);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorIntrinsics.storeWithMap(LongMaxVector.class, long.class, LENGTH, vix.getClass(),
                               a, Unsafe.ARRAY_LONG_BASE_OFFSET, vix,
                               this,
                               a, ix, b, iy,
                               (arr, idx, v, indexMap, idy) -> v.forEach((i, e) -> arr[idx+indexMap[idy+i]] = e));
    }

     @Override
     @ForceInline
     public final void intoArray(long[] a, int ax, VectorMask<Long> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         LongVector oldVal = LongVector.fromArray(SPECIES, a, ax, b, iy);
         LongVector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(LongMaxVector.class, long.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   LongBuffer tb = bbc.asLongBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, VectorMask<Long> m) {
        LongMaxVector oldVal = (LongMaxVector) LongVector.fromByteArray(SPECIES, a, ix);
        LongMaxVector newVal = oldVal.blend(this, m);
        newVal.intoByteArray(a, ix);
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix) {
        if (bb.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException();
        }
        if (bb.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
        VectorIntrinsics.store(LongMaxVector.class, long.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   LongBuffer tb = bbc.asLongBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, VectorMask<Long> m) {
        LongMaxVector oldVal = (LongMaxVector) LongVector.fromByteBuffer(SPECIES, bb, ix);
        LongMaxVector newVal = oldVal.blend(this, m);
        newVal.intoByteBuffer(bb, ix);
    }

    //

    @Override
    public String toString() {
        return Arrays.toString(getElements());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        LongMaxVector that = (LongMaxVector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    LongMaxMask bTest(Vector<Long> o, FBinTest f) {
        long[] vec1 = getElements();
        long[] vec2 = ((LongMaxVector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new LongMaxMask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public LongMaxMask equal(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;

        return VectorIntrinsics.compare(
            BT_eq, LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public LongMaxMask notEqual(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ne, LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public LongMaxMask lessThan(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;

        return VectorIntrinsics.compare(
            BT_lt, LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public LongMaxMask lessThanEq(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;

        return VectorIntrinsics.compare(
            BT_le, LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public LongMaxMask greaterThan(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;

        return (LongMaxMask) VectorIntrinsics.compare(
            BT_gt, LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public LongMaxMask greaterThanEq(Vector<Long> o) {
        Objects.requireNonNull(o);
        LongMaxVector v = (LongMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ge, LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        long[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(VectorMask<Long> o, FUnCon f) {
        boolean[] mbits = ((LongMaxMask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }


    DoubleMaxVector toFP() {
        long[] vec = getElements();
        double[] res = new double[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Double.longBitsToDouble(vec[i]);
        }
        return new DoubleMaxVector(res);
    }

    @Override
    public LongMaxVector rotateLanesLeft(int j) {
        long[] vec = getElements();
        long[] res = new long[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new LongMaxVector(res);
    }

    @Override
    public LongMaxVector rotateLanesRight(int j) {
        long[] vec = getElements();
        long[] res = new long[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new LongMaxVector(res);
    }

    @Override
    public LongMaxVector shiftLanesLeft(int j) {
        long[] vec = getElements();
        long[] res = new long[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new LongMaxVector(res);
    }

    @Override
    public LongMaxVector shiftLanesRight(int j) {
        long[] vec = getElements();
        long[] res = new long[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new LongMaxVector(res);
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(Vector<Long> v,
                                  VectorShuffle<Long> s, VectorMask<Long> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> o1) {
        Objects.requireNonNull(o1);
        LongMaxShuffle s =  (LongMaxShuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            LongMaxVector.class, LongMaxShuffle.class, long.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
                int ei = s_.lane(i);
                return v1.lane(ei);
            }));
    }

    @Override
    @ForceInline
    public LongMaxVector blend(Vector<Long> o1, VectorMask<Long> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        LongMaxVector v = (LongMaxVector)o1;
        LongMaxMask   m = (LongMaxMask)o2;

        return VectorIntrinsics.blend(
            LongMaxVector.class, LongMaxMask.class, long.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.lane(i) ? b : a));
    }

    // Accessors

    @Override
    public long lane(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return (long) VectorIntrinsics.extract(
                                LongMaxVector.class, long.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    long[] vecarr = vec.getElements();
                                    return (long)vecarr[ix];
                                });
    }

    @Override
    public LongMaxVector with(int i, long e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                LongMaxVector.class, long.class, LENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    long[] res = v.getElements().clone();
                                    res[ix] = (long)bits;
                                    return new LongMaxVector(res);
                                });
    }

    // Mask

    static final class LongMaxMask extends AbstractMask<Long> {
        static final LongMaxMask TRUE_MASK = new LongMaxMask(true);
        static final LongMaxMask FALSE_MASK = new LongMaxMask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public LongMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        public LongMaxMask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public LongMaxMask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        LongMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new LongMaxMask(res);
        }

        @Override
        LongMaxMask bOp(VectorMask<Long> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((LongMaxMask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new LongMaxMask(res);
        }

        @Override
        public VectorSpecies<Long> species() {
            return SPECIES;
        }

        @Override
        public LongMaxVector toVector() {
            long[] res = new long[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (long) (bits[i] ? -1 : 0);
            }
            return new LongMaxVector(res);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <E> VectorMask<E> cast(VectorSpecies<E> species) {
            if (length() != species.length())
                throw new IllegalArgumentException("VectorMask length and species length differ");
            Class<?> stype = species.elementType();
            boolean [] maskArray = toArray();
            if (stype == byte.class) {
                return (VectorMask <E>) new ByteMaxVector.ByteMaxMask(maskArray);
            } else if (stype == short.class) {
                return (VectorMask <E>) new ShortMaxVector.ShortMaxMask(maskArray);
            } else if (stype == int.class) {
                return (VectorMask <E>) new IntMaxVector.IntMaxMask(maskArray);
            } else if (stype == long.class) {
                return (VectorMask <E>) new LongMaxVector.LongMaxMask(maskArray);
            } else if (stype == float.class) {
                return (VectorMask <E>) new FloatMaxVector.FloatMaxMask(maskArray);
            } else if (stype == double.class) {
                return (VectorMask <E>) new DoubleMaxVector.DoubleMaxMask(maskArray);
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }
        }

        // Unary operations

        @Override
        @ForceInline
        public LongMaxMask not() {
            return (LongMaxMask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, LongMaxMask.class, long.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public LongMaxMask and(VectorMask<Long> o) {
            Objects.requireNonNull(o);
            LongMaxMask m = (LongMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, LongMaxMask.class, long.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public LongMaxMask or(VectorMask<Long> o) {
            Objects.requireNonNull(o);
            LongMaxMask m = (LongMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, LongMaxMask.class, long.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(BT_ne, LongMaxMask.class, long.class, LENGTH,
                                         this, this,
                                         (m, __) -> anyTrueHelper(((LongMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(BT_overflow, LongMaxMask.class, long.class, LENGTH,
                                         this, VectorMask.maskAllTrue(species()),
                                         (m, __) -> allTrueHelper(((LongMaxMask)m).getBits()));
        }
    }

    // Shuffle

    static final class LongMaxShuffle extends AbstractShuffle<Long> {
        LongMaxShuffle(byte[] reorder) {
            super(reorder);
        }

        public LongMaxShuffle(int[] reorder) {
            super(reorder);
        }

        public LongMaxShuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public LongMaxShuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public VectorSpecies<Long> species() {
            return SPECIES;
        }

        @Override
        public LongVector toVector() {
            long[] va = new long[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (long) lane(i);
            }
            return LongVector.fromArray(SPECIES, va, 0);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> VectorShuffle<F> cast(VectorSpecies<F> species) {
            if (length() != species.length())
                throw new IllegalArgumentException("Shuffle length and species length differ");
            Class<?> stype = species.elementType();
            int [] shuffleArray = toArray();
            if (stype == byte.class) {
                return (VectorShuffle<F>) new ByteMaxVector.ByteMaxShuffle(shuffleArray);
            } else if (stype == short.class) {
                return (VectorShuffle<F>) new ShortMaxVector.ShortMaxShuffle(shuffleArray);
            } else if (stype == int.class) {
                return (VectorShuffle<F>) new IntMaxVector.IntMaxShuffle(shuffleArray);
            } else if (stype == long.class) {
                return (VectorShuffle<F>) new LongMaxVector.LongMaxShuffle(shuffleArray);
            } else if (stype == float.class) {
                return (VectorShuffle<F>) new FloatMaxVector.FloatMaxShuffle(shuffleArray);
            } else if (stype == double.class) {
                return (VectorShuffle<F>) new DoubleMaxVector.DoubleMaxShuffle(shuffleArray);
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }
        }

        @Override
        public LongMaxShuffle rearrange(VectorShuffle<Long> o) {
            LongMaxShuffle s = (LongMaxShuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new LongMaxShuffle(r);
        }
    }

    // VectorSpecies

    @Override
    public VectorSpecies<Long> species() {
        return SPECIES;
    }
}