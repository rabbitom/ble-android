package net.erabbit.csl;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class CSLTest {
    @Test
    public void decode_object_empty() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"object\",\"attributes\":[]}");
        Object result = CSL.decode(new byte[]{}, 0, config, null);
        assertTrue(result.getClass() == HashMap.class);
    }
    @Test
    public void decode_object_number_uint8() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint8\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFF}, 0, config, null);
        assertEquals(0xFF, result);
    }
    @Test
    public void decode_object_number_uint16be() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint16be\"}");
        Object result = CSL.decode(new byte[]{0x00,0x01}, 0, config, null);
        assertEquals(0x0001, result);
    }
    @Test
    public void decode_object_number_uint16le() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint16le\"}");
        Object result = CSL.decode(new byte[]{0x00,(byte)0x80}, 0, config, null);
        assertEquals(0x8000, result);
    }
    @Test
    public void decode_object_number_int16be() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int16be\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFF,(byte)0xFE}, 0, config, null);
        assertEquals((short)-2, result);
    }
    @Test
    public void decode_object_number_int16le() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int16le\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFE,(byte)0xFF}, 0, config, null);
        assertEquals((short)-2, result);
    }
    @Test
    public void decode_object_number_int32be() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int32be\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE}, 0, config, null);
        assertEquals(-2, result);
    }
    @Test
    public void decode_object_number_int32le() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"int32le\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0, config, null);
        assertEquals(-2, result);
    }
    @Test
    public void decode_object_number_uint32be() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint32be\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE}, 0, config, null);
        assertEquals(0xFFFFFFFEL, result);
    }
    @Test
    public void decode_object_number_uint32le() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"uint32le\"}");
        Object result = CSL.decode(new byte[]{(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0, config, null);
        assertEquals(0xFFFFFFFEL, result);
    }
    @Test
    public void decode_object_number_float32le() throws JSONException {
        JSONObject config = new JSONObject("{\"type\":\"number\",\"numberType\":\"float32le\"}");
        Object result = CSL.decode(new byte[]{(byte)0x00,(byte)0x00,(byte)0xD0,(byte)0x40}, 0, config, null);
        assertEquals(6.5F, result);
    }
}
