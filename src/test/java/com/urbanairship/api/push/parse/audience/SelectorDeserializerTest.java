package com.urbanairship.api.push.parse.audience;

import com.urbanairship.api.push.parse.PushObjectMapper;
import com.urbanairship.api.common.parse.APIParsingException;
import com.urbanairship.api.push.model.audience.Selector;
import com.urbanairship.api.push.model.audience.ValueSelector;
import com.urbanairship.api.push.model.audience.CompoundSelector;
import com.urbanairship.api.push.model.audience.SelectorType;
import com.google.common.collect.Iterables;
import org.codehaus.jackson.map.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Iterator;
import java.util.UUID;

public class SelectorDeserializerTest {
    private static final ObjectMapper mapper = PushObjectMapper.getInstance();
    private static final Logger log = LogManager.getLogger(SelectorDeserializerTest.class);

    @Test
    public void testDeserializeBroadcast() throws Exception {
        Selector value = mapper.readValue("\"all\"", Selector.class);
        assertEquals(value.getType(), SelectorType.ALL);
    }

    @Test
    public void testDeserializeTriggered() throws Exception {
        Selector value = mapper.readValue("\"triggered\"", Selector.class);
        assertEquals(value.getType(), SelectorType.TRIGGERED);
    }

    @Test
    public void testDeserializeTag() throws Exception {
        Selector value = mapper.readValue("{ \"tag\" : \"derp\" }", Selector.class);
        assertTrue(value.getType() == SelectorType.TAG);
        assertTrue(value instanceof ValueSelector);
    }

    @Test
    public void testTagClass() throws Exception {
        String json = "{\n"
            + "  \"tag\" : \"1\",\n"
            + "  \"tag_class\" : \"autogroup\"\n"
            + "}";
        Selector value = mapper.readValue(json, Selector.class);
        assertTrue(value.getType() == SelectorType.TAG);
        assertTrue(value instanceof ValueSelector);
        ValueSelector vs = (ValueSelector)value;
        assertTrue(vs.getAttributes().isPresent());
        assertEquals(1, vs.getAttributes().get().size());
        assertEquals("autogroup", vs.getAttributes().get().get("tag_class"));
    }


    @Test
    public void testAtomicCaseInsensitivity() throws Exception {
        assertEquals(SelectorType.ALL, mapper.readValue("\"all\"", Selector.class).getType());
        assertEquals(SelectorType.ALL, mapper.readValue("\"ALL\"", Selector.class).getType());
        assertEquals(SelectorType.ALL, mapper.readValue("\"aLl\"", Selector.class).getType());
        assertEquals(SelectorType.TRIGGERED, mapper.readValue("\"triggered\"", Selector.class).getType());
        assertEquals(SelectorType.TRIGGERED, mapper.readValue("\"TRIGGERED\"", Selector.class).getType());
        assertEquals(SelectorType.TRIGGERED, mapper.readValue("\"trIGGeRed\"", Selector.class).getType());
    }

    @Test
    public void testCompoundSelector() throws Exception {
        String json = "{\n"
            + "  \"and\" : [\n"
            + "    { \"tag\" : \"herp\" }, \n"
            + "    { \"tag\" : \"derp\" } \n"
            + "  ]\n"
            + "}";
        Selector s = mapper.readValue(json, Selector.class);
        assertTrue(s instanceof CompoundSelector);
        assertEquals(SelectorType.AND, s.getType());

        CompoundSelector cs = (CompoundSelector)s;
        assertEquals(2, Iterables.size(cs.getChildren()));

        Iterator<Selector> i = cs.getChildren().iterator();

        Selector c = i.next();
        assertTrue(c instanceof ValueSelector);
        ValueSelector vs = (ValueSelector)c;
        assertEquals(SelectorType.TAG, c.getType());
        assertEquals("herp", vs.getValue());

        c = i.next();
        assertTrue(c instanceof ValueSelector);
        vs = (ValueSelector)c;
        assertEquals(SelectorType.TAG, c.getType());
        assertEquals("derp", vs.getValue());
    }

    @Test
    public void testNOT() throws Exception {
        String json = "{"
            + "  \"not\" : {"
            + "    \"tag\" : \"derp\""
            + "  }"
            + "}";

        Selector s = mapper.readValue(json, Selector.class);
        assertTrue(s instanceof CompoundSelector);
        assertEquals(SelectorType.NOT, s.getType());
        CompoundSelector cs = (CompoundSelector)s;
        assertEquals(1, Iterables.size(cs.getChildren()));
    }

    @Test
    public void testImplicitOR() throws Exception {
        String json = "{\n"
            + "  \"tag\": [\n"
            + "    \"Joy\",\n"
            + "    \"Division\",\n"
            + "    \"New\",\n"
            + "    \"Order\"\n"
            + "  ]\n"
            + "}";

        Selector s = mapper.readValue(json, Selector.class);
        assertTrue(s instanceof CompoundSelector);
        assertEquals(SelectorType.OR, s.getType());

        CompoundSelector cs = (CompoundSelector)s;
        assertEquals(4, Iterables.size(cs.getChildren()));

        Iterator<Selector> i = cs.getChildren().iterator();

        s = i.next();
        assertTrue(s instanceof ValueSelector);
        assertEquals(SelectorType.TAG, s.getType());
        assertEquals("Joy", ((ValueSelector)s).getValue());

        s = i.next();
        assertTrue(s instanceof ValueSelector);
        assertEquals(SelectorType.TAG, s.getType());
        assertEquals("Division", ((ValueSelector)s).getValue());

        s = i.next();
        assertTrue(s instanceof ValueSelector);
        assertEquals(SelectorType.TAG, s.getType());
        assertEquals("New", ((ValueSelector)s).getValue());

        s = i.next();
        assertTrue(s instanceof ValueSelector);
        assertEquals(SelectorType.TAG, s.getType());
        assertEquals("Order", ((ValueSelector)s).getValue());
    }

    @Test
    public void testImplicitORForPlatform() throws Exception {
        String apid1 = UUID.randomUUID().toString();
        String apid2 = UUID.randomUUID().toString();

        String json = "{\n"
                + "  \"apid\": [\n"
                + "    \""+apid1+"\",\n"
                + "    \""+apid2+"\"\n"
                + "  ]\n"
                + "}";

        Selector s = mapper.readValue(json, Selector.class);
        assertTrue(s instanceof CompoundSelector);
        assertEquals(SelectorType.OR, s.getType());

        CompoundSelector cs = (CompoundSelector)s;
        assertEquals(2, Iterables.size(cs.getChildren()));

        Iterator<Selector> i = cs.getChildren().iterator();

        s = i.next();
        assertTrue(s instanceof ValueSelector);
        assertEquals(SelectorType.APID, s.getType());
        assertEquals(apid1, ((ValueSelector)s).getValue());

        s = i.next();
        assertTrue(s instanceof ValueSelector);
        assertEquals(SelectorType.APID, s.getType());
        assertEquals(apid2, ((ValueSelector)s).getValue());

    }

    @Test
    public void testNestedCompound() throws Exception {
        String json = "{\n"
            + "  \"and\" : [\n"
            + "    { \"or\" : [\n"
            + "        { \"alias\" : \"s1\" },\n"
            + "        { \"alias\" : \"s2\" }\n"
            + "      ] },\n"
            + "    { \"or\" : [\n"
            + "        { \"tag\" : \"t1\" },\n"
            + "        { \"tag\" : \"t2\" }\n"
            + "      ] }\n"
            + "  ]\n"
            + "}";
        Selector s = mapper.readValue(json, Selector.class);
        assertEquals(SelectorType.AND, s.getType());

        assertEquals(2, Iterables.size(((CompoundSelector)s).getChildren()));

        Iterator<Selector> i = ((CompoundSelector)s).getChildren().iterator();
        Selector c = i.next();
        assertTrue(c instanceof CompoundSelector);
        assertEquals(SelectorType.OR, c.getType());
        assertEquals(SelectorType.ALIAS, ((CompoundSelector)c).getChildren().iterator().next().getType());
    }

    @Test
    public void testCase() throws Exception {
        String json = "{\"and\" : [{\"tag\" : \"t1\"}]}";
        Selector s = mapper.readValue(json, Selector.class);
        assertEquals(SelectorType.AND, s.getType());


        json = "{\"AND\" : [{\"TAG\" : \"t1\"}]}";
        s = mapper.readValue(json, Selector.class);
        assertEquals(SelectorType.AND, s.getType());

        json = "{\"Or\" : [{\"tag\" : \"t1\"}]}";
        s = mapper.readValue(json, Selector.class);
        assertEquals(SelectorType.OR, s.getType());
    }

    /*
     * Illegal expressions
     */

    @Test(expected=APIParsingException.class)
    public void testInvalidAtomicSelector() throws Exception {
        mapper.readValue("\"derped\"", Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testAtomicWithArgument() throws Exception {
        mapper.readValue("{ \"all\" : \"some\" }", Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testBadTagValue() throws Exception {
        mapper.readValue("{ \"tag\" : 10 }", Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testUnknownSelectorType() throws Exception {
        mapper.readValue("{ \"derp\" : \"value\" }", Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testAtomicNestedInCompound() throws Exception {
        String json = "{\n"
            + "  \"or\" : [\n"
            + "    \"all\",\n"
            + "    \"triggered\"\n"
            + "  ]\n"
            + "}";
        mapper.readValue(json, Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testEmptyCompoundExpression() throws Exception {
        mapper.readValue("{ \"OR\" : [ ] }", Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testTooManyArgumentsToNOT() throws Exception {
        String json = "{\n"
            + "  \"not\" : [\n"
            + "    { \"tag\" : \"wat\" },\n"
            + "    { \"tag\" : \"derp\" }\n"
            + "  ]\n"
            + "}";
        mapper.readValue(json, Selector.class);
    }

    @Test(expected=APIParsingException.class)
    public void testBadImplicitOR() throws Exception {
        String json = "{\n"
            + "  \"alias\" : [\n"
            + "    \"seg1\",\n"
            + "    { \"tag\" : \"whoops\" }\n"
            + "  ]\n"
            + "}";
        mapper.readValue(json, Selector.class);
    }
}
