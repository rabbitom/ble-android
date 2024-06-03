package net.erabbit.csl;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class CSLTest {
    @Test
    public void number_uint8() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint8\"}");
        Object value = CSL.decode(new byte[]{(byte)0xFF}, 0, config, null);
        assertEquals(0xFF, value);
        byte[] bytes = CSL.encode(0xFF, config);
        assertArrayEquals(new byte[]{(byte)0xFF}, bytes);
    }
    @Test
    public void number_uint16be() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint16be\"}");
        Object value = CSL.decode(new byte[]{0x00,0x01}, 0, config, null);
        assertEquals(0x0001, value);
        byte[] bytes = CSL.encode(0x0001, config);
        assertArrayEquals(new byte[]{0x00,0x01}, bytes);
    }
    @Test
    public void number_uint16le() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint16le\"}");
        Object value = CSL.decode(new byte[]{0x00,(byte)0x80}, 0, config, null);
        assertEquals(0x8000, value);
        byte[] bytes = CSL.encode(0x8000, config);
        assertArrayEquals(new byte[]{0x00,(byte)0x80}, bytes);
    }
    @Test
    public void number_int16be() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int16be\"}");
        Object value = CSL.decode(new byte[]{(byte)0xFF,(byte)0xFE}, 0, config, null);
        assertEquals((short)-2, value);
        byte[] bytes = CSL.encode(-2, config);
        assertArrayEquals(new byte[]{(byte)0xFF,(byte)0xFE}, bytes);
    }
    @Test
    public void number_int16le() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int16le\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFE,(byte)0xFF}, 0, config, null);
        assertEquals((short)-2, result);
        byte[] bytes = CSL.encode(-2, config);
        assertArrayEquals(new byte[]{(byte)0xFE,(byte)0xFF}, bytes);
    }
    @Test
    public void number_int32be() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int32be\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE}, 0, config, null);
        assertEquals(-2, result);
        byte[] bytes = CSL.encode(-2, config);
        assertArrayEquals(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE}, bytes);
    }
    @Test
    public void number_int32le() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int32le\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0, config, null);
        assertEquals(-2, result);
        byte[] bytes = CSL.encode(-2, config);
        assertArrayEquals(new byte[]{(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF}, bytes);
    }
    @Test
    public void number_uint32be() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint32be\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE}, 0, config, null);
        assertEquals(0xFFFFFFFEL, result);
        byte[] bytes = CSL.encode(0xFFFFFFFEL, config);
        assertArrayEquals(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE}, bytes);
    }
    @Test
    public void number_uint32le() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint32le\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0, config, null);
        assertEquals(0xFFFFFFFEL, result);
        byte[] bytes = CSL.encode(0xFFFFFFFEL, config);
        assertArrayEquals(new byte[]{(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF}, bytes);
    }
    @Test
    public void number_float32le() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"float32le\"}");
        Object result = CSL.decode(new byte[]{(byte)0x00,(byte)0x00,(byte)0xD0,(byte)0x40}, 0, config, null);
        assertEquals(6.5F, result);
        byte[] bytes = CSL.encode(6.5F, config);
        assertArrayEquals(new byte[]{(byte)0x00,(byte)0x00,(byte)0xD0,(byte)0x40}, bytes);
    }
    @Test
    public void number_scale() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint8\",\"scale\":0.1}");
        Object result = CSL.decode(new byte[]{0x0A}, 0, config, null);
        assertEquals(1.0, result);
        byte[] bytes = CSL.encode(1.0, config);
        assertArrayEquals(new byte[]{0x0A}, bytes);
    }
    @Test
    public void decode_string() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"string\",\"byteLength\":5}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object result = CSL.decode(new byte[]{0x48,0x65,0x6C,0x6C,0x6F}, 0, config, report);
        assertEquals("Hello", result);
        assertEquals((long)5, (long)report.length);
    }
    @Test
    public void decode_string_padding() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"string\",\"byteLength\":10}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object result = CSL.decode(new byte[]{0x48,0x65,0x6C,0x6C,0x6F,0x00,0x00,0x00,0x00,0x00}, 0, config, report);
        assertEquals("Hello", result);
        assertEquals((long)10, (long)report.length);
    }
    @Test
    public void decode_string_hex() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"string\",\"byteLength\":2,\"stringEncoding\":\"hex\"}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object result = CSL.decode(new byte[]{(byte)0xFF,0x12}, 0, config, report);
        assertEquals("FF12", result);
        assertEquals((long)2, (long)report.length);
    }
    @Test
    public void decode_bytes() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"bytes\",\"byteLength\":3}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object result = CSL.decode(new byte[]{0x01,0x02,0x03}, 0, config, report);
        assertArrayEquals(new byte[]{0x01,0x02,0x03}, (byte[])result);
        assertEquals((long)3, (long)report.length);
    }
    @Test
    public void decode_bytes_no_byteLength() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"bytes\"}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object result = CSL.decode(new byte[]{0x01,0x02,0x03}, 0, config, report);
        assertArrayEquals(new byte[]{0x01,0x02,0x03}, (byte[])result);
        assertEquals((long)3, (long)report.length);
    }
    @Test
    public void decode_boolean() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"boolean\"}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object result = CSL.decode(new byte[]{0x01}, 0, config, report);
        assertEquals(true, result);
        assertEquals((long)1, (long)report.length);
    }
    @Test
    public void decode_object_bitmask() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"object\",\"objectType\":\"bitmask\",\"attributes\":[{\"name\":\"a\",\"type\":\"boolean\",\"mask\":1},{\"name\":\"b\",\"type\":\"boolean\",\"mask\":2},{\"name\":\"c\",\"type\":\"number\",\"numberType\":\"uint8\",\"mask\":240}]}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Map<String,Object> result = (Map<String,Object>)CSL.decode(new byte[]{0x21}, 0, config, report);
        assertSame(result.getClass(), HashMap.class);
        assertEquals(true, result.get("a"));
        assertEquals(false, result.get("b"));
        assertEquals(2, result.get("c"));
    }
    @Test
    public void decode_object() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"object\",\"attributes\":[{\"name\":\"n\",\"type\":\"number\",\"numberType\":\"uint8\"},{\"name\":\"s\",\"type\":\"string\",\"byteLength\":3}]}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Map<String,Object> result = (Map<String,Object>)CSL.decode(new byte[]{0x01,0x30,0x31,0x32}, 0, config, report);
        assertSame(result.getClass(), HashMap.class);
        assertEquals(1, result.get("n"));
        assertEquals("012", result.get("s"));
    }
    @Test
    public void decode_object_variable_type() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"object\",\"attributes\":[{\"name\":\"category\",\"type\":\"number\",\"numberType\":\"uint8\",\"values\":[{\"name\":\"book\",\"value\":1},{\"name\":\"movie\",\"value\":2}]},{\"name\":\"info\",\"type\":\"variable\",\"typeIndex\":\"category\",\"types\":[{\"index\":1,\"type\":\"object\",\"attributes\":[{\"name\":\"pages\",\"type\":\"number\",\"numberType\":\"uint8\"}]},{\"index\":2,\"type\":\"object\",\"attributes\":[{\"name\":\"minutes\",\"type\":\"number\",\"numberType\":\"uint8\"}]}]}]}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Map<String,Object> result = (Map<String,Object>)CSL.decode(new byte[]{0x01,(byte)180}, 0, config, report);
        assertEquals((long)2, (long)report.length);
        assertSame(result.getClass(), HashMap.class);
        assertEquals(1, result.get("category"));
        Map<String,Object> info = (Map<String,Object>)result.get("info");
        assertEquals(180, info.get("pages"));
    }
    @Test
    public void decode_array() throws Exception {
        JSONObject config = new JSONObject("{\"type\":\"array\",\"byteLength\":3,\"arrayItem\":{\"type\":\"number\",\"numberType\":\"uint8\"}}");
        CSL.CSLDecodeReport report = new CSL.CSLDecodeReport();
        Object[] result = (Object[])CSL.decode(new byte[]{0x01,0x02,0x03}, 0, config, report);
        assertArrayEquals(new Object[]{1,2,3}, result);
        assertEquals((long)3, (long)report.length);
    }
}
