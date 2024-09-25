package com.mee.quartz.test.json;

/*
Public Domain.
*/

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.quartz.json.JSONArray;
import org.quartz.json.JSONException;
import org.quartz.json.JSONObject;
import org.quartz.json.JSONParserConfiguration;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * JSONObject, along with JSONArray, are the central classes of the reference app.
 * All of the other classes interact with them, and JSON functionality would
 * otherwise be impossible.
 */
public class JSONObjectTest {

    /**
     *  Regular Expression Pattern that matches JSON Numbers. This is primarily used for
     *  output to guarantee that we are always writing valid JSON. 
     */
    static final Pattern NUMBER_PATTERN = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    /**
     * Tests that the similar method is working as expected.
     */
    @Test
    public void verifySimilar() {
        final String string1 = "HasSameRef";
        final String string2 = "HasDifferentRef";
        JSONObject obj1 = new JSONObject()
                .put("key1", "abc")
                .put("key2", 2)
                .put("key3", string1);
        String s = obj1.toString();
        System.out.println(s);
    }


    /**
     * The JSON parser is permissive of unambiguous unquoted keys and values.
     * Such JSON text should be allowed, even if it does not strictly conform
     * to the spec. However, after being parsed, toString() should emit strictly
     * conforming JSON text.  
     */
    @Test
    public void unquotedText() {
        String str = "{key1:value1, key2:42, 1.2 : 3.4, -7e5 : something!}";
        JSONObject jsonObject = new JSONObject(str);
        String textStr = jsonObject.toString();
        System.out.println(textStr);
    }
    

    /**
     * A JSONObject can be created from another JSONObject plus a list of names.
     * In this test, some of the starting JSONObject keys are not in the 
     * names list.
     */
    @Test
    public void jsonObjectByNames() {
        String str = 
            "{"+
                "\"trueKey\":true,"+
                "\"falseKey\":false,"+
                "\"nullKey\":null,"+
                "\"stringKey\":\"hello world!\","+
                "\"escapeStringKey\":\"h\be\tllo w\u1234orld!\","+
                "\"intKey\":42,"+
                "\"doubleKey\":-23.45e67"+
            "}";
        String[] keys = {"falseKey", "stringKey", "nullKey", "doubleKey"};
        JSONObject jsonObject = new JSONObject(str);

        // validate JSON
        JSONObject jsonObjectByName = new JSONObject(jsonObject, keys);
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObjectByName.toString());
        assertTrue("expected 4 top level items", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 4);
        assertTrue("expected \"falseKey\":false", Boolean.FALSE.equals(jsonObjectByName.query("/falseKey")));
        assertTrue("expected \"nullKey\":null", JSONObject.NULL.equals(jsonObjectByName.query("/nullKey")));
        assertTrue("expected \"stringKey\":\"hello world!\"", "hello world!".equals(jsonObjectByName.query("/stringKey")));
        assertTrue("expected \"doubleKey\":-23.45e67", new BigDecimal("-23.45e67").equals(jsonObjectByName.query("/doubleKey")));
    }



    /**
     * JSONObjects can be built from a Map<String, Object>. 
     * In this test one of the map values is null 
     */
    @Test
    public void jsonObjectByMapWithNullValue() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trueKey", Boolean.valueOf(true));
        map.put("falseKey", Boolean.valueOf(false));
        map.put("stringKey", "hello world!");
        map.put("nullKey", null);
        map.put("escapeStringKey", "h\be\tllo w\u1234orld!");
        map.put("intKey", Long.valueOf(42));
        map.put("doubleKey", Double.valueOf(-23.45e67));
        JSONObject jsonObject = new JSONObject(map);

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 6 top level items", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 6);
        assertTrue("expected \"trueKey\":true", Boolean.TRUE.equals(jsonObject.query("/trueKey")));
        assertTrue("expected \"falseKey\":false", Boolean.FALSE.equals(jsonObject.query("/falseKey")));
        assertTrue("expected \"stringKey\":\"hello world!\"", "hello world!".equals(jsonObject.query("/stringKey")));
        assertTrue("expected \"escapeStringKey\":\"h\be\tllo w\u1234orld!\"", "h\be\tllo w\u1234orld!".equals(jsonObject.query("/escapeStringKey")));
        assertTrue("expected \"intKey\":42", Long.valueOf("42").equals(jsonObject.query("/intKey")));
        assertTrue("expected \"doubleKey\":-23.45e67", Double.valueOf("-23.45e67").equals(jsonObject.query("/doubleKey")));
    }

    /**
     * Exercise the JSONObject append() functionality
     */
    @SuppressWarnings("boxing")
    @Test
    public void jsonObjectAppend() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("myArray", true);
        jsonObject.append("myArray", false);
        jsonObject.append("myArray", "hello world!");
        jsonObject.append("myArray", "h\be\tllo w\u1234orld!");
        jsonObject.append("myArray", 42);
        jsonObject.append("myArray", -23.45e7);
        // include an unsupported object for coverage
        try {
            jsonObject.append("myArray", Double.NaN);
        } catch (JSONException ignored) {}

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 1 top level item", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 1);
        assertTrue("expected 6 myArray items", ((List<?>)(JsonPath.read(doc, "$.myArray"))).size() == 6);
        assertTrue("expected true", Boolean.TRUE.equals(jsonObject.query("/myArray/0")));
        assertTrue("expected false", Boolean.FALSE.equals(jsonObject.query("/myArray/1")));
        assertTrue("expected hello world!", "hello world!".equals(jsonObject.query("/myArray/2")));
        assertTrue("expected h\be\tllo w\u1234orld!", "h\be\tllo w\u1234orld!".equals(jsonObject.query("/myArray/3")));
        assertTrue("expected 42", Integer.valueOf(42).equals(jsonObject.query("/myArray/4")));
        assertTrue("expected -23.45e7", Double.valueOf(-23.45e7).equals(jsonObject.query("/myArray/5")));
    }

    /**
     * Exercise some JSONObject get[type] and opt[type] methods
     */
    @Test
    public void jsonObjectValues() {
        String str = 
            "{"+
                "\"trueKey\":true,"+
                "\"falseKey\":false,"+
                "\"trueStrKey\":\"true\","+
                "\"falseStrKey\":\"false\","+
                "\"stringKey\":\"hello world!\","+
                "\"intKey\":42,"+
                "\"intStrKey\":\"43\","+
                "\"longKey\":1234567890123456789,"+
                "\"longStrKey\":\"987654321098765432\","+
                "\"doubleKey\":-23.45e7,"+
                "\"doubleStrKey\":\"00001.000\","+
                "\"BigDecimalStrKey\":\"19007199254740993.35481234487103587486413587843213584\","+
                "\"negZeroKey\":-0.0,"+
                "\"negZeroStrKey\":\"-0.0\","+
                "\"arrayKey\":[0,1,2],"+
                "\"objectKey\":{\"myKey\":\"myVal\"}"+
            "}";
        JSONObject jsonObject = new JSONObject(str);
        System.out.println(jsonObject);
    }

    /**
     * Check whether JSONObject handles large or high precision numbers correctly
     */
    @Test
    public void stringToValueNumbersTest() {
        assertTrue("-0 Should be a Double!",JSONObject.stringToValue("-0")  instanceof Double);
        assertTrue("-0.0 Should be a Double!",JSONObject.stringToValue("-0.0") instanceof Double);
        assertTrue("'-' Should be a String!",JSONObject.stringToValue("-") instanceof String);
        assertTrue( "0.2 should be a BigDecimal!",
                JSONObject.stringToValue( "0.2" ) instanceof BigDecimal );
        assertTrue( "Doubles should be BigDecimal, even when incorrectly converting floats!",
                JSONObject.stringToValue( Double.valueOf( "0.2f" ).toString() ) instanceof BigDecimal );
        /**
         * This test documents a need for BigDecimal conversion.
         */
        Object obj = JSONObject.stringToValue( "299792.457999999984" );
        assertTrue( "does not evaluate to 299792.457999999984 BigDecimal!",
                 obj.equals(new BigDecimal("299792.457999999984")) );
        assertTrue( "1 should be an Integer!",
                JSONObject.stringToValue( "1" ) instanceof Integer );
        assertTrue( "Integer.MAX_VALUE should still be an Integer!",
                JSONObject.stringToValue( Integer.valueOf( Integer.MAX_VALUE ).toString() ) instanceof Integer );
        assertTrue( "Large integers should be a Long!",
                JSONObject.stringToValue( Long.valueOf(((long)Integer.MAX_VALUE) + 1 ) .toString() ) instanceof Long );
        assertTrue( "Long.MAX_VALUE should still be an Integer!",
                JSONObject.stringToValue( Long.valueOf( Long.MAX_VALUE ).toString() ) instanceof Long );

        String str = new BigInteger( Long.valueOf( Long.MAX_VALUE ).toString() ).add( BigInteger.ONE ).toString();
        assertTrue( "Really large integers currently evaluate to BigInteger",
                JSONObject.stringToValue(str).equals(new BigInteger("9223372036854775808")));
    }

    /**
     * This test documents numeric values which could be numerically
     * handled as BigDecimal or BigInteger. It helps determine what outputs
     * will change if those types are supported.
     */
    @Test
    public void jsonValidNumberValuesNeitherLongNorIEEE754Compatible() {
        // Valid JSON Numbers, probably should return BigDecimal or BigInteger objects
        String str = 
            "{"+
                "\"numberWithDecimals\":299792.457999999984,"+
                "\"largeNumber\":12345678901234567890,"+
                "\"preciseNumber\":0.2000000000000000111,"+
                "\"largeExponent\":-23.45e2327"+
            "}";
        JSONObject jsonObject = new JSONObject(str);
        // Comes back as a double, but loses precision
        assertTrue( "numberWithDecimals currently evaluates to double 299792.458",
                jsonObject.get( "numberWithDecimals" ).equals( new BigDecimal( "299792.457999999984" ) ) );
        Object obj = jsonObject.get( "largeNumber" );
        assertTrue("largeNumber currently evaluates to BigInteger",
                new BigInteger("12345678901234567890").equals(obj));

    }




    /**
     * This test documents an unexpected numeric behavior.
     * A double that ends with .0 is parsed, serialized, then
     * parsed again. On the second parse, it has become an int.
     */
    @Test
    public void unexpectedDoubleToIntConversion() {
        String key30 = "key30";
        String key31 = "key31";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key30, Double.valueOf(3.0));
        jsonObject.put(key31, Double.valueOf(3.1));

        assertTrue("3.0 should remain a double",
                jsonObject.getDouble(key30) == 3); 
        assertTrue("3.1 should remain a double",
                jsonObject.getDouble(key31) == 3.1); 
 
        // turns 3.0 into 3.
        String serializedString = jsonObject.toString();
        JSONObject deserialized = new JSONObject(serializedString);
        assertTrue("3.0 is now an int", deserialized.get(key30) instanceof Integer);
        assertTrue("3.0 can still be interpreted as a double",
                deserialized.getDouble(key30) == 3.0);
        assertTrue("3.1 remains a double", deserialized.getDouble(key31) == 3.1);
    }


    /**
     * Populate a JSONArray from a JSONObject names() method.
     * Confirm that it contains the expected names.
     */
    @Test
    public void jsonObjectNamesToJsonAray() {
        String str = 
            "{"+
                "\"trueKey\":true,"+
                "\"falseKey\":false,"+
                "\"stringKey\":\"hello world!\","+
            "}";

        JSONObject jsonObject = new JSONObject(str);
        JSONArray jsonArray = jsonObject.names();

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonArray.toString());
        assertTrue("expected 3 top level items", ((List<?>)(JsonPath.read(doc, "$"))).size() == 3);
        assertTrue("expected to find trueKey", ((List<?>) JsonPath.read(doc, "$[?(@=='trueKey')]")).size() == 1);
        assertTrue("expected to find falseKey", ((List<?>) JsonPath.read(doc, "$[?(@=='falseKey')]")).size() == 1);
        assertTrue("expected to find stringKey", ((List<?>) JsonPath.read(doc, "$[?(@=='stringKey')]")).size() == 1);
    }

    /**
     * Exercise the JSONObject increment() method.
     */
    @SuppressWarnings("cast")
    @Test
    public void jsonObjectIncrement() {
        String str = 
            "{"+
                "\"keyLong\":9999999991,"+
                "\"keyDouble\":1.1"+
             "}";
        JSONObject jsonObject = new JSONObject(str);
        jsonObject.increment("keyInt");
        jsonObject.increment("keyInt");
        jsonObject.increment("keyLong");
        jsonObject.increment("keyDouble");
        jsonObject.increment("keyInt");
        jsonObject.increment("keyLong");
        jsonObject.increment("keyDouble");
        /**
         * JSONObject constructor won't handle these types correctly, but
         * adding them via put works.
         */
        jsonObject.put("keyFloat", 1.1f);
        jsonObject.put("keyBigInt", new BigInteger("123456789123456789123456789123456780"));
        jsonObject.put("keyBigDec", new BigDecimal("123456789123456789123456789123456780.1"));
        jsonObject.increment("keyFloat");
        jsonObject.increment("keyFloat");
        jsonObject.increment("keyBigInt");
        jsonObject.increment("keyBigDec");

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 6 top level items", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 6);
        assertTrue("expected 3", Integer.valueOf(3).equals(jsonObject.query("/keyInt")));
        assertTrue("expected 9999999993", Long.valueOf(9999999993L).equals(jsonObject.query("/keyLong")));
        assertTrue("expected 3.1", BigDecimal.valueOf(3.1).equals(jsonObject.query("/keyDouble")));
        assertTrue("expected 123456789123456789123456789123456781", new BigInteger("123456789123456789123456789123456781").equals(jsonObject.query("/keyBigInt")));
        assertTrue("expected 123456789123456789123456789123456781.1", new BigDecimal("123456789123456789123456789123456781.1").equals(jsonObject.query("/keyBigDec")));

    }

    /**
     * Exercise JSONObject numberToString() method
     */
    @SuppressWarnings("boxing")
    @Test
    public void jsonObjectNumberToString() {
        String str;
        Double dVal;
        Integer iVal = 1;
        str = JSONObject.numberToString(iVal);
        assertTrue("expected "+iVal+" actual "+str, iVal.toString().equals(str));
        dVal = 12.34;
        str = JSONObject.numberToString(dVal);
        assertTrue("expected "+dVal+" actual "+str, dVal.toString().equals(str));
        dVal = 12.34e27;
        str = JSONObject.numberToString(dVal);
        assertTrue("expected "+dVal+" actual "+str, dVal.toString().equals(str));
        // trailing .0 is truncated, so it doesn't quite match toString()
        dVal = 5000000.0000000;
        str = JSONObject.numberToString(dVal);
        assertTrue("expected 5000000 actual "+str, str.equals("5000000"));
    }

    /**
     * Exercise JSONObject put() and similar() methods
     */
    @SuppressWarnings("boxing")
    @Test
    public void jsonObjectPut() {
        String expectedStr = 
            "{"+
                "\"trueKey\":true,"+
                "\"falseKey\":false,"+
                "\"arrayKey\":[0,1,2],"+
                "\"objectKey\":{"+
                    "\"myKey1\":\"myVal1\","+
                    "\"myKey2\":\"myVal2\","+
                    "\"myKey3\":\"myVal3\","+
                    "\"myKey4\":\"myVal4\""+
                "}"+
            "}";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("trueKey", true);
        jsonObject.put("falseKey", false);
        Integer [] intArray = { 0, 1, 2 };
        jsonObject.put("arrayKey", Arrays.asList(intArray));
        Map<String, Object> myMap = new HashMap<String, Object>();
        myMap.put("myKey1", "myVal1");
        myMap.put("myKey2", "myVal2");
        myMap.put("myKey3", "myVal3");
        myMap.put("myKey4", "myVal4");
        jsonObject.put("objectKey", myMap);

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 4 top level items", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 4);
        assertTrue("expected true", Boolean.TRUE.equals(jsonObject.query("/trueKey")));
        assertTrue("expected false", Boolean.FALSE.equals(jsonObject.query("/falseKey")));
        assertTrue("expected 3 arrayKey items", ((List<?>)(JsonPath.read(doc, "$.arrayKey"))).size() == 3);
        assertTrue("expected 0", Integer.valueOf(0).equals(jsonObject.query("/arrayKey/0")));
        assertTrue("expected 1", Integer.valueOf(1).equals(jsonObject.query("/arrayKey/1")));
        assertTrue("expected 2", Integer.valueOf(2).equals(jsonObject.query("/arrayKey/2")));
        assertTrue("expected 4 objectKey items", ((Map<?,?>)(JsonPath.read(doc, "$.objectKey"))).size() == 4);
        assertTrue("expected myVal1", "myVal1".equals(jsonObject.query("/objectKey/myKey1")));
        assertTrue("expected myVal2", "myVal2".equals(jsonObject.query("/objectKey/myKey2")));
        assertTrue("expected myVal3", "myVal3".equals(jsonObject.query("/objectKey/myKey3")));
        assertTrue("expected myVal4", "myVal4".equals(jsonObject.query("/objectKey/myKey4")));

        jsonObject.remove("trueKey");
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        assertTrue("unequal jsonObjects should not be similar",
                !jsonObject.similar(expectedJsonObject));
        assertTrue("jsonObject should not be similar to jsonArray",
                !jsonObject.similar(new JSONArray()));

        String aCompareValueStr = "{\"a\":\"aval\",\"b\":true}";
        String bCompareValueStr = "{\"a\":\"notAval\",\"b\":true}";
        JSONObject aCompareValueJsonObject = new JSONObject(aCompareValueStr);
        JSONObject bCompareValueJsonObject = new JSONObject(bCompareValueStr);
        assertTrue("different values should not be similar",
                !aCompareValueJsonObject.similar(bCompareValueJsonObject));

        String aCompareObjectStr = "{\"a\":\"aval\",\"b\":{}}";
        String bCompareObjectStr = "{\"a\":\"aval\",\"b\":true}";
        JSONObject aCompareObjectJsonObject = new JSONObject(aCompareObjectStr);
        JSONObject bCompareObjectJsonObject = new JSONObject(bCompareObjectStr);
        assertTrue("different nested JSONObjects should not be similar",
                !aCompareObjectJsonObject.similar(bCompareObjectJsonObject));

        String aCompareArrayStr = "{\"a\":\"aval\",\"b\":[]}";
        String bCompareArrayStr = "{\"a\":\"aval\",\"b\":true}";
        JSONObject aCompareArrayJsonObject = new JSONObject(aCompareArrayStr);
        JSONObject bCompareArrayJsonObject = new JSONObject(bCompareArrayStr);
        assertTrue("different nested JSONArrays should not be similar",
                !aCompareArrayJsonObject.similar(bCompareArrayJsonObject));

    }

    /**
     * Exercise JSONObject toString() method
     */
    @Test
    public void jsonObjectToString() {
        String str = 
            "{"+
                "\"trueKey\":true,"+
                "\"falseKey\":false,"+
                "\"arrayKey\":[0,1,2],"+
                "\"objectKey\":{"+
                    "\"myKey1\":\"myVal1\","+
                    "\"myKey2\":\"myVal2\","+
                    "\"myKey3\":\"myVal3\","+
                    "\"myKey4\":\"myVal4\""+
                "}"+
            "}";
        JSONObject jsonObject = new JSONObject(str);

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 4 top level items", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 4);
        assertTrue("expected true", Boolean.TRUE.equals(jsonObject.query("/trueKey")));
        assertTrue("expected false", Boolean.FALSE.equals(jsonObject.query("/falseKey")));
        assertTrue("expected 3 arrayKey items", ((List<?>)(JsonPath.read(doc, "$.arrayKey"))).size() == 3);
        assertTrue("expected 0", Integer.valueOf(0).equals(jsonObject.query("/arrayKey/0")));
        assertTrue("expected 1", Integer.valueOf(1).equals(jsonObject.query("/arrayKey/1")));
        assertTrue("expected 2", Integer.valueOf(2).equals(jsonObject.query("/arrayKey/2")));
        assertTrue("expected 4 objectKey items", ((Map<?,?>)(JsonPath.read(doc, "$.objectKey"))).size() == 4);
        assertTrue("expected myVal1", "myVal1".equals(jsonObject.query("/objectKey/myKey1")));
        assertTrue("expected myVal2", "myVal2".equals(jsonObject.query("/objectKey/myKey2")));
        assertTrue("expected myVal3", "myVal3".equals(jsonObject.query("/objectKey/myKey3")));
        assertTrue("expected myVal4", "myVal4".equals(jsonObject.query("/objectKey/myKey4")));
    }

    /**
     * Exercise JSONObject toString() method with various indent levels.
     */
    @Test
    public void jsonObjectToStringIndent() {
        String jsonObject0Str =
                "{"+
                        "\"key1\":" +
                                "[1,2," +
                                        "{\"key3\":true}" +
                                "],"+
                        "\"key2\":" +
                                "{\"key1\":\"val1\",\"key2\":" +
                                        "{\"key2\":\"val2\"}" +
                                "},"+
                        "\"key3\":" +
                                "[" +
                                        "[1,2.1]" +
                                "," +
                                        "[null]" +
                                "]"+
                        "}";

        String jsonObject1Str =
                "{\n" +
                " \"key1\": [\n" +
                "  1,\n" +
                "  2,\n" +
                "  {\"key3\": true}\n" +
                " ],\n" +
                " \"key2\": {\n" +
                "  \"key1\": \"val1\",\n" +
                "  \"key2\": {\"key2\": \"val2\"}\n" +
                " },\n" +
                " \"key3\": [\n" +
                "  [\n" +
                "   1,\n" +
                "   2.1\n" +
                "  ],\n" +
                "  [null]\n" +
                " ]\n" +
                "}";
        String jsonObject4Str =
                "{\n" +
                "    \"key1\": [\n" +
                "        1,\n" +
                "        2,\n" +
                "        {\"key3\": true}\n" +
                "    ],\n" +
                "    \"key2\": {\n" +
                "        \"key1\": \"val1\",\n" +
                "        \"key2\": {\"key2\": \"val2\"}\n" +
                "    },\n" +
                "    \"key3\": [\n" +
                "        [\n" +
                "            1,\n" +
                "            2.1\n" +
                "        ],\n" +
                "        [null]\n" +
                "    ]\n" +
                "}";
        JSONObject jsonObject = new JSONObject(jsonObject0Str);
        System.out.println(jsonObject4Str);
    }

    /**
     * Explores how JSONObject handles maps. Insert a string/string map
     * as a value in a JSONObject. It will remain a map. Convert the 
     * JSONObject to string, then create a new JSONObject from the string. 
     * In the new JSONObject, the value will be stored as a nested JSONObject.
     * Confirm that map and nested JSONObject have the same contents.
     */
    @Test
    public void jsonObjectToStringSuppressWarningOnCastToMap() {
        JSONObject jsonObject = new JSONObject();
        Map<String, String> map = new HashMap<>();
        map.put("abc", "def");
        jsonObject.put("key", map);

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 1 top level item", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 1);
        assertTrue("expected 1 key item", ((Map<?,?>)(JsonPath.read(doc, "$.key"))).size() == 1);
        assertTrue("expected def", "def".equals(jsonObject.query("/key/abc")));
    }

    /**
     * Explores how JSONObject handles collections. Insert a string collection
     * as a value in a JSONObject. It will remain a collection. Convert the 
     * JSONObject to string, then create a new JSONObject from the string. 
     * In the new JSONObject, the value will be stored as a nested JSONArray.
     * Confirm that collection and nested JSONArray have the same contents.
     */
    @Test
    public void jsonObjectToStringSuppressWarningOnCastToCollection() {
        JSONObject jsonObject = new JSONObject();
        Collection<String> collection = new ArrayList<String>();
        collection.add("abc");
        // ArrayList will be added as an object
        jsonObject.put("key", collection);

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toString());
        assertTrue("expected 1 top level item", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 1);
        assertTrue("expected 1 key item", ((List<?>)(JsonPath.read(doc, "$.key"))).size() == 1);
        assertTrue("expected abc", "abc".equals(jsonObject.query("/key/0")));
    }

    /**
     * Exercise the JSONObject wrap() method. Sometimes wrap() will change
     * the object being wrapped, other times not. The purpose of wrap() is
     * to ensure the value is packaged in a way that is compatible with how
     * a JSONObject value or JSONArray value is supposed to be stored.
     */
    @Test
    public void wrapObject() {
        // wrap(null) returns NULL
        assertTrue("null wrap() incorrect",
                JSONObject.NULL == JSONObject.wrap(null));

        // wrap(Integer) returns Integer
        Integer in = Integer.valueOf(1);
        assertTrue("Integer wrap() incorrect",
                in == JSONObject.wrap(in));

        /**
         * This test is to document the preferred behavior if BigDecimal is
         * supported. Previously bd returned as a string, since it
         * is recognized as being a Java package class. Now with explicit
         * support for big numbers, it remains a BigDecimal 
         */
        Object bdWrap = JSONObject.wrap(BigDecimal.ONE);
        assertTrue("BigDecimal.ONE evaluates to ONE",
                bdWrap.equals(BigDecimal.ONE));

        // wrap JSONObject returns JSONObject
        String jsonObjectStr = 
                "{"+
                    "\"key1\":\"val1\","+
                    "\"key2\":\"val2\","+
                    "\"key3\":\"val3\""+
                 "}";
        JSONObject jsonObject = new JSONObject(jsonObjectStr);
        assertTrue("JSONObject wrap() incorrect",
                jsonObject == JSONObject.wrap(jsonObject));

        // wrap collection returns JSONArray
        Collection<Integer> collection = new ArrayList<Integer>();
        collection.add(Integer.valueOf(1));
        collection.add(Integer.valueOf(2));
        collection.add(Integer.valueOf(3));
        JSONArray jsonArray = (JSONArray) (JSONObject.wrap(collection));

        // validate JSON
        Object doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonArray.toString());
        assertTrue("expected 3 top level items", ((List<?>)(JsonPath.read(doc, "$"))).size() == 3);
        assertTrue("expected 1", Integer.valueOf(1).equals(jsonArray.query("/0")));
        assertTrue("expected 2", Integer.valueOf(2).equals(jsonArray.query("/1")));
        assertTrue("expected 3", Integer.valueOf(3).equals(jsonArray.query("/2")));

        // wrap Array returns JSONArray
        Integer[] array = { Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3) };
        JSONArray integerArrayJsonArray = (JSONArray)(JSONObject.wrap(array));

        // validate JSON
        doc = Configuration.defaultConfiguration().jsonProvider().parse(jsonArray.toString());
        assertTrue("expected 3 top level items", ((List<?>)(JsonPath.read(doc, "$"))).size() == 3);
        assertTrue("expected 1", Integer.valueOf(1).equals(jsonArray.query("/0")));
        assertTrue("expected 2", Integer.valueOf(2).equals(jsonArray.query("/1")));
        assertTrue("expected 3", Integer.valueOf(3).equals(jsonArray.query("/2")));

        // validate JSON
        doc = Configuration.defaultConfiguration().jsonProvider().parse(integerArrayJsonArray.toString());
        assertTrue("expected 3 top level items", ((List<?>)(JsonPath.read(doc, "$"))).size() == 3);
        assertTrue("expected 1", Integer.valueOf(1).equals(jsonArray.query("/0")));
        assertTrue("expected 2", Integer.valueOf(2).equals(jsonArray.query("/1")));
        assertTrue("expected 3", Integer.valueOf(3).equals(jsonArray.query("/2")));

        // wrap map returns JSONObject
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "val1");
        map.put("key2", "val2");
        map.put("key3", "val3");
        JSONObject mapJsonObject = (JSONObject) (JSONObject.wrap(map));

        // validate JSON
        doc = Configuration.defaultConfiguration().jsonProvider().parse(mapJsonObject.toString());
        assertTrue("expected 3 top level items", ((Map<?,?>)(JsonPath.read(doc, "$"))).size() == 3);
        assertTrue("expected val1", "val1".equals(mapJsonObject.query("/key1")));
        assertTrue("expected val2", "val2".equals(mapJsonObject.query("/key2")));
        assertTrue("expected val3", "val3".equals(mapJsonObject.query("/key3")));

    }


    /**
     * Confirm behavior when putOnce() is called with null parameters
     */
    @Test
    public void jsonObjectPutOnceNull() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOnce(null, null);
        assertTrue("jsonObject should be empty", jsonObject.isEmpty());
        jsonObject.putOnce("", null);
        assertTrue("jsonObject should be empty", jsonObject.isEmpty());
        jsonObject.putOnce(null, "");
        assertTrue("jsonObject should be empty", jsonObject.isEmpty());
    }



    /**
     * Exercise JSONObject quote() method
     * This purpose of quote() is to ensure that for strings with embedded
     * quotes, the quotes are properly escaped.
     */
    @Test
    public void jsonObjectQuote() {
        String str;
        str = "";
        String quotedStr;
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped quotes, found "+quotedStr,
                "\"\"".equals(quotedStr));
        str = "\"\"";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped quotes, found "+quotedStr,
                "\"\\\"\\\"\"".equals(quotedStr));
        str = "</";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped frontslash, found "+quotedStr,
                "\"<\\/\"".equals(quotedStr));
        str = "AB\bC";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped backspace, found "+quotedStr,
                "\"AB\\bC\"".equals(quotedStr));
        str = "ABC\n";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped newline, found "+quotedStr,
                "\"ABC\\n\"".equals(quotedStr));
        str = "AB\fC";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped formfeed, found "+quotedStr,
                "\"AB\\fC\"".equals(quotedStr));
        str = "\r";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped return, found "+quotedStr,
                "\"\\r\"".equals(quotedStr));
        str = "\u1234\u0088";
        quotedStr = JSONObject.quote(str);
        assertTrue("quote() expected escaped unicode, found "+quotedStr,
                "\"\u1234\\u0088\"".equals(quotedStr));
    }

    /**
     * Confirm behavior when JSONObject stringToValue() is called for an
     * empty string
     */
    @Test
    public void stringToValue() {
        String str = "";
        String valueStr = (String)(JSONObject.stringToValue(str));
        assertTrue("stringToValue() expected empty String, found "+valueStr,
                "".equals(valueStr));
    }

    /**
     * Confirm behavior when toJSONArray is called with a null value
     */
    @Test
    public void toJSONArray() {
        assertTrue("toJSONArray() with null names should be null",
                null == new JSONObject().toJSONArray(null));
    }

    /**
     * Exercise the JSONObject equals() method
     */
    @Test
    public void equals() {
        String str = "{\"key\":\"value\"}";
        JSONObject aJsonObject = new JSONObject(str);
        assertTrue("Same JSONObject should be equal to itself",
                aJsonObject.equals(aJsonObject));
    }


    /**
     * Exercise JSONObject toMap() method.
     */
    @Test
    public void toMap() {
        String jsonObjectStr =
                "{" +
                "\"key1\":" +
                    "[1,2," +
                        "{\"key3\":true}" +
                    "]," +
                "\"key2\":" +
                    "{\"key1\":\"val1\",\"key2\":" +
                        "{\"key2\":null}," +
                    "\"key3\":42" +
                    "}," +
                "\"key3\":" +
                    "[" +
                        "[\"value1\",2.1]" +
                    "," +
                        "[null]" +
                    "]" +
                "}";

        JSONObject jsonObject = new JSONObject(jsonObjectStr);
        Map<?,?> map = jsonObject.toMap();

        assertTrue("Map should not be null", map != null);
        assertTrue("Map should have 3 elements", map.size() == 3);

        List<?> key1List = (List<?>)map.get("key1");
        assertTrue("key1 should not be null", key1List != null);
        assertTrue("key1 list should have 3 elements", key1List.size() == 3);
        assertTrue("key1 value 1 should be 1", key1List.get(0).equals(Integer.valueOf(1)));
        assertTrue("key1 value 2 should be 2", key1List.get(1).equals(Integer.valueOf(2)));

        Map<?,?> key1Value3Map = (Map<?,?>)key1List.get(2);
        assertTrue("Map should not be null", key1Value3Map != null);
        assertTrue("Map should have 1 element", key1Value3Map.size() == 1);
        assertTrue("Map key3 should be true", key1Value3Map.get("key3").equals(Boolean.TRUE));

        Map<?,?> key2Map = (Map<?,?>)map.get("key2");
        assertTrue("key2 should not be null", key2Map != null);
        assertTrue("key2 map should have 3 elements", key2Map.size() == 3);
        assertTrue("key2 map key 1 should be val1", key2Map.get("key1").equals("val1"));
        assertTrue("key2 map key 3 should be 42", key2Map.get("key3").equals(Integer.valueOf(42)));

        Map<?,?> key2Val2Map = (Map<?,?>)key2Map.get("key2");
        assertTrue("key2 map key 2 should not be null", key2Val2Map != null);
        assertTrue("key2 map key 2 should have an entry", key2Val2Map.containsKey("key2"));
        assertTrue("key2 map key 2 value should be null", key2Val2Map.get("key2") == null);

        List<?> key3List = (List<?>)map.get("key3");
        assertTrue("key3 should not be null", key3List != null);
        assertTrue("key3 list should have 3 elements", key3List.size() == 2);

        List<?> key3Val1List = (List<?>)key3List.get(0);
        assertTrue("key3 list val 1 should not be null", key3Val1List != null);
        assertTrue("key3 list val 1 should have 2 elements", key3Val1List.size() == 2);
        assertTrue("key3 list val 1 list element 1 should be value1", key3Val1List.get(0).equals("value1"));
        assertTrue("key3 list val 1 list element 2 should be 2.1", key3Val1List.get(1).equals(new BigDecimal("2.1")));

        List<?> key3Val2List = (List<?>)key3List.get(1);
        assertTrue("key3 list val 2 should not be null", key3Val2List != null);
        assertTrue("key3 list val 2 should have 1 element", key3Val2List.size() == 1);
        assertTrue("key3 list val 2 list element 1 should be null", key3Val2List.get(0) == null);

        // Assert that toMap() is a deep copy
        jsonObject.getJSONArray("key3").getJSONArray(0).put(0, "still value 1");
        assertTrue("key3 list val 1 list element 1 should be value1", key3Val1List.get(0).equals("value1"));

        // assert that the new map is mutable
        assertTrue("Removing a key should succeed", map.remove("key3") != null);
        assertTrue("Map should have 2 elements", map.size() == 2);
    }
    



    @Test
    public void testPutNullDouble() {
        // null put key 
        JSONObject jsonObject = new JSONObject("{}");
        jsonObject.put(null, 0.0d);
    }
    @Test
    public void testPutNullFloat() {
        // null put key 
        JSONObject jsonObject = new JSONObject("{}");
        jsonObject.put(null, 0.0f);
    }
    @Test
    public void testPutNullInt() {
        // null put key 
        JSONObject jsonObject = new JSONObject("{}");
        jsonObject.put(null, 0);
    }
    @Test
    public void testPutNullLong() {
        // null put key 
        JSONObject jsonObject = new JSONObject("{}");
        jsonObject.put(null, 0L);
    }

    /**
    * Tests if calling JSONObject clear() method actually makes the JSONObject empty
    */
    @Test
    public void jsonObjectClearMethodTest() {
        //Adds random stuff to the JSONObject
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", 123);
        jsonObject.put("key2", "456");
        jsonObject.put("key3", new JSONObject());
        jsonObject.clear(); //Clears the JSONObject
        assertTrue("expected jsonObject.length() == 0", jsonObject.length() == 0); //Check if its length is 0
        jsonObject.getInt("key1"); //Should throws org.json.JSONException: JSONObject["asd"] not found
    }

    private static final Number[] NON_FINITE_NUMBERS = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN };




    @Test
    public void issue743SerializationMap() {
      HashMap<String, Object> map = new HashMap<>();
      map.put("t", map);
      JSONObject object = new JSONObject(map);
      String jsonString = object.toString();
    }

    @Test
    public void testCircularReferenceMultipleLevel() {
      HashMap<String, Object> inside = new HashMap<>();
      HashMap<String, Object> jsonObject = new HashMap<>();
      inside.put("inside", jsonObject);
      jsonObject.put("test", inside);
      new JSONObject(jsonObject);
    }

    @Test
    public void testCircleReferenceFirstLevel() {
        Map<Object, Object> jsonObject = new HashMap<>();
        jsonObject.put("test", jsonObject);
        new JSONObject(jsonObject, new JSONParserConfiguration());
    }

    @Test
    public void test01(){
        String json = "{\"status\":1,\"msg\":\"success\",\"timestamp\":1727242748832,\"data\":[{\"id\":\"1\",\"pid\":\"0\",\"type\":1,\"title\":\"主页\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":1,\"create_time\":\"2020-11-27 03:09:21\",\"create_by\":\"sys\",\"update_time\":\"2023-05-08 09:59:43\",\"update_by\":\"SYS\",\"level\":0,\"level_locking\":16},{\"id\":\"2\",\"pid\":\"0\",\"type\":1,\"title\":\"业务系统1\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":4,\"show\":1,\"sort\":2,\"create_time\":\"2020-11-27 03:09:24\",\"create_by\":\"sys\",\"update_time\":\"2023-06-16 14:19:04\",\"update_by\":\"1\",\"level\":0,\"level_locking\":16},{\"id\":\"2306161419041075\",\"pid\":\"2\",\"type\":1,\"title\":\"地区\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"\",\"sub_count\":2,\"show\":1,\"sort\":1,\"create_time\":\"2023-06-16 14:19:04\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 14:19:04\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"202\",\"pid\":\"2306161419041075\",\"type\":1,\"title\":\"华南电商\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":9,\"show\":1,\"sort\":2,\"create_time\":\"2020-11-27 03:28:43\",\"create_by\":\"sys\",\"update_time\":\"2023-05-24 17:51:42\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"20201\",\"pid\":\"202\",\"type\":2,\"title\":\"品牌一部\",\"icon\":null,\"path\":\"/huanan/brand\",\"target\":\"_content\",\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":1,\"create_time\":\"2020-09-22 06:53:33\",\"create_by\":\"1\",\"update_time\":null,\"update_by\":null,\"level\":3,\"level_locking\":64},{\"id\":\"20202\",\"pid\":\"202\",\"type\":2,\"title\":\"直营渠道\",\"icon\":null,\"path\":\"/huanan/official\",\"target\":\"_content\",\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":2,\"create_time\":\"2020-09-22 06:53:53\",\"create_by\":\"1\",\"update_time\":null,\"update_by\":null,\"level\":3,\"level_locking\":64},{\"id\":\"20203\",\"pid\":\"202\",\"type\":2,\"title\":\"产品研发\",\"icon\":null,\"path\":\"/huanan/developer\",\"target\":\"_content\",\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":3,\"create_time\":\"2020-10-09 11:31:11\",\"create_by\":\"1\",\"update_time\":null,\"update_by\":null,\"level\":3,\"level_locking\":64},{\"id\":\"205\",\"pid\":\"2306161419041075\",\"type\":1,\"title\":\"店铺日报\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":3,\"show\":0,\"sort\":4,\"create_time\":\"2020-11-27 03:44:24\",\"create_by\":\"admin\",\"update_time\":\"2023-06-16 14:16:04\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"20501\",\"pid\":\"205\",\"type\":2,\"title\":\"店铺01\",\"icon\":null,\"path\":\"\",\"target\":\"/mall/01\",\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":1,\"create_time\":\"2020-11-27 03:44:24\",\"create_by\":\"admin\",\"update_time\":null,\"update_by\":null,\"level\":3,\"level_locking\":64},{\"id\":\"20502\",\"pid\":\"205\",\"type\":2,\"title\":\"店铺02\",\"icon\":null,\"path\":\"\",\"target\":\"/mall/02\",\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":2,\"create_time\":\"2020-11-27 03:44:24\",\"create_by\":\"admin\",\"update_time\":null,\"update_by\":null,\"level\":3,\"level_locking\":64},{\"id\":\"20503\",\"pid\":\"205\",\"type\":2,\"title\":\"店铺03\",\"icon\":null,\"path\":\"\",\"target\":\"/mall/03\",\"permission\":null,\"sub_count\":0,\"show\":1,\"sort\":3,\"create_time\":\"2021-04-02 14:54:07\",\"create_by\":\"admin\",\"update_time\":null,\"update_by\":null,\"level\":3,\"level_locking\":64},{\"id\":\"207\",\"pid\":\"2\",\"type\":1,\"title\":\"基础信息\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":5,\"show\":1,\"sort\":null,\"create_time\":\"2020-11-27 03:45:34\",\"create_by\":\"admin\",\"update_time\":\"2023-06-16 14:33:09\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"20703\",\"pid\":\"207\",\"type\":2,\"title\":\"品牌\",\"icon\":null,\"path\":\"/mallinfo/brand\",\"target\":\"_content\",\"permission\":\"brand:list\",\"sub_count\":0,\"show\":1,\"sort\":1,\"create_time\":\"2020-11-27 03:45:34\",\"create_by\":\"admin\",\"update_time\":null,\"update_by\":null,\"level\":2,\"level_locking\":48},{\"id\":\"20701\",\"pid\":\"207\",\"type\":2,\"title\":\"线下店铺\",\"icon\":null,\"path\":\"/sv1/t_offline_store\",\"target\":\"_content\",\"permission\":\"sv1:t_offline_store:list\",\"sub_count\":6,\"show\":1,\"sort\":2,\"create_time\":\"2020-11-27 03:45:34\",\"create_by\":\"admin\",\"update_time\":\"2024-03-12 14:20:00\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306161410011000\",\"pid\":\"20701\",\"type\":3,\"title\":\"查询\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sv1:t_offline_store:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-16 14:10:01\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 14:10:01\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306161411461001\",\"pid\":\"20701\",\"type\":3,\"title\":\"新增\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sv1:t_offline_store:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-16 14:11:46\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 14:11:46\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306161412271002\",\"pid\":\"20701\",\"type\":3,\"title\":\"修改\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sv1:t_offline_store:update\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-16 14:12:27\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 14:12:27\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306161413241003\",\"pid\":\"20701\",\"type\":3,\"title\":\"删除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sv1:t_offline_store:delete\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2023-06-16 14:13:24\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 14:13:24\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306161645031000\",\"pid\":\"20701\",\"type\":3,\"title\":\"导出\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sv1:t_offline_store:export\",\"sub_count\":0,\"show\":0,\"sort\":5,\"create_time\":\"2023-06-16 16:45:03\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 16:45:03\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2403121419441000\",\"pid\":\"20701\",\"type\":3,\"title\":\"导入\",\"icon\":\"\",\"path\":\"\",\"target\":\"_self\",\"permission\":\"sv1:t_offline_store:import\",\"sub_count\":0,\"show\":0,\"sort\":6,\"create_time\":\"2024-03-12 14:19:44\",\"create_by\":\"1\",\"update_time\":\"2024-03-12 14:20:00\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"20702\",\"pid\":\"207\",\"type\":2,\"title\":\"渠道\",\"icon\":null,\"path\":\"/mallinfo/channel\",\"target\":\"_content\",\"permission\":\"channel:list\",\"sub_count\":0,\"show\":1,\"sort\":3,\"create_time\":\"2020-11-27 03:45:34\",\"create_by\":\"admin\",\"update_time\":null,\"update_by\":null,\"level\":2,\"level_locking\":48},{\"id\":\"20704\",\"pid\":\"207\",\"type\":2,\"title\":\"商品\",\"icon\":null,\"path\":\"/mallinfo/goods\",\"target\":\"_content\",\"permission\":\"goods:list\",\"sub_count\":0,\"show\":1,\"sort\":4,\"create_time\":\"2022-04-13 13:29:21\",\"create_by\":\"admin\",\"update_time\":null,\"update_by\":null,\"level\":2,\"level_locking\":48},{\"id\":\"2306161418111074\",\"pid\":\"207\",\"type\":2,\"title\":\"其他\",\"icon\":\"\",\"path\":\"https://cn.bing.com/\",\"target\":\"_content\",\"permission\":\"\",\"sub_count\":0,\"show\":1,\"sort\":5,\"create_time\":\"2023-06-16 14:18:11\",\"create_by\":\"1\",\"update_time\":\"2023-06-16 14:33:09\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306061317111000\",\"pid\":\"0\",\"type\":1,\"title\":\"业务系统2\",\"icon\":\"\",\"path\":\"https://www.csdn.net/\",\"target\":\"_content\",\"permission\":\"\",\"sub_count\":0,\"show\":1,\"sort\":3,\"create_time\":\"2023-06-06 13:17:11\",\"create_by\":\"1\",\"update_time\":\"2023-06-06 13:29:08\",\"update_by\":\"1\",\"level\":0,\"level_locking\":16},{\"id\":\"4\",\"pid\":\"0\",\"type\":1,\"title\":\"系统管理\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":2,\"show\":1,\"sort\":5,\"create_time\":\"2020-11-27 03:09:29\",\"create_by\":\"sys\",\"update_time\":\"2023-05-24 17:08:59\",\"update_by\":\"1\",\"level\":0,\"level_locking\":16},{\"id\":\"401\",\"pid\":\"4\",\"type\":1,\"title\":\"系统配置\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":5,\"show\":1,\"sort\":1,\"create_time\":\"2020-11-27 03:09:40\",\"create_by\":\"sys\",\"update_time\":\"2023-06-07 10:41:49\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"2305312137051000\",\"pid\":\"401\",\"type\":2,\"title\":\"用户配置\",\"icon\":\"\",\"path\":\"/sys/sys_user\",\"target\":\"_content\",\"permission\":\"sys:sys_user:list\",\"sub_count\":9,\"show\":1,\"sort\":1,\"create_time\":\"2023-05-31 21:37:05\",\"create_by\":\"1\",\"update_time\":\"2023-06-11 19:08:17\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071059211044\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 10:59:21\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:59:21\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071059581045\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 10:59:58\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:59:58\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071100531046\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"更新\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:update\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 11:00:53\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:00:53\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306111908171001\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"导出\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:export\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2023-06-11 19:08:17\",\"create_by\":\"1\",\"update_time\":\"2023-06-11 19:08:17\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071101221047\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:delete\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2023-06-07 11:01:22\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:01:22\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071102041048\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"狀態切換\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:change_status\",\"sub_count\":0,\"show\":0,\"sort\":5,\"create_time\":\"2023-06-07 11:02:04\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:02:04\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071103001049\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"解鎖操作\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:unlock\",\"sub_count\":0,\"show\":0,\"sort\":6,\"create_time\":\"2023-06-07 11:03:00\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:03:00\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071103581050\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"重置密碼\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:reset_pwd\",\"sub_count\":0,\"show\":0,\"sort\":7,\"create_time\":\"2023-06-07 11:03:58\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:03:58\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071104431051\",\"pid\":\"2305312137051000\",\"type\":3,\"title\":\"角色信息\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_user:get_roles\",\"sub_count\":0,\"show\":0,\"sort\":8,\"create_time\":\"2023-06-07 11:04:43\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:04:43\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2305281726041000\",\"pid\":\"401\",\"type\":2,\"title\":\"角色配置\",\"icon\":\"\",\"path\":\"/sys/sys_role\",\"target\":\"_content\",\"permission\":\"sys:sys_role:list\",\"sub_count\":4,\"show\":1,\"sort\":2,\"create_time\":\"2023-05-28 17:26:04\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:57:06\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071054451040\",\"pid\":\"2305281726041000\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 10:54:45\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:54:45\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071055271041\",\"pid\":\"2305281726041000\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 10:55:27\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:55:27\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071056141042\",\"pid\":\"2305281726041000\",\"type\":3,\"title\":\"修改\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role:update\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 10:56:14\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:56:14\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071057061043\",\"pid\":\"2305281726041000\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role:delete\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2023-06-07 10:57:06\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:57:06\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"40106\",\"pid\":\"401\",\"type\":2,\"title\":\"菜单配置\",\"icon\":null,\"path\":\"/sys/sys_menu\",\"target\":\"_content\",\"permission\":\"sys:sys_menu:list\",\"sub_count\":4,\"show\":1,\"sort\":4,\"create_time\":\"2023-05-06 05:04:12\",\"create_by\":\"sys\",\"update_time\":\"2023-06-07 10:38:15\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071035211000\",\"pid\":\"40106\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_menu:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 10:35:21\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:36:29\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071036061001\",\"pid\":\"40106\",\"type\":3,\"title\":\"新增\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_menu:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 10:36:06\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:36:46\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071037411002\",\"pid\":\"40106\",\"type\":3,\"title\":\"更新\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_menu:update\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 10:37:41\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:37:41\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071038151003\",\"pid\":\"40106\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_menu:delete\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2023-06-07 10:38:15\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:38:15\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"40104\",\"pid\":\"401\",\"type\":2,\"title\":\"角色&用户配置\",\"icon\":null,\"path\":\"/sys/sys_role_user\",\"target\":\"_content\",\"permission\":\"sys:sys_role_user:list\",\"sub_count\":3,\"show\":1,\"sort\":5,\"create_time\":\"2020-11-27 03:09:13\",\"create_by\":\"sys\",\"update_time\":\"2023-06-07 10:51:31\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071050181034\",\"pid\":\"40104\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role_user:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 10:50:18\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:50:18\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071050571035\",\"pid\":\"40104\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role_user:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 10:50:57\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:50:57\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071051311036\",\"pid\":\"40104\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role_user:delete\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 10:51:31\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:51:31\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"40105\",\"pid\":\"401\",\"type\":2,\"title\":\"角色&菜单配置\",\"icon\":null,\"path\":\"/sys/sys_role_menu\",\"target\":\"_content\",\"permission\":\"sys:sys_role_menu:list\",\"sub_count\":3,\"show\":1,\"sort\":6,\"create_time\":\"2020-11-27 08:04:41\",\"create_by\":\"sys\",\"update_time\":\"2023-06-07 10:54:10\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071052151037\",\"pid\":\"40105\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role_menu:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 10:52:15\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:52:15\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071052451038\",\"pid\":\"40105\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role_menu:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 10:52:45\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:52:45\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071054101039\",\"pid\":\"40105\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_role_menu:delete\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 10:54:10\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 10:54:10\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"402\",\"pid\":\"4\",\"type\":1,\"title\":\"基础管理\",\"icon\":null,\"path\":\"\",\"target\":null,\"permission\":null,\"sub_count\":7,\"show\":1,\"sort\":2,\"create_time\":\"2020-11-27 03:09:36\",\"create_by\":\"sys\",\"update_time\":\"2024-09-14 17:49:50\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"2305252217261000\",\"pid\":\"402\",\"type\":2,\"title\":\"字典配置\",\"icon\":\"\",\"path\":\"/sys/sys_dict\",\"target\":\"_content\",\"permission\":\"sys:sys_dict:list\",\"sub_count\":4,\"show\":1,\"sort\":1,\"create_time\":\"2023-05-25 22:17:26\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:18:30\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071117001052\",\"pid\":\"2305252217261000\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_dict:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 11:17:00\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:17:00\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071117421053\",\"pid\":\"2305252217261000\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_dict:add\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 11:17:42\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:17:42\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071118051054\",\"pid\":\"2305252217261000\",\"type\":3,\"title\":\"更新\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_dict:update\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 11:18:05\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:18:05\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071118301055\",\"pid\":\"2305252217261000\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_dict:delete\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2023-06-07 11:18:30\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:18:30\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"40204\",\"pid\":\"402\",\"type\":2,\"title\":\"文件管理\",\"icon\":null,\"path\":\"/sys/sys_file\",\"target\":\"_content\",\"permission\":\"sys:sys_file:list\",\"sub_count\":2,\"show\":1,\"sort\":2,\"create_time\":\"2020-09-21 08:33:16\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:20:45\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071120161056\",\"pid\":\"40204\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_file:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 11:20:16\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:20:16\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071120451057\",\"pid\":\"40204\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_file:delete\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 11:20:45\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:20:45\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"40202\",\"pid\":\"402\",\"type\":2,\"title\":\"日志配置\",\"icon\":null,\"path\":\"/sys/sys_log\",\"target\":\"_content\",\"permission\":\"sys:sys_log:list\",\"sub_count\":3,\"show\":1,\"sort\":3,\"create_time\":\"2020-11-27 03:09:52\",\"create_by\":\"sys\",\"update_time\":\"2023-06-07 11:23:17\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071122221058\",\"pid\":\"40202\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_log:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 11:22:22\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:22:22\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071122481059\",\"pid\":\"40202\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_log:delete\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 11:22:48\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:22:48\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071123171060\",\"pid\":\"40202\",\"type\":3,\"title\":\"導出\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_log:export\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 11:23:17\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:23:17\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306040014281000\",\"pid\":\"402\",\"type\":2,\"title\":\"系统信息\",\"icon\":\"\",\"path\":\"/sys/server_info\",\"target\":\"_content\",\"permission\":\"sys:server_info:list\",\"sub_count\":0,\"show\":1,\"sort\":4,\"create_time\":\"2023-06-04 00:14:28\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:24:11\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"40205\",\"pid\":\"402\",\"type\":2,\"title\":\"定时任务\",\"icon\":null,\"path\":\"/sys/sys_shedlock_app\",\"target\":\"_content\",\"permission\":\"sys:sys_shedlock:list\",\"sub_count\":4,\"show\":1,\"sort\":5,\"create_time\":\"2020-10-19 05:46:22\",\"create_by\":\"1\",\"update_time\":\"2024-06-19 11:07:38\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2306071125011061\",\"pid\":\"40205\",\"type\":3,\"title\":\"查詢\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_shedlock:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2023-06-07 11:25:01\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:25:29\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071125291062\",\"pid\":\"40205\",\"type\":3,\"title\":\"更新\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_shedlock:update\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2023-06-07 11:25:29\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:25:29\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2306071126091063\",\"pid\":\"40205\",\"type\":3,\"title\":\"刪除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_shedlock:delete\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2023-06-07 11:26:09\",\"create_by\":\"1\",\"update_time\":\"2023-06-07 11:26:09\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2406191107381000\",\"pid\":\"40205\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:sys_shedlock:add\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2024-06-19 11:07:38\",\"create_by\":\"1\",\"update_time\":\"2024-06-19 11:07:38\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141044001000\",\"pid\":\"402\",\"type\":2,\"title\":\"QUARTZ任务-应用&节点\",\"icon\":\"\",\"path\":\"/sys/qrtz_app\",\"target\":\"_content\",\"permission\":\"sys:qrtz_app:list\",\"sub_count\":4,\"show\":1,\"sort\":6,\"create_time\":\"2024-09-14 10:44:00\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 17:46:40\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2409141103551001\",\"pid\":\"2409141044001000\",\"type\":3,\"title\":\"查询\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_app:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2024-09-14 11:03:55\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 11:13:03\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141104551002\",\"pid\":\"2409141044001000\",\"type\":3,\"title\":\"更新\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_app:update\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2024-09-14 11:04:55\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 11:13:22\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141105481003\",\"pid\":\"2409141044001000\",\"type\":3,\"title\":\"删除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_app:delete\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2024-09-14 11:05:48\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 11:13:38\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141106261004\",\"pid\":\"2409141044001000\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_app:add\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2024-09-14 11:06:26\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 11:14:07\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141749091000\",\"pid\":\"402\",\"type\":2,\"title\":\"QUARTZ任务-任务&执行\",\"icon\":\"\",\"path\":\"/sys/qrtz_job\",\"target\":\"_content\",\"permission\":\"sys:qrtz_job:list\",\"sub_count\":4,\"show\":1,\"sort\":7,\"create_time\":\"2024-09-14 17:49:09\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 17:51:57\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2409141750251001\",\"pid\":\"2409141749091000\",\"type\":3,\"title\":\"查询\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_job:list\",\"sub_count\":0,\"show\":0,\"sort\":1,\"create_time\":\"2024-09-14 17:50:25\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 17:50:25\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141750581002\",\"pid\":\"2409141749091000\",\"type\":3,\"title\":\"更新\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_job:update\",\"sub_count\":0,\"show\":0,\"sort\":2,\"create_time\":\"2024-09-14 17:50:58\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 17:50:58\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141751291003\",\"pid\":\"2409141749091000\",\"type\":3,\"title\":\"删除\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_job:delete\",\"sub_count\":0,\"show\":0,\"sort\":3,\"create_time\":\"2024-09-14 17:51:29\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 17:51:29\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2409141751571004\",\"pid\":\"2409141749091000\",\"type\":3,\"title\":\"添加\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"sys:qrtz_job:add\",\"sub_count\":0,\"show\":0,\"sort\":4,\"create_time\":\"2024-09-14 17:51:57\",\"create_by\":\"1\",\"update_time\":\"2024-09-14 17:51:57\",\"update_by\":\"1\",\"level\":3,\"level_locking\":64},{\"id\":\"2406241713591000\",\"pid\":\"0\",\"type\":1,\"title\":\"测试目录01\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"\",\"sub_count\":0,\"show\":0,\"sort\":5,\"create_time\":\"2024-06-24 17:13:59\",\"create_by\":\"1\",\"update_time\":\"2024-07-19 09:22:42\",\"update_by\":\"1\",\"level\":0,\"level_locking\":16},{\"id\":\"2409240939281000\",\"pid\":\"0\",\"type\":1,\"title\":\"测试主目录\",\"icon\":\"\",\"path\":\"\",\"target\":null,\"permission\":\"\",\"sub_count\":3,\"show\":1,\"sort\":7,\"create_time\":\"2024-09-24 09:39:28\",\"create_by\":\"1\",\"update_time\":\"2024-09-24 10:05:44\",\"update_by\":\"1\",\"level\":0,\"level_locking\":16},{\"id\":\"2409240942591001\",\"pid\":\"2409240939281000\",\"type\":2,\"title\":\"测试菜单01\",\"icon\":\"\",\"path\":\"/sys/usr\",\"target\":\"_content\",\"permission\":\"\",\"sub_count\":0,\"show\":1,\"sort\":1,\"create_time\":\"2024-09-24 09:42:59\",\"create_by\":\"1\",\"update_time\":\"2024-09-24 09:42:59\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"2409240943271002\",\"pid\":\"2409240939281000\",\"type\":2,\"title\":\"测试菜单02\",\"icon\":\"\",\"path\":\"/sys/usr2\",\"target\":\"_content\",\"permission\":\"\",\"sub_count\":2,\"show\":1,\"sort\":2,\"create_time\":\"2024-09-24 09:43:27\",\"create_by\":\"1\",\"update_time\":\"2024-09-24 10:06:19\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"2409240944221003\",\"pid\":\"2409240943271002\",\"type\":3,\"title\":\"增加\",\"icon\":\"\",\"path\":\"\",\"target\":null,\"permission\":\"usr:add2\",\"sub_count\":0,\"show\":1,\"sort\":1,\"create_time\":\"2024-09-24 09:44:22\",\"create_by\":\"1\",\"update_time\":\"2024-09-24 10:06:19\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2409240945081004\",\"pid\":\"2409240943271002\",\"type\":3,\"title\":\"删除\",\"icon\":\"\",\"path\":\"\",\"target\":null,\"permission\":\"usr:delete\",\"sub_count\":0,\"show\":1,\"sort\":2,\"create_time\":\"2024-09-24 09:45:08\",\"create_by\":\"1\",\"update_time\":\"2024-09-24 09:46:46\",\"update_by\":\"1\",\"level\":2,\"level_locking\":48},{\"id\":\"2409241002361006\",\"pid\":\"2409240939281000\",\"type\":1,\"title\":\"测试目录03\",\"icon\":\"\",\"path\":\"\",\"target\":null,\"permission\":\"\",\"sub_count\":0,\"show\":1,\"sort\":3,\"create_time\":\"2024-09-24 10:02:36\",\"create_by\":\"1\",\"update_time\":\"2024-09-24 10:05:44\",\"update_by\":\"1\",\"level\":1,\"level_locking\":32},{\"id\":\"2306111900571000\",\"pid\":\"0\",\"type\":3,\"title\":\"开发者\",\"icon\":\"\",\"path\":\"\",\"target\":\"\",\"permission\":\"dev\",\"sub_count\":0,\"show\":0,\"sort\":9,\"create_time\":\"2023-06-11 19:00:57\",\"create_by\":\"1\",\"update_time\":\"2023-06-11 19:00:57\",\"update_by\":\"1\",\"level\":0,\"level_locking\":16}]}";
        JSONObject jsonObject = new JSONObject(json);
        System.out.println(jsonObject);
        Map<String, Object> jm = jsonObject.toMap();
        JSONObject jsonObject1 = new JSONObject(jm);
        System.out.println(jsonObject1);
        SysMenu sysMenu = new SysMenu();
        sysMenu.setId("2");
        sysMenu.setType(1);
        sysMenu.setTitle("这是菜单");
        sysMenu.setPermission("sys:user:add");
        sysMenu.setSub_count(223);
        sysMenu.setCreate_time(LocalDateTime.now());
        JSONObject jsonObject2 = new JSONObject(sysMenu);
        System.out.println(jsonObject2);
    }

    @Test
    public void test02(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("k1","hello");
        jsonObject.put("k2",true);
        jsonObject.put("k3",new Date());
        jsonObject.put("k4",Arrays.asList(11,22,33));
        jsonObject.put("k5",234.56F);
        jsonObject.put("k6",123455556666777888L);
        System.out.println(jsonObject);
        jsonObject.remove("k2");
        jsonObject.put("k6","UY");
        System.out.println(jsonObject);
    }

    @Test
    public void test03(){
        JSONArray objects = new JSONArray();
        objects.put("hello");
        objects.put(false);
        objects.put(Arrays.asList(11,22,33));
        objects.put(123455556666777888L);
        objects.put(new Date());
        objects.put(LocalDateTime.now());
        System.out.println(objects);
    }


}
