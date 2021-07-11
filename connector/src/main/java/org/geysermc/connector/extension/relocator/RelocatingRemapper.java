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
import org.objectweb.asm.commons.Remapper;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * https://github.com/lucko/jar-relocator/blob/master/src/main/java/me/lucko/jarrelocator/RelocatingRemapper.java
 * @author lucko
 */
@ToString
public class RelocatingRemapper extends Remapper {
    private static final Pattern CLASS_PATTERN = Pattern.compile("(\\[*)?L(.+);");

    private final Collection<Relocation> rules;

    RelocatingRemapper(Collection<Relocation> rules) {
        this.rules = rules;
    }

    @Override
    public Object mapValue(Object object) {
        if (object instanceof String) {
            String relocatedName = relocate((String) object, true);
            if (relocatedName != null) {
                return relocatedName;
            }
        }
        return super.mapValue(object);
    }

    @Override
    public String map(String name) {
        String relocatedName = relocate(name, false);
        if (relocatedName != null) {
            return relocatedName;
        }
        return super.map(name);
    }

    private String relocate(String name, boolean isClass) {
        String prefix = "";
        String suffix = "";

        Matcher m = CLASS_PATTERN.matcher(name);
        if (m.matches()) {
            prefix = m.group(1) + "L";
            suffix = ";";
            name = m.group(2);
        }

        for (Relocation r : this.rules) {
            if (isClass && r.canRelocateClass(name)) {
                return prefix + r.relocateClass(name) + suffix;
            } else if (r.canRelocatePath(name)) {
                return prefix + r.relocatePath(name) + suffix;
            }
        }

        return null;
    }
}
