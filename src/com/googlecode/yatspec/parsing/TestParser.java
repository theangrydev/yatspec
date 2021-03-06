package com.googlecode.yatspec.parsing;

import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Predicate;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.yatspec.state.TestMethod;
import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.AbstractBaseJavaEntity;
import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlecode.totallylazy.Files.path;
import static com.googlecode.totallylazy.Files.recursiveFiles;
import static com.googlecode.totallylazy.Files.workingDirectory;
import static com.googlecode.totallylazy.Methods.annotation;
import static com.googlecode.totallylazy.Option.none;
import static com.googlecode.totallylazy.Option.option;
import static com.googlecode.totallylazy.Predicates.*;
import static com.googlecode.totallylazy.Sequences.empty;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.Strings.endsWith;
import static com.googlecode.totallylazy.URLs.toURL;
import static com.googlecode.yatspec.parsing.Files.toJavaPath;
import static com.googlecode.yatspec.parsing.Files.toJavaResourcePath;
import static java.util.Arrays.asList;

public class TestParser {

    private static final Option<URL> NO_URL = none(URL.class);

    public static List<TestMethod> parseTestMethods(Class aClass) throws Exception {
        final Sequence<Method> methods = getMethods(aClass);
        return collectTestMethods(aClass, methods).toList();
    }

    private static Sequence<TestMethod> collectTestMethods(Class aClass, Sequence<Method> methods) throws IOException {
        final Option<JavaClass> javaClass = getJavaClass(aClass);
        if (javaClass.isEmpty()) {
            return empty();
        }

        Map<String, List<JavaMethod>> sourceMethodsByName = getMethods(javaClass.get()).toMap(AbstractBaseJavaEntity::getName);
        Map<String, List<Method>> reflectionMethodsByName = methods.toMap((Callable1<? super Method,String>) (Callable1<Method, String>) Method::getName);

        List<TestMethod> testMethods = new ArrayList<>();
        TestMethodExtractor extractor = new TestMethodExtractor();
        for (String name : sourceMethodsByName.keySet()) {
            List<JavaMethod> javaMethods = sourceMethodsByName.get(name);
            List<Method> reflectionMethods = reflectionMethodsByName.get(name);
            testMethods.add(extractor.toTestMethod(aClass, javaMethods.get(0), reflectionMethods.get(0)));
            // TODO: If people overload test methods we will have to use the full name rather than the short name
        }

        Sequence<TestMethod> myTestMethods = sequence(testMethods);
        Sequence<TestMethod> parentTestMethods = collectTestMethods(aClass.getSuperclass(), methods);

        return myTestMethods.join(parentTestMethods);
    }

    private static Option<JavaClass> getJavaClass(final Class aClass) throws IOException {
        Option<URL> option = getJavaSourceFromClassPath(aClass);
        option = !option.isEmpty() ? option : getJavaSourceFromFileSystem(aClass);
        return option.map(asAJavaClass(aClass));
    }

    private static Callable1<URL, JavaClass> asAJavaClass(final Class aClass) {
        return url -> {
            JavaDocBuilder builder = new JavaDocBuilder();
            builder.addSource(url);
            return builder.getClassByName(aClass.getName());
        };
    }

    private static Sequence<Method> getMethods(Class aClass) {
        return sequence(aClass.getDeclaredMethods()).join(asList(aClass.getMethods()))
                .filter(or(
                        where(annotation(Test.class), notNullValue()),
                        where(annotation(org.junit.jupiter.api.Test.class), notNullValue()),
                        where(annotation(ParameterizedTest.class), notNullValue())));
    }

    @SuppressWarnings("unchecked")
    private static Sequence<JavaMethod> getMethods(JavaClass javaClass) {
        return sequence(javaClass.getMethods())
                .filter(where(annotations(), or(
                        contains(Test.class),
                        contains(org.junit.jupiter.api.Test.class),
                        contains(ParameterizedTest.class))));
    }

    private static Option<URL> getJavaSourceFromClassPath(Class aClass) {
        return isObject(aClass) ? NO_URL : option(aClass.getClassLoader().getResource(toJavaResourcePath(aClass)));
    }

    private static Option<URL> getJavaSourceFromFileSystem(Class aClass) {
        return isObject(aClass) ? NO_URL : recursiveFiles(workingDirectory()).find(where(path(), endsWith(toJavaPath(aClass)))).map(toURL());
    }

    private static boolean isObject(Class aClass) {
        return aClass.equals(Object.class);
    }

    private static Predicate<? super Sequence<Annotation>> contains(final Class aClass) {
        return annotations -> annotations.exists(where(name(), is(aClass.getName())));
    }

    public static Callable1<? super Annotation, String> name() {
        return annotation -> annotation.getType().getFullyQualifiedName();
    }

    public static Callable1<? super JavaMethod, Sequence<Annotation>> annotations() {
        return javaMethod -> sequence(javaMethod.getAnnotations());
    }
}
