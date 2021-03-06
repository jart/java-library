package com.urbanairship.api.client.parse;

import com.urbanairship.api.client.model.APIPushResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class APIPushResponseTest {

    @Test
    public void testAPIPushResponse() {
        String pushJSON = "{\n" +
                "    \"ok\" : true,\n" +
                "    \"operation_id\" : \"df6a6b50\",\n" +
                "    \"push_ids\": [\n" +
                "        \"id1\",\n" +
                "        \"id2\"\n" +
                "    ],\n" +
                "    \"message_ids\": [],\n" +
                "    \"content_urls\" : []\n" +
            "}";

        ObjectMapper mapper = APIResponseObjectMapper.getInstance();
        try {
            APIPushResponse response = mapper.readValue(pushJSON, APIPushResponse.class);
            assertTrue("Error in response operationId",
                    response.getOperationId().get().equals("df6a6b50"));
            assertTrue("Error in response pushIds",
                    response.getPushIds().get().get(0).equals("id1"));
            assertTrue("Error in response pushIds",
                    response.getPushIds().get().get(1).equals("id2"));
            assertTrue("Error in response status",
                    response.getOk());
            assertTrue("Error in response messageIds",
                response.getMessageIds().get().isEmpty());
            assertTrue("Error in response contentUrls",
                response.getContentUrls().get().isEmpty());
        } catch (IOException ex) {
            fail("Exception in APIPushResponseTest Message: " + ex.getMessage());
        }
    }
}
