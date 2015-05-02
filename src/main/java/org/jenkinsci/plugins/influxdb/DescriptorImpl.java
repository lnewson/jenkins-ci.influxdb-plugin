package org.jenkinsci.plugins.influxdb;

import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Iterator;

/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 *
 * 
 */
public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject {

	public static final String DISPLAY_NAME = "Publish metrics to InfluxDb Server";
	private final CopyOnWriteList<Server> servers = new CopyOnWriteList<Server>();

	private InfluxDbValidator validator = new InfluxDbValidator();

	/**
	 * The default constructor.
	 */
	public DescriptorImpl() {
		super(InfluxDbPublisher.class);
		load();
	}

	public DescriptorImpl(boolean loadConfiguration) {
		super(InfluxDbPublisher.class);
		if(loadConfiguration)
 		  load();
	}

	/**
	 * @return servers
	 */
	public Server[] getServers() {
		Iterator<Server> it = servers.iterator();
		int size = 0;
		while (it.hasNext()) {
			it.next();
			size++;
		}
		return servers.toArray(new Server[size]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Descriptor#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
	 */
	@Override
	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
	 */
	@Override
	public Publisher newInstance(StaplerRequest req, JSONObject formData) {
		InfluxDbPublisher publisher = new InfluxDbPublisher();
		req.bindParameters(publisher, "publisherBinding.");
		return publisher;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
	 */
	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) {
		servers.replaceBy(req.bindParametersToList(Server.class, "serverBinding."));
		save();
		return true;
	}

	public InfluxDbValidator getValidator() {
	return validator;
}


	public void setValidator(InfluxDbValidator validator) {
	this.validator = validator;
}

	/**
	 * @param value
	 * @return  form validation of description
	 */
	public FormValidation doCheckDescription(@QueryParameter final String value) {
		if (!validator.isDescriptionPresent(value)) {
			return FormValidation.error("Server description is mandatory");
		}
		if (validator.isDescriptionTooLong(value)) {
			return FormValidation.error("Description is limited to 100 characters");
		}

		return FormValidation.ok("Description is correctly configured");
	}

	/**
	 * @param value
	 * @return  form validation of port.
	 */
	public FormValidation doCheckPort(@QueryParameter final String value) {
		if (!validator.isPortPresent(value)) {
			return FormValidation.error("Please set a port");
		}

		if (!validator.validatePortFormat(value)) {
			return FormValidation.error("Please check the port format");
		}

		return FormValidation.ok("Port is correctly configured");
	}

	/**
	 * 
	 * @param value
	 * @return  form validation of base queue name
	 */
	public FormValidation doCheckDatabaseName(@QueryParameter final String value) {
	    if(!validator.isDatabaseNamePresent(value)){
	        return FormValidation.error("Database name is mandatory");
	    }

	    return FormValidation.ok("Database name is correctly Configured");
	    
	}
}
