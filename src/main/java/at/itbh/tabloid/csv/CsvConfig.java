package at.itbh.tabloid.csv;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration mapping for CSV output settings.
 * Defines all configurable properties for generating CSV files,
 * allowing for type-safe access to values in application.properties.
 *
 * @see <a href="https://quarkus.io/guides/config-mappings">Quarkus Config
 *      Mappings</a>
 */
@ConfigMapping(prefix = "tabloid.output.csv")
public interface CsvConfig {

    /**
     * The character used to separate fields in the CSV file.
     *
     * @return The delimiter character.
     */
    @WithDefault(",")
    String delimiter();

    /**
     * The character set (encoding) to be used for the output file.
     *
     * @return The charset name.
     */
    @WithDefault("UTF-8")
    String charset();

    /**
     * Whether to include a header row with column names at the beginning of the
     * file.
     *
     * @return True to include the header, false otherwise.
     */
    @WithDefault("true")
    boolean includeHeader();

    /**
     * Whether to always enclose empty string values in quotes.
     *
     * @return True to quote empty strings, false otherwise.
     */
    @WithDefault("true")
    boolean alwaysQuoteEmptyStrings();

    /**
     * Whether to always enclose all non-empty string values in quotes.
     *
     * @return True to always quote strings, false otherwise.
     */
    @WithDefault("false")
    boolean alwaysQuoteStrings();

    /**
     * An optional character used to escape control characters.
     *
     * @return An Optional containing the escape character, if configured.
     */
    Optional<String> escapeControlCharsWithEscapeChar();

    /**
     * An optional character used to escape the quote character itself.
     *
     * @return An Optional containing the escape character, if configured.
     */
    Optional<String> escapeQuoteCharWithEscapeChar();

    /**
     * Whether to omit trailing empty columns from a row.
     *
     * @return True to omit missing tail columns, false otherwise.
     */
    @WithDefault("true")
    boolean omitMissingTailColumns();

    /**
     * Whether to trim leading and trailing whitespace from non-null values before
     * writing.
     *
     * @return True to trim values, false otherwise.
     */
    @WithDefault("true")
    boolean trimValues();
}