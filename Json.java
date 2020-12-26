/*
 * Json.java - one file json serializer and parser -
 *
 * Copyright (c) 2020, Olaf Ritter von Ruppert
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.sql.Timestamp;

/**
 * Json serializer and parser.
 */
public final class Json {

	/**
	 * Thrown when input is malformed.
	 */
	public static final class ParseException extends RuntimeException {
		public ParseException(String message) {
			super(message);
		}
	}

	/**
	 * Thrown when an object can't be serialized.
	 */
	public static final class SerializeException extends RuntimeException {
		public SerializeException(String message) {
			super(message);
		}
	}

	/**
	 * Digit character class.
	 */
	private static final String DIGIT = "0123456789";

	/**
	 * Hex digit character class.
	 */
	private static final String HEXDIGIT = "abcdefABCDEF0123456789";

	/**
	 * Whitespace character class.
	 */
	private static final String WHITESPACE = " \t\n\r";

	/**
	 * Holds an object.
	 * Used for `out' parameters.
	 */
	private static final class Box { Object value; }

	/**
	 * Serialize object to an json string.
	 * @param object the object to serialize.
	 * @return the json string.
	 * @throws RuntimeException if object can't be serialized.
	 */
	public static String toJson(Object object) {
		StringBuilder sb = new StringBuilder();
		serialize(sb, object);
		return sb.toString();
	}

	/**
	 * Parse json input to an java object.
	 * @param input the json input string.
	 * @returns a the parsed java object.
	 * @throws RuntimeException if input can't be parsed.
	 */
	public static Object fromJson(String input) {
		Box out = new Box();
		int n = parseValue(input, out);
		if (n == 0)
			throw new RuntimeException("Can't parse " + input);
		n += strspn(input.substring(n), WHITESPACE);
		if (n != input.length())
			throw new RuntimeException("Trailing garbage");
		return out.value;
	}

	/**
	 * Serializes object.
	 * @param sb the StringBuilder to append to.
	 * @param object the object to serialize.
	 * @throws RuntimeException if object can't be serialized.
	 */
	private static void serialize(StringBuilder sb, Object object) {
		if (object == null) sb.append("null");
		else if (object == Boolean.TRUE) sb.append("true");
		else if (object == Boolean.FALSE) sb.append("false");
		else if (object instanceof Number) sb.append((Number)object);
		else if (object instanceof String) serialize(sb, (String)object);
		else if (object instanceof Map) serialize(sb, (Map)object);
		else if (object instanceof Iterable) serialize(sb, (Iterable)object);
		else if (object instanceof Timestamp) serialize(sb, object.toString());
		else throw new SerializeException("Can't serialize " + object.getClass());
	}

	/**
	 * Serializes a character.
	 * @param sb StringBuilder to append to.
	 * @param c the character to serialize.
	 */
	private static void serialize(StringBuilder sb, char c) {
		if (c == '"') sb.append("\\\"");
		else if (c == '\\') sb.append("\\\\");
		else if (c == '/') sb.append("\\/");
		else if (c == '\b') sb.append("\\b");
		else if (c == '\f') sb.append("\\f");
		else if (c == '\n') sb.append("\\n");
		else if (c == '\r') sb.append("\\r");
		else if (c == '\t') sb.append("\\t");
		/* control characters */
		else if (c < 32) sb.append(String.format("\\u%04x", (int)c));
		else sb.append(c);
	}

	/**
	 * Serializes a string.
	 * @param sb the StringBuilder to append to.
	 * @param string the string to serialize.
	 */
	private static void serialize(StringBuilder sb, String string) {
		sb.append('"');
		for (char c : string.toCharArray())
			serialize(sb, c);
		sb.append('"');
	}

	/**
	 * Serializes a map.
	 * @throws RuntimeException if key is not of type string.
	 */
	private static void serialize(StringBuilder sb, Map map) {
		sb.append('{');
		Iterator iter = map.keySet().iterator();
		while (iter.hasNext()) {
			Object key = iter.next();
			if (!(key instanceof String))
				throw new SerializeException("Map key must be a string.");
			serialize(sb, (String)key);
			sb.append(": ");
			serialize(sb, map.get(key));
			if (iter.hasNext())
				sb.append(", ");
		}
		sb.append('}');
	}

	/**
	 * Serializes a list.
	 * @param sb the StringBuilder to append to.
	 * @param list iter iterable to serialize.
	 * @thrown NullPointerException if list is null.
	 */
	private static void serialize(StringBuilder sb, Iterable list) {
		sb.append('[');
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			serialize(sb, iter.next());
			if (iter.hasNext())
				sb.append(", ");
		}
		sb.append(']');
	}

	/**
	 * Return string length if string is a prefix of input,
	 * otherwise return 0.
	 * @return the prefix length or 0.
	 */
	private static int prefix(String input, String string) {

		int len = string.length();

		if (len > input.length())
			return 0;

		for (int i = 0; i != len; i++)
			if (input.charAt(i) != string.charAt(i))
				return 0;

		return len;

	}

	/**
	 * Calculates the length of the initial segment of input which
	 * consists entirely of characters in accept (from the unix
	 * man page of strspn).
	 * @param input the input string in question.
	 * @param accept characters to accept.
	 */
	private static int strspn(String input, String accept) {
		int i;
		for (i = 0; i != input.length(); i++)
			if (accept.indexOf(input.charAt(i)) == -1)
				break;
		return i;

	}

	/**
	 * Parses a json input string.
	 * Sets the Box value on success.
	 * @return the number of consumed characters.
	 */
	private static int parseValue(String input, Box out) {
		int n = 0, pos = strspn(input, WHITESPACE);
		if (n == 0) pos += n = parseString(input.substring(pos), out);
		if (n == 0) pos += n = parseNumber(input.substring(pos), out);
		if (n == 0) pos += n = parseArray(input.substring(pos), out);
		if (n == 0) pos += n = parseObject(input.substring(pos), out);
		if (n == 0) pos += n = parseString(input.substring(pos), out);
		if (n == 0) pos += n = parseTrue(input.substring(pos), out);
		if (n == 0) pos += n = parseFalse(input.substring(pos), out);
		if (n == 0) pos += n = parseNull(input.substring(pos), out);
		if (n == 0) return 0;
		pos += n = strspn(input.substring(pos), WHITESPACE);
		return pos;

	}

	/**
	 * Parses an escaped character.
	 * @param input the input string to parse.
	 * @param out the StringBuilder to append to.
	 * @return the number of consumed characters.
	 */
	private static int parseEscapedChar(String input, StringBuilder out) {
		if (input.startsWith("\\\"")) out.append("\"");
		else if (input.startsWith("\\\\")) out.append("\\");
		else if (input.startsWith("\\/")) out.append("/");
		else if (input.startsWith("\\b")) out.append("\b");
		else if (input.startsWith("\\f")) out.append("\f");
		else if (input.startsWith("\\n")) out.append("\n");
		else if (input.startsWith("\\r")) out.append("\r");
		else if (input.startsWith("\\t")) out.append("\t");
		else return 0;
		return 2;
	}

	/**
	 * Parses a hex encoded character of the form '\\uXXXX'.
	 * @param input the input string to parse.
	 * @param out the StringBuilder to write to.
	 * @return the number of consumed characters.
	 */
	private static int parseUXXXX(String input, StringBuilder out) {
		int n, pos = 0;
		pos += n = prefix(input.substring(pos), "\\u");
		if (n == 0) return 0;
		pos += n = strspn(input.substring(pos), HEXDIGIT);
		if (n < 4) return 0;
		out.append((char)Integer.parseInt(input.substring(2, 6), 16));
		return 6;
	}

	/**
	 * Parses a string character except for '\\' and appends it to
	 * StringBuilder out.
	 * @param input the input string to parse.
	 * @param out the StringBuilder to write to.
	 * @return the number of consumed characters.
	 */
	 private static int parseStringChar(String input, StringBuilder out) {

		int n = parseEscapedChar(input, out);
		if (n > 0)
			return n;

		n = parseUXXXX(input, out);
		if (n > 0)
			return n;

		char c = input.charAt(0);

		if (c == '\\')
			throw new ParseException("Invalid backslash sequence.");

		if (c < 32)
			throw new ParseException("Unescaped control character.");

		out.append(c);

		return 1;
	}


	/**
	 * Parses an json string and sets the out value to string.
	 * @param input the string to parse.
	 * @param out the output box.
	 * @return the number of consumed characters.
	 */
	private static int parseString(String input, Box out) {
		int n, pos = 0;
		pos += n = prefix(input.substring(pos), "\"");
		if (n == 0) return 0;
		StringBuilder sb = new StringBuilder();
		for (;;) {
			if (pos == input.length())
				throw new ParseException("Unterminated string literal.");

			pos += n = prefix(input.substring(pos), "\"");
			if (n > 0) break;

			pos += n = parseStringChar(input.substring(pos), sb);
		}
		out.value = sb.toString();
		return pos;
	}


	/**
	 * Parses the 'true' literal.
	 * @return the number of consumed characters.
	 */
	private static int parseTrue(String input, Box out) {
		int pos = prefix(input, "true");
		if (pos > 0) out.value = true;
		return pos;
	}

	/**
	 * Parses the 'false' literal.
	 * @return the number of consumed characters.
	 */
	private static int parseFalse(String input, Box out) {
		int pos = prefix(input, "false");
		if (pos > 0) out.value = false;
		return pos;
	}

	/**
	 * Parses the 'null' literal.
	 * @return the number of consumed characters.
	 */
	private static int parseNull(String input, Box out) {
		int pos = prefix(input, "null");
		if (pos > 0) out.value = null;
		return pos;
	}

	/**
	 * Parses the fraction part of a number.
	 * @return the number of consumed characters.
	 */
	private static int parseFraction(String input) {
		int n, pos = 0;
		pos += n = prefix(input.substring(pos), ".");
		if (n == 0) return 0;
		pos += n = strspn(input.substring(pos), DIGIT);
		if (n == 0) return 0;
		return pos;
	}

	/**
	 * Parses exponent part of a number.
	 * @return the number of consumed characters.
	 */
	private static int parseExponent(String input) {
		int n, pos = 0;
		pos += n = strspn(input.substring(pos), "eE");
		if (n != 1) return 0;
		pos += n = strspn(input.substring(pos), "+-");
		if (n > 1) return 0;
		pos += n = strspn(input.substring(pos), DIGIT);
		if (n == 0) return 0;
		return pos;
	}

	/**
	 * Parses a json number.
	 * @return the number of consumed characters.
	 */
	private static int parseNumber(String input, Box out) {
		boolean is_double = false;
		int n, pos = 0;

		pos += n = prefix(input.substring(pos), "-");
		pos += n = prefix(input.substring(pos), "0");
		if (n == 0) {
			pos += n = strspn(input.substring(pos), DIGIT);
			if (n == 0) return 0;
		}

		pos += n = parseFraction(input.substring(pos));
		if (n > 0) is_double = true;

		pos += n = parseExponent(input.substring(pos));
		if (n > 0) is_double = true;

		String result = input.substring(0, pos);

		if (is_double)
			out.value = Double.parseDouble(result);
		else
			out.value = Integer.parseInt(result);
		return pos;
	}

	/**
	 * Parses a json array.
	 * @param input the input string to parse.
	 * @param out the output value.
	 * @return the number of consumed characters.
	 */
	private static int parseArray(String input, Box out) {
		int n, pos = 0;
		pos += n = prefix(input.substring(pos), "[");
		if (n == 0) return 0;
		List<Object> list = new ArrayList<Object>();

		pos += n = strspn(input.substring(pos), WHITESPACE);
		pos += n = prefix(input.substring(pos), "]");

		if (n > 0) {
			out.value = list;
			return pos;
		}

		for (;;) {
			Box box = new Box();
			pos += n = parseValue(input.substring(pos), box);
			if (n == 0)
				throw new ParseException("Expected list element.");
			list.add(box.value);
			pos += n = prefix(input.substring(pos), ",");
			if (n == 0) break;
		}
		pos += n = strspn(input.substring(pos), WHITESPACE);
		pos += n = prefix(input.substring(pos), "]");
		if (n == 0) return 0;
		out.value = list;
		return pos;
	}

	/**
	 * Parses an json object.
	 * @param input the input string to parse.
	 * @param out the output value.
	 * @return the number of consumed characters.
	 */
	private static int parseObject(String input, Box out) {
		int n, pos = 0;
		pos += n = prefix(input.substring(pos), "{");

		if (n == 0) return 0;
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		pos += n = strspn(input.substring(pos), WHITESPACE);
		pos += n = prefix(input.substring(pos), "}");

		if (n > 0) {
			out.value = map;
			return pos;
		}

		for (;;) {
			Box key = new Box();
			Box val = new Box();
			pos += n = strspn(input.substring(pos), WHITESPACE);
			pos += n = parseString(input.substring(pos), key);
			if (n == 0)
				throw new ParseException("Expected a map key");
			pos += n = strspn(input.substring(pos), WHITESPACE);
			pos += n = prefix(input.substring(pos), ":");
			if (n == 0) return 0;
			pos += n = parseValue(input.substring(pos), val);
			if (n == 0) return 0;
			map.put((String)key.value, val.value);
			pos += n = strspn(input.substring(pos), WHITESPACE);
			pos += n = prefix(input.substring(pos), ",");
			if (n == 0) break;

		}
		pos += n = strspn(input.substring(pos), WHITESPACE);
		pos += n = prefix(input.substring(pos), "}");

		if (n == 0) return 0;
		out.value = map;

		return pos;

	}

}
