package org.jenkinsci.plugins.influxdb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jrajala on 1.5.2015.
 */
public class InfluxDbValidatorTest {

    InfluxDbValidator validator;
    @Before
    public void setupTest() {
        validator = new InfluxDbValidator();
    }

    @Test
    public void shouldAllowValidPortNumbers() {
        for(int i = 1; i <=65535; i++) {
            Assert.assertTrue(validator.validatePortFormat("" + i));
        }
    }

    @Test
    public void shouldNotAllowPortsOusideOfValidRange() {
        Assert.assertFalse(validator.validatePortFormat(""+0));
        Assert.assertFalse(validator.validatePortFormat(""+65536));
    }

    @Test
    public void shouldNotAcceptAnythingThatDoesNotParseAsNumber() {
        Assert.assertFalse(validator.validatePortFormat("adsf"));
    }
}
