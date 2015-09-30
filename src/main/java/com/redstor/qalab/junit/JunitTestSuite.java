package com.redstor.qalab.junit;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.MarkerFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.redstor.qalab.junit.mongo.MongoRunListenerBuilder;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class JunitTestSuite {
    private final OptionParser parser;
    private final OptionSpec<String> testJarFoldersOption;
    private final OptionSpec<String> jarSuffixOption;
    private final OptionSpec<Void> verboseOption;
    private final OptionSpec<Void> dryRunOption;
    private final OptionSpec<PublishTarget> publishOption;
    private final OptionSpec<String> mongoHostOption;
    private final OptionSpec<Integer> mongoPortOption;
    private final OptionSpec<Void> helpOption;
    private final OptionSpec<String> includeCategoryOption;
    private final OptionSpec<String> excludeCategoryOption;

    public JunitTestSuite() {
        parser = new OptionParser();
        testJarFoldersOption = parser.nonOptions("test jars folders");
        jarSuffixOption = parser.accepts("jar-suffix", "Jars in which to search for tests").withRequiredArg().defaultsTo(".jar");
        verboseOption = parser.accepts("verbose", "Print verbose information about jar and class discovery");
        dryRunOption = parser.accepts("dry-run", "Load all jars and find all classes to test, but do not execute any tests");
        publishOption = parser.accepts("publish", "Publish results to Console or MongoDB").withRequiredArg().ofType(PublishTarget.class).defaultsTo(PublishTarget.Console);
        mongoHostOption = parser.accepts("mongo-host", "MongoDB host").withRequiredArg().defaultsTo("localhost");
        mongoPortOption = parser.accepts("mongo-port", "MongoDB port").withRequiredArg().ofType(Integer.class).defaultsTo(27017);
        helpOption = parser.accepts("help").forHelp();
        includeCategoryOption = parser.accepts("include", "Category to include").withRequiredArg().describedAs("Category class name");
        excludeCategoryOption = parser.accepts("exclude", "Category to exclude").withRequiredArg().describedAs("Category class name");
    }

    private static Class<?>[] getSortedTestClasses(ClassesFinder finder) {
        List<Class<?>> classes = finder.find();
        Collections.sort(classes, getClassComparator());
        return classes.toArray(new Class[classes.size()]);
    }

    private static Comparator<Class<?>> getClassComparator() {
        return (o1, o2) -> o1.getName().compareTo(o2.getName());
    }

    private static JarFinder concatenateDirectoryJarFinders(List<String> folders, JarFileFilter filter) {
        JarFinder finder = JarFinder.empty();
        for (String folder : folders) {
            finder = finder.and(new DirectoryJarFinder(folder, filter));
        }
        return finder;
    }

    private static MarkerFilter createMarkerFilter(boolean verbose) {
        final MarkerFilter filter = new MarkerFilter();
        filter.setMarker(Markers.VERBOSE.getName());
        filter.setOnMatch(verbose ? FilterReply.ACCEPT.name() : FilterReply.DENY.name());
        filter.start();
        return filter;
    }

    private static List<Class<?>> loadCategoryClassList(OptionSet options, OptionSpec<String> categoryOption) throws ClassNotFoundException {
        final List<Class<?>> list = new ArrayList<>();
        if (options.has(categoryOption)) {
            for (String className : options.valuesOf(categoryOption)) {
                final Class<?> klass = Class.forName(className);
                list.add(klass);
            }
        }
        return list;
    }

    public void run(String[] args) throws IOException, ClassNotFoundException {
        final OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.out);
            return;
        }

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.addTurboFilter(createMarkerFilter(options.has(verboseOption)));

        // load all the jars in the test folder
        final List<String> testJarFolders = options.valuesOf(testJarFoldersOption);
        final JarFinder testJarToLoadFinder = concatenateDirectoryJarFinders(testJarFolders, JarFileFilter.any());
        testJarToLoadFinder.find().forEachRemaining(ClassPathHack::addFile);

        // find the classes to test
        final JarFileFilter jarFileFilter = JarFileFilter.endsWith(options.valueOf(jarSuffixOption));
        final JarFinder testJarToTestFinder = concatenateDirectoryJarFinders(testJarFolders, jarFileFilter);
        final Class<?>[] classes = getSortedTestClasses(new JarClassesFinder(testJarToTestFinder));

        // if this is a dry-run then stop here
        if (options.has(dryRunOption)) {
            return;
        }

        final RunListener listener = createRunListener(options);
        final Optional<CategoryFilter> filter = createCategoryFilter(options);

        // start testing
        final JUnitCore junit = new JUnitCore();
        junit.addListener(listener);
        Request request = Request.classes(Computer.serial(), classes);
        if (filter.isPresent()) {
            request = request.filterWith(filter.get());
        }
        final Result result = junit.run(request);
        System.exit(result.getFailureCount());
    }

    private Optional<CategoryFilter> createCategoryFilter(OptionSet options) throws ClassNotFoundException {
        Optional<CategoryFilter> filter;
        final List<Class<?>> includeList = loadCategoryClassList(options, includeCategoryOption);
        final List<Class<?>> excludeList = loadCategoryClassList(options, excludeCategoryOption);
        if (!includeList.isEmpty() || !excludeList.isEmpty()) {
            filter = Optional.of(new CategoryFilter(includeList, excludeList));
        } else {
            filter = Optional.empty();
        }
        return filter;
    }

    private RunListener createRunListener(OptionSet options) {
        RunListener listener;
        switch (publishOption.value(options)) {
            case MongoDB:
                listener = new MongoRunListenerBuilder()
                        .host(mongoHostOption.value(options))
                        .port(mongoPortOption.value(options))
                        .build();
                break;
            case Console:
            default:
                listener = new TestListener(System.out);
        }
        return listener;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        new JunitTestSuite().run(args);
    }

}

