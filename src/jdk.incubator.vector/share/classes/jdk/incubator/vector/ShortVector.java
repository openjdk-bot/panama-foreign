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
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.concurrent.ThreadLocalRandom;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;

import static jdk.incubator.vector.VectorIntrinsics.*;
import static jdk.incubator.vector.VectorOperators.*;

// -- This file was mechanically generated: Do not edit! -- //

/**
 * A specialized {@link Vector} representing an ordered immutable sequence of
 * {@code short} values.
 */
@SuppressWarnings("cast")  // warning: redundant cast
public abstract class ShortVector extends AbstractVector<Short> {

    ShortVector() {}

    static final int FORBID_OPCODE_KIND = VO_ONLYFP;

    @ForceInline
    static final int opCode(Operator op) {
        return VectorOperators.opCode(op, VO_OPCODE_VALID, FORBID_OPCODE_KIND);
    }
    @ForceInline
    static final int opCode(Operator op, int requireKind) {
        requireKind |= VO_OPCODE_VALID;
        return VectorOperators.opCode(op, requireKind, FORBID_OPCODE_KIND);
    }
    @ForceInline
    static final boolean opKind(Operator op, int bit) {
        return VectorOperators.opKind(op, bit);
    }

    // Virtualized factories and operators,
    // coded with portable definitions.
    // These are all @ForceInline in case
    // they need to be used performantly.
    // The various shape-specific subclasses
    // also specialize them by wrapping
    // them in a call like this:
    //    return (Byte128Vector)
    //       super.bOp((Byte128Vector) o);
    // The purpose of that is to forcibly inline
    // the generic definition from this file
    // into a sharply type- and size-specific
    // wrapper in the subclass file, so that
    // the JIT can specialize the code.
    // The code is only inlined and expanded
    // if it gets hot.  Think of it as a cheap
    // and lazy version of C++ templates.

    // Virtualized getter

    /*package-private*/
    abstract short[] getElements();

    // Virtualized constructors

    /**
     * Build a vector directly using my own constructor.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    abstract ShortVector vectorFactory(short[] vec);

    /**
     * Build a mask directly using my species.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    @ForceInline
    AbstractMask<Short> maskFactory(boolean[] bits) {
        return vspecies().maskFactory(bits);
    }

    // Constant loader (takes dummy as vector arg)
    interface FVOp {
        short apply(int i);
    }

    /*package-private*/
    @ForceInline
    ShortVector vOp(FVOp f) {
        short[] res = new short[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i);
        }
        return vectorFactory(res);
    }

    @ForceInline
    ShortVector vOp(VectorMask<Short> m, FVOp f) {
        short[] res = new short[length()];
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(i);
            }
        }
        return vectorFactory(res);
    }

    // Unary operator

    /*package-private*/
    interface FUnOp {
        short apply(int i, short a);
    }

    /*package-private*/
    @ForceInline
    ShortVector uOp(FUnOp f) {
        short[] vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    ShortVector uOp(VectorMask<Short> m,
                             FUnOp f) {
        short[] vec = getElements();
        short[] res = new short[length()];
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return vectorFactory(res);
    }

    // Binary operator

    /*package-private*/
    interface FBinOp {
        short apply(int i, short a, short b);
    }

    /*package-private*/
    @ForceInline
    ShortVector bOp(Vector<Short> o,
                             FBinOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortVector)o).getElements();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    ShortVector bOp(Vector<Short> o,
                             VectorMask<Short> m,
                             FBinOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortVector)o).getElements();
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Ternary operator

    /*package-private*/
    interface FTriOp {
        short apply(int i, short a, short b, short c);
    }

    /*package-private*/
    @ForceInline
    ShortVector tOp(Vector<Short> o1,
                             Vector<Short> o2,
                             FTriOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortVector)o1).getElements();
        short[] vec3 = ((ShortVector)o2).getElements();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    ShortVector tOp(Vector<Short> o1,
                             Vector<Short> o2,
                             VectorMask<Short> m,
                             FTriOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortVector)o1).getElements();
        short[] vec3 = ((ShortVector)o2).getElements();
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Reduction operator

    /*package-private*/
    @ForceInline
    short rOp(short v, FBinOp f) {
        short[] vec = getElements();
        for (int i = 0; i < vec.length; i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Memory reference

    /*package-private*/
    interface FLdOp<M> {
        short apply(M memory, int offset, int i);
    }

    /*package-private*/
    @ForceInline
    <M> ShortVector ldOp(M memory, int offset,
                                  FLdOp<M> f) {
        //dummy; no vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(memory, offset, i);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    <M> ShortVector ldOp(M memory, int offset,
                                  VectorMask<Short> m,
                                  FLdOp<M> f) {
        //short[] vec = getElements();
        short[] res = new short[length()];
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(memory, offset, i);
            }
        }
        return vectorFactory(res);
    }

    interface FStOp<M> {
        void apply(M memory, int offset, int i, short a);
    }

    /*package-private*/
    @ForceInline
    <M> void stOp(M memory, int offset,
                  FStOp<M> f) {
        short[] vec = getElements();
        for (int i = 0; i < vec.length; i++) {
            f.apply(memory, offset, i, vec[i]);
        }
    }

    /*package-private*/
    @ForceInline
    <M> void stOp(M memory, int offset,
                  VectorMask<Short> m,
                  FStOp<M> f) {
        short[] vec = getElements();
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < vec.length; i++) {
            if (mbits[i]) {
                f.apply(memory, offset, i, vec[i]);
            }
        }
    }

    // Binary test

    /*package-private*/
    interface FBinTest {
        boolean apply(int cond, int i, short a, short b);
    }

    /*package-private*/
    @ForceInline
    AbstractMask<Short> bTest(int cond,
                                  Vector<Short> o,
                                  FBinTest f) {
        short[] vec1 = getElements();
        short[] vec2 = ((ShortVector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(cond, i, vec1[i], vec2[i]);
        }
        return maskFactory(bits);
    }

    /*package-private*/
    @ForceInline
    static boolean doBinTest(int cond, short a, short b) {
        switch (cond) {
        case BT_eq:  return a == b;
        case BT_ne:  return a != b;
        case BT_lt:  return a < b;
        case BT_le:  return a <= b;
        case BT_gt:  return a > b;
        case BT_ge:  return a >= b;
        }
        throw new AssertionError(Integer.toHexString(cond));
    }

    /*package-private*/
    @ForceInline
    @Override
    abstract ShortSpecies vspecies();

    /*package-private*/
    @ForceInline
    static long toBits(short e) {
        return  e;
    }

    /*package-private*/
    @ForceInline
    static short fromBits(long bits) {
        return ((short)bits);
    }

    // Static factories (other than memory operations)

    // Note: A surprising behavior in javadoc
    // sometimes makes a lone /** {@inheritDoc} */
    // comment drop the method altogether,
    // apparently if the method mentions an
    // parameter or return type of Vector<Short>
    // instead of Vector<E> as originally specified.
    // Adding an empty HTML fragment appears to
    // nudge javadoc into providing the desired
    // inherited documentation.  We use the HTML
    // comment <!--workaround--> for this.

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public static ShortVector zero(VectorSpecies<Short> species) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.zero();
    }

    /**
     * Returns a vector of the same species as this one
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @see #broadcast(VectorSpecies,long)
     * @see Vector#broadcast(long)
     * @see VectorSpecies#broadcast(long)
     */
    public abstract ShortVector broadcast(short e);

    /**
     * Returns a vector of the given species
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * @param species species of the desired vector
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @see #broadcast(long)
     * @see Vector#broadcast(long)
     * @see VectorSpecies#broadcast(long)
     */
    public static ShortVector broadcast(VectorSpecies<Short> species, short e) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final ShortVector broadcastTemplate(short e) {
        ShortSpecies vsp = vspecies();
        return vsp.broadcast(e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract ShortVector broadcast(long e);

    /**
     * Returns a vector of the given species
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * The {@code long} value must be accurately representable
     * by the {@code ETYPE} of the vector species, so that
     * {@code e==(long)(ETYPE)e}.
     *
     * @param species species of the desired vector
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector's {@code ETYPE}
     * @see #broadcast(VectorSpecies,short)
     * @see VectorSpecies#checkValue(VectorSpecies,short)
     */
    public static ShortVector broadcast(VectorSpecies<Short> species, long e) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final ShortVector broadcastTemplate(long e) {
        return vspecies().broadcast(e);
    }

    /**
     * Returns a vector where each lane element is set to given
     * primitive values.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index, the
     * the primitive value at index {@code N} is placed into the resulting
     * vector at lane index {@code N}.
     *
     * @param species species of the desired vector
     * @param es the given primitive values
     * @return a vector where each lane element is set to given primitive
     * values
     * @throws IllegalArgumentException
     *         if {@code es.length != species.length()}
     */
    @ForceInline
    @SuppressWarnings("unchecked")
    public static ShortVector fromValues(VectorSpecies<Short> species, short... es) {
        ShortSpecies vsp = (ShortSpecies) species;
        int vlength = vsp.laneCount();
        VectorIntrinsics.requireLength(es.length, vlength);
        // Get an unaliased copy and use it directly:
        return vsp.vectorFactory(Arrays.copyOf(es, vlength));
    }

    /**
     * Returns a vector where the first lane element is set to the primtive
     * value {@code e}, all other lane elements are set to the default
     * value.
     *
     * @param species species of the desired vector
     * @param e the value
     * @return a vector where the first lane element is set to the primitive
     * value {@code e}
     */
    // FIXME: Does this carry its weight?
    @ForceInline
    public static final ShortVector single(VectorSpecies<Short> species, short e) {
        return zero(species).withLane(0, e);
    }

    /**
     * Returns a vector where each lane element is set to a randomly
     * generated primitive value.
     *
     * The semantics are equivalent to calling
     * {@code (short)}{@link ThreadLocalRandom#nextInt()}
     * for each lane, from first to last.
     *
     * @param species species of the desired vector
     * @return a vector where each lane elements is set to a randomly
     * generated primitive value
     */
    public static ShortVector random(VectorSpecies<Short> species) {
        ShortSpecies vsp = (ShortSpecies) species;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return vsp.vOp(i -> nextRandom(r));
    }
    private static short nextRandom(ThreadLocalRandom r) {
        return (short) r.nextInt();
    }

    // Unary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public abstract ShortVector lanewise(VectorOperators.Unary op);

    @ForceInline
    final
    ShortVector lanewiseTemplate(VectorOperators.Unary op) {
        if (opKind(op, VO_SPECIAL)) {
            if (op == ZOMO) {
                return blend(broadcast(-1), compare(NE, 0));
            }
            if (op == NEG) {
                // FIXME: Support this in the JIT.
                return broadcast(0).lanewiseTemplate(SUB, this);
            }
        }
        int opc = opCode(op);
        return VectorIntrinsics.unaryOp(
            opc, getClass(), short.class, length(),
            this,
            UN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_NEG: return v0 ->
                        v0.uOp((i, a) -> (short) -a);
                case VECTOR_OP_ABS: return v0 ->
                        v0.uOp((i, a) -> (short) Math.abs(a));
                case VECTOR_OP_NOT: return v0 ->
                        v0.uOp((i, a) -> (short) ~a);
                default: return null;
              }}));
    }
    private static final
    ImplCache<Unary,UnaryOperator<ShortVector>> UN_IMPL
        = new ImplCache<>(Unary.class, ShortVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public ShortVector lanewise(VectorOperators.Unary op,
                                         VectorMask<Short> m) {
        return blend(lanewise(op), m);
    }

    // Binary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @Override
    public abstract ShortVector lanewise(VectorOperators.Binary op,
                                                  Vector<Short> v);
    @ForceInline
    final
    ShortVector lanewiseTemplate(VectorOperators.Binary op,
                                          Vector<Short> v) {
        ShortVector that = (ShortVector) v;
        that.check(this);
        if (opKind(op, VO_SPECIAL  | VO_SHIFT)) {
            if (op == FIRST_NONZERO) {
                // FIXME: Support this in the JIT.
                VectorMask<Short> thisNZ
                    = this.viewAsIntegralLanes().compare(NE, (short) 0);
                that = that.blend((short) 0, thisNZ.cast(vspecies()));
                op = OR_UNCHECKED;
            }
            if (opKind(op, VO_SHIFT)) {
                // As per shift specification for Java, mask the shift count.
                // This allows the JIT to ignore some ISA details.
                that = that.lanewise(AND, SHIFT_MASK);
            }
            if (op == ROR || op == ROL) {  // FIXME: JIT should do this
                ShortVector neg = that.lanewise(NEG);
                ShortVector hi = this.lanewise(LSHL, (op == ROR) ? neg : that);
                ShortVector lo = this.lanewise(LSHR, (op == ROR) ? that : neg);
                return hi.lanewise(OR, lo);
            } else if (op == ANDC2) {
                // FIXME: Support this in the JIT.
                that = that.lanewise(NOT);
                op = AND;
            } else if (op == DIV) {
                VectorMask<Short> eqz = that.eq((short)0);
                if (eqz.anyTrue()) {
                    throw that.divZeroException();
                }
            }
        }
        int opc = opCode(op);
        return VectorIntrinsics.binaryOp(
            opc, getClass(), short.class, length(),
            this, that,
            BIN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_ADD: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a + b));
                case VECTOR_OP_SUB: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a - b));
                case VECTOR_OP_MUL: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a * b));
                case VECTOR_OP_DIV: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a / b));
                case VECTOR_OP_MAX: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)Math.max(a, b));
                case VECTOR_OP_MIN: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)Math.min(a, b));
                case VECTOR_OP_FIRST_NONZERO: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> toBits(a) != 0 ? a : b);
                case VECTOR_OP_AND: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a & b));
                case VECTOR_OP_OR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a | b));
                case VECTOR_OP_ANDC2: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a & ~b));
                case VECTOR_OP_XOR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a ^ b));
                case VECTOR_OP_LSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)(a << n));
                case VECTOR_OP_RSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)(a >> n));
                case VECTOR_OP_URSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)((a & LSHR_SETUP_MASK) >>> n));
                case VECTOR_OP_LROTATE: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)((a << n)|(a >> -n)));
                case VECTOR_OP_RROTATE: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)((a >> n)|(a << -n)));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,BinaryOperator<ShortVector>> BIN_IMPL
        = new ImplCache<>(Binary.class, ShortVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  Vector<Short> v,
                                  VectorMask<Short> m) {
        ShortVector that = (ShortVector) v;
        if (op == DIV) {
            // suppress div/0 exceptions in unset lanes
            that = that.lanewise(NOT, that.eq((short)0));
            return blend(lanewise(DIV, that), m);
        }
        return blend(lanewise(op, v), m);
    }
    // FIXME: Maybe all of the public final methods in this file (the
    // simple ones that just call lanewise) should be pushed down to
    // the X-VectorBits template.  They can't optimize properly at
    // this level, and must rely on inlining.  Does it work?
    // (If it works, of course keep the code here.)

    /**
     * Combines the lane values of this vector
     * with the value of a broadcast scalar.
     * <p>
     * This is a lane-wise binary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e))}.
     *
     * @param e the input scalar
     * @return the result of applying the operation lane-wise
     *         to the two input vectors
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  short e) {
        int opc = opCode(op);
        if (opKind(op, VO_SHIFT) && (short)(int)e == e) {
            return lanewiseShift(op, (int) e);
        }
        if (op == ANDC2) {
            op = AND; e = (short) ~e;
        }
        return lanewise(op, broadcast(e));
    }

    /**
     * Combines the lane values of this vector
     * with the value of a broadcast scalar,
     * with selection of lane elements controlled by a mask.
     * <p>
     * This is a lane-wise binary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e), m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vector and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Binary,short)
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  short e,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  long e) {
        short e1 = (short) e;
        if ((long)e1 != e
            // allow shift ops to clip down their int parameters
            && !(opKind(op, VO_SHIFT) && (int)e1 == e)
            ) {
            vspecies().checkValue(e);  // for exception
        }
        return lanewise(op, e1);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  long e, VectorMask<Short> m) {
        return blend(lanewise(op, e), m);
    }

    /*package-private*/
    abstract ShortVector
    lanewiseShift(VectorOperators.Binary op, int e);

    /*package-private*/
    @ForceInline
    final ShortVector
    lanewiseShiftTemplate(VectorOperators.Binary op, int e) {
        // Special handling for these.  FIXME: Refactor?
        int opc = opCode(op);
        assert(opKind(op, VO_SHIFT));
        // As per shift specification for Java, mask the shift count.
        e &= SHIFT_MASK;
        if (op == ROR || op == ROL) {  // FIXME: JIT should do this
            ShortVector hi = this.lanewise(LSHL, (op == ROR) ? -e : e);
            ShortVector lo = this.lanewise(LSHR, (op == ROR) ? e : -e);
            return hi.lanewise(OR, lo);
        }
        return VectorIntrinsics.broadcastInt(
            opc, getClass(), short.class, length(),
            this, e,
            BIN_INT_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_LSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (short)(a << n));
                case VECTOR_OP_RSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (short)(a >> n));
                case VECTOR_OP_URSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (short)((a & LSHR_SETUP_MASK) >>> n));
                case VECTOR_OP_LROTATE: return (v, n) ->
                        v.uOp((i, a) -> (short)((a << n)|(a >> -n)));
                case VECTOR_OP_RROTATE: return (v, n) ->
                        v.uOp((i, a) -> (short)((a >> n)|(a << -n)));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,VectorBroadcastIntOp<ShortVector>> BIN_INT_IMPL
        = new ImplCache<>(Binary.class, ShortVector.class);

    // As per shift specification for Java, mask the shift count.
    // We mask 0X3F (long), 0X1F (int), 0x0F (short), 0x7 (byte).
    // The latter two maskings go beyond the JLS, but seem reasonable
    // since our lane types are first-class types, not just dressed
    // up ints.
    private static final int SHIFT_MASK = (Short.SIZE - 1);
    // Also simulate >>> on sub-word variables with a mask.
    private static final int LSHR_SETUP_MASK = ((1 << Short.SIZE) - 1);

    // Ternary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,short,short)
     */
    @Override
    public abstract ShortVector lanewise(VectorOperators.Ternary op,
                                                  Vector<Short> v1,
                                                  Vector<Short> v2);
    @ForceInline
    final
    ShortVector lanewiseTemplate(VectorOperators.Ternary op,
                                          Vector<Short> v1,
                                          Vector<Short> v2) {
        ShortVector that = (ShortVector) v1;
        ShortVector tother = (ShortVector) v2;
        // It's a word: https://www.dictionary.com/browse/tother
        // See also Chapter 11 of Dickens, Our Mutual Friend:
        // "Totherest Governor," replied Mr Riderhood...
        that.check(this);
        tother.check(this);
        if (op == BITWISE_BLEND) {
            // FIXME: Support this in the JIT.
            that = this.lanewise(XOR, that).lanewise(AND, tother);
            return this.lanewise(XOR, that);
        }
        int opc = opCode(op);
        return VectorIntrinsics.ternaryOp(
            opc, getClass(), short.class, length(),
            this, that, tother,
            TERN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_BITWISE_BLEND: return (v0, v1_, v2_) ->
                        v0.tOp(v1_, v2_, (i, a, b, c) -> (short)(a^((a^b)&c)));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Ternary,TernaryOperation<ShortVector>> TERN_IMPL
        = new ImplCache<>(Ternary.class, ShortVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op,
                                  Vector<Short> v1,
                                  Vector<Short> v2,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, v1, v2), m);
    }

    // There are no broadcasting versions of ternary lanewise,
    // because this class does not implement any ternary operations.

    // ShortVector lanewise(VectorOperators.Ternary op,
    //                               short e1,
    //                               short e2) {
    //     return lanewise(op, broadcast(e1), broadcast(e1));
    // }

    // ShortVector lanewise(VectorOperators.Ternary op,
    //                               short e1,
    //                               short e2,
    //                               VectorMask<Short> m) {
    //     return blend(lanewise(op, e), m);
    // }

    /// FULL-SERVICE BINARY METHODS: ADD, SUB, MUL, DIV
    //
    // These include masked and non-masked versions.
    // This subclass adds broadcast (masked or not).

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(short)
     */
    @Override
    public final ShortVector add(Vector<Short> v) {
        return lanewise(ADD, v);
    }

    /**
     * Adds this vector to the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , s)}.
     *
     * @param e the input scalar
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector)
     * @see #broadcast(short)
     * @see #add(int,VectorMask)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector add(short e) {
        return lanewise(ADD, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(short,VectorMask)
     */
    @Override
    public final ShortVector add(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(ADD, v, m);
    }

    /**
     * Adds this vector to the broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #add(int)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector add(short e,
                                          VectorMask<Short> m) {
        return lanewise(ADD, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(short)
     */
    @Override
    public final ShortVector sub(Vector<Short> v) {
        return lanewise(SUB, v);
    }

    /**
     * Subtracts an input scalar from this vector.
     *
     * This is a lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , s)}.
     *
     * @param e the input scalar
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector)
     * @see #broadcast(short)
     * @see #sub(int,VectorMask)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector sub(short e) {
        return lanewise(SUB, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(short,VectorMask)
     */
    @Override
    public final ShortVector sub(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(SUB, v, m);
    }

    /**
     * Subtracts an input scalar from this vector
     * under the control of a mask.
     *
     * This is a lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #sub(int)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector sub(short e,
                                          VectorMask<Short> m) {
        return lanewise(SUB, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(short)
     */
    @Override
    public final ShortVector mul(Vector<Short> v) {
        return lanewise(MUL, v);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , s)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #mul(Vector)
     * @see #broadcast(short)
     * @see #mul(int,VectorMask)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector mul(short e) {
        return lanewise(MUL, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(short,VectorMask)
     */
    @Override
    public final ShortVector mul(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(MUL, v, m);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of muling each lane of this vector to the scalar
     * @see #mul(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #mul(int)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector mul(short e,
                                          VectorMask<Short> m) {
        return lanewise(MUL, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #div(short)
     */
    @Override
    public final ShortVector div(Vector<Short> v) {
        return lanewise(DIV, v);
    }

    /**
     * Divides this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , s)}.
     *
     * <p>
     * If the underlying scalar operator does not support
     * division by zero, but is presented with a zero divisor,
     * an {@code ArithmeticException} will be thrown.
     *
     * @param e the input scalar
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector)
     * @see #broadcast(short)
     * @see #div(int,VectorMask)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector div(short e) {
        return lanewise(DIV, e);
    }

    /**
    /**
     * {@inheritDoc} <!--workaround-->
     * @see #div(short,VectorMask)
     */
    @Override
    public final ShortVector div(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(DIV, v, m);
    }

    /**
     * Divides this vector by the broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , s, m)}.
     *
     * <p>
     * If the underlying scalar operator does not support
     * division by zero, but is presented with a zero divisor,
     * an {@code ArithmeticException} will be thrown.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #div(int)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    public final ShortVector div(short e,
                                          VectorMask<Short> m) {
        return lanewise(DIV, e, m);
    }

    /// END OF FULL-SERVICE BINARY METHODS

    /// SECOND-TIER BINARY METHODS
    //
    // There are no masked or broadcast versions.

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public final ShortVector min(Vector<Short> v) {
        return lanewise(MIN, v);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public final ShortVector max(Vector<Short> v) {
        return lanewise(MAX, v);
    }

    /// UNARY OPERATIONS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public final
    ShortVector neg() {
        return lanewise(NEG);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public final
    ShortVector abs() {
        return lanewise(ABS);
    }

    /// COMPARISONS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public final
    VectorMask<Short> eq(Vector<Short> v) {
        return compare(EQ, v);
    }

    /**
     * Tests if this vector is equal to an input scalar.
     * <p>
     * This is a lane-wise binary test operation which applies
     * the primitive equals operation ({@code ==}) to each lane.
     * The result is the same as {@code compare(VectorOperators.Comparison.EQ, e)}.
     *
     * @param e the input scalar
     * @return the result mask of testing if this vector
     *         is equal to {@code e}
     * @see #compare(VectorOperators.Comparison, short)
     */
    public final
    VectorMask<Short> eq(short e) {
        return compare(EQ, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public final
    VectorMask<Short> lt(Vector<Short> v) {
        return compare(LT, v);
    }

    /**
     * Tests if this vector is less than an input scalar.
     * <p>
     * This is a lane-wise binary test operation which applies
     * the primitive less than operation ({@code <}) to each lane.
     * The result is the same as {@code compare(VectorOperators.LT, e)}.
     *
     * @param e the input scalar
     * @return the mask result of testing if this vector
     *         is less than the input scalar
     * @see #compare(VectorOperators.Comparison, short)
     */
    public final
    VectorMask<Short> lt(short e) {
        return compare(LT, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Short> compare(VectorOperators.Comparison op, Vector<Short> v);

    /*package-private*/
    final
    <M extends VectorMask<Short>>
    M compareTemplate(Class<M> maskType, Comparison op, Vector<Short> v) {
        Objects.requireNonNull(v);
        ShortSpecies vsp = vspecies();
        int opc = opCode(op);
        return VectorIntrinsics.compare(
            opc, getClass(), maskType, short.class, length(),
            this, (ShortVector) v,
            (cond, v0, v1) -> {
                AbstractMask<Short> m
                    = v0.bTest(cond, v1, (cond_, i, a, b)
                               -> compareWithOp(cond, a, b));
                @SuppressWarnings("unchecked")
                M m2 = (M) m;
                return m2;
            });
    }

    private static
    boolean compareWithOp(int cond, short a, short b) {
        switch (cond) {
        case VectorIntrinsics.BT_eq:  return a == b;
        case VectorIntrinsics.BT_ne:  return a != b;
        case VectorIntrinsics.BT_lt:  return a <  b;
        case VectorIntrinsics.BT_le:  return a <= b;
        case VectorIntrinsics.BT_gt:  return a >  b;
        case VectorIntrinsics.BT_ge:  return a >= b;
        }
        throw new AssertionError();
    }

    /**
     * Tests this vector by comparing it with an input scalar,
     * according to the given comparison operation.
     * <p>
     * This is a lane-wise binary test operation which applies
     * the comparison operation to each lane.
     * <p>
     * The result is the same as
     * {@code compare(op, broadcast(species(), s))}.
     * That is, the scalar may be regarded as broadcast to
     * a vector of the same species, and then compared
     * against the original vector, using the selected
     * comparison operation.
     *
     * @param e the input scalar
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator
     * @see #eq(short)
     * @see #lessThan(short)
     */
    public abstract
    VectorMask<Short> compare(Comparison op, short e);

    /*package-private*/
    final
    <M extends VectorMask<Short>>
    M compareTemplate(Class<M> maskType, Comparison op, short e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    VectorMask<Short> compare(Comparison op, long e);

    /*package-private*/
    final
    <M extends VectorMask<Short>>
    M compareTemplate(Class<M> maskType, Comparison op, long e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    ShortVector blend(Vector<Short> v, VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    <M extends VectorMask<Short>>
    ShortVector
    blendTemplate(Class<M> maskType, ShortVector v, M m) {
        v.check(this);
        return VectorIntrinsics.blend(
            getClass(), maskType, short.class, length(),
            this, v, m,
            (v0, v1, m_) -> v0.bOp(v1, m_, (i, a, b) -> b));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract ShortVector addIndex(int scale);

    /*package-private*/
    @ForceInline
    final ShortVector addIndexTemplate(int scale) {
        ShortSpecies vsp = vspecies();
        // make sure VLENGTH*scale doesn't overflow:
        vsp.checkScale(scale);
        return VectorIntrinsics.indexVector(
            getClass(), short.class, length(),
            this, scale, vsp,
            (v, scale_, s)
            -> {
                // If the platform doesn't support an INDEX
                // instruction directly, load IOTA from memory
                // and multiply.
                ShortVector iota = s.iota();
                short sc = (short) scale_;
                return v.add(sc == 1 ? iota : iota.mul(sc));
            });
    }

    /**
     * Blends the lane elements of this vector with those of the broadcast of an
     * input scalar, selecting lanes controlled by a mask.
     * <p>
     * For each lane of the mask, at lane index {@code N}, if the mask lane
     * is set then the lane element at {@code N} from the input vector is
     * selected and placed into the resulting vector at {@code N},
     * otherwise the lane element at {@code N} from this input vector is
     * selected and placed into the resulting vector at {@code N}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of blending the lane elements of this vector with
     * those of the broadcast of an input scalar
     */
    public final ShortVector blend(short e,
                                            VectorMask<Short> m) {
        return blend(broadcast(e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    ShortVector slice(int origin, Vector<Short> v1);

    /*package-private*/
    final
    @ForceInline
    ShortVector sliceTemplate(int origin, Vector<Short> v1) {
        ShortVector that = (ShortVector) v1;
        that.check(this);
        short[] a0 = this.getElements();
        short[] a1 = that.getElements();
        short[] res = new short[a0.length];
        int vlen = res.length;
        int firstPart = vlen - origin;
        System.arraycopy(a0, origin, res, 0, firstPart);
        System.arraycopy(a1, 0, res, firstPart, origin);
        return vectorFactory(res);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public final ShortVector
    slice(int origin, Vector<Short> w, VectorMask<Short> m) {
        return broadcast(0).blend(slice(origin, w), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public final ShortVector slice(int origin) {
        return slice(origin, broadcast(0));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    ShortVector unslice(int origin, Vector<Short> w, int part);

    /*package-private*/
    final
    @ForceInline
    ShortVector
    unsliceTemplate(int origin, Vector<Short> w, int part) {
        ShortVector that = (ShortVector) w;
        that.check(this);
        short[] slice = this.getElements();
        short[] res = that.getElements();
        int vlen = res.length;
        int firstPart = vlen - origin;
        switch (part) {
        case 0:
            System.arraycopy(slice, 0, res, origin, firstPart);
            break;
        case 1:
            System.arraycopy(slice, firstPart, res, 0, origin);
            break;
        default:
            throw wrongPartForSlice(part);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    final
    @ForceInline
    <M extends VectorMask<Short>>
    ShortVector
    unsliceTemplate(Class<M> maskType, int origin, Vector<Short> w, int part, M m) {
        ShortVector that = (ShortVector) w;
        that.check(this);
        ShortVector slice = that.sliceTemplate(origin, that);
        slice = slice.blendTemplate(maskType, this, m);
        return slice.unsliceTemplate(origin, w, part);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract ShortVector
    unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public final ShortVector unslice(int origin) {
        return unslice(origin, broadcast(0), 0);
    }

    private ArrayIndexOutOfBoundsException
    wrongPartForSlice(int part) {
        String msg = String.format("bad part number %d for slice operation",
                                   part);
        return new ArrayIndexOutOfBoundsException(msg);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract ShortVector rearrange(VectorShuffle<Short> m);

    /*package-private*/
    @ForceInline
    <S extends VectorShuffle<Short>>
    ShortVector rearrangeTemplate(Class<S> shuffletype, S shuffle) {
        shuffle.checkIndexes();
        return VectorIntrinsics.rearrangeOp(
            getClass(), shuffletype, short.class, length(),
            this, shuffle,
            (v1, s_) -> v1.uOp((i, a) -> {
                int ei = s_.laneSource(i);
                return v1.lane(ei);
            }));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract ShortVector rearrange(VectorShuffle<Short> s,
                                                   VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    <S extends VectorShuffle<Short>>
    ShortVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           VectorMask<Short> m) {
        ShortVector unmasked =
            VectorIntrinsics.rearrangeOp(
                getClass(), shuffletype, short.class, length(),
                this, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return ei < 0 ? 0 : v1.lane(ei);
                }));
        VectorMask<Short> valid = shuffle.laneIsValid();
        if (m.andNot(valid).anyTrue()) {
            shuffle.checkIndexes();
            throw new AssertionError();
        }
        return broadcast((short)0).blend(unmasked, valid);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract ShortVector rearrange(VectorShuffle<Short> s,
                                                   Vector<Short> v);

    /*package-private*/
    @ForceInline
    <S extends VectorShuffle<Short>>
    ShortVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           ShortVector v) {
        VectorMask<Short> valid = shuffle.laneIsValid();
        VectorShuffle<Short> ws = shuffle.wrapIndexes();
        ShortVector r1 =
            VectorIntrinsics.rearrangeOp(
                getClass(), shuffletype, short.class, length(),
                this, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v1.lane(ei);
                }));
        ShortVector r2 =
            VectorIntrinsics.rearrangeOp(
                getClass(), shuffletype, short.class, length(),
                v, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v1.lane(ei);
                }));
        return r2.blend(r1, valid);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract ShortVector selectFrom(Vector<Short> v);

    /*package-private*/
    @ForceInline
    final ShortVector selectFromTemplate(ShortVector v) {
        return v.rearrange(this.toShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract ShortVector selectFrom(Vector<Short> s, VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    final ShortVector selectFromTemplate(ShortVector v,
                                                  AbstractMask<Short> m) {
        return v.rearrange(this.toShuffle(), m);
    }

    /// FMA


    // Type specific horizontal reductions

    /**
     * Returns a value accumulated from all the lanes of this vector.
     * <p>
     * This is an associative cross-lane reduction operation which
     * applies the specified operation to all the lane elements.
     *
     * <p>
     * A few reduction operations do not support arbitrary reordering
     * of their operands, yet are included here because of their
     * usefulness.
     *
     * <ul>
     * <li>
     * In the case of {@code FIRST_NONZERO}, the reduction returns
     * the value from the lowest-numbered non-zero lane. As with
     * {@code MAX} and {@code MIN}, floating point {@code -0.0}
     * is treated as a value distinct from the default zero value.
     *
     * <li>
     * In the case of floating point addition and multiplication, the
     * precise result will reflect the choice of an arbitrary order
     * of operations, which may even vary over time.
     *
     * <li>
     * All other reduction operations are fully commutative and
     * associative.  The implementation can choose any order of
     * processing, yet it will always produce the same result.
     *
     * </ul>
     *
     *
     * @param op the operation used to combine lane values
     * @return the accumulated result
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #reduceLanes(VectorOperators.Associative,VectorMask)
     */
    public abstract short reduceLanes(VectorOperators.Associative op);

    /**
     * Returns a value accumulated from selected lanes of this vector,
     * controlled by a mask.
     * <p>
     * This is an associative cross-lane reduction operation which
     * applies the specified operation to the selected lane elements.
     * <p>
     * If no elements are selected, an operation-specific identity
     * value is returned.
     * <ul>
     * <li>
     * If the operation is {@code ADD}, {#if[BITWISE]?{@code XOR}, {@code OR},}
     * or {@code FIRST_NONZERO},
     * then the identity value is zero, the default {@code short} value.
     * <li>
     * If the operation is {@code MUL},
     * then the identity value is one.
     * <li>
     * If the operation is {@code AND},
     * then the identity value is minus one (all bits set).
     * <li>
     * If the operation is {@code MAX},
     * then the identity value is {@code Short.MIN_VALUE}.
     * <li>
     * If the operation is {@code MIN},
     * then the identity value is {@code Short.MAX_VALUE}.
     * </ul>
     *
     * @param op the operation used to combine lane values
     * @param m the mask controlling lane selection
     * @return the reduced result accumulated from the selected lane values
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #reduceLanes(VectorOperators.Associative)
     */
    public abstract short reduceLanes(VectorOperators.Associative op,
                                       VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    short reduceLanesTemplate(VectorOperators.Associative op,
                               VectorMask<Short> m) {
        ShortVector v = reduceIdentityVector(op).blend(this, m);
        return v.reduceLanesTemplate(op);
    }

    /*package-private*/
    @ForceInline
    short reduceLanesTemplate(VectorOperators.Associative op) {
        if (op == FIRST_NONZERO) {
            // FIXME:  The JIT should handle this, and other scan ops alos.
            VectorMask<Short> thisNZ
                = this.viewAsIntegralLanes().compare(NE, (short) 0);
            return this.lane(thisNZ.firstTrue());
        }
        int opc = opCode(op);
        return fromBits(VectorIntrinsics.reductionCoerced(
            opc, getClass(), short.class, length(),
            this,
            REDUCE_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
              case VECTOR_OP_ADD: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> (short)(a + b)));
              case VECTOR_OP_MUL: return v ->
                      toBits(v.rOp((short)1, (i, a, b) -> (short)(a * b)));
              case VECTOR_OP_MIN: return v ->
                      toBits(v.rOp(MAX_OR_INF, (i, a, b) -> (short) Math.min(a, b)));
              case VECTOR_OP_MAX: return v ->
                      toBits(v.rOp(MIN_OR_INF, (i, a, b) -> (short) Math.max(a, b)));
              case VECTOR_OP_FIRST_NONZERO: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> toBits(a) != 0 ? a : b));
              case VECTOR_OP_AND: return v ->
                      toBits(v.rOp((short)-1, (i, a, b) -> (short)(a & b)));
              case VECTOR_OP_OR: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> (short)(a | b)));
              case VECTOR_OP_XOR: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> (short)(a ^ b)));
              default: return null;
              }})));
    }
    private static final
    ImplCache<Associative,Function<ShortVector,Long>> REDUCE_IMPL
        = new ImplCache<>(Associative.class, ShortVector.class);

    private
    @ForceInline
    ShortVector reduceIdentityVector(VectorOperators.Associative op) {
        int opc = opCode(op);
        UnaryOperator<ShortVector> fn
            = REDUCE_ID_IMPL.find(op, opc, (opc_) -> {
                switch (opc_) {
                case VECTOR_OP_ADD:
                case VECTOR_OP_OR:
                case VECTOR_OP_XOR:
                case VECTOR_OP_FIRST_NONZERO:
                    return v -> v.broadcast(0);
                case VECTOR_OP_MUL:
                    return v -> v.broadcast(1);
                case VECTOR_OP_AND:
                    return v -> v.broadcast(-1);
                case VECTOR_OP_MIN:
                    return v -> v.broadcast(MAX_OR_INF);
                case VECTOR_OP_MAX:
                    return v -> v.broadcast(MIN_OR_INF);
                default: return null;
                }
            });
        return fn.apply(this);
    }
    private static final
    ImplCache<Associative,UnaryOperator<ShortVector>> REDUCE_ID_IMPL
        = new ImplCache<>(Associative.class, ShortVector.class);

    private static final short MIN_OR_INF = Short.MIN_VALUE;
    private static final short MAX_OR_INF = Short.MAX_VALUE;

    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op);
    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op,
                                                     VectorMask<Short> m);

    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract short lane(int i);

    /**
     * Replaces the lane element of this vector at lane index {@code i} with
     * value {@code e}.
     * <p>
     * This is a cross-lane operation and behaves as if it returns the result
     * of blending this vector with an input vector that is the result of
     * broadcasting {@code e} and a mask that has only one lane set at lane
     * index {@code i}.
     *
     * @param i the lane index of the lane element to be replaced
     * @param e the value to be placed
     * @return the result of replacing the lane element of this vector at lane
     * index {@code i} with value {@code e}.
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract ShortVector withLane(int i, short e);

    // Memory load operations

    /**
     * Returns an array of type {@code short[]}
     * containing all the lane values.
     * The array length is the same as the vector length.
     * The array elements are stored in lane order.
     * <p>
     * This method behaves as if it stores
     * this vector into an allocated array
     * (using {@link #intoArray(short[], int) intoArray})
     * and returns the array as follows:
     * <pre>{@code
     *   short[] a = new short[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the lane values of this vector
     */
    @ForceInline
    @Override
    public final short[] toArray() {
        short[] a = new short[vspecies().laneCount()];
        intoArray(a, 0);
        return a;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type ShortVector,
     * there will be no loss of precision or range.
     */
    @ForceInline
    @Override
    public final long[] toLongArray() {
        short[] a = toArray();
        long[] res = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = (long) a[i];
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type ShortVector,
     * there will be no loss of precision.
     */
    @ForceInline
    @Override
    public final double[] toDoubleArray() {
        short[] a = toArray();
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = (double) a[i];
        }
        return res;
    }

    /**
     * Loads a vector from a byte array starting at an offset.
     * Bytes are composed into primitive lane elements according
     * to {@linkplain ByteOrder#LITTLE_ENDIAN little endian} ordering.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var bo = ByteOrder.LITTLE_ENDIAN;
     * var m = species.maskAll(true);
     * return fromByteBuffer(species, bb, offset, m, bo);
     * }</pre>
     *
     * @param species species of desired vector
     * @param a the byte array
     * @param offset the offset into the array
     * @return a vector loaded from a byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromByteArray(VectorSpecies<Short> species,
                                       byte[] a, int offset) {
        return fromByteArray(species, a, offset, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Loads a vector from a byte array starting at an offset.
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var m = species.maskAll(true);
     * return fromByteBuffer(species, bb, offset, m, bo);
     * }</pre>
     *
     * @param species species of desired vector
     * @param a the byte array
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @return a vector loaded from a byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromByteArray(VectorSpecies<Short> species,
                                       byte[] a, int offset,
                                       ByteOrder bo) {
        ShortSpecies vsp = (ShortSpecies) species;
        offset = checkFromIndexSize(offset,
                                    vsp.vectorBitSize() / Byte.SIZE,
                                    a.length);
        return vsp.dummyVector()
            .fromByteArray0(a, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a byte array starting at an offset
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of short (zero).
     * Bytes are composed into primitive lane elements according
     * to {@linkplain ByteOrder#LITTLE_ENDIAN little endian} ordering.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var bo = ByteOrder.LITTLE_ENDIAN;
     * return fromByteBuffer(species, bb, offset, bo, m);
     * }</pre>
     *
     * @param species species of desired vector
     * @param a the byte array
     * @param offset the offset into the array
     * @param m the mask controlling lane selection
     * @return a vector loaded from a byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromByteArray(VectorSpecies<Short> species,
                                       byte[] a, int offset,
                                       VectorMask<Short> m) {
        return fromByteArray(species, a, offset, ByteOrder.LITTLE_ENDIAN, m);
    }

    /**
     * Loads a vector from a byte array starting at an offset
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of short (zero).
     * Bytes are composed into primitive lane elements according
     * to {@linkplain ByteOrder#LITTLE_ENDIAN little endian} ordering.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * return fromByteBuffer(species, bb, offset, m, bo);
     * }</pre>
     *
     * @param species species of desired vector
     * @param a the byte array
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @param m the mask controlling lane selection
     * @return a vector loaded from a byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromByteArray(VectorSpecies<Short> species,
                                       byte[] a, int offset,
                                       ByteOrder bo,
                                       VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        ShortVector zero = vsp.zero();
        ShortVector iota = zero.addIndex(1);
        ((AbstractMask<Short>)m)
            .checkIndexByLane(offset, a.length, iota, 2);
        ShortVector v = zero.fromByteArray0(a, offset);
        return zero.blend(v.maybeSwap(bo), m);
    }

    /**
     * Loads a vector from an array of type {@code short[]}
     * starting at an offset.
     * For each vector lane, where {@code N} is the vector lane index, the
     * array element at index {@code offset + N} is placed into the
     * resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset) {
        ShortSpecies vsp = (ShortSpecies) species;
        offset = checkFromIndexSize(offset,
                                    vsp.laneCount(),
                                    a.length);
        return vsp.dummyVector().fromArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code short[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of short (zero).
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the array element at
     * index {@code offset + N} is placed into the resulting vector at lane index
     * {@code N}, otherwise the default element value is placed into the
     * resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array
     * @param m the mask controlling lane selection
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset,
                                   VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        ShortVector zero = vsp.zero();
        ShortVector iota = vsp.iota();
        ((AbstractMask<Short>)m)
            .checkIndexByLane(offset, a.length, iota, 1);
        return zero.blend(zero.fromArray0(a, offset), m);
    }

    /**
     * FIXME: EDIT THIS
     * Loads a vector from an array using indexes obtained from an index
     * map.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index, the
     * array element at index {@code offset + indexMap[mapOffset + N]} is placed into the
     * resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException if {@code mapOffset < 0}, or
     * {@code mapOffset > indexMap.length - species.length()},
     * or for any vector lane index {@code N} the result of
     * {@code offset + indexMap[mapOffset + N]} is {@code < 0} or {@code >= a.length}
     */
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset,
                                   int[] indexMap, int mapOffset) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.vOp(n -> a[offset + indexMap[mapOffset + n]]);
    }

    /**
     * Loads a vector from an array using indexes obtained from an index
     * map and using a mask.
     * FIXME: EDIT THIS
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the array element at
     * index {@code offset + indexMap[mapOffset + N]} is placed into the resulting vector
     * at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask controlling lane selection
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException if {@code mapOffset < 0}, or
     * {@code mapOffset > indexMap.length - species.length()},
     * or for any vector lane index {@code N} where the mask at lane
     * {@code N} is set the result of {@code offset + indexMap[mapOffset + N]} is
     * {@code < 0} or {@code >= a.length}
     */
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset,
                                   int[] indexMap, int mapOffset,
                                   VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;

        // Do it the slow way.
        return vsp.vOp(m, n -> a[offset + indexMap[mapOffset + n]]);

    }

    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer.
     * <p>
     * Bytes are composed into primitive lane elements according to
     * {@link ByteOrder#LITTLE_ENDIAN little endian} byte order.
     * To avoid errors, the
     * {@linkplain ByteBuffer#order() intrinsic byte order}
     * of the buffer must be little-endian.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var bo = ByteOrder.LITTLE_ENDIAN;
     * var m = species.maskAll(true);
     * return fromByteBuffer(species, bb, offset, m, bo);
     * }</pre>
     *
     * @param species species of desired vector
     * @param bb the byte buffer
     * @param offset the offset into the byte buffer
     * @return a vector loaded from a byte buffer
     * @throws IllegalArgumentException if byte order of bb
     *         is not {@link ByteOrder#LITTLE_ENDIAN}
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*2 < 0}
     *         or {@code offset+N**2 >= bb.limit()}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromByteBuffer(VectorSpecies<Short> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo) {
        ShortSpecies vsp = (ShortSpecies) species;
        offset = checkFromIndexSize(offset,
                                    vsp.laneCount(),
                                    bb.limit());
        return vsp.dummyVector()
            .fromByteBuffer0(bb, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer
     * and using a mask.
     * <p>
     * Bytes are composed into primitive lane elements according to
     * {@link ByteOrder#LITTLE_ENDIAN little endian} byte order.
     * To avoid errors, the
     * {@linkplain ByteBuffer#order() intrinsic byte order}
     * of the buffer must be little-endian.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var bo = ByteOrder.LITTLE_ENDIAN;
     * var m = species.maskAll(true);
     * return fromByteBuffer(species, bb, offset, m, bo);
     * }</pre>
     *
     * @param species species of desired vector
     * @param bb the byte buffer
     * @param offset the offset into the byte buffer
     * @param m the mask controlling lane selection
     * @return a vector loaded from a byte buffer
     * @throws IllegalArgumentException if byte order of bb
     *         is not {@link ByteOrder#LITTLE_ENDIAN}
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*2 < 0}
     *         or {@code offset+N**2 >= bb.limit()}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromByteBuffer(VectorSpecies<Short> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo,
                                        VectorMask<Short> m) {
        if (m.allTrue()) {
            return fromByteBuffer(species, bb, offset, bo);
        }
        ShortSpecies vsp = (ShortSpecies) species;
        checkMaskFromIndexSize(offset,
                               vsp, m, 1,
                               bb.limit());
        ShortVector zero = zero(vsp);
        ShortVector v = zero.fromByteBuffer0(bb, offset);
        return zero.blend(v.maybeSwap(bo), m);
    }

    // Memory store operations

    /**
     * Stores this vector into an array of type {@code short[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     *
     * @param a the array, of type {@code short[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public void intoArray(short[] a, int offset) {
        ShortSpecies vsp = vspecies();
        offset = checkFromIndexSize(offset,
                                    vsp.laneCount(),
                                    a.length);
        VectorIntrinsics.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            this,
            a, offset,
            (arr, off, v)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> arr_[off_ + i] = e));
    }

    /**
     * Stores this vector into an array of short
     * starting at offset and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     * If the mask lane at {@code N} is unset then the corresponding
     * array element {@code a[offset+N]} is left unchanged.
     * <p>
     * Array range checking is done for lanes where the mask is set.
     * Lanes where the mask is unset are not stored and do not need
     * to correspond to legitimate elements of {@code a}.
     * That is, unset lanes may correspond to array indexes less than
     * zero or beyond the end of the array.
     *
     * @param a the array, of type {@code short[]}
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final void intoArray(short[] a, int offset,
                                VectorMask<Short> m) {
        if (m.allTrue()) {
            intoArray(a, offset);
        } else {
            // FIXME: Cannot vectorize yet, if there's a mask.
            stOp(a, offset, m, (arr, off, i, v) -> arr[off+i] = v);
        }
    }

    /**
     * Stores this vector into an array of type {@code short[]}
     * using indexes obtained from an index map
     * and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then
     * the lane element at index {@code N} is stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param a the array
     * @param offset an offset to combine with the index map offsets
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask
     * @returns a vector of the values {@code m ? a[f(N)] : 0},
     *          {@code f(N) = offset + indexMap[mapOffset + N]]}.
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public void intoArray(short[] a, int offset,
                          int[] indexMap, int mapOffset) {
        ShortSpecies vsp = vspecies();
        if (length() == 1) {
            intoArray(a, offset + indexMap[mapOffset]);
            return;
        }
        IntVector.IntSpecies isp = (IntVector.IntSpecies) vsp.indexSpecies();
        if (isp.laneCount() != vsp.laneCount()) {
            stOp(a, offset,
                 (arr, off, i, e) -> {
                     int j = indexMap[mapOffset + i];
                     arr[off + j] = e;
                 });
            return;
        }

        // Index vector: vix[0:n] = i -> offset + indexMap[mo + i]
        IntVector vix = IntVector
            .fromArray(isp, indexMap, mapOffset)
            .add(offset);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorIntrinsics.storeWithMap(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            isp.vectorType(),
            a, arrayAddress(a, 0), vix,
            this,
            a, offset, indexMap, mapOffset,
            (arr, off, v, map, mo)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> {
                          int j = map[mo + i];
                          arr[off + j] = e;
                      }));
    }

    /**
     * Stores this vector into an array of type {@code short[]}
     * using indexes obtained from an index map
     * and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then
     * the lane element at index {@code N} is stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param a the array
     * @param offset an offset to combine with the index map offsets
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask
     * @returns a vector of the values {@code m ? a[f(N)] : 0},
     *          {@code f(N) = offset + indexMap[mapOffset + N]]}.
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final void intoArray(short[] a, int offset,
                                int[] indexMap, int mapOffset,
                                VectorMask<Short> m) {
        ShortSpecies vsp = vspecies();
        if (m.allTrue()) {
            intoArray(a, offset, indexMap, mapOffset);
            return;
        }
        throw new AssertionError("fixme");
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int offset) {
        offset = checkFromIndexSize(offset,
                                    bitSize() / Byte.SIZE,
                                    a.length);
        this.maybeSwap(ByteOrder.LITTLE_ENDIAN)
            .intoByteArray0(a, offset);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int offset,
                                    VectorMask<Short> m) {
        if (m.allTrue()) {
            intoByteArray(a, offset);
            return;
        }
        ShortSpecies vsp = vspecies();
        checkMaskFromIndexSize(offset, vsp, m, 2, a.length);
        conditionalStoreNYI(offset, vsp, m, 2, a.length);
        var oldVal = fromByteArray0(a, offset);
        var newVal = oldVal.blend(this, m);
        newVal.intoByteArray0(a, offset);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int offset,
                                    ByteOrder bo,
                                    VectorMask<Short> m) {
        maybeSwap(bo).intoByteArray(a, offset, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int offset,
                               ByteOrder bo) {
        maybeSwap(bo).intoByteBuffer0(bb, offset);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int offset,
                               ByteOrder bo,
                               VectorMask<Short> m) {
        if (m.allTrue()) {
            intoByteBuffer(bb, offset, bo);
            return;
        }
        ShortSpecies vsp = vspecies();
        checkMaskFromIndexSize(offset, vsp, m, 2, bb.limit());
        conditionalStoreNYI(offset, vsp, m, 2, bb.limit());
        var oldVal = fromByteBuffer0(bb, offset);
        var newVal = oldVal.blend(this.maybeSwap(bo), m);
        newVal.intoByteBuffer0(bb, offset);
    }

    // ================================================

    // Low-level memory operations.
    //
    // Note that all of these operations *must* inline into a context
    // where the exact species of the involved vector is a
    // compile-time constant.  Otherwise, the intrinsic generation
    // will fail and performance will suffer.
    //
    // In many cases this is achieved by re-deriving a version of the
    // method in each concrete subclass (per species).  The re-derived
    // method simply calls one of these generic methods, with exact
    // parameters for the controlling metadata, which is either a
    // typed vector or constant species instance.

    // Unchecked loading operations in native byte order.
    // Caller is reponsible for applying index checks, masking, and
    // byte swapping.

    /*package-private*/
    @ForceInline
    ShortVector fromArray0(short[] a, int offset) {
        ShortSpecies vsp = vspecies();
        return VectorIntrinsics.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> arr_[off_ + i]));
    }

    @ForceInline
    @Override
    ShortVector fromByteArray0(byte[] a, int offset) {
        ShortSpecies vsp = vspecies();
        return VectorIntrinsics.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> {
                ShortBuffer tb = wrapper(arr, off, NATIVE_ENDIAN);
                return s.ldOp(tb, 0, (tb_, __, i) -> tb_.get(i));
            });
    }

    @ForceInline
    ShortVector fromByteBuffer0(ByteBuffer bb, int offset) {
        ShortSpecies vsp = vspecies();
        return VectorIntrinsics.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            bufferBase(bb), bufferAddress(bb, offset),
            bb, offset, vsp,
            (buf, off, s) -> {
                ShortBuffer tb = wrapper(buf, off, NATIVE_ENDIAN);
                return s.ldOp(tb, 0, (tb_, __, i) -> tb_.get(i));
           });
    }

    // Unchecked storing operations in native byte order.
    // Caller is reponsible for applying index checks, masking, and
    // byte swapping.

    @ForceInline
    void intoArray0(short[] a, int offset) {
        ShortSpecies vsp = vspecies();
        VectorIntrinsics.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            this, a, offset,
            (arr, off, v)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> arr_[off_+i] = e));
    }

    @ForceInline
    void intoByteArray0(byte[] a, int offset) {
        ShortSpecies vsp = vspecies();
        VectorIntrinsics.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            this, a, offset,
            (arr, off, v) -> {
                ShortBuffer tb = wrapper(arr, off, NATIVE_ENDIAN);
                v.stOp(tb, 0, (tb_, __, i, e) -> tb_.put(i, e));
            });
    }

    @ForceInline
    void intoByteBuffer0(ByteBuffer bb, int offset) {
        ShortSpecies vsp = vspecies();
        VectorIntrinsics.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            bufferBase(bb), bufferAddress(bb, offset),
            this, bb, offset,
            (buf, off, v) -> {
                ShortBuffer tb = wrapper(buf, off, NATIVE_ENDIAN);
                v.stOp(tb, 0, (tb_, __, i, e) -> tb_.put(i, e));
            });
    }

    // End of low-level memory operations.

    private static
    void checkMaskFromIndexSize(int offset,
                                ShortSpecies vsp,
                                VectorMask<Short> m,
                                int scale,
                                int limit) {
        ((AbstractMask<Short>)m)
            .checkIndexByLane(offset, limit, vsp.iota(), scale);
    }

    private void conditionalStoreNYI(int offset,
                                     ShortSpecies vsp,
                                     VectorMask<Short> m,
                                     int scale,
                                     int limit) {
        if (offset < 0 || offset + vsp.laneCount() * scale > limit) {
            String msg =
                String.format("unimplemented: store @%d in [0..%d), %s in %s",
                              offset, limit, m, vsp);
            throw new AssertionError(msg);
        }
    }

    /*package-private*/
    @Override
    @ForceInline
    ShortVector maybeSwap(ByteOrder bo) {
        if (bo != NATIVE_ENDIAN) {
            return this.reinterpretAsBytes()
                .rearrange(swapBytesShuffle())
                .reinterpretAsShorts();
        }
        return this;
    }

    static final int ARRAY_SHIFT =
        31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_SHORT_INDEX_SCALE);
    static final long ARRAY_BASE =
        Unsafe.ARRAY_SHORT_BASE_OFFSET;

    @ForceInline
    static long arrayAddress(short[] a, int index) {
        return ARRAY_BASE + (((long)index) << ARRAY_SHIFT);
    }

    @ForceInline
    static long byteArrayAddress(byte[] a, int index) {
        return Unsafe.ARRAY_BYTE_BASE_OFFSET + index;
    }

    // Byte buffer wrappers.
    private static ShortBuffer wrapper(ByteBuffer bb, int offset,
                                        ByteOrder bo) {
        return bb.duplicate().position(offset).slice()
            .order(bo).asShortBuffer();
    }
    private static ShortBuffer wrapper(byte[] a, int offset,
                                        ByteOrder bo) {
        return ByteBuffer.wrap(a, offset, a.length - offset)
            .order(bo).asShortBuffer();
    }

    // ================================================

    /// Reinterpreting view methods:
    //   lanewise reinterpret: viewAsXVector()
    //   keep shape, redraw lanes: reinterpretAsEs()

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final ByteVector reinterpretAsBytes() {
         // Going to ByteVector, pay close attention to byte order.
         assert(REGISTER_ENDIAN == ByteOrder.LITTLE_ENDIAN);
         return asByteVectorRaw();
         //return asByteVectorRaw().rearrange(swapBytesShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final ShortVector viewAsIntegralLanes() {
        return this;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     *
     * @implNote This method always throws
     * {@code IllegalArgumentException}, because there is no floating
     * point type of the same size as {@code short}.  The return type
     * of this method is arbitrarily designated as
     * {@code Vector<?>}.  Future versions of this API may change the return
     * type if additional floating point types become available.
     */
    @ForceInline
    @Override
    public final
    Vector<?>
    viewAsFloatingLanes() {
        LaneType flt = LaneType.SHORT.asFloating();
        throw new AssertionError();  // should already throw IAE
    }

    // ================================================

    /// Object methods: toString, equals, hashCode
    //
    // Object methods are defined as if via Arrays.toString, etc.,
    // is applied to the array of elements.  Two equal vectors
    // are required to have equal species and equal lane values.

    /**
     * Returns a string representation of this vector, of the form
     * {@code "[0,1,2...]"}, reporting the lane values of this vector,
     * in lane order.
     *
     * The string is produced as if by a call to {@linkplain
     * java.util.Arrays#toString(short[]) the {@code Arrays.toString}
     * method} appropriate to the short array returned by
     * {@linkplain #toArray this vector's {@code toArray} method}.
     *
     * @return a string of the form {@code "[0,1,2...]"}
     * reporting the lane values of this vector
     */
    @Override
    public final String toString() {
        // now that toArray is strongly typed, we can define this
        return Arrays.toString(toArray());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public boolean equals(Object obj) {
        if (obj instanceof Vector) {
            Vector<?> that = (Vector<?>) obj;
            if (this.species().equals(that.species())) {
                return this.eq(that.check(this.species())).allTrue();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public int hashCode() {
        // now that toArray is strongly typed, we can define this
        return Objects.hash(species(), Arrays.hashCode(toArray()));
    }

    // ================================================

    // Species

    /**
     * Class representing {@link ShortVector}'s of the same {@link VectorShape VectorShape}.
     */
    /*package-private*/
    static final class ShortSpecies extends AbstractSpecies<Short> {
        private ShortSpecies(VectorShape shape,
                Class<? extends ShortVector> vectorType,
                Class<? extends AbstractMask<Short>> maskType,
                Function<Object, ShortVector> vectorFactory) {
            super(shape, LaneType.of(short.class),
                  vectorType, maskType,
                  vectorFactory);
            assert(this.elementSize() == Short.SIZE);
        }

        // Specializing overrides:

        @Override
        @ForceInline
        public final Class<Short> elementType() {
            return short.class;
        }

        @Override
        @ForceInline
        public final Class<Short> genericElementType() {
            return Short.class;
        }

        @Override
        @ForceInline
        public final Class<short[]> arrayType() {
            return short[].class;
        }

        @SuppressWarnings("unchecked")
        @Override
        @ForceInline
        public final Class<? extends ShortVector> vectorType() {
            return (Class<? extends ShortVector>) vectorType;
        }

        @Override
        @ForceInline
        public final long checkValue(long e) {
            longToElementBits(e);  // only for exception
            return e;
        }

        /*package-private*/
        @Override
        @ForceInline
        final ShortVector broadcastBits(long bits) {
            return (ShortVector)
                VectorIntrinsics.broadcastCoerced(
                    vectorType, short.class, laneCount,
                    bits, this,
                    (bits_, s_) -> s_.rvOp(i -> bits_));
        }

        /*package-private*/
        @ForceInline
        
        final ShortVector broadcast(short e) {
            return broadcastBits(toBits(e));
        }

        @Override
        @ForceInline
        public final ShortVector broadcast(long e) {
            return broadcastBits(longToElementBits(e));
        }

        /*package-private*/
        final @Override
        @ForceInline
        long longToElementBits(long value) {
            // Do the conversion, and then test it for failure.
            short e = (short) value;
            if ((long) e != value) {
                throw badElementBits(value, e);
            }
            return toBits(e);
        }

        @Override
        @ForceInline
        public final ShortVector fromValues(long... values) {
            VectorIntrinsics.requireLength(values.length, laneCount);
            short[] va = new short[laneCount()];
            for (int i = 0; i < va.length; i++) {
                long lv = values[i];
                short v = (short) lv;
                va[i] = v;
                if ((long)v != lv) {
                    throw badElementBits(lv, v);
                }
            }
            return dummyVector().fromArray0(va, 0);
        }

        /* this non-public one is for internal conversions */
        @Override
        @ForceInline
        final ShortVector fromIntValues(int[] values) {
            VectorIntrinsics.requireLength(values.length, laneCount);
            short[] va = new short[laneCount()];
            for (int i = 0; i < va.length; i++) {
                int lv = values[i];
                short v = (short) lv;
                va[i] = v;
                if ((int)v != lv) {
                    throw badElementBits(lv, v);
                }
            }
            return dummyVector().fromArray0(va, 0);
        }

        // Virtual constructors

        @ForceInline
        @Override final
        public ShortVector fromArray(Object a, int offset) {
            // User entry point:  Be careful with inputs.
            return ShortVector
                .fromArray(this, (short[]) a, offset);
        }

        @Override final
        ShortVector dummyVector() {
            return (ShortVector) super.dummyVector();
        }

        final
        ShortVector vectorFactory(short[] vec) {
            // Species delegates all factory requests to its dummy
            // vector.  The dummy knows all about it.
            return dummyVector().vectorFactory(vec);
        }

        /*package-private*/
        final @Override
        @ForceInline
        ShortVector rvOp(RVOp f) {
            short[] res = new short[laneCount()];
            for (int i = 0; i < res.length; i++) {
                short bits = (short) f.apply(i);
                res[i] = fromBits(bits);
            }
            return dummyVector().vectorFactory(res);
        }

        ShortVector vOp(FVOp f) {
            short[] res = new short[laneCount()];
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i);
            }
            return dummyVector().vectorFactory(res);
        }

        ShortVector vOp(VectorMask<Short> m, FVOp f) {
            short[] res = new short[laneCount()];
            boolean[] mbits = ((AbstractMask<Short>)m).getBits();
            for (int i = 0; i < res.length; i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return dummyVector().vectorFactory(res);
        }

        /*package-private*/
        @ForceInline
        <M> ShortVector ldOp(M memory, int offset,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> ShortVector ldOp(M memory, int offset,
                                      AbstractMask<Short> m,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, m, f);
        }

        /*package-private*/
        @ForceInline
        <M> void stOp(M memory, int offset, FStOp<M> f) {
            dummyVector().stOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> void stOp(M memory, int offset,
                      AbstractMask<Short> m,
                      FStOp<M> f) {
            dummyVector().stOp(memory, offset, m, f);
        }

        // N.B. Make sure these constant vectors and
        // masks load up correctly into registers.
        //
        // Also, see if we can avoid all that switching.
        // Could we cache both vectors and both masks in
        // this species object?

        // Zero and iota vector access
        @Override
        @ForceInline
        public final ShortVector zero() {
            if ((Class<?>) vectorType() == ShortMaxVector.class)
                return ShortMaxVector.ZERO;
            switch (vectorBitSize()) {
                case 64: return Short64Vector.ZERO;
                case 128: return Short128Vector.ZERO;
                case 256: return Short256Vector.ZERO;
                case 512: return Short512Vector.ZERO;
            }
            throw new AssertionError();
        }        

        @Override
        @ForceInline
        public final ShortVector iota() {
            if ((Class<?>) vectorType() == ShortMaxVector.class)
                return ShortMaxVector.IOTA;
            switch (vectorBitSize()) {
                case 64: return Short64Vector.IOTA;
                case 128: return Short128Vector.IOTA;
                case 256: return Short256Vector.IOTA;
                case 512: return Short512Vector.IOTA;
            }
            throw new AssertionError();
        }

        // Mask access
        @Override
        @ForceInline
        public final VectorMask<Short> maskAll(boolean bit) {
            if ((Class<?>) vectorType() == ShortMaxVector.class)
                return ShortMaxVector.ShortMaxMask.maskAll(bit);
            switch (vectorBitSize()) {
                case 64: return Short64Vector.Short64Mask.maskAll(bit);
                case 128: return Short128Vector.Short128Mask.maskAll(bit);
                case 256: return Short256Vector.Short256Mask.maskAll(bit);
                case 512: return Short512Vector.Short512Mask.maskAll(bit);
            }
            throw new AssertionError();
        }
    }

    /**
     * Finds a species for an element type of {@code short} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code short} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    static ShortSpecies species(VectorShape s) {
        Objects.requireNonNull(s);
        switch (s) {
            case S_64_BIT: return (ShortSpecies) SPECIES_64;
            case S_128_BIT: return (ShortSpecies) SPECIES_128;
            case S_256_BIT: return (ShortSpecies) SPECIES_256;
            case S_512_BIT: return (ShortSpecies) SPECIES_512;
            case S_Max_BIT: return (ShortSpecies) SPECIES_MAX;
            default: throw new IllegalArgumentException("Bad shape: " + s);
        }
    }

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_64_BIT VectorShape.S_64_BIT}. */
    public static final VectorSpecies<Short> SPECIES_64
        = new ShortSpecies(VectorShape.S_64_BIT,
                            Short64Vector.class,
                            Short64Vector.Short64Mask.class,
                            Short64Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_128_BIT VectorShape.S_128_BIT}. */
    public static final VectorSpecies<Short> SPECIES_128
        = new ShortSpecies(VectorShape.S_128_BIT,
                            Short128Vector.class,
                            Short128Vector.Short128Mask.class,
                            Short128Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_256_BIT VectorShape.S_256_BIT}. */
    public static final VectorSpecies<Short> SPECIES_256
        = new ShortSpecies(VectorShape.S_256_BIT,
                            Short256Vector.class,
                            Short256Vector.Short256Mask.class,
                            Short256Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_512_BIT VectorShape.S_512_BIT}. */
    public static final VectorSpecies<Short> SPECIES_512
        = new ShortSpecies(VectorShape.S_512_BIT,
                            Short512Vector.class,
                            Short512Vector.Short512Mask.class,
                            Short512Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_Max_BIT VectorShape.S_Max_BIT}. */
    public static final VectorSpecies<Short> SPECIES_MAX
        = new ShortSpecies(VectorShape.S_Max_BIT,
                            ShortMaxVector.class,
                            ShortMaxVector.ShortMaxMask.class,
                            ShortMaxVector::new);

    /**
     * Preferred species for {@link ShortVector}s.
     * A preferred species is a species of maximal bit-size for the platform.
     */
    public static final VectorSpecies<Short> SPECIES_PREFERRED
        = (ShortSpecies) VectorSpecies.ofPreferred(short.class);


    // ==== JROSE NAME CHANGES ====

    /** Use lanewise(NEG, m). */
    @Deprecated
    public final ShortVector neg(VectorMask<Short> m) {
        return lanewise(NEG, m);
    }

    /** Use lanewise(ABS, m). */
    @Deprecated
    public final ShortVector abs(VectorMask<Short> m) {
        return lanewise(ABS, m);
    }

    /** Use explicit argument of ByteOrder.LITTLE_ENDIAN */
    @Deprecated
    public static
    ShortVector fromByteBuffer(VectorSpecies<Short> species,
                                        ByteBuffer bb, int offset) {
        ByteOrder bo = ByteOrder.LITTLE_ENDIAN;
        if (bb.order() != bo)  throw new IllegalArgumentException();
        return fromByteBuffer(species, bb, offset, bo);
    }

    /** Use explicit argument of ByteOrder.LITTLE_ENDIAN */
    @Deprecated
    public static
    ShortVector fromByteBuffer(VectorSpecies<Short> species,
                                        ByteBuffer bb, int offset,
                                        VectorMask<Short> m) {
        ByteOrder bo = ByteOrder.LITTLE_ENDIAN;
        if (bb.order() != bo)  throw new IllegalArgumentException();
        return fromByteBuffer(species, bb, offset, bo, m);
    }

    /** Use fromValues(s, value...) */
    @Deprecated
    public static
    ShortVector scalars(VectorSpecies<Short> species,
                                 short... values) {
        return fromValues(species, values);
    }

    @Deprecated public final short addLanes() { return reduceLanes(ADD); }
    @Deprecated public final short addLanes(VectorMask<Short> m) { return reduceLanes(ADD, m); }
    @Deprecated public final short mulLanes() { return reduceLanes(MUL); }
    @Deprecated public final short mulLanes(VectorMask<Short> m) { return reduceLanes(MUL, m); }
    @Deprecated public final short minLanes() { return reduceLanes(MIN); }
    @Deprecated public final short minLanes(VectorMask<Short> m) { return reduceLanes(MIN, m); }
    @Deprecated public final short maxLanes() { return reduceLanes(MAX); }
    @Deprecated public final short maxLanes(VectorMask<Short> m) { return reduceLanes(MAX, m); }
    @Deprecated public final short orLanes() { return reduceLanes(OR); }
    @Deprecated public final short orLanes(VectorMask<Short> m) { return reduceLanes(OR, m); }
    @Deprecated public final short andLanes() { return reduceLanes(AND); }
    @Deprecated public final short andLanes(VectorMask<Short> m) { return reduceLanes(AND, m); }
    @Deprecated public final short xorLanes() { return reduceLanes(XOR); }
    @Deprecated public final short xorLanes(VectorMask<Short> m) { return reduceLanes(XOR, m); }
    @Deprecated public final ShortVector sqrt() { return lanewise(SQRT); }
    @Deprecated public final ShortVector sqrt(VectorMask<Short> m) { return lanewise(SQRT, m); }
    @Deprecated public final ShortVector tan() { return lanewise(TAN); }
    @Deprecated public final ShortVector tan(VectorMask<Short> m) { return lanewise(TAN, m); }
    @Deprecated public final ShortVector tanh() { return lanewise(TANH); }
    @Deprecated public final ShortVector tanh(VectorMask<Short> m) { return lanewise(TANH, m); }
    @Deprecated public final ShortVector sin() { return lanewise(SIN); }
    @Deprecated public final ShortVector sin(VectorMask<Short> m) { return lanewise(SIN, m); }
    @Deprecated public final ShortVector sinh() { return lanewise(SINH); }
    @Deprecated public final ShortVector sinh(VectorMask<Short> m) { return lanewise(SINH, m); }
    @Deprecated public final ShortVector cos() { return lanewise(COS); }
    @Deprecated public final ShortVector cos(VectorMask<Short> m) { return lanewise(COS, m); }
    @Deprecated public final ShortVector cosh() { return lanewise(COSH); }
    @Deprecated public final ShortVector cosh(VectorMask<Short> m) { return lanewise(COSH, m); }
    @Deprecated public final ShortVector asin() { return lanewise(ASIN); }
    @Deprecated public final ShortVector asin(VectorMask<Short> m) { return lanewise(ASIN, m); }
    @Deprecated public final ShortVector acos() { return lanewise(ACOS); }
    @Deprecated public final ShortVector acos(VectorMask<Short> m) { return lanewise(ACOS, m); }
    @Deprecated public final ShortVector atan() { return lanewise(ATAN); }
    @Deprecated public final ShortVector atan(VectorMask<Short> m) { return lanewise(ATAN, m); }
    @Deprecated public final ShortVector atan2(Vector<Short> v) { return lanewise(ATAN2, v); }
    @Deprecated public final ShortVector atan2(short s) { return lanewise(ATAN2, s); }
    @Deprecated public final ShortVector atan2(Vector<Short> v, VectorMask<Short> m) { return lanewise(ATAN2, v, m); }
    @Deprecated public final ShortVector atan2(short s, VectorMask<Short> m) { return lanewise(ATAN2, s, m); }
    @Deprecated public final ShortVector cbrt() { return lanewise(CBRT); }
    @Deprecated public final ShortVector cbrt(VectorMask<Short> m) { return lanewise(CBRT, m); }
    @Deprecated public final ShortVector log() { return lanewise(LOG); }
    @Deprecated public final ShortVector log(VectorMask<Short> m) { return lanewise(LOG, m); }
    @Deprecated public final ShortVector log10() { return lanewise(LOG10); }
    @Deprecated public final ShortVector log10(VectorMask<Short> m) { return lanewise(LOG10, m); }
    @Deprecated public final ShortVector log1p() { return lanewise(LOG1P); }
    @Deprecated public final ShortVector log1p(VectorMask<Short> m) { return lanewise(LOG1P, m); }
    @Deprecated public final ShortVector pow(Vector<Short> v) { return lanewise(POW, v); }
    @Deprecated public final ShortVector pow(short s) { return lanewise(POW, s); }
    @Deprecated public final ShortVector pow(Vector<Short> v, VectorMask<Short> m) { return lanewise(POW, v, m); }
    @Deprecated public final ShortVector pow(short s, VectorMask<Short> m) { return lanewise(POW, s, m); }
    @Deprecated public final ShortVector exp() { return lanewise(EXP); }
    @Deprecated public final ShortVector exp(VectorMask<Short> m) { return lanewise(EXP, m); }
    @Deprecated public final ShortVector expm1() { return lanewise(EXPM1); }
    @Deprecated public final ShortVector expm1(VectorMask<Short> m) { return lanewise(EXPM1, m); }
    @Deprecated public final ShortVector hypot(Vector<Short> v) { return lanewise(HYPOT, v); }
    @Deprecated public final ShortVector hypot(short s) { return lanewise(HYPOT, s); }
    @Deprecated public final ShortVector hypot(Vector<Short> v, VectorMask<Short> m) { return lanewise(HYPOT, v, m); }
    @Deprecated public final ShortVector hypot(short s, VectorMask<Short> m) { return lanewise(HYPOT, s, m); }
    @Deprecated 
    public final ShortVector and(Vector<Short> v) { return lanewise(AND, v); }
    @Deprecated public final ShortVector and(short s) { return lanewise(AND, s); }
    @Deprecated public final ShortVector and(Vector<Short> v, VectorMask<Short> m) { return lanewise(AND, v, m); }
    @Deprecated public final ShortVector and(short s, VectorMask<Short> m) { return lanewise(AND, s, m); }
    @Deprecated public final ShortVector or(Vector<Short> v) { return lanewise(OR, v); }
    @Deprecated public final ShortVector or(short s) { return lanewise(OR, s); }
    @Deprecated public final ShortVector or(Vector<Short> v, VectorMask<Short> m) { return lanewise(OR, v, m); }
    @Deprecated public final ShortVector or(short s, VectorMask<Short> m) { return lanewise(OR, s, m); }
    @Deprecated public final ShortVector xor(Vector<Short> v) { return lanewise(XOR, v); }
    @Deprecated public final ShortVector xor(short s) { return lanewise(XOR, s); }
    @Deprecated public final ShortVector xor(Vector<Short> v, VectorMask<Short> m) { return lanewise(XOR, v, m); }
    @Deprecated public final ShortVector xor(short s, VectorMask<Short> m) { return lanewise(XOR, s, m); }
    @Deprecated public final ShortVector not() { return lanewise(NOT); }
    @Deprecated public final ShortVector not(VectorMask<Short> m) { return lanewise(NOT, m); }
    @Deprecated public final ShortVector shiftLeft(int s) { return lanewise(LSHL, (short) s); }
    @Deprecated public final ShortVector shiftLeft(int s, VectorMask<Short> m) { return lanewise(LSHL, (short) s, m); }
    @Deprecated public final ShortVector shiftLeft(Vector<Short> v) { return lanewise(LSHL, v); }
    @Deprecated public final ShortVector shiftLeft(Vector<Short> v, VectorMask<Short> m) { return lanewise(LSHL, v, m); }
    @Deprecated public final ShortVector shiftRight(int s) { return lanewise(LSHR, (short) s); }
    @Deprecated public final ShortVector shiftRight(int s, VectorMask<Short> m) { return lanewise(LSHR, (short) s, m); }
    @Deprecated public final ShortVector shiftRight(Vector<Short> v) { return lanewise(LSHR, v); }
    @Deprecated public final ShortVector shiftRight(Vector<Short> v, VectorMask<Short> m) { return lanewise(LSHR, v, m); }
    @Deprecated public final ShortVector shiftArithmeticRight(int s) { return lanewise(ASHR, (short) s); }
    @Deprecated public final ShortVector shiftArithmeticRight(int s, VectorMask<Short> m) { return lanewise(ASHR, (short) s, m); }
    @Deprecated public final ShortVector shiftArithmeticRight(Vector<Short> v) { return lanewise(ASHR, v); }
    @Deprecated public final ShortVector shiftArithmeticRight(Vector<Short> v, VectorMask<Short> m) { return lanewise(ASHR, v, m); }
    @Deprecated public final ShortVector rotateLeft(int s) { return lanewise(ROL, (short) s); }
    @Deprecated public final ShortVector rotateLeft(int s, VectorMask<Short> m) { return lanewise(ROL, (short) s, m); }
    @Deprecated public final ShortVector rotateRight(int s) { return lanewise(ROR, (short) s); }
    @Deprecated public final ShortVector rotateRight(int s, VectorMask<Short> m) { return lanewise(ROR, (short) s, m); }
    @Deprecated @Override public ShortVector rotateLanesLeft(int i) { return (ShortVector) super.rotateLanesLeft(i); }
    @Deprecated @Override public ShortVector rotateLanesRight(int i) { return (ShortVector) super.rotateLanesRight(i); }
    @Deprecated @Override public ShortVector shiftLanesLeft(int i) { return (ShortVector) super.shiftLanesLeft(i); }
    @Deprecated @Override public ShortVector shiftLanesRight(int i) { return (ShortVector) super.shiftLanesRight(i); }
    @Deprecated public ShortVector with(int i, short e) { return withLane(i, e); }
}