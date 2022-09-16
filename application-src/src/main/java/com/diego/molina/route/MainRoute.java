package com.diego.molina.route;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MainRoute extends RouteBuilder{

	@Value("${quartz.retrieve.demo-once}")
	String fromQuartz;
	
	@Value("${finish-process.create-file}")
	String createFile;
	
	@Override
	public void configure() throws Exception {

		onException(Exception.class).handled(true)
			.log("Error during the execution of the process: ${exception.stacktrace}")
			.to("direct:finishProcess")
		.end();
		
		from(fromQuartz).routeId("quartzExecution").streamCaching()
			.log("Apache camel demo is now running!")
			.log("Log executed at ${date:now:yyyy-MM-dd HH:mm:ss}")
			.to("direct:finishProcess")
		.end();		

		
		/*
		 * This route creates a file in /tmp/pod/ in order to demote all the containers associated to this process and save resources,
		 * this route is been called as the last step in the integration
		 */		
		from("direct:finishProcess").routeId("finishProcess")
			.removeHeaders("*")
			.setBody(constant("END OF THE PROCESS"))
			.to(createFile)
		.end();
	}

}
