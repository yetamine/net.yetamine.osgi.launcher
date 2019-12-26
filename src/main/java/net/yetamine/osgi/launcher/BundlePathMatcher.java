/*
 * Copyright 2019 Yetamine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yetamine.osgi.launcher;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Tests if a bundle path (in the platform independent form with <i>/</i> as the
 * path component separator) matches a restricted glob expression, which may use
 * merely <i>?</i>, <i>*</i> and </i>**</i> wildcards.
 *
 * <p>
 * The implementation provides specific features that allow sorting the matchers
 * by their specificity, i.e., how many literal characters they contain.
 */
final class BundlePathMatcher implements Comparable<BundlePathMatcher>, Predicate<String> {

    /**
     * Contains all significant meta characters of regular expressions without
     * the glob expression meta characters. The characters are ordered by the
     * probability of occurrence in a bundle path (which is low anyway).
     */
    private static final String REGEX_META_CHARACTERS = ".+$^[](){\\|";

    private final Pattern pattern;
    private final int literals;
    private final String glob;

    /**
     * Creates a new instance.
     *
     * @param givenGlob
     *            the restricted glob expression to use. It must not be
     *            {@code null}.
     */
    public BundlePathMatcher(String givenGlob) {
        glob = Objects.requireNonNull(givenGlob);

        int literalsEncountered = 0;
        final StringBuilder patternBuilder = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            final char c = glob.charAt(i);

            switch (c) {
                case '?':
                    patternBuilder.append("[^/]");
                    break;

                case '*': {
                    final int next = i + 1;
                    if ((next < glob.length()) && (glob.charAt(next) == '*')) {
                        // Traverse the path component boundaries
                        patternBuilder.append(".*");
                        i = next; // Skip the star
                    } else {
                        patternBuilder.append("[^/]*");
                    }

                    break;
                }

                default:
                    if (REGEX_META_CHARACTERS.indexOf(c) != -1) {
                        patternBuilder.append('\\').append(c);
                        break;
                    }

                    patternBuilder.append(c);
                    ++literalsEncountered;
            }
        }

        pattern = Pattern.compile(patternBuilder.append('$').toString());
        literals = literalsEncountered;
    }

    /**
     * Returns the original glob expression.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return glob;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BundlePathMatcher) & glob.equals(((BundlePathMatcher) obj).glob);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return glob.hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(BundlePathMatcher o) {
        final int result = o.literals - literals; // Both are positive, hence no overflow can occur
        if (result != 0) {
            return result;
        }

        return glob.compareTo(o.glob); // Make it deterministic and consistent with equals
    }

    /**
     * @see java.util.function.Predicate#test(java.lang.Object)
     */
    @Override
    public boolean test(String t) {
        return pattern.matcher(t).find();
    }

    /**
     * @return the specificity of this matcher
     */
    public int ranking() {
        return literals;
    }
}
