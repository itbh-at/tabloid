package at.itbh.tabloid.util;

/**
 * Provides a simple heuristic to estimate the required column width for a given
 * text content.
 */
public final class ColumnWidthHeuristic {

    /**
     * The padding to add for long text to ensure full visibility.
     */
    private static final int PADDING_FOR_LONG_TEXT = 30;

    /**
     * A smaller, specific padding for short text to give it a bit of breathing
     * room.
     */
    private static final int PADDING_FOR_SHORT_TEXT = 10;

    /**
     * The character length at which we consider text "long" and start adding the
     * larger padding.
     */
    private static final int PADDING_THRESHOLD = 30;

    private ColumnWidthHeuristic() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Calculates the estimated column width using a dual-padding system.
     * This version uses stable integer arithmetic to avoid rounding errors.
     *
     * @param length The character length of the longest text in the column.
     * @return An estimated width sufficient to display the text.
     */
    public static int calculateWidth(final int length) {
        if (length <= 0) {
            return 10; // A small, fixed default for empty columns
        }

        // Use stable integer math to calculate (length * 1.5) without losing precision
        int baseWidth = (length * 15) / 10;

        // Apply the correct padding based on the threshold
        if (length > PADDING_THRESHOLD) {
            return baseWidth + PADDING_FOR_LONG_TEXT;
        } else {
            return baseWidth + PADDING_FOR_SHORT_TEXT;
        }
    }
}