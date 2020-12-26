import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

public final class Test {


	/*
	 * Tests
	 */

	public static boolean equals(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return a.equals(b);
	}

	private static void test(Object a, Object b) {
		if (equals(a, b))
			return;

		System.out.println("FAIL: " + a + " != " + b);
		System.exit(1);
	}

	private static void test(Object obj) {
		test(obj, Json.fromJson(Json.toJson(obj)));
	}

	public static void test() {
		test(null);
		test(true);
		test(false);
		test("");
		test("fo\"oo\n");
		test(0);
		test(123);
		test(-123);
		test(0.123);
		test(Arrays.asList());
		test(Arrays.asList(1));
		test(Arrays.asList(1, 2));
		test(Arrays.asList(1, 2, 3));
		test(Arrays.asList(1, 2, 3, "a"));
		test(Arrays.asList(1, 2, 3, "a", "b"));
		test(Arrays.asList(1, 2, 3, "a", "b", "c"));

		Map<String, Object> map = new LinkedHashMap<String, Object>();

		test(map);

		map.put("gg", Arrays.asList("foo", 123, "bar"));
		map.put("aa", Arrays.asList());
		test(map);


	}



	static void parser_test(boolean should_succeed, String description, String input) {

		boolean result = false;

		try {
			Object obj = Json.fromJson(input);
			result = true;
		} catch (Exception e) {
		}

		if (result == should_succeed)
			return;

		if (should_succeed)
			System.err.println("Parsing should have been successful");

		if (!should_succeed)
			System.err.println("Parsing should have failed");

		System.err.println("  Description: " + description);
		System.err.println("        Input: " + input);

		System.exit(1);

	}

	static String readFile(File file) throws IOException {
		InputStream in = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			in = new FileInputStream(file);
			byte[] buf = new byte[0x1000];
			for (int n = in.read(buf); n != -1; n = in.read(buf))
				out.write(buf, 0, n);
			return new String(out.toByteArray(), "UTF-8");

		} finally {
			if (in != null)
				in.close();
		}

	}

	/*
	 * Tests from
	 * https://github.com/nst/JSONTestSuite
	 */

	static void print_error(File file,  String message) {
		System.out.println(file.getPath() + ":0: " + message);
	}

	static int n = 0;

	static void testFile(File file) throws IOException {

		// these two test are currently not supported.
		// need to define max stack depth.
		if ("n_structure_open_array_object.json".equals(file.getName()))
			return;
		if ("n_structure_100000_opening_arrays.json".equals(file.getName()))
			return;

		String name = file.getName();

		String input = readFile(file);

		n++;

		try {
			Object obj = Json.fromJson(input);
			/*
			 * Parser success
			 */
			if (name.startsWith("y_"))
				return;
			if (name.startsWith("i_"))
				return;
			print_error(file, n + " Parsing should have failed.");
			System.exit(1);
		} catch (Exception e) {
			/*
			 * Parser failed.
			 */
			if (name.startsWith("i_"))
				return;
			if (name.startsWith("n_"))
				return;
			print_error(file, n + " Parsing should have succeed.");
			System.exit(1);
		}





	}



	public static void main(String[] args) throws IOException {

		test();

		File test_folder = new File("JSONTestSuite/test_parsing/");
		File[] test_files = test_folder.listFiles();


		if (test_files == null) {
			System.out.println("Can't find the json test suit.");
			return;

		}

		for (File file : test_files)
			testFile(file);

	}

}
