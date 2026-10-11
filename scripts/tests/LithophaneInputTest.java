package scripts.tests;

import ru.ytkab0bp.slicebeam.utils.LithophaneInput;
import ru.ytkab0bp.slicebeam.utils.LithophaneToneMapper;

public final class LithophaneInputTest {
    public static void main(String[] args) {
        equal(1.25f, LithophaneInput.parseClamped(" 1,25 ", 2f, .4f, 10f), "German decimal comma");
        equal(1.25f, LithophaneInput.parseClamped("1.25", 2f, .4f, 10f), "decimal point");
        equal(.4f, LithophaneInput.parseClamped("0,2", 2f, .4f, 10f), "lower bound");
        equal(10f, LithophaneInput.parseClamped("12,5", 2f, .4f, 10f), "upper bound");
        equal(2f, LithophaneInput.parseClamped("not a number", 2f, .4f, 10f), "invalid fallback");
        equal(2f, LithophaneInput.parseClamped("NaN", 2f, .4f, 10f), "non-finite fallback");
        toneMapping();
        System.out.println("LithophaneInputTest passed: locale inputs, tone mapping, transparency, inversion, thickness and invalid-value fallbacks.");
    }

    private static void toneMapping() {
        equal(0f, LithophaneToneMapper.gray(0f, 0f, 0f, 1f, 0f, 1f, 0, false), "black image pixel");
        equal(1f, LithophaneToneMapper.gray(1f, 1f, 1f, 1f, 0f, 1f, 0, false), "white image pixel");
        equal(1f, LithophaneToneMapper.gray(0f, 0f, 0f, 0f, 0f, 1f, 0, false), "transparent pixel uses white background");
        equal(1f, LithophaneToneMapper.gray(0f, 0f, 0f, 1f, 0f, 1f, 0, true), "inverted black");
        equal(0f, LithophaneToneMapper.gray(0f, 0f, 0f, 1f, 0f, 1f, 1, false), "black-and-white threshold");
        equal(1.2f, LithophaneToneMapper.thickness(1f, 1.2f, 4f, 3), "white becomes minimum relief");
        equal(4f, LithophaneToneMapper.thickness(0f, 1.2f, 4f, 3), "black becomes maximum relief");
        if (!(LithophaneToneMapper.thickness(.75f, 1.2f, 4f, 3) <
                LithophaneToneMapper.thickness(.25f, 1.2f, 4f, 3)))
            throw new AssertionError("darker tones must create thicker relief");
    }

    private static void equal(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > .0001f) throw new AssertionError(label + ": " + actual);
    }
}
