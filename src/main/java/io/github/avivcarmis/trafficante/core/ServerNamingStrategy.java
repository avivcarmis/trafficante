package io.github.avivcarmis.trafficante.core;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Common {@link PropertyNamingStrategy} instances that may be used for server naming.
 */
public class ServerNamingStrategy {

    /**
     * A naming strategy that leaves all naming as is.
     * When used, all endpoint paths will take the class names, and
     * IO properties will remain as they are in java code.
     */
    public static final PropertyNamingStrategy UNPROCESSED = new PropertyNamingStrategy();

    /**
     * A naming strategy that alter naming from camel case to snake case.
     * When used, all endpoint paths will take the snake case form of their class names,
     * and IO properties will take the snake case form of their java fields.
     */
    public static final PropertyNamingStrategy SNAKE_CASE = new ImprovedSnakeCase("_");

    /**
     * A naming strategy that alter naming from camel case to lower camel case.
     * When used, all endpoint paths will take the class names with a lowered first character,
     * IO properties will remain as they are in java code with a lowered first character.
     */
    public static final PropertyNamingStrategy CAMEL_CASE = new PropertyNamingStrategy.UpperCamelCaseStrategy() {

        @Override
        public String translate(String input) {
            String result = super.translate(input);
            return Character.toLowerCase(result.charAt(0)) + result.substring(1);
        }

    };

    /**
     * An implementation of Jackson property naming strategy of Snake Case.
     * Fixes an abbreviation issue with {@link PropertyNamingStrategy.SnakeCaseStrategy}, in which
     * `myXMLParser` would get translated into `my_xmlparser` instead of `my_xml_parser`.
     */
    public static class ImprovedSnakeCase extends PropertyNamingStrategy.PropertyNamingStrategyBase {

        // Constants

        /**
         * A regex to split camel case expressions
         */
        private static final Pattern SPLIT_PATTERN = Pattern.compile(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
        ));

        // Fields

        /**
         * Contains all prefixes that should be ignored.
         * For example, if "_" is passed in the constructor, then "_myProperty" would
         * be translated to "my_property" instead of "_my_property".
         */
        private final Pattern _ignorablePrefixPattern;

        // Constructors

        public ImprovedSnakeCase(String... ignorablePrefixes) {
            Arrays.sort(ignorablePrefixes, (o1, o2) -> o2.length() - o1.length());
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < ignorablePrefixes.length; i++) {
                if (i > 0) {
                    builder.append("|");
                }
                builder.append("^");
                builder.append(ignorablePrefixes[i]);
            }
            _ignorablePrefixPattern = Pattern.compile(builder.toString());
        }

        @Override
        public String translate(String propertyName) {
            return String
                    .join("_", SPLIT_PATTERN.split(
                            _ignorablePrefixPattern.matcher(propertyName).replaceAll(""))
                    )
                    .toLowerCase()
                    .replaceAll("_+", "_");
        }

    }
}
