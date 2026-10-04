/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.buildtools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor that registers all HortonMachine modules of the compiled
 * sources as SPI services.
 *
 * <p>Every concrete public subclass of <code>org.hortonmachine.gears.libs.modules.HMModel</code>
 * that has a public no-arg constructor and declares or inherits a method annotated with
 * <code>oms3.annotations.Execute</code> is written to
 * <code>META-INF/services/org.hortonmachine.gears.libs.modules.HMModel</code>, so that it
 * can be discovered at runtime through the classpath.
 *
 * <p>Since incremental builds (ex. in IDEs) compile only the changed sources, the entries
 * of an existing service file are kept, as long as they still are modules.
 *
 * <p>External projects get their modules registered by simply having this jar
 * on the compile classpath (or annotation processor path).
 *
 * <p>Types are referenced by name, so this module has no dependency on the rest of HortonMachine.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
@SupportedAnnotationTypes("*")
public class HMModelServiceProcessor extends AbstractProcessor {

    public static final String HMMODEL_CLASS = "org.hortonmachine.gears.libs.modules.HMModel";
    public static final String EXECUTE_ANNOTATION = "oms3.annotations.Execute";
    public static final String SERVICE_FILE = "META-INF/services/" + HMMODEL_CLASS;

    private final Set<String> modelClasses = new TreeSet<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process( Set< ? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        TypeElement hmModelElement = processingEnv.getElementUtils().getTypeElement(HMMODEL_CLASS);
        if (hmModelElement == null) {
            // HMModel is not visible, nothing to register
            return false;
        }
        TypeMirror hmModelType = processingEnv.getTypeUtils().erasure(hmModelElement.asType());

        if (!roundEnv.processingOver()) {
            for( TypeElement type : ElementFilter.typesIn(roundEnv.getRootElements()) ) {
                collect(type, hmModelType);
            }
        } else {
            // incremental builds (ex. IDEs) compile only the changed sources, so
            // the entries of the existing file are kept, if they still are modules
            Set<String> existingClasses = readExistingServiceFile();
            for( String className : existingClasses ) {
                TypeElement type = processingEnv.getElementUtils().getTypeElement(className.replace('$', '.'));
                if (type != null && isModel(type, hmModelType)) {
                    modelClasses.add(className);
                }
            }
            if (!modelClasses.isEmpty() || !existingClasses.isEmpty()) {
                writeServiceFile();
            }
        }
        // never claim annotations, other processors might need them
        return false;
    }

    private void collect( TypeElement type, TypeMirror hmModelType ) {
        if (isModel(type, hmModelType)) {
            modelClasses.add(processingEnv.getElementUtils().getBinaryName(type).toString());
        }
        for( TypeElement nested : ElementFilter.typesIn(type.getEnclosedElements()) ) {
            collect(nested, hmModelType);
        }
    }

    private boolean isModel( TypeElement type, TypeMirror hmModelType ) {
        if (type.getKind() != ElementKind.CLASS) {
            return false;
        }
        Set<Modifier> modifiers = type.getModifiers();
        if (!modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.ABSTRACT)) {
            return false;
        }
        if (type.getNestingKind() == NestingKind.MEMBER && !modifiers.contains(Modifier.STATIC)) {
            return false;
        }
        if (type.getNestingKind() != NestingKind.TOP_LEVEL && type.getNestingKind() != NestingKind.MEMBER) {
            return false;
        }
        TypeMirror typeMirror = processingEnv.getTypeUtils().erasure(type.asType());
        if (processingEnv.getTypeUtils().isSameType(typeMirror, hmModelType)
                || !processingEnv.getTypeUtils().isAssignable(typeMirror, hmModelType)) {
            return false;
        }
        return hasPublicNoArgConstructor(type) && hasExecuteMethod(type);
    }

    private boolean hasPublicNoArgConstructor( TypeElement type ) {
        // if no constructor is declared, the compiler generates a public default one
        for( ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements()) ) {
            if (constructor.getParameters().isEmpty() && constructor.getModifiers().contains(Modifier.PUBLIC)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExecuteMethod( TypeElement type ) {
        for( Element member : processingEnv.getElementUtils().getAllMembers(type) ) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }
            for( AnnotationMirror annotation : member.getAnnotationMirrors() ) {
                TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
                if (annotationType.getQualifiedName().contentEquals(EXECUTE_ANNOTATION)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> readExistingServiceFile() {
        Set<String> classNames = new TreeSet<>();
        try {
            FileObject file = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", SERVICE_FILE);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while( (line = reader.readLine()) != null ) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        classNames.add(line);
                    }
                }
            }
        } catch (IOException e) {
            // no previous file, which is the case of clean builds
        }
        return classNames;
    }

    private void writeServiceFile() {
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", SERVICE_FILE);
            try (Writer writer = new OutputStreamWriter(file.openOutputStream(), StandardCharsets.UTF_8)) {
                for( String className : modelClasses ) {
                    writer.write(className);
                    writer.write("\n");
                }
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Registered " + modelClasses.size() + " HMModel modules in " + SERVICE_FILE);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Unable to write " + SERVICE_FILE + ": " + e.getMessage());
        }
    }
}
