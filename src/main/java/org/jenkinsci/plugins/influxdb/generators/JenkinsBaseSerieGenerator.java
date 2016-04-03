package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.PrintStream;

/**
 * Created by jrajala on 15.5.2015.
 */
public class JenkinsBaseSerieGenerator extends AbstractSerieGenerator {

    public static final String BUILD_DURATION = "build_duration";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";

    public JenkinsBaseSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        super(build, logger);
    }

    public boolean hasReport() {
        return true;
    }

    public Point[] generate() {
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        logger.println("Influxdb starting to generate basic report");

        addJenkinsBuildNumber(columns, values);
        addJenkinsProjectName(columns, values);
        addBuildDuration(columns, values);
        addBuildStatusSummaryMesssage(columns, values);
        addProjectBuildHealth(columns, values);

        if(hasTestResults()) {
            addTestsFailed(columns, values);
            addTestsSkipped(columns, values);
            addTestsTotal(columns, values);
        }

        HashMap<String, Object> fields = zipListsToMap(columns, values);

        logger.println("Influxdb basic report generation finished");

        return new Point[]{Point.measurement(getSerieName())
                .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .fields(fields)
                .build()};
    }


    private void addBuildDuration(List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_DURATION);
        values.add(build.getDuration());
    }

    private void addBuildStatusSummaryMesssage(List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_STATUS_MESSAGE);
        values.add(build.getBuildStatusSummary().message);
    }

    private void addProjectBuildHealth(List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_BUILD_HEALTH);
        values.add(build.getProject().getBuildHealth().getScore());
    }

    private boolean hasTestResults() {
        return build.getAction(AbstractTestResultAction.class) != null;
    }

    private void addTestsTotal(List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getTotalCount());
        columnNames.add(TESTS_TOTAL);
    }

    private void addTestsFailed(List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getFailCount());
        columnNames.add(TESTS_FAILED);
    }

    private void addTestsSkipped(List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getSkipCount());
        columnNames.add(TESTS_SKIPPED);
    }

    // Measurement
    private String getSerieName() {
        return build.getProject().getName()+".jenkins";
    }
}
