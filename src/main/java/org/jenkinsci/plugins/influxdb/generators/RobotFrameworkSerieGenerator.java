package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import org.influxdb.dto.Point;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 15.5.2015.
 */
public class RobotFrameworkSerieGenerator extends AbstractSerieGenerator {

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

    private final Map<String, RobotTagResult> tagResults;

    public RobotFrameworkSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        super(build, logger);
        tagResults = new Hashtable<String, RobotTagResult>();
    }

    public boolean hasReport() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
    }

    public Point[] generate() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);

        List<Point> seriesList = new ArrayList<Point>();

        logger.println("Influxdb starting to generate Robot Framework report");

        seriesList.add(generateOverviewSerie(robotBuildAction));
        seriesList.addAll(generateSubSeries(robotBuildAction.getResult()));

        logger.println("Influxdb Robot Framework report generation finished");

        return seriesList.toArray(new Point[seriesList.size()]);
    }

    private Point generateOverviewSerie(RobotBuildAction robotBuildAction) {
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

        HashMap<String, Object> fields = zipListsToMap(columns, values);

        return Point.measurement(getSeriePrefix())
                .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .fields(fields)
                .build();
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


    private List<Point> generateSubSeries(RobotResult robotResult) {
        List<Point> subSeries = new ArrayList<Point>();
        for(RobotSuiteResult suiteResult : robotResult.getAllSuites()) {
            subSeries.add(generateSuiteSerie(suiteResult));

            for(RobotCaseResult caseResult : suiteResult.getAllCases()) {
                subSeries.add(generateCaseSerie(caseResult));
            }

        }

        for(Map.Entry<String, RobotTagResult> entry : tagResults.entrySet()) {
            subSeries.add(generateTagSerie(entry.getValue()));
        }
        return subSeries;
    }

    private Point generateCaseSerie(RobotCaseResult caseResult) {
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        columns.add(RF_CRITICAL_FAILED);
        values.add(caseResult.getCriticalFailed());

        columns.add(RF_CRITICAL_PASSED);
        values.add(caseResult.getCriticalPassed());

        columns.add(RF_FAILED);
        values.add(caseResult.getFailed());

        columns.add(RF_PASSED);
        values.add(caseResult.getPassed());

        columns.add(RF_DURATION);
        values.add(caseResult.getDuration());

//        Serie.Builder builder = new Serie.Builder(getCaseSerieName(caseResult));

        for(String tag : caseResult.getTags()) {
            markTagResult(tag, caseResult);
        }

        HashMap<String, Object> fields = zipListsToMap(columns, values);
        return Point.measurement(getCaseSerieName(caseResult))
                .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .fields(fields)
                .build();

//        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
    }

    private final class RobotTagResult {
        protected final String name;
        protected RobotTagResult(String name) {
            this.name = name;
        }
        protected final List<String> testCases = new ArrayList<String>();
        protected int failed = 0;
        protected int passed = 0;
        protected int criticalFailed = 0;
        protected int criticalPassed = 0;
        protected long duration = 0;
    }


    private void markTagResult(String tag, RobotCaseResult caseResult) {
        if(tagResults.get(tag) == null)
            tagResults.put(tag, new RobotTagResult(tag));

        RobotTagResult tagResult = tagResults.get(tag);
        if(!tagResult.testCases.contains(caseResult.getDuplicateSafeName())) {
            tagResult.failed += caseResult.getFailed();
            tagResult.passed += caseResult.getPassed();
            tagResult.criticalFailed += caseResult.getCriticalFailed();
            tagResult.criticalPassed += caseResult.getCriticalPassed();
            tagResult.duration += caseResult.getDuration();
            tagResult.testCases.add(caseResult.getDuplicateSafeName());
        }


    }

    private Point generateTagSerie(RobotTagResult tagResult) {
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        columns.add(RF_CRITICAL_FAILED);
        values.add(tagResult.criticalFailed);

        columns.add(RF_CRITICAL_PASSED);
        values.add(tagResult.criticalPassed);

        columns.add(RF_CRITICAL_TOTAL);
        values.add(tagResult.criticalPassed + tagResult.criticalFailed);

        columns.add(RF_FAILED);
        values.add(tagResult.failed);

        columns.add(RF_PASSED);
        values.add(tagResult.passed);

        columns.add(RF_TOTAL);
        values.add(tagResult.passed + tagResult.failed);

        columns.add(RF_DURATION);
        values.add(tagResult.duration);

        HashMap<String, Object> fields = zipListsToMap(columns, values);
        return Point.measurement(getTagSerieName(tagResult))
                .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .fields(fields)
                .build();
    }

    private Point generateSuiteSerie(RobotSuiteResult suiteResult) {
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

        columns.add(RF_DURATION);
        values.add(suiteResult.getDuration());

        HashMap<String, Object> fields = zipListsToMap(columns, values);
        return Point.measurement(getSuiteSerieName(suiteResult))
                .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .fields(fields)
                .build();
//
//        Serie.Builder builder = new Serie.Builder(getSuiteSerieName(suiteResult));
//        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
    }



    private String getTagSerieName(RobotTagResult tagResult) {
        return getTagSeriePrefix()+"."+tagResult.name;
    }

    private String getCaseSerieName(RobotCaseResult caseResult) {
        return getCaseSeriePrefix()+"."+caseResult.getDuplicateSafeName();
    }

    private String getCaseSeriePrefix() {
        return getSeriePrefix()+".testcase";
    }

    private String getSuiteSerieName(RobotSuiteResult suiteResult) {
        return getSuiteSeriePrefix()+"."+suiteResult.getDuplicateSafeName();
    }

    private String getTagSeriePrefix() {
        return getSeriePrefix()+".tag";
    }

    private String getSuiteSeriePrefix() {
        return getSeriePrefix()+".suite";
    }

    private String getSeriePrefix() {
        return build.getProject().getName()+".rf";
    }
}
