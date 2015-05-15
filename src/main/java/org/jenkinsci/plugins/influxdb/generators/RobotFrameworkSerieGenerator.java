package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import hudson.tasks.junit.SuiteResult;
import org.influxdb.dto.Serie;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jrajala on 15.5.2015.
 */
public class RobotFrameworkSerieGenerator extends AbstractSerieGenerator {

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

        Serie.Builder builder = new Serie.Builder("RobotFrameworkResults");
        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();

    }

    private void addOverallFailCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_overall_failed");
        values.add(robotBuildAction.getResult().getOverallFailed());
    }
    private void addOverallPassedCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_overall_passed");
        values.add(robotBuildAction.getResult().getOverallPassed());
    }
    private void addOverallTotalCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_overall_total");
        values.add(robotBuildAction.getResult().getOverallTotal());
    }

    private void addCriticalFailCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_critical_fail");
        values.add(robotBuildAction.getResult().getCriticalFailed());
    }

    private void addCriticalPassedCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_critical_passed");
        values.add(robotBuildAction.getResult().getCriticalPassed());
    }

    private void addCriticalTotalCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_critical_total");
        values.add(robotBuildAction.getResult().getCriticalTotal());
    }

    private void addOverallCritialPassPercentage(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_critical_pass_percentage");
        values.add(robotBuildAction.getCriticalPassPercentage());
    }

    private void addOverallPassPercentage(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_overall_pass_percentage");
        values.add(robotBuildAction.getOverallPassPercentage());
    }

    private void addDuration(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_duration");
        values.add(robotBuildAction.getResult().getDuration());
    }

    private void addSuites(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_suites");
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

        columns.add("rf_testcases");
        values.add(suiteResult.getAllCases().size());

        columns.add("rf_suite");
        values.add(suiteResult.getDuplicateSafeName());

        Serie.Builder builder = new Serie.Builder("RobotFrameworkResults");
        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
    }
}
