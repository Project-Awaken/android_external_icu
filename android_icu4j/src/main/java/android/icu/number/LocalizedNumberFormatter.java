/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package android.icu.number;

import java.math.BigInteger;
import java.text.Format;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import android.icu.impl.StandardPlural;
import android.icu.impl.Utility;
import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.DecimalQuantity_DualStorageBCD;
import android.icu.impl.number.LocalizedNumberFormatterAsFormat;
import android.icu.impl.number.MacroProps;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.math.BigDecimal;
import android.icu.util.CurrencyAmount;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;

/**
 * A NumberFormatter that has a locale associated with it; this means .format() methods are available.
 *
 * @see NumberFormatter
 * @see NumberFormatter
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
public class LocalizedNumberFormatter extends NumberFormatterSettings<LocalizedNumberFormatter> {

    static final AtomicLongFieldUpdater<LocalizedNumberFormatter> callCount = AtomicLongFieldUpdater
            .newUpdater(LocalizedNumberFormatter.class, "callCountInternal");

    volatile long callCountInternal; // do not access directly; use callCount instead
    volatile LocalizedNumberFormatter savedWithUnit;
    volatile NumberFormatterImpl compiled;

    LocalizedNumberFormatter(NumberFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    /**
     * Format the given byte, short, int, or long to a string using the settings specified in the
     * NumberFormatter fluent setting chain.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @see NumberFormatter
     * @hide draft / provisional / internal are hidden on Android
     */
    public FormattedNumber format(long input) {
        return format(new DecimalQuantity_DualStorageBCD(input));
    }

    /**
     * Format the given float or double to a string using the settings specified in the NumberFormatter
     * fluent setting chain.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @see NumberFormatter
     * @hide draft / provisional / internal are hidden on Android
     */
    public FormattedNumber format(double input) {
        return format(new DecimalQuantity_DualStorageBCD(input));
    }

    /**
     * Format the given {@link BigInteger}, {@link BigDecimal}, or other {@link Number} to a string using
     * the settings specified in the NumberFormatter fluent setting chain.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @see NumberFormatter
     * @hide draft / provisional / internal are hidden on Android
     */
    public FormattedNumber format(Number input) {
        return format(new DecimalQuantity_DualStorageBCD(input));
    }

    /**
     * Format the given {@link Measure} or {@link CurrencyAmount} to a string using the settings
     * specified in the NumberFormatter fluent setting chain.
     *
     * <p>
     * The unit specified here overrides any unit that may have been specified in the setter chain. This
     * method is intended for cases when each input to the number formatter has a different unit.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @see NumberFormatter
     * @hide draft / provisional / internal are hidden on Android
     */
    public FormattedNumber format(Measure input) {
        MeasureUnit unit = input.getUnit();
        Number number = input.getNumber();
        // Use this formatter if possible
        if (Utility.equals(resolve().unit, unit)) {
            return format(number);
        }
        // This mechanism saves the previously used unit, so if the user calls this method with the
        // same unit multiple times in a row, they get a more efficient code path.
        LocalizedNumberFormatter withUnit = savedWithUnit;
        if (withUnit == null || !Utility.equals(withUnit.resolve().unit, unit)) {
            withUnit = new LocalizedNumberFormatter(this, KEY_UNIT, unit);
            savedWithUnit = withUnit;
        }
        return withUnit.format(number);
    }

    /**
     * Creates a representation of this LocalizedNumberFormat as a {@link java.text.Format}, enabling the
     * use of this number formatter with APIs that need an object of that type, such as MessageFormat.
     * <p>
     * This API is not intended to be used other than for enabling API compatibility. The {@link #format}
     * methods should normally be used when formatting numbers, not the Format object returned by this
     * method.
     *
     * @return A Format wrapping this LocalizedNumberFormatter.
     * @see NumberFormatter
     * @hide draft / provisional / internal are hidden on Android
     */
    public Format toFormat() {
        return new LocalizedNumberFormatterAsFormat(this, resolve().loc);
    }

    /**
     * This is the core entrypoint to the number formatting pipeline. It performs self-regulation: a
     * static code path for the first few calls, and compiling a more efficient data structure if called
     * repeatedly.
     *
     * <p>
     * This function is very hot, being called in every call to the number formatting pipeline.
     *
     * @param fq
     *            The quantity to be formatted.
     * @return The formatted number result.
     *
     * @deprecated ICU 60 This API is ICU internal only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public FormattedNumber format(DecimalQuantity fq) {
        NumberStringBuilder string = new NumberStringBuilder();
        if (computeCompiled()) {
            compiled.apply(fq, string);
        } else {
            NumberFormatterImpl.applyStatic(resolve(), fq, string);
        }
        return new FormattedNumber(string, fq);
    }

    /**
     * @deprecated This API is ICU internal only. Use {@link FormattedNumber#populateFieldPosition} or
     *             {@link FormattedNumber#getFieldIterator} for similar functionality.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public String getAffixImpl(boolean isPrefix, boolean isNegative) {
        NumberStringBuilder string = new NumberStringBuilder();
        byte signum = (byte) (isNegative ? -1 : 1);
        // Always return affixes for plural form OTHER.
        StandardPlural plural = StandardPlural.OTHER;
        int prefixLength;
        if (computeCompiled()) {
            prefixLength = compiled.getPrefixSuffix(signum, plural, string);
        } else {
            prefixLength = NumberFormatterImpl.getPrefixSuffixStatic(resolve(), signum, plural, string);
        }
        if (isPrefix) {
            return string.subSequence(0, prefixLength).toString();
        } else {
            return string.subSequence(prefixLength, string.length()).toString();
        }
    }

    private boolean computeCompiled() {
        MacroProps macros = resolve();
        // NOTE: In Java, the atomic increment logic is slightly different than ICU4C.
        // It seems to be more efficient to make just one function call instead of two.
        // Further benchmarking is required.
        long currentCount = callCount.incrementAndGet(this);
        if (currentCount == macros.threshold.longValue()) {
            compiled = NumberFormatterImpl.fromMacros(macros);
            return true;
        } else if (compiled != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    LocalizedNumberFormatter create(int key, Object value) {
        return new LocalizedNumberFormatter(this, key, value);
    }
}