package com.diego.molina.processors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.lang.reflect.Type;
import java.util.Map;

public class ObjectTemplateProcessor implements Processor{
    @Override
    public void process(Exchange exchange) throws Exception {
        String body                          = exchange.getIn().getBody(String.class);

        Type type = new TypeToken<Map<String, Object>>(){}.getType();

        Map<String, Object> ageGuessing      = new Gson().fromJson(exchange.getProperty("personAge", String.class), type);
        Map<String, Object> genderGuessing   = new Gson().fromJson(exchange.getProperty("personGender", String.class), type);

        Double age = (Double) ageGuessing.get("age");
        String gender = genderGuessing.get("gender").toString();
        Double gender_prob = (Double) genderGuessing.get("probability");
        body = body.replace("::NAME::", ageGuessing.get("name").toString());
        body = body.replace("::AGE::", age.toString())
                .replace("::GENDER::", gender)
                .replace("::GENDER_PROB::", gender_prob.toString()
        );

        exchange.getIn().setBody(body);
    }
}
