package com.acibfunin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * @author Miguel Sanchez - Aurotech
 */
public class JSONTransformer {

	static final ObjectMapper mapper = new ObjectMapper();

	public static String toJSON(Map<String, Object> model) {

		try {

			return mapper.writeValueAsString(model);

		} catch (Exception e) {
			throw new RuntimeException("Couldn't convert Map to Json:" + model, e);
		}
	}

	public static Map<String, Object> toMAP(String json) {

		try {
			return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});

		} catch (Exception e) {
			throw new RuntimeException("Couldnt parse json:" + json, e);
		}
	}

//	public static Object toObject(String json, Class type) {
//
//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			return mapper.readValue(json, type);
//		} catch (Exception e) {
//			throw new RuntimeException("Couldnt parse json:" + json, e);
//		}
//	}

}
