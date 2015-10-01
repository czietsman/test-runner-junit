package com.redstor.qalab.junit;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.MarkerFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.redstor.qalab.junit.jacoco.JacocoCoverageAgent;
import com.redstor.qalab.junit.mongo.MongoRedirectStrategy;
import com.redstor.qalab.junit.mongo.MongoRunListenerBuilder;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class JunitTestSuite {
    private static final Logger LOGGER = LoggerFactory.getLogger(JunitTestSuite.class);
    private final OptionParser parser;
    private final OptionSpec<String> jarFoldersOption;
    private final OptionSpec<String> testJarIncludePatternOption;
    private final OptionSpec<String> testJarExcludePatternOption;
    private final OptionSpec<Void> verboseOption;
    private final OptionSpec<Void> dryRunOption;
    private final OptionSpec<PublishTarget> publishOption;
    private final OptionSpec<String> mongoHostOption;
    private final OptionSpec<Integer> mongoPortOption;
    private final OptionSpec<String> mongoRunIdOption;
    private final OptionSpec<MongoRedirectStrategy> mongoRedirectStrategy;
    private final OptionSpec<Void> helpOption;
    private final OptionSpec<CoverageTool> coverageOption;
    private final OptionSpec<String> coverageJarIncludePatternOption;
    private final OptionSpec<String> coverageJarExcludePatternOption;
    private final OptionSpec<Void> coverageReportOption;
    private final OptionSpec<String> includeCategoryOption;
    private final OptionSpec<String> excludeCategoryOption;

    public JunitTestSuite() {
        parser = new OptionParser();
        jarFoldersOption = parser.nonOptions("folders scanned for jars");
        testJarIncludePatternOption = parser.acceptsAll(Arrays.asList("ti", "test-include-jars"), "Jar filename pattern to include for tests").withRequiredArg();
        testJarExcludePatternOption = parser.acceptsAll(Arrays.asList("te", "test-exclude-jars"), "Jar filename pattern to exclude from tests").withRequiredArg();
        verboseOption = parser.accepts("verbose", "Print verbose information about jar and class discovery");
        dryRunOption = parser.accepts("dry-run", "Load all jars and find all classes to test, but do not execute any tests");
        publishOption = parser.accepts("publish", "Publish results [Console, MongoDB]").withRequiredArg().ofType(PublishTarget.class).defaultsTo(PublishTarget.Console);
        mongoHostOption = parser.accepts("mongo-host", "MongoDB host").withRequiredArg().defaultsTo("localhost");
        mongoPortOption = parser.accepts("mongo-port", "MongoDB port").withRequiredArg().ofType(Integer.class).defaultsTo(27017);
        mongoRunIdOption = parser.accepts("mongo-run-id", "MongoDB test run id").withRequiredArg();
        mongoRedirectStrategy = parser.accepts("mongo-redirect-strategy", "MongoDB strategy to use for redirecting test output [Node, Split, Combine]").withRequiredArg().ofType(MongoRedirectStrategy.class).defaultsTo(MongoRedirectStrategy.Split);
        coverageOption = parser.accepts("coverage", "Tool to use to track test coverage [None, JaCoCo]").withRequiredArg().ofType(CoverageTool.class).defaultsTo(CoverageTool.None);
        coverageJarIncludePatternOption = parser.acceptsAll(Arrays.asList("ci", "coverage-include-jars"), "Jar filename pattern to include for coverage reports").withRequiredArg();
        coverageJarExcludePatternOption = parser.acceptsAll(Arrays.asList("ce", "coverage-exclude-jars"), "Jar filename pattern to exclude from coverage reports").withRequiredArg();
        coverageReportOption = parser.accepts("coverage-report", "Save a coverage report");

        helpOption = parser.accepts("help").forHelp();
        includeCategoryOption = parser.accepts("include", "Category to include").withRequiredArg().describedAs("Category class name");
        excludeCategoryOption = parser.accepts("exclude", "Category to exclude").withRequiredArg().describedAs("Category class name");
    }

    private static Class<?>[] getSortedTestClasses(List<Class<?>> classes) {
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
        final List<String> jarFolders = options.valuesOf(jarFoldersOption);
        final JarFinder testJarToLoadFinder = concatenateDirectoryJarFinders(jarFolders, JarFileFilter.any());
        testJarToLoadFinder.find().forEachRemaining(ClassPathHack::addFile);

        // find the classes to test
        final JarFileFilter testJarFileFilter = createTestJarFileFilter(options, testJarIncludePatternOption, testJarExcludePatternOption);
        final JarFinder testJarToTestFinder = concatenateDirectoryJarFinders(jarFolders, testJarFileFilter);

        LOGGER.info("Search for test classes");
        final Class<?>[] classes = getSortedTestClasses(new JarClassesFinder(testJarToTestFinder).find(new TestClassCollector()).toList());

        // if this is a dry-run then stop here
        if (options.has(dryRunOption)) {
            LOGGER.info("Dry run selected, exiting...");
            return;
        }

        final CoverageAgent agent = createCoverageAgent(options);
        final RunListener listener = createRunListener(options, agent);
        final Optional<CategoryFilter> filter = createCategoryFilter(options);

        // start testing
        final JUnitCore junit = new JUnitCore();
        junit.addListener(listener);
        Request request = Request.classes(Computer.serial(), classes);
        if (filter.isPresent()) {
            LOGGER.info("Category filtering enabled");
            request = request.filterWith(filter.get());
        }

        agent.reset();

        LOGGER.info("Testing started");
        final Result result = junit.run(request);
        LOGGER.info("Testing finished");

        // save the coverage report
        if (options.has(coverageReportOption)) {
            LOGGER.info("Dumping coverage report");
            final JarFileFilter reportJarFileFilter = createTestJarFileFilter(options, coverageJarIncludePatternOption, coverageJarExcludePatternOption);
            final JarFinder reportJarFinder = concatenateDirectoryJarFinders(jarFolders, reportJarFileFilter);
            agent.publish(new JarClassesFinder(reportJarFinder));
        }

        System.exit(result.getFailureCount());
    }

    private Optional<JarFileFilter> createJarFileFilterFromPatternOption(OptionSet options, OptionSpec<String> patternOption) {
        final List<String> patternList = patternOption.values(options);
        if (patternList.isEmpty()) {
            return Optional.empty();
        } else {
            JarFileFilter filter = JarFileFilter.none();
            for (String pattern : patternList) {
                filter = filter.or(JarFileFilter.wildcard(pattern));
            }
            return Optional.of(filter);
        }
    }

    private JarFileFilter createTestJarFileFilter(OptionSet options, OptionSpec<String> includePatternOption, OptionSpec<String> excludePatternOption) {
        final JarFileFilter includeFilter = createJarFileFilterFromPatternOption(options, includePatternOption).orElse(JarFileFilter.any());
        final JarFileFilter excludeFilter = createJarFileFilterFromPatternOption(options, excludePatternOption).orElse(JarFileFilter.none());
        return JarFileFilter.wildcard("*.jar").and(
                includeFilter.and(JarFileFilter.not(excludeFilter))
        );
    }

    private CoverageAgent createCoverageAgent(OptionSet options) {
        switch (coverageOption.value(options)) {
            case JaCoCo:
                LOGGER.info("JaCoCo coverage enabled");
                return new JacocoCoverageAgent();
            case None:
            default:
                LOGGER.info(Markers.VERBOSE, "Code coverage disabled");
                return new NoCoverageAgent();
        }
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

    private RunListener createRunListener(OptionSet options, CoverageAgent agent) {
        RunListener listener;
        switch (publishOption.value(options)) {
            case MongoDB:
                listener = new MongoRunListenerBuilder()
                        .host(mongoHostOption.value(options))
                        .port(mongoPortOption.value(options))
                        .agent(agent)
                        .runId(options.has(mongoRunIdOption) ? mongoRunIdOption.value(options) : null)
                        .redirectStrategy(mongoRedirectStrategy.value(options))
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

