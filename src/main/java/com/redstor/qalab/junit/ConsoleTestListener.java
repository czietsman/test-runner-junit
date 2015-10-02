package com.redstor.qalab.junit;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;

/**
 * Based on org.junit.internal.TextListener
 */
class ConsoleTestListener extends AbstractTestListener {
    private final PrintStream writer;

    enum Outcome {
        PASSED,
        FAILED,
        IGNORED
    }

    public ConsoleTestListener(PrintStream writer) {
        this.writer = writer;
    }

    private long startTime;
    private Outcome outcome;
    private Failure failure;

    @Override
    public void testStarted(Description description) {
        this.startTime = System.currentTimeMillis();
        this.outcome = Outcome.PASSED;
        this.failure = null;
    }

    @Override
    public void testFailure(Failure failure) {
        this.outcome = Outcome.FAILED;
        this.failure = failure;
    }

    @Override
    public void testIgnored(Description description) {
        this.outcome = Outcome.IGNORED;
    }

    @Override
    public void testFinished(Description description) throws Exception {
        long duration = System.currentTimeMillis() - startTime;
        switch (outcome) {
            case PASSED:
                writer.println("PASSED (" + duration + " ms): " + description);
                break;
            case FAILED:
                writer.println("FAILED (" + duration + " ms): " + failure);
                break;
            case IGNORED:
                writer.println("IGNORED (" + duration + " ms): " + description);
                break;
        }
    }

    @Override
    public void testRunFinished(Result result) {
        printHeader(result.getRunTime());
        printFailures(result);
        printFooter(result);
    }

    /*
      * Internal methods
      */

    private PrintStream getWriter() {
        return writer;
    }

    protected void printHeader(long runTime) {
        getWriter().println();
        getWriter().println("Time: " + elapsedTimeAsString(runTime));
    }

    protected void printFailures(Result result) {
        List<Failure> failures = result.getFailures();
        if (failures.size() == 0) {
            return;
        }
        if (failures.size() == 1) {
            getWriter().println("There was " + failures.size() + " failure:");
        } else {
            getWriter().println("There were " + failures.size() + " failures:");
        }
        int i = 1;
        for (Failure each : failures) {
            printFailure(each, "" + i++);
        }
    }

    protected void printFailure(Failure each, String prefix) {
        getWriter().println(prefix + ") " + each.getTestHeader());
        getWriter().print(each.getTrace());
    }

    protected void printFooter(Result result) {
        if (result.wasSuccessful()) {
            getWriter().println();
            getWriter().print("OK");
            getWriter().println(" (" + result.getRunCount() + " test" + (result.getRunCount() == 1 ? "" : "s") + ")");

        } else {
            getWriter().println();
            getWriter().println("FAILURES!!!");
            getWriter().println("Tests run: " + result.getRunCount() + ",  Failures: " + result.getFailureCount());
        }
        getWriter().println();
    }

    /**
     * Returns the formatted string of the elapsed time. Duplicated from
     * BaseTestRunner. Fix it.
     */
    protected String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format((double) runTime / 1000);
    }
}

