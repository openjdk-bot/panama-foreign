/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */

/*
 * @test
 * @modules jdk.incubator.vector
 * @run testng/othervm -ea -esa Double256VectorTests
 */

import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.Vector;

import jdk.incubator.vector.DoubleVector;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.Integer;
import java.util.List;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Test
public class Double256VectorTests extends AbstractVectorTest {

    static final VectorSpecies<Double> SPECIES =
                DoubleVector.SPECIES_256;

    static final int INVOC_COUNT = Integer.getInteger("jdk.incubator.vector.test.loop-iterations", 100);

    interface FUnOp {
        double apply(double a);
    }

    static void assertArraysEquals(double[] a, double[] r, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i]), "at index #" + i + ", input = " + a[i]);
        }
    }

    static void assertArraysEquals(double[] a, double[] r, boolean[] mask, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i], "at index #" + i + ", input = " + a[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    interface FReductionOp {
        double apply(double[] a, int idx);
    }

    interface FReductionAllOp {
        double apply(double[] a);
    }

    static void assertReductionArraysEquals(double[] a, double[] b, double c,
                                            FReductionOp f, FReductionAllOp fa) {
        int i = 0;
        try {
            Assert.assertEquals(c, fa.apply(a));
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(b[i], f.apply(a, i));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(c, fa.apply(a), "Final result is incorrect!");
            Assert.assertEquals(b[i], f.apply(a, i), "at index #" + i);
        }
    }

    interface FReductionMaskedOp {
        double apply(double[] a, int idx, boolean[] mask);
    }

    interface FReductionAllMaskedOp {
        double apply(double[] a, boolean[] mask);
    }

    static void assertReductionArraysEqualsMasked(double[] a, double[] b, double c, boolean[] mask,
                                            FReductionMaskedOp f, FReductionAllMaskedOp fa) {
        int i = 0;
        try {
            Assert.assertEquals(c, fa.apply(a, mask));
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(b[i], f.apply(a, i, mask));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(c, fa.apply(a, mask), "Final result is incorrect!");
            Assert.assertEquals(b[i], f.apply(a, i, mask), "at index #" + i);
        }
    }

    interface FBoolReductionOp {
        boolean apply(boolean[] a, int idx);
    }

    static void assertReductionBoolArraysEquals(boolean[] a, boolean[] b, FBoolReductionOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(f.apply(a, i), b[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a, i), b[i], "at index #" + i);
        }
    }

    static void assertInsertArraysEquals(double[] a, double[] b, double element, int index) {
        int i = 0;
        try {
            for (; i < a.length; i += 1) {
                if(i%SPECIES.length() == index) {
                    Assert.assertEquals(b[i], element);
                } else {
                    Assert.assertEquals(b[i], a[i]);
                }
            }
        } catch (AssertionError e) {
            if (i%SPECIES.length() == index) {
                Assert.assertEquals(b[i], element, "at index #" + i);
            } else {
                Assert.assertEquals(b[i], a[i], "at index #" + i);
            }
        }
    }

    static void assertRearrangeArraysEquals(double[] a, double[] r, int[] order, int vector_len) {
        int i = 0, j = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    Assert.assertEquals(r[i+j], a[i+order[i+j]]);
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            Assert.assertEquals(r[i+j], a[i+order[i+j]], "at index #" + idx + ", input = " + a[i+order[i+j]]);
        }
    }

    interface FBinOp {
        double apply(double a, double b);
    }

    interface FBinMaskOp {
        double apply(double a, double b, boolean m);

        static FBinMaskOp lift(FBinOp f) {
            return (a, b, m) -> m ? f.apply(a, b) : a;
        }
    }

    static void assertArraysEquals(double[] a, double[] b, double[] r, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i], b[i]), r[i], "(" + a[i] + ", " + b[i] + ") at index #" + i);
        }
    }

    static void assertArraysEquals(double[] a, double[] b, double[] r, boolean[] mask, FBinOp f) {
        assertArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(double[] a, double[] b, double[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertShiftArraysEquals(double[] a, double[] b, double[] r, FBinOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
                for (i = 0; i < SPECIES.length(); i++) {
                    Assert.assertEquals(f.apply(a[i+j], b[j]), r[i+j]);
                }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i+j], b[j]), r[i+j], "at index #" + i + ", " + j);
        }
    }

    static void assertShiftArraysEquals(double[] a, double[] b, double[] r, boolean[] mask, FBinOp f) {
        assertShiftArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertShiftArraysEquals(double[] a, double[] b, double[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
                for (i = 0; i < SPECIES.length(); i++) {
                    Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j], mask[i]));
                }
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j], mask[i]), "at index #" + i + ", input1 = " + a[i+j] + ", input2 = " + b[j] + ", mask = " + mask[i]);
        }
    }

    interface FTernOp {
        double apply(double a, double b, double c);
    }

    interface FTernMaskOp {
        double apply(double a, double b, double c, boolean m);

        static FTernMaskOp lift(FTernOp f) {
            return (a, b, c, m) -> m ? f.apply(a, b, c) : a;
        }
    }

    static void assertArraysEquals(double[] a, double[] b, double[] c, double[] r, FTernOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i]);
        }
    }

    static void assertArraysEquals(double[] a, double[] b, double[] c, double[] r, boolean[] mask, FTernOp f) {
        assertArraysEquals(a, b, c, r, mask, FTernMaskOp.lift(f));
    }

    static void assertArraysEquals(double[] a, double[] b, double[] c, double[] r, boolean[] mask, FTernMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = "
              + b[i] + ", input3 = " + c[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static boolean isWithin1Ulp(double actual, double expected) {
        if (Double.isNaN(expected) && !Double.isNaN(actual)) {
            return false;
        } else if (!Double.isNaN(expected) && Double.isNaN(actual)) {
            return false;
        }

        double low = Math.nextDown(expected);
        double high = Math.nextUp(expected);

        if (Double.compare(low, expected) > 0) {
            return false;
        }

        if (Double.compare(high, expected) < 0) {
            return false;
        }

        return true;
    }

    static void assertArraysEqualsWithinOneUlp(double[] a, double[] r, FUnOp mathf, FUnOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i])) == 0 ||
                                    isWithin1Ulp(r[i], strictmathf.apply(a[i])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i])) == 0, "at index #" + i + ", input = " + a[i] + ", actual = " + r[i] + ", expected = " + mathf.apply(a[i]));
            Assert.assertTrue(isWithin1Ulp(r[i], strictmathf.apply(a[i])), "at index #" + i + ", input = " + a[i] + ", actual = " + r[i] + ", expected (within 1 ulp) = " + strictmathf.apply(a[i]));
        }
    }

    static void assertArraysEqualsWithinOneUlp(double[] a, double[] b, double[] r, FBinOp mathf, FBinOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i], b[i])) == 0 ||
                                    isWithin1Ulp(r[i], strictmathf.apply(a[i], b[i])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i], b[i])) == 0, "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", actual = " + r[i] + ", expected = " + mathf.apply(a[i], b[i]));
            Assert.assertTrue(isWithin1Ulp(r[i], strictmathf.apply(a[i], b[i])), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", actual = " + r[i] + ", expected (within 1 ulp) = " + strictmathf.apply(a[i], b[i]));
        }
    }

    interface FBinArrayOp {
        double apply(double[] a, int b);
    }

    static void assertArraysEquals(double[] a, double[] r, FBinArrayOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a, i), r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a,i), r[i], "at index #" + i);
        }
    }
    interface FGatherScatterOp {
        double[] apply(double[] a, int ix, int[] b, int iy);
    }

    static void assertArraysEquals(double[] a, int[] b, double[] r, FGatherScatterOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, i, b, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, i, b, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(ref, res,
              "(ref: " + Arrays.toString(ref) + ", res: " + Arrays.toString(res) + ", a: "
              + Arrays.toString(Arrays.copyOfRange(a, i, i+SPECIES.length()))
              + ", b: "
              + Arrays.toString(Arrays.copyOfRange(b, i, i+SPECIES.length()))
              + " at index #" + i);
        }
    }


    static final List<IntFunction<double[]>> DOUBLE_GENERATORS = List.of(
            withToString("double[-i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (double)(-i * 5));
            }),
            withToString("double[i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (double)(i * 5));
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (((double)(i + 1) == 0) ? 1 : (double)(i + 1)));
            }),
            withToString("double[cornerCaseValue(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerCaseValue(i));
            })
    );

    // Create combinations of pairs
    // @@@ Might be sensitive to order e.g. div by 0
    static final List<List<IntFunction<double[]>>> DOUBLE_GENERATOR_PAIRS =
        Stream.of(DOUBLE_GENERATORS.get(0)).
                flatMap(fa -> DOUBLE_GENERATORS.stream().skip(1).map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] boolUnaryOpProvider() {
        return BOOL_ARRAY_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<List<IntFunction<double[]>>> DOUBLE_GENERATOR_TRIPLES =
        DOUBLE_GENERATOR_PAIRS.stream().
                flatMap(pair -> DOUBLE_GENERATORS.stream().map(f -> List.of(pair.get(0), pair.get(1), f))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] doubleBinaryOpProvider() {
        return DOUBLE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleIndexedOpProvider() {
        return DOUBLE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleBinaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATOR_PAIRS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleTernaryOpProvider() {
        return DOUBLE_GENERATOR_TRIPLES.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleTernaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATOR_TRIPLES.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpProvider() {
        return DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpShuffleProvider() {
        return INT_SHUFFLE_GENERATORS.stream().
                flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpIndexProvider() {
        return INT_INDEX_GENERATORS.stream().
                flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }


    static final List<IntFunction<double[]>> DOUBLE_COMPARE_GENERATORS = List.of(
            withToString("double[i]", (int s) -> {
                return fill(s * 1000,
                            i -> (double)i);
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (double)(i + 1));
            }),
            withToString("double[i - 2]", (int s) -> {
                return fill(s * 1000,
                            i -> (double)(i - 2));
            }),
            withToString("double[zigZag(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> i%3 == 0 ? (double)i : (i%3 == 1 ? (double)(i + 1) : (double)(i - 2)));
            }),
            withToString("double[cornerCaseValue(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerCaseValue(i));
            })
    );

    static final List<List<IntFunction<double[]>>> DOUBLE_COMPARE_GENERATOR_PAIRS =
        DOUBLE_COMPARE_GENERATORS.stream().
                flatMap(fa -> DOUBLE_COMPARE_GENERATORS.stream().map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] doubleCompareOpProvider() {
        return DOUBLE_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    interface ToDoubleF {
        double apply(int i);
    }

    static double[] fill(int s , ToDoubleF f) {
        return fill(new double[s], f);
    }

    static double[] fill(double[] a, ToDoubleF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static double cornerCaseValue(int i) {
        switch(i % 7) {
            case 0:
                return Double.MAX_VALUE;
            case 1:
                return Double.MIN_VALUE;
            case 2:
                return Double.NEGATIVE_INFINITY;
            case 3:
                return Double.POSITIVE_INFINITY;
            case 4:
                return Double.NaN;
            case 5:
                return (double)0.0;
            default:
                return (double)-0.0;
        }
    }
   static double get(double[] a, int i) {
       return (double) a[i];
   }

   static final IntFunction<double[]> fr = (vl) -> {
        int length = 1000 * vl;
        return new double[length];
    };

    static final IntFunction<boolean[]> fmr = (vl) -> {
        int length = 1000 * vl;
        return new boolean[length];
    };
    static double add(double a, double b) {
        return (double)(a + b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void addDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.add(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::add);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void addDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.add(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::add);
    }
    static double sub(double a, double b) {
        return (double)(a - b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void subDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.sub(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::sub);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void subDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.sub(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::sub);
    }

    static double div(double a, double b) {
        return (double)(a / b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void divDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.div(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::div);
    }



    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void divDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.div(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::div);
    }

    static double mul(double a, double b) {
        return (double)(a * b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void mulDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.mul(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::mul);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void mulDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.mul(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::mul);
    }










































    static double max(double a, double b) {
        return (double)(Math.max(a, b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void maxDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.max(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::max);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void maxDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.max(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::max);
    }
    static double min(double a, double b) {
        return (double)(Math.min(a, b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void minDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.min(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::min);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void minDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.min(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::min);
    }












    static double addLanes(double[] a, int idx) {
        double res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res += a[i];
        }

        return res;
    }

    static double addLanes(double[] a) {
        double res = 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            double tmp = 0;
            for (int j = 0; j < SPECIES.length(); j++) {
                tmp += a[i + j];
            }
            res += tmp;
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpProvider")
    static void addLanesDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = 0;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.addLanes();
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 0;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra += av.addLanes();
            }
        }

        assertReductionArraysEquals(a, r, ra, Double256VectorTests::addLanes, Double256VectorTests::addLanes);
    }
    static double addLanesMasked(double[] a, int idx, boolean[] mask) {
        double res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if(mask[i % SPECIES.length()])
                res += a[i];
        }

        return res;
    }

    static double addLanesMasked(double[] a, boolean[] mask) {
        double res = 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            double tmp = 0;
            for (int j = 0; j < SPECIES.length(); j++) {
                if(mask[(i + j) % SPECIES.length()])
                    tmp += a[i + j];
            }
            res += tmp;
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void addLanesDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);
        double ra = 0;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.addLanes(vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 0;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra += av.addLanes(vmask);
            }
        }

        assertReductionArraysEqualsMasked(a, r, ra, mask, Double256VectorTests::addLanesMasked, Double256VectorTests::addLanesMasked);
    }
    static double mulLanes(double[] a, int idx) {
        double res = 1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res *= a[i];
        }

        return res;
    }

    static double mulLanes(double[] a) {
        double res = 1;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            double tmp = 1;
            for (int j = 0; j < SPECIES.length(); j++) {
                tmp *= a[i + j];
            }
            res *= tmp;
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpProvider")
    static void mulLanesDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = 1;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.mulLanes();
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 1;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra *= av.mulLanes();
            }
        }

        assertReductionArraysEquals(a, r, ra, Double256VectorTests::mulLanes, Double256VectorTests::mulLanes);
    }
    static double mulLanesMasked(double[] a, int idx, boolean[] mask) {
        double res = 1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if(mask[i % SPECIES.length()])
                res *= a[i];
        }

        return res;
    }

    static double mulLanesMasked(double[] a, boolean[] mask) {
        double res = 1;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            double tmp = 1;
            for (int j = 0; j < SPECIES.length(); j++) {
                if(mask[(i + j) % SPECIES.length()])
                    tmp *= a[i + j];
            }
            res *= tmp;
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void mulLanesDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);
        double ra = 1;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.mulLanes(vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 1;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra *= av.mulLanes(vmask);
            }
        }

        assertReductionArraysEqualsMasked(a, r, ra, mask, Double256VectorTests::mulLanesMasked, Double256VectorTests::mulLanesMasked);
    }
    static double minLanes(double[] a, int idx) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = (double)Math.min(res, a[i]);
        }

        return res;
    }

    static double minLanes(double[] a) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            res = (double)Math.min(res, a[i]);
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpProvider")
    static void minLanesDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = Double.POSITIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.minLanes();
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.POSITIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double)Math.min(ra, av.minLanes());
            }
        }

        assertReductionArraysEquals(a, r, ra, Double256VectorTests::minLanes, Double256VectorTests::minLanes);
    }
    static double minLanesMasked(double[] a, int idx, boolean[] mask) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if(mask[i % SPECIES.length()])
                res = (double)Math.min(res, a[i]);
        }

        return res;
    }

    static double minLanesMasked(double[] a, boolean[] mask) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if(mask[i % SPECIES.length()])
                res = (double)Math.min(res, a[i]);
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void minLanesDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);
        double ra = Double.POSITIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.minLanes(vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.POSITIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double)Math.min(ra, av.minLanes(vmask));
            }
        }

        assertReductionArraysEqualsMasked(a, r, ra, mask, Double256VectorTests::minLanesMasked, Double256VectorTests::minLanesMasked);
    }
    static double maxLanes(double[] a, int idx) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = (double)Math.max(res, a[i]);
        }

        return res;
    }

    static double maxLanes(double[] a) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            res = (double)Math.max(res, a[i]);
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpProvider")
    static void maxLanesDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = Double.NEGATIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.maxLanes();
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double)Math.max(ra, av.maxLanes());
            }
        }

        assertReductionArraysEquals(a, r, ra, Double256VectorTests::maxLanes, Double256VectorTests::maxLanes);
    }
    static double maxLanesMasked(double[] a, int idx, boolean[] mask) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if(mask[i % SPECIES.length()])
                res = (double)Math.max(res, a[i]);
        }

        return res;
    }

    static double maxLanesMasked(double[] a, boolean[] mask) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if(mask[i % SPECIES.length()])
                res = (double)Math.max(res, a[i]);
        }

        return res;
    }
    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void maxLanesDouble256VectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);
        double ra = Double.NEGATIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.maxLanes(vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double)Math.max(ra, av.maxLanes(vmask));
            }
        }

        assertReductionArraysEqualsMasked(a, r, ra, mask, Double256VectorTests::maxLanesMasked, Double256VectorTests::maxLanesMasked);
    }





    @Test(dataProvider = "doubleUnaryOpProvider")
    static void withDouble256VectorTests(IntFunction<double []> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.with(0, (double)4).intoArray(r, i);
            }
        }

        assertInsertArraysEquals(a, r, (double)4, 0);
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void lessThanDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.lessThan(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.lane(j), a[i + j] < b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "doubleCompareOpProvider")
    static void greaterThanDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.greaterThan(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.lane(j), a[i + j] > b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "doubleCompareOpProvider")
    static void equalDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.equal(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.lane(j), a[i + j] == b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "doubleCompareOpProvider")
    static void notEqualDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.notEqual(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.lane(j), a[i + j] != b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "doubleCompareOpProvider")
    static void lessThanEqDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.lessThanEq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.lane(j), a[i + j] <= b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "doubleCompareOpProvider")
    static void greaterThanEqDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.greaterThanEq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.lane(j), a[i + j] >= b[i + j]);
                }
            }
        }
    }


    static double blend(double a, double b, boolean mask) {
        return mask ? b : a;
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void blendDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.blend(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Double256VectorTests::blend);
    }

    @Test(dataProvider = "doubleUnaryOpShuffleProvider")
    static void RearrangeDouble256VectorTests(IntFunction<double[]> fa,
                                           BiFunction<Integer,Integer,int[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.rearrange(VectorShuffle.fromArray(SPECIES, order, i)).intoArray(r, i);
            }
        }

        assertRearrangeArraysEquals(a, r, order, SPECIES.length());
    }




    @Test(dataProvider = "doubleUnaryOpProvider")
    static void getDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                int num_lanes = SPECIES.length();
                // Manually unroll because full unroll happens after intrinsification.
                // Unroll is needed because get intrinsic requires for index to be a known constant.
                if (num_lanes == 1) {
                    r[i]=av.lane(0);
                } else if (num_lanes == 2) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                } else if (num_lanes == 4) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                } else if (num_lanes == 8) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                } else if (num_lanes == 16) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                    r[i+8]=av.lane(8);
                    r[i+9]=av.lane(9);
                    r[i+10]=av.lane(10);
                    r[i+11]=av.lane(11);
                    r[i+12]=av.lane(12);
                    r[i+13]=av.lane(13);
                    r[i+14]=av.lane(14);
                    r[i+15]=av.lane(15);
                } else if (num_lanes == 32) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                    r[i+8]=av.lane(8);
                    r[i+9]=av.lane(9);
                    r[i+10]=av.lane(10);
                    r[i+11]=av.lane(11);
                    r[i+12]=av.lane(12);
                    r[i+13]=av.lane(13);
                    r[i+14]=av.lane(14);
                    r[i+15]=av.lane(15);
                    r[i+16]=av.lane(16);
                    r[i+17]=av.lane(17);
                    r[i+18]=av.lane(18);
                    r[i+19]=av.lane(19);
                    r[i+20]=av.lane(20);
                    r[i+21]=av.lane(21);
                    r[i+22]=av.lane(22);
                    r[i+23]=av.lane(23);
                    r[i+24]=av.lane(24);
                    r[i+25]=av.lane(25);
                    r[i+26]=av.lane(26);
                    r[i+27]=av.lane(27);
                    r[i+28]=av.lane(28);
                    r[i+29]=av.lane(29);
                    r[i+30]=av.lane(30);
                    r[i+31]=av.lane(31);
                } else if (num_lanes == 64) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                    r[i+8]=av.lane(8);
                    r[i+9]=av.lane(9);
                    r[i+10]=av.lane(10);
                    r[i+11]=av.lane(11);
                    r[i+12]=av.lane(12);
                    r[i+13]=av.lane(13);
                    r[i+14]=av.lane(14);
                    r[i+15]=av.lane(15);
                    r[i+16]=av.lane(16);
                    r[i+17]=av.lane(17);
                    r[i+18]=av.lane(18);
                    r[i+19]=av.lane(19);
                    r[i+20]=av.lane(20);
                    r[i+21]=av.lane(21);
                    r[i+22]=av.lane(22);
                    r[i+23]=av.lane(23);
                    r[i+24]=av.lane(24);
                    r[i+25]=av.lane(25);
                    r[i+26]=av.lane(26);
                    r[i+27]=av.lane(27);
                    r[i+28]=av.lane(28);
                    r[i+29]=av.lane(29);
                    r[i+30]=av.lane(30);
                    r[i+31]=av.lane(31);
                    r[i+32]=av.lane(32);
                    r[i+33]=av.lane(33);
                    r[i+34]=av.lane(34);
                    r[i+35]=av.lane(35);
                    r[i+36]=av.lane(36);
                    r[i+37]=av.lane(37);
                    r[i+38]=av.lane(38);
                    r[i+39]=av.lane(39);
                    r[i+40]=av.lane(40);
                    r[i+41]=av.lane(41);
                    r[i+42]=av.lane(42);
                    r[i+43]=av.lane(43);
                    r[i+44]=av.lane(44);
                    r[i+45]=av.lane(45);
                    r[i+46]=av.lane(46);
                    r[i+47]=av.lane(47);
                    r[i+48]=av.lane(48);
                    r[i+49]=av.lane(49);
                    r[i+50]=av.lane(50);
                    r[i+51]=av.lane(51);
                    r[i+52]=av.lane(52);
                    r[i+53]=av.lane(53);
                    r[i+54]=av.lane(54);
                    r[i+55]=av.lane(55);
                    r[i+56]=av.lane(56);
                    r[i+57]=av.lane(57);
                    r[i+58]=av.lane(58);
                    r[i+59]=av.lane(59);
                    r[i+60]=av.lane(60);
                    r[i+61]=av.lane(61);
                    r[i+62]=av.lane(62);
                    r[i+63]=av.lane(63);
                } else {
                    for (int j = 0; j < SPECIES.length(); j++) {
                        r[i+j]=av.lane(j);
                    }
                }
            }
        }

        assertArraysEquals(a, r, Double256VectorTests::get);
    }

    static double sin(double a) {
        return (double)(Math.sin((double)a));
    }

    static double strictsin(double a) {
        return (double)(StrictMath.sin((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void sinDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.sin().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::sin, Double256VectorTests::strictsin);
    }


    static double exp(double a) {
        return (double)(Math.exp((double)a));
    }

    static double strictexp(double a) {
        return (double)(StrictMath.exp((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void expDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.exp().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::exp, Double256VectorTests::strictexp);
    }


    static double log1p(double a) {
        return (double)(Math.log1p((double)a));
    }

    static double strictlog1p(double a) {
        return (double)(StrictMath.log1p((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void log1pDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.log1p().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::log1p, Double256VectorTests::strictlog1p);
    }


    static double log(double a) {
        return (double)(Math.log((double)a));
    }

    static double strictlog(double a) {
        return (double)(StrictMath.log((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void logDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.log().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::log, Double256VectorTests::strictlog);
    }


    static double log10(double a) {
        return (double)(Math.log10((double)a));
    }

    static double strictlog10(double a) {
        return (double)(StrictMath.log10((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void log10Double256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.log10().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::log10, Double256VectorTests::strictlog10);
    }


    static double expm1(double a) {
        return (double)(Math.expm1((double)a));
    }

    static double strictexpm1(double a) {
        return (double)(StrictMath.expm1((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void expm1Double256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.expm1().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::expm1, Double256VectorTests::strictexpm1);
    }


    static double cos(double a) {
        return (double)(Math.cos((double)a));
    }

    static double strictcos(double a) {
        return (double)(StrictMath.cos((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void cosDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.cos().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::cos, Double256VectorTests::strictcos);
    }


    static double tan(double a) {
        return (double)(Math.tan((double)a));
    }

    static double stricttan(double a) {
        return (double)(StrictMath.tan((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void tanDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.tan().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::tan, Double256VectorTests::stricttan);
    }


    static double sinh(double a) {
        return (double)(Math.sinh((double)a));
    }

    static double strictsinh(double a) {
        return (double)(StrictMath.sinh((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void sinhDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.sinh().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::sinh, Double256VectorTests::strictsinh);
    }


    static double cosh(double a) {
        return (double)(Math.cosh((double)a));
    }

    static double strictcosh(double a) {
        return (double)(StrictMath.cosh((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void coshDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.cosh().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::cosh, Double256VectorTests::strictcosh);
    }


    static double tanh(double a) {
        return (double)(Math.tanh((double)a));
    }

    static double stricttanh(double a) {
        return (double)(StrictMath.tanh((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void tanhDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.tanh().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::tanh, Double256VectorTests::stricttanh);
    }


    static double asin(double a) {
        return (double)(Math.asin((double)a));
    }

    static double strictasin(double a) {
        return (double)(StrictMath.asin((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void asinDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.asin().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::asin, Double256VectorTests::strictasin);
    }


    static double acos(double a) {
        return (double)(Math.acos((double)a));
    }

    static double strictacos(double a) {
        return (double)(StrictMath.acos((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void acosDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.acos().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::acos, Double256VectorTests::strictacos);
    }


    static double atan(double a) {
        return (double)(Math.atan((double)a));
    }

    static double strictatan(double a) {
        return (double)(StrictMath.atan((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void atanDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.atan().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::atan, Double256VectorTests::strictatan);
    }


    static double cbrt(double a) {
        return (double)(Math.cbrt((double)a));
    }

    static double strictcbrt(double a) {
        return (double)(StrictMath.cbrt((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void cbrtDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.cbrt().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Double256VectorTests::cbrt, Double256VectorTests::strictcbrt);
    }


    static double hypot(double a, double b) {
        return (double)(Math.hypot((double)a, (double)b));
    }

    static double stricthypot(double a, double b) {
        return (double)(StrictMath.hypot((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void hypotDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.hypot(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, b, r, Double256VectorTests::hypot, Double256VectorTests::stricthypot);
    }



    static double pow(double a, double b) {
        return (double)(Math.pow((double)a, (double)b));
    }

    static double strictpow(double a, double b) {
        return (double)(StrictMath.pow((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void powDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.pow(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, b, r, Double256VectorTests::pow, Double256VectorTests::strictpow);
    }



    static double atan2(double a, double b) {
        return (double)(Math.atan2((double)a, (double)b));
    }

    static double strictatan2(double a, double b) {
        return (double)(StrictMath.atan2((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void atan2Double256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.atan2(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, b, r, Double256VectorTests::atan2, Double256VectorTests::strictatan2);
    }



    static double fma(double a, double b, double c) {
        return (double)(Math.fma(a, b, c));
    }


    @Test(dataProvider = "doubleTernaryOpProvider")
    static void fmaDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
                av.fma(bv, cv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, c, r, Double256VectorTests::fma);
    }


    @Test(dataProvider = "doubleTernaryOpMaskProvider")
    static void fmaDouble256VectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<double[]> fc, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
                av.fma(bv, cv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, c, r, mask, Double256VectorTests::fma);
    }


    static double neg(double a) {
        return (double)(-((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void negDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.neg().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Double256VectorTests::neg);
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void negMaskedDouble256VectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.neg(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Double256VectorTests::neg);
    }

    static double abs(double a) {
        return (double)(Math.abs((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void absDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.abs().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Double256VectorTests::abs);
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void absMaskedDouble256VectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.abs(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Double256VectorTests::abs);
    }





    static double sqrt(double a) {
        return (double)(Math.sqrt((double)a));
    }



    @Test(dataProvider = "doubleUnaryOpProvider")
    static void sqrtDouble256VectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.sqrt().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Double256VectorTests::sqrt);
    }



    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void sqrtMaskedDouble256VectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromValues(SPECIES, mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.sqrt(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Double256VectorTests::sqrt);
    }


    static double[] gather(double a[], int ix, int[] b, int iy) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0; i < SPECIES.length(); i++) {
            int bi = iy + i;
            res[i] = a[b[bi] + ix];
        }
        return res;
    }

    @Test(dataProvider = "doubleUnaryOpIndexProvider")
    static void gatherDouble256VectorTests(IntFunction<double[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        int[] b    = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i, b, i);
                av.intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::gather);
    }


    static double[] scatter(double a[], int ix, int[] b, int iy) {
      double[] res = new double[SPECIES.length()];
      for (int i = 0; i < SPECIES.length(); i++) {
        int bi = iy + i;
        res[b[bi]] = a[i + ix];
      }
      return res;
    }

    @Test(dataProvider = "doubleUnaryOpIndexProvider")
    static void scatterDouble256VectorTests(IntFunction<double[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.intoArray(r, i, b, i);
            }
        }

        assertArraysEquals(a, b, r, Double256VectorTests::scatter);
    }

}
