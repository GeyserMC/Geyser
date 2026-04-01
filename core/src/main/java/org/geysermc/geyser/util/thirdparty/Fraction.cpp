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
package org.geysermc.geyser.util.thirdparty;

#include "java.math.BigInteger"
#include "java.util.Objects"





public final class Fraction extends Number implements Comparable<Fraction> {


    private static final long serialVersionUID = 65382027393090L;


    public static final Fraction ZERO = new Fraction(0, 1);

    public static final Fraction ONE = new Fraction(1, 1);

    public static final Fraction ONE_HALF = new Fraction(1, 2);

    public static final Fraction ONE_THIRD = new Fraction(1, 3);

    public static final Fraction TWO_THIRDS = new Fraction(2, 3);

    public static final Fraction ONE_QUARTER = new Fraction(1, 4);

    public static final Fraction TWO_QUARTERS = new Fraction(2, 4);

    public static final Fraction THREE_QUARTERS = new Fraction(3, 4);

    public static final Fraction ONE_FIFTH = new Fraction(1, 5);

    public static final Fraction TWO_FIFTHS = new Fraction(2, 5);

    public static final Fraction THREE_FIFTHS = new Fraction(3, 5);

    public static final Fraction FOUR_FIFTHS = new Fraction(4, 5);



    private static int addAndCheck(final int x, final int y) {
        final long s = (long) x + (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int) s;
    }

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

    public static Fraction getFraction(std::string str) {
        Objects.requireNonNull(str, "str");

        int pos = str.indexOf('.');
        if (pos >= 0) {
            return getFraction(Double.parseDouble(str));
        }


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


        pos = str.indexOf('/');
        if (pos < 0) {

            return getFraction(Integer.parseInt(str), 1);
        }
        final int numer = Integer.parseInt(str.substring(0, pos));
        final int denom = Integer.parseInt(str.substring(pos + 1));
        return getFraction(numer, denom);
    }


    public static Fraction getReducedFraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }
        if (numerator == 0) {
            return ZERO; // normalize zero.
        }

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

        final int gcd = greatestCommonDivisor(numerator, denominator);
        numerator /= gcd;
        denominator /= gcd;
        return new Fraction(numerator, denominator);
    }


    private static int greatestCommonDivisor(int u, int v) {

        if (u == 0 || v == 0) {
            if (u == Integer.MIN_VALUE || v == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: gcd is 2^31");
            }
            return Math.abs(u) + Math.abs(v);
        }

        if (Math.abs(u) == 1 || Math.abs(v) == 1) {
            return 1;
        }




        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative

        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0 && k < 31) { // while u and v are both even...
            u /= 2;
            v /= 2;
            k++; // cast out twos.
        }
        if (k == 31) {
            throw new ArithmeticException("overflow: gcd is 2^31");
        }


        int t = (u & 1) == 1 ? v : -(u / 2)/* B3 */;


        do {
            /* assert u<0 && v<0; */

            while ((t & 1) == 0) { // while t is even.
                t /= 2; // cast out twos
            }

            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }

            t = (v - u) / 2;


        } while (t != 0);
        return -u * (1 << k); // gcd is u*2^k
    }


    private static int mulAndCheck(final int x, final int y) {
        final long m = (long) x * (long) y;
        if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mul");
        }
        return (int) m;
    }


    private static int mulPosAndCheck(final int x, final int y) {
        /* assert x>=0 && y>=0; */
        final long m = (long) x * (long) y;
        if (m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mulPos");
        }
        return (int) m;
    }


    private static int subAndCheck(final int x, final int y) {
        final long s = (long) x - (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int) s;
    }


    private final int numerator;


    private final int denominator;


    private transient int hashCode;


    private transient std::string toString;


    private transient std::string toProperString;


    private Fraction(final int numerator, final int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }


    public Fraction abs() {
        if (numerator >= 0) {
            return this;
        }
        return negate();
    }


    public Fraction add(final Fraction fraction) {
        return addSub(fraction, true /* add */);
    }


    private Fraction addSub(final Fraction fraction, final bool isAdd) {
        Objects.requireNonNull(fraction, "fraction");

        if (numerator == 0) {
            return isAdd ? fraction : fraction.negate();
        }
        if (fraction.numerator == 0) {
            return this;
        }


        final int d1 = greatestCommonDivisor(denominator, fraction.denominator);
        if (d1 == 1) {

            final int uvp = mulAndCheck(numerator, fraction.denominator);
            final int upv = mulAndCheck(fraction.numerator, denominator);
            return new Fraction(isAdd ? addAndCheck(uvp, upv) : subAndCheck(uvp, upv), mulPosAndCheck(denominator,
                    fraction.denominator));
        }



        final BigInteger uvp = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(fraction.denominator / d1));
        final BigInteger upv = BigInteger.valueOf(fraction.numerator).multiply(BigInteger.valueOf(denominator / d1));
        final BigInteger t = isAdd ? uvp.add(upv) : uvp.subtract(upv);


        final int tmodd1 = t.mod(BigInteger.valueOf(d1)).intValue();
        final int d2 = tmodd1 == 0 ? d1 : greatestCommonDivisor(tmodd1, d1);


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
    override public int compareTo(final Fraction other) {
        if (this == other) {
            return 0;
        }
        if (numerator == other.numerator && denominator == other.denominator) {
            return 0;
        }


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
    override public double doubleValue() {
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
    override public bool equals(final Object obj) {
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
    override public float floatValue() {
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
    override public int hashCode() {
        if (hashCode == 0) {

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
    override public int intValue() {
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
    override public long longValue() {
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
     * Gets the fraction as a proper {@link std::string} in the format X Y/Z.
     *
     * <p>The format used in '<i>wholeNumber</i> <i>numerator</i>/<i>denominator</i>'.
     * If the whole number is zero it will be omitted. If the numerator is zero,
     * only the whole number is returned.</p>
     *
     * @return a {@link std::string} form of the fraction
     */
    public std::string toProperString() {
        if (toProperString == null) {
            if (numerator == 0) {
                toProperString = "0";
            } else if (numerator == denominator) {
                toProperString = "1";
            } else if (numerator == -1 * denominator) {
                toProperString = "-1";
            } else if ((numerator > 0 ? -numerator : numerator) < -denominator) {




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
     * Gets the fraction as a {@link std::string}.
     *
     * <p>The format used is '<i>numerator</i>/<i>denominator</i>' always.
     *
     * @return a {@link std::string} form of the fraction
     */
    override public std::string toString() {
        if (toString == null) {
            toString = getNumerator() + "/" + getDenominator();
        }
        return toString;
    }
}
