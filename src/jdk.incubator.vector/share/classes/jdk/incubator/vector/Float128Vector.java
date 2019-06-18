/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import jdk.internal.vm.annotation.Stable;

import static jdk.incubator.vector.VectorIntrinsics.*;
import static jdk.incubator.vector.VectorOperators.*;

// -- This file was mechanically generated: Do not edit! -- //

@SuppressWarnings("cast")  // warning: redundant cast
final class Float128Vector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_128;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Float128Vector> VCLASS = Float128Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount();

    static final Class<Float> ETYPE = float.class;

    // The JVM expects to find the state here.
    private final float[] vec; // Don't access directly, use getElements() instead.

    Float128Vector(float[] v) {
        vec = v;
    }

    // For compatibility as Float128Vector::new,
    // stored into species.vectorFactory.
    Float128Vector(Object v) {
        this((float[]) v);
    }

    static final Float128Vector ZERO = new Float128Vector(new float[VLENGTH]);
    static final Float128Vector IOTA = new Float128Vector(VSPECIES.iotaArray());

    static {
        // Warm up a few species caches.
        // If we do this too much we will
        // get NPEs from bootstrap circularity.
        VSPECIES.dummyVector();
        VSPECIES.withLanes(LaneType.BYTE);
    }

    // Specialized extractors

    @ForceInline
    final @Override
    public FloatSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }


    /*package-private*/
    @ForceInline
    final @Override
    float[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final Float128Vector broadcast(float e) {
        return (Float128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Vector broadcast(long e) {
        return (Float128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Float128Mask maskFromArray(boolean[] bits) {
        return new Float128Mask(bits);
    }

    @Override
    @ForceInline
    Float128Shuffle iotaShuffle() { return Float128Shuffle.IOTA; }

    @Override
    @ForceInline
    Float128Shuffle shuffleFromBytes(byte[] reorder) { return new Float128Shuffle(reorder); }

    @Override
    @ForceInline
    Float128Shuffle shuffleFromArray(int[] indexes, int i) { return new Float128Shuffle(indexes, i); }

    @Override
    @ForceInline
    Float128Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Float128Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Float128Vector vectorFactory(float[] vec) {
        return new Float128Vector(vec);
    }

    @ForceInline
    final @Override
    Byte128Vector asByteVectorRaw() {
        return (Byte128Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    final @Override
    Float128Vector uOp(FUnOp f) {
        return (Float128Vector) super.uOp(f);  // specialize
    }

    @ForceInline
    final @Override
    Float128Vector uOp(VectorMask<Float> m, FUnOp f) {
        return (Float128Vector) super.uOp((Float128Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Float128Vector bOp(Vector<Float> o, FBinOp f) {
        return (Float128Vector) super.bOp((Float128Vector)o, f);  // specialize
    }

    @ForceInline
    final @Override
    Float128Vector bOp(Vector<Float> o,
                     VectorMask<Float> m, FBinOp f) {
        return (Float128Vector) super.bOp((Float128Vector)o, (Float128Mask)m,
                                        f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Float128Vector tOp(Vector<Float> o1, Vector<Float> o2, FTriOp f) {
        return (Float128Vector) super.tOp((Float128Vector)o1, (Float128Vector)o2,
                                        f);  // specialize
    }

    @ForceInline
    final @Override
    Float128Vector tOp(Vector<Float> o1, Vector<Float> o2,
                     VectorMask<Float> m, FTriOp f) {
        return (Float128Vector) super.tOp((Float128Vector)o1, (Float128Vector)o2,
                                        (Float128Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    float rOp(float v, FBinOp f) {
        return super.rOp(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Float,F> conv,
                           VectorSpecies<F> rsp, int part) {
        return super.convertShapeTemplate(conv, rsp, part);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> reinterpretShape(VectorSpecies<F> toSpecies, int part) {
        return super.reinterpretShapeTemplate(toSpecies, part);  // specialize
    }

    // Specialized algebraic operations:

    // The following definition forces a specialized version of this
    // crucial method into the v-table of this class.  A call to add()
    // will inline to a call to lanewise(ADD,), at which point the JIT
    // intrinsic will have the opcode of ADD, plus all the metadata
    // for this particular class, enabling it to generate precise
    // code.
    //
    // There is probably no benefit to the JIT to specialize the
    // masked or broadcast versions of the lanewise method.

    @Override
    @ForceInline
    public Float128Vector lanewise(Unary op) {
        return (Float128Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector lanewise(Binary op, Vector<Float> v) {
        return (Float128Vector) super.lanewiseTemplate(op, v);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Float128Vector
    lanewise(VectorOperators.Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (Float128Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float128Vector addIndex(int scale) {
        return (Float128Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final float reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final float reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Float> m) {
        return super.reduceLanesTemplate(op, m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Float> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @Override
    @ForceInline
    public VectorShuffle<Float> toShuffle() {
        float[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(VSPECIES, sa, 0);
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(Float128Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, float s) {
        return super.compareTemplate(Float128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float128Mask compare(Comparison op, long s) {
        return super.compareTemplate(Float128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector blend(Vector<Float> v, VectorMask<Float> m) {
        return (Float128Vector)
            super.blendTemplate(Float128Mask.class,
                                (Float128Vector) v,
                                (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector slice(int origin, Vector<Float> v) {
        return (Float128Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector unslice(int origin, Vector<Float> w, int part) {
        return (Float128Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (Float128Vector)
            super.unsliceTemplate(Float128Mask.class,
                                  origin, w, part,
                                  (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector rearrange(VectorShuffle<Float> s) {
        return (Float128Vector)
            super.rearrangeTemplate(Float128Shuffle.class,
                                    (Float128Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (Float128Vector)
            super.rearrangeTemplate(Float128Shuffle.class,
                                    (Float128Shuffle) shuffle,
                                    (Float128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (Float128Vector)
            super.rearrangeTemplate(Float128Shuffle.class,
                                    (Float128Shuffle) s,
                                    (Float128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector selectFrom(Vector<Float> v) {
        return (Float128Vector)
            super.selectFromTemplate((Float128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float128Vector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (Float128Vector)
            super.selectFromTemplate((Float128Vector) v,
                                     (Float128Mask) m);  // specialize
    }


    @Override
    public float lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        int bits = (int) VectorIntrinsics.extract(
                                VCLASS, ETYPE, VLENGTH,
                                this, i,
                                (vec, ix) -> {
                                    float[] vecarr = vec.getElements();
                                    return (long)Float.floatToIntBits(vecarr[ix]);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    public Float128Vector withLane(int i, float e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return VectorIntrinsics.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    float[] res = v.getElements().clone();
                                    res[ix] = Float.intBitsToFloat((int)bits);
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class Float128Mask extends AbstractMask<Float> {

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Float128Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Float128Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[vspecies().laneCount()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Float128Mask(boolean val) {
            boolean[] bits = new boolean[vspecies().laneCount()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        @ForceInline
        final @Override
        public FloatSpecies vspecies() {
            // ISSUE:  This should probably be a @Stable
            // field inside AbstractMask, rather than
            // a megamorphic method.
            return VSPECIES;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Float128Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Float128Mask(res);
        }

        @Override
        Float128Mask bOp(VectorMask<Float> o, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Float128Mask)o).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Float128Mask(res);
        }

        @ForceInline
        @Override
        public final
        Float128Vector toVector() {
            return (Float128Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        public <E> VectorMask<E> cast(VectorSpecies<E> s) {
            AbstractSpecies<E> species = (AbstractSpecies<E>) s;
            if (length() != species.laneCount())
                throw new IllegalArgumentException("VectorMask length and species length differ");
            boolean[] maskArray = toArray();
            // enum-switches don't optimize properly JDK-8161245
            switch (species.laneType.switchKey) {
            case LaneType.SK_BYTE:
                return new Byte128Vector.Byte128Mask(maskArray).check(species);
            case LaneType.SK_SHORT:
                return new Short128Vector.Short128Mask(maskArray).check(species);
            case LaneType.SK_INT:
                return new Int128Vector.Int128Mask(maskArray).check(species);
            case LaneType.SK_LONG:
                return new Long128Vector.Long128Mask(maskArray).check(species);
            case LaneType.SK_FLOAT:
                return new Float128Vector.Float128Mask(maskArray).check(species);
            case LaneType.SK_DOUBLE:
                return new Double128Vector.Double128Mask(maskArray).check(species);
            }

            // Should not reach here.
            throw new AssertionError(species);
        }

        // Unary operations

        @Override
        @ForceInline
        public Float128Mask not() {
            return (Float128Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Float128Mask.class, int.class, VLENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Float128Mask and(VectorMask<Float> o) {
            Objects.requireNonNull(o);
            Float128Mask m = (Float128Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Float128Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float128Mask or(VectorMask<Float> o) {
            Objects.requireNonNull(o);
            Float128Mask m = (Float128Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Float128Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(BT_ne, Float128Mask.class, int.class, VLENGTH,
                                         this, this,
                                         (m, __) -> anyTrueHelper(((Float128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(BT_overflow, Float128Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Float128Mask)m).getBits()));
        }

        /*package-private*/
        static Float128Mask maskAll(boolean bit) {
            return bit ? TRUE_MASK : FALSE_MASK;
        }
        static final Float128Mask TRUE_MASK = new Float128Mask(true);
        static final Float128Mask FALSE_MASK = new Float128Mask(false);
    }

    // Shuffle

    static final class Float128Shuffle extends AbstractShuffle<Float> {
        Float128Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Float128Shuffle(int[] reorder) {
            super(reorder);
        }

        public Float128Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Float128Shuffle(IntUnaryOperator fn) {
            super(fn);
        }

        @Override
        public FloatSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Float128Shuffle IOTA = new Float128Shuffle(IDENTITY);

        @Override
        public Float128Vector toVector() {
            return (Float128Vector) super.toVectorTemplate();  // specialize
        }

        @Override
        @ForceInline
        public <F> VectorShuffle<F> cast(VectorSpecies<F> s) {
            AbstractSpecies<F> species = (AbstractSpecies<F>) s;
            if (length() != species.laneCount())
                throw new AssertionError("NYI: Shuffle length and species length differ");
            int[] shuffleArray = toArray();
            // enum-switches don't optimize properly JDK-8161245
            switch (species.laneType.switchKey) {
            case LaneType.SK_BYTE:
                return new Byte128Vector.Byte128Shuffle(shuffleArray).check(species);
            case LaneType.SK_SHORT:
                return new Short128Vector.Short128Shuffle(shuffleArray).check(species);
            case LaneType.SK_INT:
                return new Int128Vector.Int128Shuffle(shuffleArray).check(species);
            case LaneType.SK_LONG:
                return new Long128Vector.Long128Shuffle(shuffleArray).check(species);
            case LaneType.SK_FLOAT:
                return new Float128Vector.Float128Shuffle(shuffleArray).check(species);
            case LaneType.SK_DOUBLE:
                return new Double128Vector.Double128Shuffle(shuffleArray).check(species);
            }

            // Should not reach here.
            throw new AssertionError(species);
        }

        @Override
        public Float128Shuffle rearrange(VectorShuffle<Float> o) {
            Float128Shuffle s = (Float128Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                int ssi = s.reorder[i];
                r[i] = this.reorder[ssi];  // throws on exceptional index
            }
            return new Float128Shuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset) {
        return super.fromArray0(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset) {
        super.intoArray0(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoByteArray0(byte[] a, int offset) {
        super.intoByteArray0(a, offset);  // specialize
    }

    // End of specialized low-level memory operations.

    // ================================================

}