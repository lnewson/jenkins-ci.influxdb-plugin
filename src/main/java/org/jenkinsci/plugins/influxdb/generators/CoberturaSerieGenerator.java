package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import org.influxdb.dto.Serie;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jrajala on 15.5.2015.
 */
public class CoberturaSerieGenerator extends AbstractSerieGenerator {

    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";
    private static final String COBERTURA_REPORT_FILE = "/target/cobertura/cobertura.ser";

    private final AbstractBuild<?, ?> build;
    private ProjectData coberturaProjectData;
    private final File coberturaFile;

    public CoberturaSerieGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
        coberturaFile = new File(build.getWorkspace() + COBERTURA_REPORT_FILE);
    }

    public boolean hasReport() {
        return (coberturaFile != null && coberturaFile.exists() && coberturaFile.canRead());
    }

    public Serie[] generate() {
        coberturaProjectData = CoverageDataFileHandler.loadCoverageData(coberturaFile);

        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addJenkinsBuildNumber(build, columns, values);
        addJenkinsProjectName(build, columns, values);

        addNumberOfPackages(coberturaProjectData, columns, values);
        addNumberOfSourceFiles(coberturaProjectData, columns, values);
        addNumberOfClasses(coberturaProjectData, columns, values);
        addBranchCoverageRate(coberturaProjectData, columns, values);
        addLineCoverageRate(coberturaProjectData, columns, values);
        addPackageCoverage(coberturaProjectData, columns, values);
        addClassCoverage(coberturaProjectData, columns, values);

        Serie.Builder builder = new Serie.Builder("CoberturaResults");

        return new Serie[] {builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build() };

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


}
