/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.extension.relocator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaRelocator {
    @Getter
    private final RelocatingRemapper remapper;

    public JavaRelocator() {
        this.remapper = new RelocatingRemapper(new ArrayList<>());
    }

    public JavaRelocator(Collection<Relocation> relocations) {
        this.remapper = new RelocatingRemapper(relocations);
    }

    /**
     * Load Relocations from a json stream
     * @param stream json stream
     */
    public JavaRelocator(InputStream stream) throws IOException {

        List<Relocation> rules = new ArrayList<>();

        if (stream != null) {
            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode root = jsonMapper.readTree(stream);
            for (JsonNode node : root) {
                rules.add(new Relocation(node.get("from").asText(), node.get("to").asText()));
            }
        }

        this.remapper = new RelocatingRemapper(rules);
    }

    public byte[] load(String name, InputStream stream) throws IOException {
        ClassReader classReader = new ClassReader(stream);
        ClassWriter classWriter = new ClassWriter(0);

        RelocatorClassVisitor classVisitor = new RelocatorClassVisitor(classWriter, this.remapper, name);
        try {
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        } catch (Throwable e) {
            throw new RuntimeException("Error processing class " + name, e);
        }

        return classWriter.toByteArray();
    }
}
