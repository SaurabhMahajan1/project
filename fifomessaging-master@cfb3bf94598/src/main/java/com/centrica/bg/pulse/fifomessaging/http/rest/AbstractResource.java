package com.centrica.bg.pulse.fifomessaging.http.rest;

import org.apache.commons.logging.Log;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.nio.charset.Charset;

abstract class AbstractResource {
    protected Log log;

    abstract public UriInfo getUriInfo();

    protected void addCharacterEncoding(Response.ResponseBuilder builder, String mediaType, Charset charset) {
        builder.type(mediaType + ";charset=" + charset.name());
    }
}
