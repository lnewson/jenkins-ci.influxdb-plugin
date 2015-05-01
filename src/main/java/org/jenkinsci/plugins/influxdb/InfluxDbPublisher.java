package org.jenkinsci.plugins.influxdb;

import hudson.tasks.test.AbstractTestResultAction;
import net.sourceforge.cobertura.check.PackageCoverage;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
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
import java.math.BigDecimal;
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


    private String selectedIp;
    private String selectedMetric;
    private String serieName;

    /**
     *
     */
    private String protocol;


    /**
     *
     */
    public InfluxDbPublisher() {
    }

    /**
     *
     */
    public InfluxDbPublisher(String ip, String metric, String protocol) {
        this.selectedIp = ip;
        this.selectedMetric = metric;
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



    /**
     * 
     * @return selectedIp
     */
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

    /**
     * 
     * @param ip
     */
    public void setSelectedIp(String ip) {
        this.selectedIp = ip;
    }

    /**
     * 
     * @param metric
     */
    public void setSelectedMetric(String metric) {
        this.selectedMetric = metric;
    }

    /**
     * 
     * @return server
     */
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
        if(hasCoberturaReport()) {
            ProjectData coberturaProjectData = getCoberturaProjectData();
            addNumberOfPackages(coberturaProjectData, columnNames, values);
            addNumberOfSourceFiles(coberturaProjectData, columnNames, values);
            addNumberOfClasses(coberturaProjectData, columnNames, values);
            addBranchCoverageRate(coberturaProjectData, columnNames, values);
            addLineCoverageRate(coberturaProjectData, columnNames, values);
        }

        return builder.columns(columnNames.toArray(new String[columnNames.size()])).values(values.toArray()).build();

    }

    private void addLineCoverageRate(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add("cobertura_line_coverage_rate");
        values.add(projectData.getLineCoverageRate());
    }

    private void addBranchCoverageRate(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add("cobertura_branch_coverage_rate");
        values.add(projectData.getBranchCoverageRate());
    }

    private void addNumberOfPackages(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add("cobertura_number_of_packages");
        values.add(projectData.getPackages().size());
    }

    private void addNumberOfSourceFiles(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add("cobertura_number_of_sourcefiles");
        values.add(projectData.getNumberOfSourceFiles());
    }

    private void addNumberOfClasses(ProjectData projectData, List<String> columnNames, List<Object> values) {
        columnNames.add("cobertura_number_of_classes");
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

    private static final String COBERTURA_REPORT_FILE = "/target/cobertura/cobertura.ser";
    private File coberturaFile = new File(COBERTURA_REPORT_FILE);

    private boolean hasCoberturaReport() {
        return coberturaFile.exists() && coberturaFile.canRead();
    }

    private ProjectData getCoberturaProjectData() {
        return CoverageDataFileHandler.loadCoverageData(coberturaFile);
    }

		/*
		File dataFile = new File(build.getWorkspace() + "/target/cobertura/cobertura.ser");

		ProjectData projectData = CoverageDataFileHandler.loadCoverageData(dataFile);

		if (projectData == null) {
			logger.print("Error: Unable to read from data file " + dataFile.getAbsolutePath());
		}

		double totalLines = 0;
		double totalLinesCovered = 0;
		double totalBranches = 0;
		double totalBranchesCovered = 0;

		Iterator<?> iter = projectData.getClasses().iterator();
		while (iter.hasNext()) {
			ClassData classData = (ClassData) iter.next();

			totalBranches += classData.getNumberOfValidBranches();
			totalBranchesCovered += classData.getNumberOfCoveredBranches();

			totalLines += classData.getNumberOfValidLines();
			totalLinesCovered += classData.getNumberOfCoveredLines();

			// for next release :
			// PackageCoverage packageCoverage = getPackageCoverage(classData.getPackageName());
			// packageCoverage.addBranchCount(classData.getNumberOfValidBranches());
			// packageCoverage.addBranchCoverage(classData.getNumberOfCoveredBranches());
			//
			// packageCoverage.addLineCount(classData.getNumberOfValidLines());
			// packageCoverage.addLineCoverage(classData.getNumberOfCoveredLines());
			//
			// + percentage(classData.getLineCoverageRate()) + "%, branch coverage rate: "
			// + percentage(classData.getBranchCoverageRate()) + "%");

		}

		for (int i = 0; i < metrics.length; i++) {
			if (metrics[i].getName().equals(MetricsEnum.COBERTURA_TOTAL_LINE_COVERAGE.name())) {
				sendMetric(server, metrics[i], percentage(totalLinesCovered / totalLines));
			}
			if (metrics[i].getName().equals(MetricsEnum.COBERTURA_TOTAL_BRANCH_COVERAGE.name())) {
				sendMetric(server, metrics[i], percentage(totalBranchesCovered / totalBranches));
			}
		}


    Map<String, PackageCoverage> packageCoverageMap = new HashMap();

    private PackageCoverage getPackageCoverage(String packageName) {
        PackageCoverage packageCoverage = packageCoverageMap.get(packageName);
        if (packageCoverage == null) {
            packageCoverage = new PackageCoverage();
            packageCoverageMap.put(packageName, packageCoverage);
        }
        return packageCoverage;
    }

    private String percentage(double coverateRate) {
        BigDecimal decimal = new BigDecimal(coverateRate * 100);
        return decimal.setScale(1, BigDecimal.ROUND_DOWN).toString();
    }
*/
}
