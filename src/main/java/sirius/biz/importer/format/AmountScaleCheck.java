/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.importer.format;

import sirius.kernel.commons.Value;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Enforces a given numeric scale and precision.
 */
public class AmountScaleCheck implements ValueCheck {

    private static final String PARAM_VALUE = "value";
    private static final String PARAM_PRECISION = "precision";
    private static final String PARAM_SCALE = "scale";

    private int precision;
    private int scale;

    /**
     * Creates a new check using the given precision and scale.
     *
     * @param precision the maximum precision of the numeric field
     * @param scale     the maximum scale of the numeric field
     */
    public AmountScaleCheck(int precision, int scale) {
        this.precision = precision;
        this.scale = scale;
    }

    @Override
    public void perform(Value value) {
        if (value.isEmptyString()) {
            return;
        }

        BigDecimal number = value.getBigDecimal();

        if (number == null) {
            throw new IllegalArgumentException(NLS.fmtr("AmountScaleCheck.errorMsg.notNumeric")
                                                  .set(PARAM_VALUE, value.toString())
                                                  .format());
        }

        BigDecimal rounded = new BigDecimal(number.unscaledValue(), number.scale()).setScale(scale, RoundingMode.FLOOR);
        if (number.subtract(rounded).compareTo(BigDecimal.valueOf(0.00001)) > 0) {
            throw new IllegalArgumentException(NLS.fmtr("AmountScaleCheck.errorMsg.scaleExceeded")
                                                  .set(PARAM_VALUE, value.toString())
                                                  .set(PARAM_SCALE, scale)
                                                  .format());
        }

        if (number.compareTo(BigDecimal.valueOf(Math.pow(10, (precision - scale)))) > -1) {
            throw new IllegalArgumentException(NLS.fmtr("AmountScaleCheck.errorMsg.precisionExceeded")
                                                  .set(PARAM_VALUE, value.toString())
                                                  .set(PARAM_PRECISION, precision)
                                                  .format());
        }
    }

    @Nullable
    @Override
    public String generateRemark() {
        return NLS.fmtr("AmountScaleCheck.remark").set(PARAM_PRECISION, precision).set(PARAM_SCALE, scale).format();
    }
}
