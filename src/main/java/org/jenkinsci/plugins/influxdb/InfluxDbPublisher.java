package org.jenkinsci.plugins.influxdb;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.jenkinsci.plugins.influxdb.generators.CoberturaSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.JenkinsBaseSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.RobotFrameworkSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.SerieGenerator;

import java.io.IOException;
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

    private String selectedServer;


    public InfluxDbPublisher() {
    }

    public InfluxDbPublisher(String server) {
        this.selectedServer = server;
        System.out.println("Selected Server: " + server);
    }

    public String getSelectedServer() {
        String ipTemp = selectedServer;
        if (ipTemp == null) {
            Server[] servers = DESCRIPTOR.getServers();
            if (servers.length > 0) {
                ipTemp = servers[0].getHost();
            }
        }
        return ipTemp;
    }

    public void setSelectedServer(String server) {
        this.selectedServer = server;
    }

    public Server getServer() {
        Server[] servers = DESCRIPTOR.getServers();
        if (selectedServer == null && servers.length > 0) {
            return servers[0];
        }
        for (Server server : servers) {
            if (server.getHost().equals(selectedServer)) {
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
        InfluxDB influxDB = openInfluxDb(server);

        JenkinsBaseSerieGenerator jGenerator = new JenkinsBaseSerieGenerator(build);
        influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, jGenerator.generate());

        CoberturaSerieGenerator cbGenerator = new CoberturaSerieGenerator(build);
        if(cbGenerator.hasReport()) {
            influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, cbGenerator.generate());
        }

        SerieGenerator rfGenerator = new RobotFrameworkSerieGenerator(build);
        if(rfGenerator.hasReport()) {
            influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, rfGenerator.generate());
        }

        return true;
    }

    private InfluxDB openInfluxDb(Server server) {
        return InfluxDBFactory.connect("http://" + server.getHost() + ":" + server.getPort(), server.getUser(), server.getPassword());
    }



}
