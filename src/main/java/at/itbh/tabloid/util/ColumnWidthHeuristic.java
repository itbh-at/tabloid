package at.itbh.tabloid.util;

/**
 * Provides a simple heuristic to estimate the required column width for a given
 * text content.
 * <p>
 * This utility class calculates an approximate width based on the number of
 * characters
 * and an average character width factor, which is a common approach for quick
 * estimations
 * where precise font metric calculations are not feasible or necessary.
 */
public final class ColumnWidthHeuristic {

    /**
     * A fixed padding added to the calculated width to ensure content is not
     * clipped.
     * This provides a minimum margin.
     */
    private static final int PADDING = 12;

    private ColumnWidthHeuristic() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Calculates the estimated column width using stable integer arithmetic.
     * The unit is intentionally abstract and has been tuned for ODS output.
     *
     * @param text The text content of the cell.
     * @return An estimated width sufficient to display the text.
     */
    public static int calculateWidth(final String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Use stable integer arithmetic to avoid floating-point inconsistencies.
        // This approximates a 10% increase in width.
        int length = text.length();
        return length + (length / 10) + PADDING;
    }
}