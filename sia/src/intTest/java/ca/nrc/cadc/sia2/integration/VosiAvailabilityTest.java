package ca.nrc.cadc.sia2.integration;

import ca.nrc.cadc.vosi.AvailabilityTest;

import java.net.URI;

public class VosiAvailabilityTest extends AvailabilityTest
{
    public VosiAvailabilityTest()
    {
        super(URI.create("ivo://cadc.nrc.ca/alma-sia"));
    }
}
