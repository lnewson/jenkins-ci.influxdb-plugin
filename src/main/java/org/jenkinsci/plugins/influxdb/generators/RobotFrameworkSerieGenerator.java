package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import org.influxdb.dto.Serie;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jrajala on 15.5.2015.
 */
public class RobotFrameworkSerieGenerator extends AbstractSerieGenerator {

    public static final String RF_OVERALL_SERIE = "rf.overall";
    public static final String RF_SUITE_SERIE = "rf.suite";

    public static final String RF_FAILED = "rf_failed";
    public static final String RF_PASSED = "rf_passed";
    public static final String RF_TOTAL = "rf_total";
    public static final String RF_CRITICAL_FAILED = "rf_critical_failed";
    public static final String RF_CRITICAL_PASSED = "rf_critical_passed";
    public static final String RF_CRITICAL_TOTAL = "rf_critical_total";
    public static final String RF_CRITICAL_PASS_PERCENTAGE = "rf_critical_pass_percentage";
    public static final String RF_PASS_PERCENTAGE = "rf_pass_percentage";
    public static final String RF_DURATION = "rf_duration";
    public static final String RF_SUITES = "rf_suites";
    public static final String RF_TESTCASES = "rf_testcases";

    private final AbstractBuild<?, ?> build;

    public RobotFrameworkSerieGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public boolean hasReport() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
    }

    public Serie[] generate() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);

        List<Serie> seriesList = new ArrayList<Serie>();

        seriesList.add(generateOverviewSerie(robotBuildAction));
        seriesList.addAll(generateSuiteSeries(robotBuildAction.getResult()));

        return seriesList.toArray(new Serie[seriesList.size()]);
    }

    private Serie generateOverviewSerie(RobotBuildAction robotBuildAction) {
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addJenkinsBuildNumber(build, columns, values);
        addJenkinsProjectName(build, columns, values);

        addOverallFailCount(robotBuildAction, columns, values);
        addOverallPassedCount(robotBuildAction, columns, values);
        addOverallTotalCount(robotBuildAction, columns, values);

        addCriticalFailCount(robotBuildAction, columns, values);
        addCriticalPassedCount(robotBuildAction, columns, values);
        addCriticalTotalCount(robotBuildAction, columns, values);

        addOverallCritialPassPercentage(robotBuildAction, columns, values);
        addOverallPassPercentage(robotBuildAction, columns, values);

        addDuration(robotBuildAction, columns, values);
        addSuites(robotBuildAction, columns, values);

        Serie.Builder builder = new Serie.Builder(RF_OVERALL_SERIE);
        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();

    }

    private void addOverallFailCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_FAILED);
        values.add(robotBuildAction.getResult().getOverallFailed());
    }
    private void addOverallPassedCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_PASSED);
        values.add(robotBuildAction.getResult().getOverallPassed());
    }
    private void addOverallTotalCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_TOTAL);
        values.add(robotBuildAction.getResult().getOverallTotal());
    }

    private void addCriticalFailCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_CRITICAL_FAILED);
        values.add(robotBuildAction.getResult().getCriticalFailed());
    }

    private void addCriticalPassedCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_CRITICAL_PASSED);
        values.add(robotBuildAction.getResult().getCriticalPassed());
    }

    private void addCriticalTotalCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_CRITICAL_TOTAL);
        values.add(robotBuildAction.getResult().getCriticalTotal());
    }

    private void addOverallCritialPassPercentage(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_CRITICAL_PASS_PERCENTAGE);
        values.add(robotBuildAction.getCriticalPassPercentage());
    }

    private void addOverallPassPercentage(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_PASS_PERCENTAGE);
        values.add(robotBuildAction.getOverallPassPercentage());
    }

    private void addDuration(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_DURATION);
        values.add(robotBuildAction.getResult().getDuration());
    }

    private void addSuites(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add(RF_SUITES);
        values.add(robotBuildAction.getResult().getAllSuites().size());
    }


    private List<Serie> generateSuiteSeries(RobotResult robotResult) {
        List<Serie> suiteSeries = new ArrayList<Serie>();
        for(RobotSuiteResult suiteResult : robotResult.getAllSuites()) {
            suiteSeries.add(generateSuiteSerie(suiteResult));
        }
        return suiteSeries;
    }

    private Serie generateSuiteSerie(RobotSuiteResult suiteResult) {
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addJenkinsBuildNumber(build, columns, values);
        addJenkinsProjectName(build, columns, values);

        columns.add(RF_TESTCASES);
        values.add(suiteResult.getAllCases().size());

        columns.add(RF_CRITICAL_FAILED);
        values.add(suiteResult.getCriticalFailed());

        columns.add(RF_CRITICAL_PASSED);
        values.add(suiteResult.getCriticalPassed());

        columns.add(RF_CRITICAL_TOTAL);
        values.add(suiteResult.getCriticalTotal());

        columns.add(RF_FAILED);
        values.add(suiteResult.getFailed());

        columns.add(RF_PASSED);
        values.add(suiteResult.getPassed());

        columns.add(RF_TOTAL);
        values.add(suiteResult.getTotal());

        Serie.Builder builder = new Serie.Builder(RF_SUITE_SERIE+"."+suiteResult.getDuplicateSafeName());
        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
    }
}
