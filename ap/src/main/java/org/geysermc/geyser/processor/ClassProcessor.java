/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.processor;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassProcessor extends AbstractProcessor {
    private final String annotationClassName;

    private Path outputPath;

    private final Set<String> locations = new HashSet<>();

    public ClassProcessor(String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Initializing processor " + this.annotationClassName);

        String outputFile = processingEnv.getOptions().get("metadataOutputFile");
        if (outputFile != null && !outputFile.isEmpty()) {
            this.outputPath = Paths.get(outputFile);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (!roundEnv.errorRaised()) {
                complete();
            }

            return false;
        }

        if (!contains(annotations, this.annotationClassName)) {
            return false;
        }

        for (Element element : roundEnv.getRootElements()) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }

            if (!contains(element.getAnnotationMirrors(), this.annotationClassName)) {
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            this.locations.add(typeElement.getQualifiedName().toString());
        }
        return false;
    }

    public boolean contains(Collection<? extends TypeElement> elements, String className) {
        if (elements.isEmpty()) {
            return false;
        }

        for (TypeElement element : elements) {
            if (element.getQualifiedName().contentEquals(className)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(List<? extends AnnotationMirror> elements, String className) {
        if (elements.isEmpty()) {
            return false;
        }

        for (AnnotationMirror element : elements) {
            if (element.getAnnotationType().toString().equals(className)) {
                return true;
            }
        }

        return false;
    }

    public void complete() {
        // Read existing annotation list and verify each class still has this annotation
        try (BufferedReader reader = this.createReader()) {
            if (reader != null) {
                reader.lines().forEach(canonicalName -> {
                    if (!locations.contains(canonicalName)) {
                        TypeElement element = this.processingEnv.getElementUtils().getTypeElement(canonicalName);
                        if (element != null && element.getKind() == ElementKind.CLASS && contains(element.getAnnotationMirrors(), this.annotationClassName)) {
                            locations.add(canonicalName);
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!locations.isEmpty()) {
            try (BufferedWriter writer = this.createWriter()) {
                for (String location : this.locations) {
                    writer.write(location);
                    writer.newLine();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Did not find any classes annotated with " + this.annotationClassName);
        }
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Completed processing for " + this.annotationClassName);
    }

    private @Nullable BufferedReader createReader() throws IOException {
        if (this.outputPath != null) {
            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Reading existing " + this.annotationClassName + " list from " + this.outputPath);
            return Files.newBufferedReader(this.outputPath);
        }

        FileObject obj = this.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", this.annotationClassName);
        if (obj != null) {
            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Reading existing " + this.annotationClassName + " list from " + obj.toUri());
            try {
                return new BufferedReader(obj.openReader(false));
            } catch (NoSuchFileException ignored) {
                return null;
            }
        }
        return null;
    }

    private BufferedWriter createWriter() throws IOException {
        if (this.outputPath != null) {
            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Writing " + this.annotationClassName + " to " + this.outputPath);
            return Files.newBufferedWriter(this.outputPath);
        }

        FileObject obj = this.processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", this.annotationClassName);
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Writing " + this.annotationClassName + " to " + obj.toUri());
        return new BufferedWriter(obj.openWriter());
    }
}
