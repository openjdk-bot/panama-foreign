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
import java.nio.FloatBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.incubator.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class Float256Vector extends FloatVector {
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;

    static final Float256Vector ZERO = new Float256Vector();

    static final int LENGTH = SPECIES.length();

    // Index vector species
    private static final IntVector.IntSpecies INDEX_SPECIES;

    static {
        int bitSize = Vector.bitSizeForVectorLength(int.class, LENGTH);
        INDEX_SPECIES = (IntVector.IntSpecies) IntVector.species(VectorShape.forBitSize(bitSize));
    }

    private final float[] vec; // Don't access directly, use getElements() instead.

    private float[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    Float256Vector() {
        vec = new float[SPECIES.length()];
    }

    Float256Vector(float[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    Float256Vector uOp(FUnOp f) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new Float256Vector(res);
    }

    @Override
    Float256Vector uOp(VectorMask<Float> o, FUnOp f) {
        float[] vec = getElements();
        float[] res = new float[length()];
        boolean[] mbits = ((Float256Mask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new Float256Vector(res);
    }

    // Binary operator

    @Override
    Float256Vector bOp(Vector<Float> o, FBinOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((Float256Vector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Float256Vector(res);
    }

    @Override
    Float256Vector bOp(Vector<Float> o1, VectorMask<Float> o2, FBinOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((Float256Vector)o1).getElements();
        boolean[] mbits = ((Float256Mask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new Float256Vector(res);
    }

    // Trinary operator

    @Override
    Float256Vector tOp(Vector<Float> o1, Vector<Float> o2, FTriOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((Float256Vector)o1).getElements();
        float[] vec3 = ((Float256Vector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new Float256Vector(res);
    }

    @Override
    Float256Vector tOp(Vector<Float> o1, Vector<Float> o2, VectorMask<Float> o3, FTriOp f) {
        float[] res = new float[length()];
        float[] vec1 = getElements();
        float[] vec2 = ((Float256Vector)o1).getElements();
        float[] vec3 = ((Float256Vector)o2).getElements();
        boolean[] mbits = ((Float256Mask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new Float256Vector(res);
    }

    @Override
    float rOp(float v, FBinOp f) {
        float[] vec = getElements();
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
            Float256Vector.class,
            float.class, LENGTH,
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

        if(s.elementType().equals(float.class)) {
            return (Vector<F>) reshape((VectorSpecies<Float>)s);
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
                Float256Vector.class,
                float.class, LENGTH,
                Byte256Vector.class,
                byte.class, Byte256Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == short.class) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Short256Vector.class,
                short.class, Short256Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == int.class) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Int256Vector.class,
                int.class, Int256Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == long.class) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Long256Vector.class,
                long.class, Long256Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == float.class) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Float256Vector.class,
                float.class, Float256Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == double.class) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Double256Vector.class,
                double.class, Double256Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    public FloatVector reshape(VectorSpecies<Float> s) {
        Objects.requireNonNull(s);
        if (s.bitSize() == 64 && (s.vectorType() == Float64Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Float64Vector.class,
                float.class, Float64Vector.LENGTH,
                this, s,
                (species, vector) -> (FloatVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 128 && (s.vectorType() == Float128Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Float128Vector.class,
                float.class, Float128Vector.LENGTH,
                this, s,
                (species, vector) -> (FloatVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 256 && (s.vectorType() == Float256Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Float256Vector.class,
                float.class, Float256Vector.LENGTH,
                this, s,
                (species, vector) -> (FloatVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 512 && (s.vectorType() == Float512Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                Float512Vector.class,
                float.class, Float512Vector.LENGTH,
                this, s,
                (species, vector) -> (FloatVector) vector.defaultReinterpret(species)
            );
        } else if ((s.bitSize() > 0) && (s.bitSize() <= 2048)
                && (s.bitSize() % 128 == 0) && (s.vectorType() == FloatMaxVector.class)) {
            return VectorIntrinsics.reinterpret(
                Float256Vector.class,
                float.class, LENGTH,
                FloatMaxVector.class,
                float.class, FloatMaxVector.LENGTH,
                this, s,
                (species, vector) -> (FloatVector) vector.defaultReinterpret(species)
            );
        } else {
            throw new InternalError("Unimplemented size");
        }
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public FloatVector add(float o) {
        return add((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector add(float o, VectorMask<Float> m) {
        return add((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public FloatVector sub(float o) {
        return sub((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector sub(float o, VectorMask<Float> m) {
        return sub((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public FloatVector mul(float o) {
        return mul((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector mul(float o, VectorMask<Float> m) {
        return mul((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public FloatVector min(float o) {
        return min((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector max(float o) {
        return max((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Float> equal(float o) {
        return equal((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Float> notEqual(float o) {
        return notEqual((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Float> lessThan(float o) {
        return lessThan((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Float> lessThanEq(float o) {
        return lessThanEq((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Float> greaterThan(float o) {
        return greaterThan((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Float> greaterThanEq(float o) {
        return greaterThanEq((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector blend(float o, VectorMask<Float> m) {
        return blend((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public FloatVector div(float o) {
        return div((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector div(float o, VectorMask<Float> m) {
        return div((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public Float256Vector div(Vector<Float> v, VectorMask<Float> m) {
        return blend(div(v), m);
    }

    @Override
    @ForceInline
    public FloatVector atan2(float o) {
        return atan2((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector atan2(float o, VectorMask<Float> m) {
        return atan2((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public FloatVector pow(float o) {
        return pow((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector pow(float o, VectorMask<Float> m) {
        return pow((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public FloatVector fma(float o1, float o2) {
        return fma((Float256Vector)FloatVector.broadcast(SPECIES, o1), (Float256Vector)FloatVector.broadcast(SPECIES, o2));
    }

    @Override
    @ForceInline
    public FloatVector fma(float o1, float o2, VectorMask<Float> m) {
        return fma((Float256Vector)FloatVector.broadcast(SPECIES, o1), (Float256Vector)FloatVector.broadcast(SPECIES, o2), m);
    }

    @Override
    @ForceInline
    public FloatVector hypot(float o) {
        return hypot((Float256Vector)FloatVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public FloatVector hypot(float o, VectorMask<Float> m) {
        return hypot((Float256Vector)FloatVector.broadcast(SPECIES, o), m);
    }


    // Unary operations

    @ForceInline
    @Override
    public Float256Vector neg(VectorMask<Float> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public Float256Vector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) Math.abs(a)));
    }

    @ForceInline
    @Override
    public Float256Vector abs(VectorMask<Float> m) {
        return blend(abs(), m);
    }

    @Override
    @ForceInline
    public Float256Vector neg() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NEG, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) -a));
    }

    @Override
    @ForceInline
    public Float256Vector div(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_DIV, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a / b)));
    }

    @Override
    @ForceInline
    public Float256Vector sqrt() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_SQRT, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) Math.sqrt((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector exp() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXP, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.exp((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector log1p() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG1P, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.log1p((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector log() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.log((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector log10() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG10, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.log10((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector expm1() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXPM1, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.expm1((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector cbrt() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_CBRT, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.cbrt((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector sin() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SIN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.sin((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector cos() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COS, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.cos((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector tan() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TAN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.tan((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector asin() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ASIN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.asin((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector acos() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ACOS, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.acos((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector atan() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ATAN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.atan((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector sinh() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SINH, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.sinh((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector cosh() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COSH, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.cosh((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector tanh() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TANH, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.tanh((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector pow(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_POW, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float)(Math.pow(a,b))));
    }

    @Override
    @ForceInline
    public Float256Vector hypot(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_HYPOT, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float)(Math.hypot(a,b))));
    }

    @Override
    @ForceInline
    public Float256Vector atan2(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_ATAN2, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float)(Math.atan2(a,b))));
    }


    // Binary operations

    @Override
    @ForceInline
    public Float256Vector add(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a + b)));
    }

    @Override
    @ForceInline
    public Float256Vector add(Vector<Float> v, VectorMask<Float> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector sub(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a - b)));
    }

    @Override
    @ForceInline
    public Float256Vector sub(Vector<Float> v, VectorMask<Float> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector mul(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a * b)));
    }

    @Override
    @ForceInline
    public Float256Vector mul(Vector<Float> v, VectorMask<Float> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector min(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public Float256Vector min(Vector<Float> v, VectorMask<Float> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector max(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float) Math.max(a, b)));
        }

    @Override
    @ForceInline
    public Float256Vector max(Vector<Float> v, VectorMask<Float> m) {
        return blend(max(v), m);
    }


    // Ternary operations

    @Override
    @ForceInline
    public Float256Vector fma(Vector<Float> o1, Vector<Float> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Float256Vector v1 = (Float256Vector)o1;
        Float256Vector v2 = (Float256Vector)o2;
        return VectorIntrinsics.ternaryOp(
            VECTOR_OP_FMA, Float256Vector.class, float.class, LENGTH,
            this, v1, v2,
            (w1, w2, w3) -> w1.tOp(w2, w3, (i, a, b, c) -> Math.fma(a, b, c)));
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public float addLanes() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_ADD, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp((float) 0, (i, a, b) -> (float) (a + b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float mulLanes() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MUL, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp((float) 1, (i, a, b) -> (float) (a * b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float minLanes() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MIN, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp(Float.POSITIVE_INFINITY , (i, a, b) -> (float) Math.min(a, b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float maxLanes() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MAX, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp(Float.NEGATIVE_INFINITY, (i, a, b) -> (float) Math.max(a, b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }


    @Override
    @ForceInline
    public float addLanes(VectorMask<Float> m) {
        return FloatVector.broadcast(SPECIES, (float) 0).blend(this, m).addLanes();
    }


    @Override
    @ForceInline
    public float mulLanes(VectorMask<Float> m) {
        return FloatVector.broadcast(SPECIES, (float) 1).blend(this, m).mulLanes();
    }

    @Override
    @ForceInline
    public float minLanes(VectorMask<Float> m) {
        return FloatVector.broadcast(SPECIES, Float.POSITIVE_INFINITY).blend(this, m).minLanes();
    }

    @Override
    @ForceInline
    public float maxLanes(VectorMask<Float> m) {
        return FloatVector.broadcast(SPECIES, Float.NEGATIVE_INFINITY).blend(this, m).maxLanes();
    }

    @Override
    @ForceInline
    public VectorShuffle<Float> toShuffle() {
        float[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(SPECIES, sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT         = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_FLOAT_INDEX_SCALE);
    private static final int BOOLEAN_ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BOOLEAN_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(float[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(Float256Vector.class, float.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(float[] a, int ax, VectorMask<Float> m) {
        FloatVector oldVal = FloatVector.fromArray(SPECIES, a, ax);
        FloatVector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }
    @Override
    @ForceInline
    public void intoArray(float[] a, int ix, int[] b, int iy) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
        IntVector vix = IntVector.fromArray(INDEX_SPECIES, b, iy).add(ix);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorIntrinsics.storeWithMap(Float256Vector.class, float.class, LENGTH, Int256Vector.class,
                               a, Unsafe.ARRAY_FLOAT_BASE_OFFSET, vix,
                               this,
                               a, ix, b, iy,
                               (arr, idx, v, indexMap, idy) -> v.forEach((i, e) -> arr[idx+indexMap[idy+i]] = e));
    }

     @Override
     @ForceInline
     public final void intoArray(float[] a, int ax, VectorMask<Float> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         FloatVector oldVal = FloatVector.fromArray(SPECIES, a, ax, b, iy);
         FloatVector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Float256Vector.class, float.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   FloatBuffer tb = bbc.asFloatBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, VectorMask<Float> m) {
        Float256Vector oldVal = (Float256Vector) FloatVector.fromByteArray(SPECIES, a, ix);
        Float256Vector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(Float256Vector.class, float.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   FloatBuffer tb = bbc.asFloatBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, VectorMask<Float> m) {
        Float256Vector oldVal = (Float256Vector) FloatVector.fromByteBuffer(SPECIES, bb, ix);
        Float256Vector newVal = oldVal.blend(this, m);
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

        Float256Vector that = (Float256Vector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    Float256Mask bTest(Vector<Float> o, FBinTest f) {
        float[] vec1 = getElements();
        float[] vec2 = ((Float256Vector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Float256Mask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public Float256Mask equal(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_eq, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public Float256Mask notEqual(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_ne, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public Float256Mask lessThan(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_lt, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public Float256Mask lessThanEq(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_le, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public Float256Mask greaterThan(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return (Float256Mask) VectorIntrinsics.compare(
            BT_gt, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public Float256Mask greaterThanEq(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_ge, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        float[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(VectorMask<Float> o, FUnCon f) {
        boolean[] mbits = ((Float256Mask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }

    Int256Vector toBits() {
        float[] vec = getElements();
        int[] res = new int[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Float.floatToIntBits(vec[i]);
        }
        return new Int256Vector(res);
    }


    @Override
    public Float256Vector rotateLanesLeft(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new Float256Vector(res);
    }

    @Override
    public Float256Vector rotateLanesRight(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new Float256Vector(res);
    }

    @Override
    public Float256Vector shiftLanesLeft(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new Float256Vector(res);
    }

    @Override
    public Float256Vector shiftLanesRight(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new Float256Vector(res);
    }

    @Override
    @ForceInline
    public Float256Vector rearrange(Vector<Float> v,
                                  VectorShuffle<Float> s, VectorMask<Float> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public Float256Vector rearrange(VectorShuffle<Float> o1) {
        Objects.requireNonNull(o1);
        Float256Shuffle s =  (Float256Shuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            Float256Vector.class, Float256Shuffle.class, float.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
                int ei = s_.lane(i);
                return v1.lane(ei);
            }));
    }

    @Override
    @ForceInline
    public Float256Vector blend(Vector<Float> o1, VectorMask<Float> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Float256Vector v = (Float256Vector)o1;
        Float256Mask   m = (Float256Mask)o2;

        return VectorIntrinsics.blend(
            Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.lane(i) ? b : a));
    }

    // Accessors

    @Override
    public float lane(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        int bits = (int) VectorIntrinsics.extract(
                                Float256Vector.class, float.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    float[] vecarr = vec.getElements();
                                    return (long)Float.floatToIntBits(vecarr[ix]);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    public Float256Vector with(int i, float e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                Float256Vector.class, float.class, LENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    float[] res = v.getElements().clone();
                                    res[ix] = Float.intBitsToFloat((int)bits);
                                    return new Float256Vector(res);
                                });
    }

    // Mask

    static final class Float256Mask extends AbstractMask<Float> {
        static final Float256Mask TRUE_MASK = new Float256Mask(true);
        static final Float256Mask FALSE_MASK = new Float256Mask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Float256Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Float256Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Float256Mask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Float256Mask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Float256Mask(res);
        }

        @Override
        Float256Mask bOp(VectorMask<Float> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Float256Mask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Float256Mask(res);
        }

        @Override
        public VectorSpecies<Float> species() {
            return SPECIES;
        }

        @Override
        public Float256Vector toVector() {
            float[] res = new float[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (float) (bits[i] ? -1 : 0);
            }
            return new Float256Vector(res);
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
                return (VectorMask <E>) new Byte256Vector.Byte256Mask(maskArray);
            } else if (stype == short.class) {
                return (VectorMask <E>) new Short256Vector.Short256Mask(maskArray);
            } else if (stype == int.class) {
                return (VectorMask <E>) new Int256Vector.Int256Mask(maskArray);
            } else if (stype == long.class) {
                return (VectorMask <E>) new Long256Vector.Long256Mask(maskArray);
            } else if (stype == float.class) {
                return (VectorMask <E>) new Float256Vector.Float256Mask(maskArray);
            } else if (stype == double.class) {
                return (VectorMask <E>) new Double256Vector.Double256Mask(maskArray);
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }
        }

        // Unary operations

        @Override
        @ForceInline
        public Float256Mask not() {
            return (Float256Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Float256Mask.class, int.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Float256Mask and(VectorMask<Float> o) {
            Objects.requireNonNull(o);
            Float256Mask m = (Float256Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Float256Mask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float256Mask or(VectorMask<Float> o) {
            Objects.requireNonNull(o);
            Float256Mask m = (Float256Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Float256Mask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(BT_ne, Float256Mask.class, int.class, LENGTH,
                                         this, this,
                                         (m, __) -> anyTrueHelper(((Float256Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(BT_overflow, Float256Mask.class, int.class, LENGTH,
                                         this, VectorMask.maskAllTrue(species()),
                                         (m, __) -> allTrueHelper(((Float256Mask)m).getBits()));
        }
    }

    // Shuffle

    static final class Float256Shuffle extends AbstractShuffle<Float> {
        Float256Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Float256Shuffle(int[] reorder) {
            super(reorder);
        }

        public Float256Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Float256Shuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public VectorSpecies<Float> species() {
            return SPECIES;
        }

        @Override
        public FloatVector toVector() {
            float[] va = new float[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (float) lane(i);
            }
            return FloatVector.fromArray(SPECIES, va, 0);
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
                return (VectorShuffle<F>) new Byte256Vector.Byte256Shuffle(shuffleArray);
            } else if (stype == short.class) {
                return (VectorShuffle<F>) new Short256Vector.Short256Shuffle(shuffleArray);
            } else if (stype == int.class) {
                return (VectorShuffle<F>) new Int256Vector.Int256Shuffle(shuffleArray);
            } else if (stype == long.class) {
                return (VectorShuffle<F>) new Long256Vector.Long256Shuffle(shuffleArray);
            } else if (stype == float.class) {
                return (VectorShuffle<F>) new Float256Vector.Float256Shuffle(shuffleArray);
            } else if (stype == double.class) {
                return (VectorShuffle<F>) new Double256Vector.Double256Shuffle(shuffleArray);
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }
        }

        @Override
        public Float256Shuffle rearrange(VectorShuffle<Float> o) {
            Float256Shuffle s = (Float256Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new Float256Shuffle(r);
        }
    }

    // VectorSpecies

    @Override
    public VectorSpecies<Float> species() {
        return SPECIES;
    }
}