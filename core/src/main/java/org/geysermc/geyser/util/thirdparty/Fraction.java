/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.geysermc.geyser.util.thirdparty; // Geyser

import java.math.BigInteger;
import java.util.Objects;

/**
 * {@link Fraction} is a {@link Number} implementation that
 * stores fractions accurately.
 *
 * <p>This class is immutable, and interoperable with most methods that accept
 * a {@link Number}.</p>
 *
 * <p>Note that this class is intended for common use cases, it is <i>int</i>
 * based and thus suffers from various overflow issues. For a BigInteger based
 * equivalent, please see the Commons Math BigFraction class.</p>
 *
 * @since 2.0
 */
// Geyser: Java Edition uses this for 1.21.3 bundle calculation. Rather than
// Reimplementing an open-source class or bringing in a whole library,
// the single class is used to ensure accuracy.
public final class Fraction extends Number implements Comparable<Fraction> {

    /**
     * Required for serialization support. Lang version 2.0.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 65382027393090L;

    /**
     * {@link Fraction} representation of 0.
     */
    public static final Fraction ZERO = new Fraction(0, 1);
    /**
     * {@link Fraction} representation of 1.
     */
    public static final Fraction ONE = new Fraction(1, 1);
    /**
     * {@link Fraction} representation of 1/2.
     */
    public static final Fraction ONE_HALF = new Fraction(1, 2);
    /**
     * {@link Fraction} representation of 1/3.
     */
    public static final Fraction ONE_THIRD = new Fraction(1, 3);
    /**
     * {@link Fraction} representation of 2/3.
     */
    public static final Fraction TWO_THIRDS = new Fraction(2, 3);
    /**
     * {@link Fraction} representation of 1/4.
     */
    public static final Fraction ONE_QUARTER = new Fraction(1, 4);
    /**
     * {@link Fraction} representation of 2/4.
     */
    public static final Fraction TWO_QUARTERS = new Fraction(2, 4);
    /**
     * {@link Fraction} representation of 3/4.
     */
    public static final Fraction THREE_QUARTERS = new Fraction(3, 4);
    /**
     * {@link Fraction} representation of 1/5.
     */
    public static final Fraction ONE_FIFTH = new Fraction(1, 5);
    /**
     * {@link Fraction} representation of 2/5.
     */
    public static final Fraction TWO_FIFTHS = new Fraction(2, 5);
    /**
     * {@link Fraction} representation of 3/5.
     */
    public static final Fraction THREE_FIFTHS = new Fraction(3, 5);
    /**
     * {@link Fraction} representation of 4/5.
     */
    public static final Fraction FOUR_FIFTHS = new Fraction(4, 5);


    /**
     * Add two integers, checking for overflow.
     *
     * @param x an addend
     * @param y an addend
     * @return the sum {@code x+y}
     * @throws ArithmeticException if the result can not be represented as
     * an int
     */
    private static int addAndCheck(final int x, final int y) {
        final long s = (long) x + (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int) s;
    }
    /**
     * Creates a {@link Fraction} instance from a {@code double} value.
     *
     * <p>This method uses the <a href="https://web.archive.org/web/20210516065058/http%3A//archives.math.utk.edu/articles/atuyl/confrac/">
     *  continued fraction algorithm</a>, computing a maximum of
     *  25 convergents and bounding the denominator by 10,000.</p>
     *
     * @param value  the double value to convert
     * @return a new fraction instance that is close to the value
     * @throws ArithmeticException if {@code |value| &gt; Integer.MAX_VALUE}
     *  or {@code value = NaN}
     * @throws ArithmeticException if the calculated denominator is {@code zero}
     * @throws ArithmeticException if the algorithm does not converge
     */
    public static Fraction getFraction(double value) {
        final int sign = value < 0 ? -1 : 1;
        value = Math.abs(value);
        if (value > Integer.MAX_VALUE || Double.isNaN(value)) {
            throw new ArithmeticException("The value must not be greater than Integer.MAX_VALUE or NaN");
        }
        final int wholeNumber = (int) value;
        value -= wholeNumber;

        int numer0 = 0; // the pre-previous
        int denom0 = 1; // the pre-previous
        int numer1 = 1; // the previous
        int denom1 = 0; // the previous
        int numer2; // the current, setup in calculation
        int denom2; // the current, setup in calculation
        int a1 = (int) value;
        int a2;
        double x1 = 1;
        double x2;
        double y1 = value - a1;
        double y2;
        double delta1, delta2 = Double.MAX_VALUE;
        double fraction;
        int i = 1;
        do {
            delta1 = delta2;
            a2 = (int) (x1 / y1);
            x2 = y1;
            y2 = x1 - a2 * y1;
            numer2 = a1 * numer1 + numer0;
            denom2 = a1 * denom1 + denom0;
            fraction = (double) numer2 / (double) denom2;
            delta2 = Math.abs(value - fraction);
            a1 = a2;
            x1 = x2;
            y1 = y2;
            numer0 = numer1;
            denom0 = denom1;
            numer1 = numer2;
            denom1 = denom2;
            i++;
        } while (delta1 > delta2 && denom2 <= 10000 && denom2 > 0 && i < 25);
        if (i == 25) {
            throw new ArithmeticException("Unable to convert double to fraction");
        }
        return getReducedFraction((numer0 + wholeNumber * denom0) * sign, denom0);
    }

    /**
     * Creates a {@link Fraction} instance with the 2 parts
     * of a fraction Y/Z.
     *
     * <p>Any negative signs are resolved to be on the numerator.</p>
     *
     * @param numerator  the numerator, for example the three in 'three sevenths'
     * @param denominator  the denominator, for example the seven in 'three sevenths'
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is {@code zero}
     * or the denominator is {@code negative} and the numerator is {@code Integer#MIN_VALUE}
     */
    public static Fraction getFraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }
        if (denominator < 0) {
            if (numerator == Integer.MIN_VALUE || denominator == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: can't negate");
            }
            numerator = -numerator;
            denominator = -denominator;
        }
        return new Fraction(numerator, denominator);
    }
    /**
     * Creates a {@link Fraction} instance with the 3 parts
     * of a fraction X Y/Z.
     *
     * <p>The negative sign must be passed in on the whole number part.</p>
     *
     * @param whole  the whole number, for example the one in 'one and three sevenths'
     * @param numerator  the numerator, for example the three in 'one and three sevenths'
     * @param denominator  the denominator, for example the seven in 'one and three sevenths'
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is {@code zero}
     * @throws ArithmeticException if the denominator is negative
     * @throws ArithmeticException if the numerator is negative
     * @throws ArithmeticException if the resulting numerator exceeds
     *  {@code Integer.MAX_VALUE}
     */
    public static Fraction getFraction(final int whole, final int numerator, final int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }
        if (denominator < 0) {
            throw new ArithmeticException("The denominator must not be negative");
        }
        if (numerator < 0) {
            throw new ArithmeticException("The numerator must not be negative");
        }
        final long numeratorValue;
        if (whole < 0) {
            numeratorValue = whole * (long) denominator - numerator;
        } else {
            numeratorValue = whole * (long) denominator + numerator;
        }
        if (numeratorValue < Integer.MIN_VALUE || numeratorValue > Integer.MAX_VALUE) {
            throw new ArithmeticException("Numerator too large to represent as an Integer.");
        }
        return new Fraction((int) numeratorValue, denominator);
    }
    /**
     * Creates a Fraction from a {@link String}.
     *
     * <p>The formats accepted are:</p>
     *
     * <ol>
     *  <li>{@code double} String containing a dot</li>
     *  <li>'X Y/Z'</li>
     *  <li>'Y/Z'</li>
     *  <li>'X' (a simple whole number)</li>
     * </ol>
     * <p>and a .</p>
     *
     * @param str  the string to parse, must not be {@code null}
     * @return the new {@link Fraction} instance
     * @throws NullPointerException if the string is {@code null}
     * @throws NumberFormatException if the number format is invalid
     */
    public static Fraction getFraction(String str) {
        Objects.requireNonNull(str, "str");
        // parse double format
        int pos = str.indexOf('.');
        if (pos >= 0) {
            return getFraction(Double.parseDouble(str));
        }

        // parse X Y/Z format
        pos = str.indexOf(' ');
        if (pos > 0) {
            final int whole = Integer.parseInt(str.substring(0, pos));
            str = str.substring(pos + 1);
            pos = str.indexOf('/');
            if (pos < 0) {
                throw new NumberFormatException("The fraction could not be parsed as the format X Y/Z");
            }
            final int numer = Integer.parseInt(str.substring(0, pos));
            final int denom = Integer.parseInt(str.substring(pos + 1));
            return getFraction(whole, numer, denom);
        }

        // parse Y/Z format
        pos = str.indexOf('/');
        if (pos < 0) {
            // simple whole number
            return getFraction(Integer.parseInt(str), 1);
        }
        final int numer = Integer.parseInt(str.substring(0, pos));
        final int denom = Integer.parseInt(str.substring(pos + 1));
        return getFraction(numer, denom);
    }

    /**
     * Creates a reduced {@link Fraction} instance with the 2 parts
     * of a fraction Y/Z.
     *
     * <p>For example, if the input parameters represent 2/4, then the created
     * fraction will be 1/2.</p>
     *
     * <p>Any negative signs are resolved to be on the numerator.</p>
     *
     * @param numerator  the numerator, for example the three in 'three sevenths'
     * @param denominator  the denominator, for example the seven in 'three sevenths'
     * @return a new fraction instance, with the numerator and denominator reduced
     * @throws ArithmeticException if the denominator is {@code zero}
     */
    public static Fraction getReducedFraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }
        if (numerator == 0) {
            return ZERO; // normalize zero.
        }
        // allow 2^k/-2^31 as a valid fraction (where k>0)
        if (denominator == Integer.MIN_VALUE && (numerator & 1) == 0) {
            numerator /= 2;
            denominator /= 2;
        }
        if (denominator < 0) {
            if (numerator == Integer.MIN_VALUE || denominator == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: can't negate");
            }
            numerator = -numerator;
            denominator = -denominator;
        }
        // simplify fraction.
        final int gcd = greatestCommonDivisor(numerator, denominator);
        numerator /= gcd;
        denominator /= gcd;
        return new Fraction(numerator, denominator);
    }

    /**
     * Gets the greatest common divisor of the absolute value of
     * two numbers, using the "binary gcd" method which avoids
     * division and modulo operations.  See Knuth 4.5.2 algorithm B.
     * This algorithm is due to Josef Stein (1961).
     *
     * @param u  a non-zero number
     * @param v  a non-zero number
     * @return the greatest common divisor, never zero
     */
    private static int greatestCommonDivisor(int u, int v) {
        // From Commons Math:
        if (u == 0 || v == 0) {
            if (u == Integer.MIN_VALUE || v == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: gcd is 2^31");
            }
            return Math.abs(u) + Math.abs(v);
        }
        // if either operand is abs 1, return 1:
        if (Math.abs(u) == 1 || Math.abs(v) == 1) {
            return 1;
        }
        // keep u and v negative, as negative integers range down to
        // -2^31, while positive numbers can only be as large as 2^31-1
        // (i.e. we can't necessarily negate a negative number without
        // overflow)
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
        // B1. [Find power of 2]
        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0 && k < 31) { // while u and v are both even...
            u /= 2;
            v /= 2;
            k++; // cast out twos.
        }
        if (k == 31) {
            throw new ArithmeticException("overflow: gcd is 2^31");
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        // one is odd.
        int t = (u & 1) == 1 ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            while ((t & 1) == 0) { // while t is even.
                t /= 2; // cast out twos
            }
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = (v - u) / 2;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        return -u * (1 << k); // gcd is u*2^k
    }

    /**
     * Multiply two integers, checking for overflow.
     *
     * @param x a factor
     * @param y a factor
     * @return the product {@code x*y}
     * @throws ArithmeticException if the result can not be represented as
     *                             an int
     */
    private static int mulAndCheck(final int x, final int y) {
        final long m = (long) x * (long) y;
        if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mul");
        }
        return (int) m;
    }

    /**
     *  Multiply two non-negative integers, checking for overflow.
     *
     * @param x a non-negative factor
     * @param y a non-negative factor
     * @return the product {@code x*y}
     * @throws ArithmeticException if the result can not be represented as
     * an int
     */
    private static int mulPosAndCheck(final int x, final int y) {
        /* assert x>=0 && y>=0; */
        final long m = (long) x * (long) y;
        if (m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mulPos");
        }
        return (int) m;
    }

    /**
     * Subtract two integers, checking for overflow.
     *
     * @param x the minuend
     * @param y the subtrahend
     * @return the difference {@code x-y}
     * @throws ArithmeticException if the result can not be represented as
     * an int
     */
    private static int subAndCheck(final int x, final int y) {
        final long s = (long) x - (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int) s;
    }

    /**
     * The numerator number part of the fraction (the three in three sevenths).
     */
    private final int numerator;

    /**
     * The denominator number part of the fraction (the seven in three sevenths).
     */
    private final int denominator;

    /**
     * Cached output hashCode (class is immutable).
     */
    private transient int hashCode;

    /**
     * Cached output toString (class is immutable).
     */
    private transient String toString;

    /**
     * Cached output toProperString (class is immutable).
     */
    private transient String toProperString;

    /**
     * Constructs a {@link Fraction} instance with the 2 parts
     * of a fraction Y/Z.
     *
     * @param numerator  the numerator, for example the three in 'three sevenths'
     * @param denominator  the denominator, for example the seven in 'three sevenths'
     */
    private Fraction(final int numerator, final int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Gets a fraction that is the positive equivalent of this one.
     * <p>More precisely: {@code (fraction &gt;= 0 ? this : -fraction)}</p>
     *
     * <p>The returned fraction is not reduced.</p>
     *
     * @return {@code this} if it is positive, or a new positive fraction
     *  instance with the opposite signed numerator
     */
    public Fraction abs() {
        if (numerator >= 0) {
            return this;
        }
        return negate();
    }

    /**
     * Adds the value of this fraction to another, returning the result in reduced form.
     * The algorithm follows Knuth, 4.5.1.
     *
     * @param fraction  the fraction to add, must not be {@code null}
     * @return a {@link Fraction} instance with the resulting values
     * @throws NullPointerException if the fraction is {@code null}
     * @throws ArithmeticException if the resulting numerator or denominator exceeds
     *  {@code Integer.MAX_VALUE}
     */
    public Fraction add(final Fraction fraction) {
        return addSub(fraction, true /* add */);
    }

    /**
     * Implement add and subtract using algorithm described in Knuth 4.5.1.
     *
     * @param fraction the fraction to subtract, must not be {@code null}
     * @param isAdd true to add, false to subtract
     * @return a {@link Fraction} instance with the resulting values
     * @throws IllegalArgumentException if the fraction is {@code null}
     * @throws ArithmeticException if the resulting numerator or denominator
     *   cannot be represented in an {@code int}.
     */
    private Fraction addSub(final Fraction fraction, final boolean isAdd) {
        Objects.requireNonNull(fraction, "fraction");
        // zero is identity for addition.
        if (numerator == 0) {
            return isAdd ? fraction : fraction.negate();
        }
        if (fraction.numerator == 0) {
            return this;
        }
        // if denominators are randomly distributed, d1 will be 1 about 61%
        // of the time.
        final int d1 = greatestCommonDivisor(denominator, fraction.denominator);
        if (d1 == 1) {
            // result is ( (u*v' +/- u'v) / u'v')
            final int uvp = mulAndCheck(numerator, fraction.denominator);
            final int upv = mulAndCheck(fraction.numerator, denominator);
            return new Fraction(isAdd ? addAndCheck(uvp, upv) : subAndCheck(uvp, upv), mulPosAndCheck(denominator,
                    fraction.denominator));
        }
        // the quantity 't' requires 65 bits of precision; see knuth 4.5.1
        // exercise 7. we're going to use a BigInteger.
        // t = u(v'/d1) +/- v(u'/d1)
        final BigInteger uvp = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(fraction.denominator / d1));
        final BigInteger upv = BigInteger.valueOf(fraction.numerator).multiply(BigInteger.valueOf(denominator / d1));
        final BigInteger t = isAdd ? uvp.add(upv) : uvp.subtract(upv);
        // but d2 doesn't need extra precision because
        // d2 = gcd(t,d1) = gcd(t mod d1, d1)
        final int tmodd1 = t.mod(BigInteger.valueOf(d1)).intValue();
        final int d2 = tmodd1 == 0 ? d1 : greatestCommonDivisor(tmodd1, d1);

        // result is (t/d2) / (u'/d1)(v'/d2)
        final BigInteger w = t.divide(BigInteger.valueOf(d2));
        if (w.bitLength() > 31) {
            throw new ArithmeticException("overflow: numerator too large after multiply");
        }
        return new Fraction(w.intValue(), mulPosAndCheck(denominator / d1, fraction.denominator / d2));
    }

    /**
     * Compares this object to another based on size.
     *
     * <p>Note: this class has a natural ordering that is inconsistent
     * with equals, because, for example, equals treats 1/2 and 2/4 as
     * different, whereas compareTo treats them as equal.
     *
     * @param other  the object to compare to
     * @return -1 if this is less, 0 if equal, +1 if greater
     * @throws ClassCastException if the object is not a {@link Fraction}
     * @throws NullPointerException if the object is {@code null}
     */
    @Override
    public int compareTo(final Fraction other) {
        if (this == other) {
            return 0;
        }
        if (numerator == other.numerator && denominator == other.denominator) {
            return 0;
        }

        // otherwise see which is less
        final long first = (long) numerator * (long) other.denominator;
        final long second = (long) other.numerator * (long) denominator;
        return Long.compare(first, second);
    }

    /**
     * Divide the value of this fraction by another.
     *
     * @param fraction  the fraction to divide by, must not be {@code null}
     * @return a {@link Fraction} instance with the resulting values
     * @throws NullPointerException if the fraction is {@code null}
     * @throws ArithmeticException if the fraction to divide by is zero
     * @throws ArithmeticException if the resulting numerator or denominator exceeds
     *  {@code Integer.MAX_VALUE}
     */
    public Fraction divideBy(final Fraction fraction) {
        Objects.requireNonNull(fraction, "fraction");
        if (fraction.numerator == 0) {
            throw new ArithmeticException("The fraction to divide by must not be zero");
        }
        return multiplyBy(fraction.invert());
    }

    /**
     * Gets the fraction as a {@code double}. This calculates the fraction
     * as the numerator divided by denominator.
     *
     * @return the fraction as a {@code double}
     */
    @Override
    public double doubleValue() {
        return (double) numerator / (double) denominator;
    }

    /**
     * Compares this fraction to another object to test if they are equal..
     *
     * <p>To be equal, both values must be equal. Thus 2/4 is not equal to 1/2.</p>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Fraction)) {
            return false;
        }
        final Fraction other = (Fraction) obj;
        return getNumerator() == other.getNumerator() && getDenominator() == other.getDenominator();
    }

    /**
     * Gets the fraction as a {@code float}. This calculates the fraction
     * as the numerator divided by denominator.
     *
     * @return the fraction as a {@code float}
     */
    @Override
    public float floatValue() {
        return (float) numerator / (float) denominator;
    }

    /**
     * Gets the denominator part of the fraction.
     *
     * @return the denominator fraction part
     */
    public int getDenominator() {
        return denominator;
    }

    /**
     * Gets the numerator part of the fraction.
     *
     * <p>This method may return a value greater than the denominator, an
     * improper fraction, such as the seven in 7/4.</p>
     *
     * @return the numerator fraction part
     */
    public int getNumerator() {
        return numerator;
    }

    /**
     * Gets the proper numerator, always positive.
     *
     * <p>An improper fraction 7/4 can be resolved into a proper one, 1 3/4.
     * This method returns the 3 from the proper fraction.</p>
     *
     * <p>If the fraction is negative such as -7/4, it can be resolved into
     * -1 3/4, so this method returns the positive proper numerator, 3.</p>
     *
     * @return the numerator fraction part of a proper fraction, always positive
     */
    public int getProperNumerator() {
        return Math.abs(numerator % denominator);
    }

    /**
     * Gets the proper whole part of the fraction.
     *
     * <p>An improper fraction 7/4 can be resolved into a proper one, 1 3/4.
     * This method returns the 1 from the proper fraction.</p>
     *
     * <p>If the fraction is negative such as -7/4, it can be resolved into
     * -1 3/4, so this method returns the positive whole part -1.</p>
     *
     * @return the whole fraction part of a proper fraction, that includes the sign
     */
    public int getProperWhole() {
        return numerator / denominator;
    }

    /**
     * Gets a hashCode for the fraction.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            // hash code update should be atomic.
            hashCode = 37 * (37 * 17 + getNumerator()) + getDenominator();
        }
        return hashCode;
    }

    /**
     * Gets the fraction as an {@code int}. This returns the whole number
     * part of the fraction.
     *
     * @return the whole number fraction part
     */
    @Override
    public int intValue() {
        return numerator / denominator;
    }

    /**
     * Gets a fraction that is the inverse (1/fraction) of this one.
     *
     * <p>The returned fraction is not reduced.</p>
     *
     * @return a new fraction instance with the numerator and denominator
     *         inverted.
     * @throws ArithmeticException if the fraction represents zero.
     */
    public Fraction invert() {
        if (numerator == 0) {
            throw new ArithmeticException("Unable to invert zero.");
        }
        if (numerator==Integer.MIN_VALUE) {
            throw new ArithmeticException("overflow: can't negate numerator");
        }
        if (numerator<0) {
            return new Fraction(-denominator, -numerator);
        }
        return new Fraction(denominator, numerator);
    }

    /**
     * Gets the fraction as a {@code long}. This returns the whole number
     * part of the fraction.
     *
     * @return the whole number fraction part
     */
    @Override
    public long longValue() {
        return (long) numerator / denominator;
    }

    /**
     * Multiplies the value of this fraction by another, returning the
     * result in reduced form.
     *
     * @param fraction  the fraction to multiply by, must not be {@code null}
     * @return a {@link Fraction} instance with the resulting values
     * @throws NullPointerException if the fraction is {@code null}
     * @throws ArithmeticException if the resulting numerator or denominator exceeds
     *  {@code Integer.MAX_VALUE}
     */
    public Fraction multiplyBy(final Fraction fraction) {
        Objects.requireNonNull(fraction, "fraction");
        if (numerator == 0 || fraction.numerator == 0) {
            return ZERO;
        }
        // knuth 4.5.1
        // make sure we don't overflow unless the result *must* overflow.
        final int d1 = greatestCommonDivisor(numerator, fraction.denominator);
        final int d2 = greatestCommonDivisor(fraction.numerator, denominator);
        return getReducedFraction(mulAndCheck(numerator / d1, fraction.numerator / d2),
                mulPosAndCheck(denominator / d2, fraction.denominator / d1));
    }

    /**
     * Gets a fraction that is the negative (-fraction) of this one.
     *
     * <p>The returned fraction is not reduced.</p>
     *
     * @return a new fraction instance with the opposite signed numerator
     */
    public Fraction negate() {
        // the positive range is one smaller than the negative range of an int.
        if (numerator==Integer.MIN_VALUE) {
            throw new ArithmeticException("overflow: too large to negate");
        }
        return new Fraction(-numerator, denominator);
    }

    /**
     * Gets a fraction that is raised to the passed in power.
     *
     * <p>The returned fraction is in reduced form.</p>
     *
     * @param power  the power to raise the fraction to
     * @return {@code this} if the power is one, {@link #ONE} if the power
     * is zero (even if the fraction equals ZERO) or a new fraction instance
     * raised to the appropriate power
     * @throws ArithmeticException if the resulting numerator or denominator exceeds
     *  {@code Integer.MAX_VALUE}
     */
    public Fraction pow(final int power) {
        if (power == 1) {
            return this;
        }
        if (power == 0) {
            return ONE;
        }
        if (power < 0) {
            if (power == Integer.MIN_VALUE) { // MIN_VALUE can't be negated.
                return this.invert().pow(2).pow(-(power / 2));
            }
            return this.invert().pow(-power);
        }
        final Fraction f = this.multiplyBy(this);
        if (power % 2 == 0) { // if even...
            return f.pow(power / 2);
        }
        return f.pow(power / 2).multiplyBy(this);
    }

    /**
     * Reduce the fraction to the smallest values for the numerator and
     * denominator, returning the result.
     *
     * <p>For example, if this fraction represents 2/4, then the result
     * will be 1/2.</p>
     *
     * @return a new reduced fraction instance, or this if no simplification possible
     */
    public Fraction reduce() {
        if (numerator == 0) {
            return equals(ZERO) ? this : ZERO;
        }
        final int gcd = greatestCommonDivisor(Math.abs(numerator), denominator);
        if (gcd == 1) {
            return this;
        }
        return getFraction(numerator / gcd, denominator / gcd);
    }

    /**
     * Subtracts the value of another fraction from the value of this one,
     * returning the result in reduced form.
     *
     * @param fraction  the fraction to subtract, must not be {@code null}
     * @return a {@link Fraction} instance with the resulting values
     * @throws NullPointerException if the fraction is {@code null}
     * @throws ArithmeticException if the resulting numerator or denominator
     *   cannot be represented in an {@code int}.
     */
    public Fraction subtract(final Fraction fraction) {
        return addSub(fraction, false /* subtract */);
    }

    /**
     * Gets the fraction as a proper {@link String} in the format X Y/Z.
     *
     * <p>The format used in '<i>wholeNumber</i> <i>numerator</i>/<i>denominator</i>'.
     * If the whole number is zero it will be omitted. If the numerator is zero,
     * only the whole number is returned.</p>
     *
     * @return a {@link String} form of the fraction
     */
    public String toProperString() {
        if (toProperString == null) {
            if (numerator == 0) {
                toProperString = "0";
            } else if (numerator == denominator) {
                toProperString = "1";
            } else if (numerator == -1 * denominator) {
                toProperString = "-1";
            } else if ((numerator > 0 ? -numerator : numerator) < -denominator) {
                // note that we do the magnitude comparison test above with
                // NEGATIVE (not positive) numbers, since negative numbers
                // have a larger range. otherwise numerator==Integer.MIN_VALUE
                // is handled incorrectly.
                final int properNumerator = getProperNumerator();
                if (properNumerator == 0) {
                    toProperString = Integer.toString(getProperWhole());
                } else {
                    toProperString = getProperWhole() + " " + properNumerator + "/" + getDenominator();
                }
            } else {
                toProperString = getNumerator() + "/" + getDenominator();
            }
        }
        return toProperString;
    }

    /**
     * Gets the fraction as a {@link String}.
     *
     * <p>The format used is '<i>numerator</i>/<i>denominator</i>' always.
     *
     * @return a {@link String} form of the fraction
     */
    @Override
    public String toString() {
        if (toString == null) {
            toString = getNumerator() + "/" + getDenominator();
        }
        return toString;
    }
}
