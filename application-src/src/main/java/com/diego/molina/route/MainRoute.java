package com.diego.molina.route;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.diego.molina.processors.BodyIntoListProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MainRoute extends RouteBuilder{

	@Value("${quartz.retrieve.demo-once}")
	String fromQuartz;
	
	@Value("${finish-process.create-file}")
	String createFile;

	//API URLS

	@Value("${api.url.predict-gender}")
	String predictGenderUrl;

	@Value("${api.url.random-name}")
	String randomNameUrl;

	@Value("${api.url.predict-age}")
	String predictAgeUrl;

	@Autowired
	private Environment env;

	private final static String DUMMY_URL        = "https://dummyurl?throwExceptionOnFailure=false";
	private final static String JDBC_URL         = "jdbc:dataSource?useHeadersAsParameters=true";
	private final static String DATETIME_NOW     = "${date:now:yyyy-MM-dd HH:mm:ss}";
	private static final String CAMEL_HTTP_URI   = "CamelHttpUri";
	private static final String CAMEL_HTTP_METHOD= "CamelHttpMethod";
	private static final String IS_OK_HTTPCODE   = "${header.CamelHttpResponseCode} == 200";

	@Override
	public void configure() throws Exception {

		onException(Exception.class).handled(true)
			.log("Error during the execution of the process: ${exception.stacktrace}")
			.to("direct:finishProcess")
		.end();
		
		from(fromQuartz).routeId("quartzExecution").streamCaching()
				.log("Apache camel demo is now running!")
				.log("Log executed at ${date:now:yyyy-MM-dd HH:mm:ss}")
				.to("direct:getNamesRoute")
				.to("direct:finishProcess")
		.end();		



		from("direct:getNamesRoute").routeId("getNamesRoute").streamCaching()
				.removeHeaders("*")
				.log("Names url: " + randomNameUrl)
				.setHeader(CAMEL_HTTP_METHOD, constant("GET"))
				.setHeader(CAMEL_HTTP_URI, constant(randomNameUrl))
				.to(DUMMY_URL)
				.choice()
					.when(simple(IS_OK_HTTPCODE))
						.log("Got a 200 response from the server")
						.log("Body response: ${body}")
						.process(new BodyIntoListProcessor())
						.log("Body processed: ${body}")
						.to("direct:getGenderAndAgeStep")
					.endChoice()
					.otherwise()
						.log("Error accessing API for names")
				.end()
		.end();

		from("direct:getGenderAndAgeStep").routeId("getGenderAndAgeRoute").streamCaching()
				.log("Splitting data to process each member")
				.split(body()).stopOnException()
					.setProperty("currentPerson", body())
					.log("Current person: ${exchangeProperty.currentPerson}")
					.to("direct:getGenderRoute")
					.to("direct:getAgeRoute")
		.end();

		from("direct:getGenderRoute").routeId("getGenderRoute").streamCaching()
				.log("Get gender for: ${exchangeProperty.currentPerson}")
				.removeHeaders("*")
				.setHeader(CAMEL_HTTP_METHOD, constant("GET"))
				.setHeader(CAMEL_HTTP_URI, simple(env.getProperty("api.url.predict-gender").replace( "::NAME::", "${exchangeProperty.currentPerson}")))
				.to(DUMMY_URL)
				.choice()
					.when(simple(IS_OK_HTTPCODE))
						.log("New body for gender: ${body}")
						.setProperty("personGender", body())
					.endChoice()
					.otherwise()
						.log("Problem accessing to gender guessing API")
				.end()
		.end();

		from("direct:getAgeRoute").routeId("getAgeRoute").streamCaching()
				.log("Get age for: ${exchangeProperty.currentPerson}")
				.removeHeaders("*")
				.setHeader(CAMEL_HTTP_METHOD, constant("GET"))
				.setHeader(CAMEL_HTTP_URI, simple(predictAgeUrl.replace( "::NAME::", "${exchangeProperty.currentPerson}")))
				.to(DUMMY_URL)
					.choice()
					.when(simple(IS_OK_HTTPCODE))
						.log("New body for age: ${body}")
						.setProperty("personAge", body())
					.endChoice()
					.otherwise()
						.log("Problem accessing to gender guessing API")
					.end()
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
