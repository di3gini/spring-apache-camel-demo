package com.diego.molina.processors;

import com.google.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BodyIntoListProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        List<Map<String, Object>> namesList = new Gson().fromJson(body, new ArrayList<Map<String, Object>>().getClass());
        exchange.getIn().setBody(namesList);
    }
}

