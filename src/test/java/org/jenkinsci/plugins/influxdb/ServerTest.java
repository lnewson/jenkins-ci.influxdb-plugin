package org.jenkinsci.plugins.influxdb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jrajala on 1.5.2015.
 */
public class ServerTest {
    private Server server;

    @Before
    public void setupTestcase() {
        server = new Server();
    }

    @Test
    public void shouldAllowSettingHost() {
        server.setHost("hostname");
        Assert.assertEquals("hostname", server.getHost());
    }

}
