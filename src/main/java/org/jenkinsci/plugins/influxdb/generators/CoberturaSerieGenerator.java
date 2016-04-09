package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import org.influxdb.dto.Point;
import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.cobertura.targets.CoverageResult;
import hudson.plugins.cobertura.CoberturaBuildAction;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 15.5.2015.
 */

public class CoberturaSerieGenerator extends AbstractSerieGenerator {

    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_SOURCEFILE_COVERAGE_RATE = "cobertura_sourcefile_coverage_rate"; 
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";
    public static final String COBERTURA_NUMBER_OF_LINES = "cobertura_number_of_lines";
    private static final String COBERTURA_REPORT_FILE = "/target/cobertura/cobertura.ser";

    private CoverageResult coberturaCoverageResult = null;
    private File coberturaFile = null;

    public CoberturaSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        super(build, logger);

        CoberturaBuildAction coberturaAction = build.getAction(CoberturaBuildAction.class);
        if (coberturaAction != null) {
            coberturaCoverageResult = coberturaAction.getResult();
        }
        else {
            coberturaFile = new File(build.getWorkspace() + COBERTURA_REPORT_FILE);
        }
    }

    public boolean hasReport() {
        return hasCoberturaReport() || hasJavaReport();
    }

    private boolean hasCoberturaReport() {
        return coberturaCoverageResult != null && coberturaCoverageResult.getCoverage(CoverageMetric.LINE) != null;
    }

    private boolean hasJavaReport() {
        return (coberturaFile != null && coberturaFile.exists() && coberturaFile.canRead());
    }

    public Point[] generate() {
        List<String> columNames = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        logger.println("Influxdb starting to generate Cobertura report");

        addJenkinsBuildNumber(columNames, values);
        addJenkinsProjectName(columNames, values);

        if (hasCoberturaReport()) {
            generateCoberturaReport(columNames, values);
        }
        else if (hasJavaReport()) {
            generateJavaReport(columNames, values);
        }

        HashMap<String, Object> fields = zipListsToMap(columNames, values);

        logger.println("Influxdb Cobertura report generation finished with " + fields.size() +  " values to report");

        return new Point[]{Point.measurement(getSerieName())
                .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .fields(fields)
                .build()};
    }

    private void generateCoberturaReport(List<String> columnNames, List<Object> values) {
        logger.println("Generating Cobertura report using cobertura plugin");

        columnNames.add(COBERTURA_PACKAGE_COVERAGE_RATE);
        values.add(getCoveragePercentage(coberturaCoverageResult, CoverageMetric.PACKAGES));
        columnNames.add(COBERTURA_NUMBER_OF_PACKAGES);
        values.add(getCoverageValue(coberturaCoverageResult, CoverageMetric.PACKAGES));

        columnNames.add(COBERTURA_SOURCEFILE_COVERAGE_RATE);
        values.add(getCoveragePercentage(coberturaCoverageResult, CoverageMetric.FILES));
        columnNames.add(COBERTURA_NUMBER_OF_SOURCEFILES);
        values.add(getCoverageValue(coberturaCoverageResult, CoverageMetric.FILES));

        /**
         * these are left out
         * getCoveragePercentage(result, CoverageMetric.METHOD);
         * getCoveragePercentage(result, CoverageMetric.CONDITIONAL);
         */

        columnNames.add(COBERTURA_CLASS_COVERAGE_RATE);
        values.add(getCoveragePercentage(coberturaCoverageResult, CoverageMetric.CLASSES));
        columnNames.add(COBERTURA_NUMBER_OF_CLASSES);
        values.add(getCoverageValue(coberturaCoverageResult, CoverageMetric.CLASSES));

        columnNames.add(COBERTURA_LINE_COVERAGE_RATE);
        values.add(getCoveragePercentage(coberturaCoverageResult, CoverageMetric.LINE));
        columnNames.add(COBERTURA_NUMBER_OF_LINES);
        values.add(getCoverageValue(coberturaCoverageResult, CoverageMetric.LINE));
    }

    private void generateJavaReport(List<String> columnNames, List<Object> values) {
        ProjectData coberturaProjectData = CoverageDataFileHandler.loadCoverageData(coberturaFile);

        addJenkinsBuildNumber(columnNames, values);
        addJenkinsProjectName(columnNames, values);

        addNumberOfPackages(coberturaProjectData, columnNames, values);
        addNumberOfSourceFiles(coberturaProjectData, columnNames, values);
        addNumberOfClasses(coberturaProjectData, columnNames, values);
        addBranchCoverageRate(coberturaProjectData, columnNames, values);
        addLineCoverageRate(coberturaProjectData, columnNames, values);
        addPackageCoverage(coberturaProjectData, columnNames, values);
        addClassCoverage(coberturaProjectData, columnNames, values);

    }

    private void addPackageCoverage(ProjectData projectData, List<String> columnNames, List<Object> values) {
        double totalPacakges = projectData.getPackages().size();
        double packagesCovered = 0;
        for(Object nextPackage : projectData.getPackages()) {
            PackageData packageData = (PackageData) nextPackage;
            if(packageData.getLineCoverageRate() > 0)
                packagesCovered++;
        }
        double packageCoverage = packagesCovered / totalPacakges;

        columnNames.add(COBERTURA_PACKAGE_COVERAGE_RATE);
        values.add(packageCoverage*100d);
    }

    private void addClassCoverage(ProjectData projectData, List<String> columnNames, List<Object> values) {
        double totalClasses = projectData.getNumberOfClasses();
        double classesCovered = 0;
        for(Object nextClass : projectData.getClasses()) {
            ClassData classData = (ClassData) nextClass;
            if(classData.getLineCoverageRate() > 0)
                classesCovered++;
        }
        double classCoverage = classesCovered / totalClasses;

        columnNames.add(COBERTURA_CLASS_COVERAGE_RATE);
        values.add(classCoverage*100d);
    }

    private void addLineCoverageRate(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add(COBERTURA_LINE_COVERAGE_RATE);
        values.add(projectData.getLineCoverageRate()*100d);
    }

    private void addBranchCoverageRate(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add(COBERTURA_BRANCH_COVERAGE_RATE);
        values.add(projectData.getBranchCoverageRate()*100d);
    }

    private void addNumberOfPackages(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add(COBERTURA_NUMBER_OF_PACKAGES);
        values.add(projectData.getPackages().size());
    }

    private void addNumberOfSourceFiles(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add(COBERTURA_NUMBER_OF_SOURCEFILES);
        values.add(projectData.getNumberOfSourceFiles());
    }

    private void addNumberOfClasses(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add(COBERTURA_NUMBER_OF_CLASSES);
        values.add(projectData.getNumberOfClasses());
    }

    private String getSerieName() {
        return build.getProject().getName()+".cobertura";
    }

    private static float getCoveragePercentage(CoverageResult result, CoverageMetric metric) {
        Ratio ratio = result.getCoverage(metric);
        if (ratio == null) {
            return 0.0f;
        }
        return ratio.getPercentageFloat();
    }

    private static int getCoverageValue(CoverageResult result, CoverageMetric metric) {
        Ratio ratio = result.getCoverage(metric);
        if (ratio == null) {
            return 0;
        }
        return Math.round(ratio.denominator);
    }

}
