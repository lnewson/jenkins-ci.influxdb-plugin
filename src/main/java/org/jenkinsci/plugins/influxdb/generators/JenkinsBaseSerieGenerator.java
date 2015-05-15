package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;
import org.influxdb.dto.Serie;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jrajala on 15.5.2015.
 */
public class JenkinsBaseSerieGenerator extends AbstractSerieGenerator {

    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";

    private final AbstractBuild<?, ?> build;

    public JenkinsBaseSerieGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public boolean hasReport() {
        return true;
    }

    public Serie[] generate() {
        Serie.Builder builder = new Serie.Builder(getSerieName());

        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addJenkinsBuildNumber(build, columns, values);
        addJenkinsProjectName(build, columns, values);
        addBuildDuration(build, columns, values);
        addBuildStatusSummaryMesssage(build, columns, values);
        addProjectBuildHealth(build, columns, values);

        if(hasTestResults(build)) {
            addTestsFailed(build, columns, values);
            addTestsSkipped(build, columns, values);
            addTestsTotal(build, columns, values);
        }

        return new Serie[] {builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build()};

    }


    private void addBuildDuration(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_TIME);
        values.add(build.getDuration());
    }

    private void addBuildStatusSummaryMesssage(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_STATUS_MESSAGE);
        values.add(build.getBuildStatusSummary().message);
    }

    private void addProjectBuildHealth(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_BUILD_HEALTH);
        values.add(build.getProject().getBuildHealth().getScore());
    }

    private boolean hasTestResults(AbstractBuild<?, ?> build) {
        return build.getAction(AbstractTestResultAction.class) != null;
    }

    private void addTestsTotal(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getTotalCount());
        columnNames.add(TESTS_TOTAL);
    }

    private void addTestsFailed(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getFailCount());
        columnNames.add(TESTS_FAILED);
    }

    private void addTestsSkipped(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getSkipCount());
        columnNames.add(TESTS_SKIPPED);
    }

    private String getSerieName() {
        return build.getProject().getName()+".jenkins";
    }

}
