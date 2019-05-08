package ca.nrc.cadc.sia2.integration;

import ca.nrc.cadc.vosi.CapabilitiesTest;

import java.net.URI;

public class VosiCapabilitiesTest extends CapabilitiesTest
{
    public VosiCapabilitiesTest()
    {
        super(URI.create("ivo://cadc.nrc.ca/alma-sia"));
    }
}
