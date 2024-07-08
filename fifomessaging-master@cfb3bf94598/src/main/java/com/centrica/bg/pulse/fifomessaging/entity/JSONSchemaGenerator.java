package com.centrica.bg.pulse.fifomessaging.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Helper class to generate JSON Schema based on java entity. in order to generate correct schema please 
 * make respective changes to java entity under package com.centrica.bg.pulse.leadgen.entity.
 * IMP note : 
 * 1) required attributes are not populated correctly, so better to copy the location and syntax from the previous version
 * 2) replace date_time with date-time (i.e date<hyphen>time) 
 * 3) for class meta it generates type : any as shown below
   "meta" : {
      "type" : "any"
    }
    
    PLEASE CHANGE THE .JSON FILE TO HAVE THE ABOVE ENTRY AS BELOW
    "meta" : {
      "type" : "object"
    }
 * @author basotim1
 *
 */
public class JSONSchemaGenerator {
	private static final Logger log = LoggerFactory
			.getLogger(JSONSchemaGenerator.class);
	public static void main(String[] args) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.constructType(Object.class);
		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
		mapper.acceptJsonFormatVisitor(MessageRequest.class, visitor);
		com.fasterxml.jackson.module.jsonSchema.JsonSchema schema = visitor
				.finalSchema();
		FileWriter fileWriter = new FileWriter("MessageSchema.json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(fileWriter, schema);
		log.debug("Done");

	}
}
