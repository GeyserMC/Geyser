/*
 * Copyright Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geysermc.connector.extension.relocator;

import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * https://github.com/lucko/jar-relocator/blob/master/src/main/java/me/lucko/jarrelocator/Relocation.java
 * @author lucko
 */
@ToString
public class Relocation {
    private final String pattern;
    private final String relocatedPattern;
    private final String pathPattern;
    private final String relocatedPathPattern;

    private final Set<String> includes;
    private final Set<String> excludes;

    /**
     * Creates a new relocation
     *
     * @param pattern the pattern to match
     * @param relocatedPattern the pattern to relocate to
     * @param includes a collection of patterns which this rule should specifically include
     * @param excludes a collection of patterns which this rule should specifically exclude
     */
    public Relocation(String pattern, String relocatedPattern, Collection<String> includes, Collection<String> excludes) {
        this.pattern = pattern.replace('/', '.');
        this.pathPattern = pattern.replace('.', '/');
        this.relocatedPattern = relocatedPattern.replace('/', '.');
        this.relocatedPathPattern = relocatedPattern.replace('.', '/');

        if (includes != null && !includes.isEmpty()) {
            this.includes = normalizePatterns(includes);
            this.includes.addAll(includes);
        } else {
            this.includes = null;
        }

        if (excludes != null && !excludes.isEmpty()) {
            this.excludes = normalizePatterns(excludes);
            this.excludes.addAll(excludes);
        } else {
            this.excludes = null;
        }
    }

    /**
     * Creates a new relocation with no specific includes or excludes
     *
     * @param pattern the pattern to match
     * @param relocatedPattern the pattern to relocate to
     */
    public Relocation(String pattern, String relocatedPattern) {
        this(pattern, relocatedPattern, Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    private boolean isIncluded(String path) {
        if (this.includes == null) {
            return true;
        }

        for (String include : this.includes) {
            if (SelectorUtils.matchPath(include, path, true)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(String path) {
        if (this.excludes == null) {
            return false;
        }

        for (String exclude : this.excludes) {
            if (SelectorUtils.matchPath(exclude, path, true)) {
                return true;
            }
        }
        return false;
    }

    boolean canRelocatePath(String path) {
        if (path.endsWith(".class")) {
            path = path.substring(0, path.length() - 6);
        }

        if (!isIncluded(path) || isExcluded(path)) {
            return false;
        }

        return path.startsWith(this.pathPattern) || path.startsWith("/" + this.pathPattern);
    }

    boolean canRelocateClass(String clazz) {
        return clazz.indexOf('/') == -1 && canRelocatePath(clazz.replace('.', '/'));
    }

    String relocatePath(String path) {
        return path.replaceFirst(this.pathPattern, this.relocatedPathPattern);
    }

    String relocateClass(String clazz) {
        return clazz.replaceFirst(this.pattern, this.relocatedPattern);
    }

    private static Set<String> normalizePatterns(Collection<String> patterns) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String pattern : patterns) {
            String classPattern = pattern.replace('.', '/');
            normalized.add(classPattern);
            if (classPattern.endsWith("/*")) {
                String packagePattern = classPattern.substring(0, classPattern.lastIndexOf('/'));
                normalized.add(packagePattern);
            }
        }
        return normalized;
    }
}
