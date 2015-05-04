package org.jenkinsci.plugins.influxdb;

import hudson.plugins.robot.RobotBuildAction;
import hudson.tasks.test.AbstractTestResultAction;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 * 
 */
public class InfluxDbPublisher extends Notifier {


    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";
    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";
    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";

    private static final String COBERTURA_REPORT_FILE = "/target/cobertura/cobertura.ser";

    private String selectedIp;
    private String serieName;

    private String protocol;


    public InfluxDbPublisher() {
    }

    public InfluxDbPublisher(String ip, String metric, String protocol) {
        this.selectedIp = ip;
        this.protocol = protocol;
        System.out.println("IP: " + ip);
        System.out.println("Protocol: " + protocol);

    }

    public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

    public String getSelectedIp() {
        String ipTemp = selectedIp;
        if (ipTemp == null) {
            Server[] servers = DESCRIPTOR.getServers();
            if (servers.length > 0) {
                ipTemp = servers[0].getHost();
            }
        }
        return ipTemp;
    }

    public String getSerieName() {
        return serieName;
    }

    public void setSerieName(String serieName) {
        this.serieName = serieName;
    }

    public void setSelectedIp(String ip) {
        this.selectedIp = ip;
    }

    public Server getServer() {
        Server[] servers = DESCRIPTOR.getServers();
        if (selectedIp == null && servers.length > 0) {
            return servers[0];
        }
        for (Server server : servers) {
            if (server.getHost().equals(selectedIp)) {
                return server;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#prebuild(hudson.model.AbstractBuild
     * , hudson.model.BuildListener)
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.tasks.Publisher#needsToRunAfterFinalized()
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.tasks.Notifier#getDescriptor()
     */
    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
     * , hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        Server server = getServer();
        Serie metricSerie = buildSerieFromBuild(build);
        InfluxDB influxDB = openInfluxDb(server);
        influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, metricSerie);

        return true;
    }

    private Serie buildSerieFromBuild(AbstractBuild<?, ?> build) {
        Serie.Builder builder = new Serie.Builder(serieName);

        List<String> columnNames = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addProjectName(build, columnNames, values);
        addBuildNumber(build, columnNames, values);
        addBuildDuration(build, columnNames, values);
        addBuildStatusSummaryMesssage(build, columnNames, values);
        addProjectBuildHealth(build, columnNames, values);

        if(hasTestResults(build)) {
            addTestsFailed(build, columnNames, values);
            addTestsSkipped(build, columnNames, values);
            addTestsTotal(build, columnNames, values);
        }

        if(hasCoberturaReport(build)) {
            ProjectData coberturaProjectData = getCoberturaProjectData(build);
            addNumberOfPackages(coberturaProjectData, columnNames, values);
            addNumberOfSourceFiles(coberturaProjectData, columnNames, values);
            addNumberOfClasses(coberturaProjectData, columnNames, values);
            addBranchCoverageRate(coberturaProjectData, columnNames, values);
            addLineCoverageRate(coberturaProjectData, columnNames, values);
            addPackageCoverage(coberturaProjectData, columnNames, values);
            addClassCoverage(coberturaProjectData, columnNames, values);
        }

        if(hasRobotFrameworkReport(build)) {
            RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
            addFailCount(robotBuildAction,columnNames, values);
            addCritialPassPercentage(robotBuildAction, columnNames, values);
            addOveralPassPercentage(robotBuildAction, columnNames, values);
        }

        return builder.columns(columnNames.toArray(new String[columnNames.size()])).values(values.toArray()).build();

    }

    private void addFailCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_fail_count");
        values.add(robotBuildAction.getFailCount());
    }
    private void addTotalCount(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_total_count");
        values.add(robotBuildAction.getTotalCount());
    }

    private void addCritialPassPercentage(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_critical_pass_percentage");
        values.add(robotBuildAction.getCriticalPassPercentage());
    }

    private void addOveralPassPercentage(RobotBuildAction robotBuildAction, List<String> columnNames, List<Object> values) {
        columnNames.add("rf_overal_pass_percentage");
        values.add(robotBuildAction.getOverallPassPercentage());
    }

    private boolean hasRobotFrameworkReport(AbstractBuild<?, ?> build) {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
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

    private void addProjectName(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_NAME);
        values.add(build.getProject().getName());
    }

    private void addBuildNumber(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_NUMBER);
        values.add(build.getNumber());
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

    private InfluxDB openInfluxDb(Server server) {
        return InfluxDBFactory.connect("http://" + server.getHost() + ":" + server.getPort(), server.getUser(), server.getPassword());
    }


    private boolean hasCoberturaReport(AbstractBuild<?, ?> build) {
        File coberturaFile = getCoberturaFile(build);
        return (coberturaFile != null && coberturaFile.exists() && coberturaFile.canRead());
    }

    private ProjectData getCoberturaProjectData(AbstractBuild<?, ?> build) {
        File coberturaFile = getCoberturaFile(build);
        return CoverageDataFileHandler.loadCoverageData(coberturaFile);
    }

    private File getCoberturaFile(AbstractBuild<?, ?> build) {
        return new File(build.getWorkspace() + COBERTURA_REPORT_FILE);
    }

}
