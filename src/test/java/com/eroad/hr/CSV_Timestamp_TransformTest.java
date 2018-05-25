/**
 * 
 */
package com.eroad.hr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * See com.eroad.hr.CSV_Timestamp_TransformTest.main(String[]) for CLI execution
 * 
 * @author drsog
 */
public class CSV_Timestamp_TransformTest {

	static final Charset cs = Charset.forName("UTF-8");
	static final String source1 = "2013-07-10 02:52:49,-44.490947,171.220966";
	static final String output1 = source1 + ",Pacific/Auckland,2013-07-10T14:52:49";
	static final String source2 = "2013-07-10 02:52:49,-33.912167,151.215820";
	static final String output2 = source2 + ",Australia/Sydney,2013-07-10T12:52:49";
	static final String source_bad = "2013-07-10 02:52:49,151.215820,-33.912167";
	static final String output_bad = source_bad + ",,";

	@Test
	public void test_single_line_input1() throws IOException {

		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());

		// Pattern of use for input stream data
		try (Reader input_data_reader = new InputStreamReader(new ByteArrayInputStream(source1.getBytes(cs)), cs);) {
			Reader stream_output = api.transform(input_data_reader);

			// Test line response
			String response = new BufferedReader(stream_output).readLine();
			Assert.assertEquals(output1, response);
		}

	}

	@Test
	public void test_single_line_input2() throws IOException {

		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());

		// Pattern of use for input stream data
		try (Reader input_data_reader = new InputStreamReader(new ByteArrayInputStream(source2.getBytes(cs)), cs);) {
			Reader stream_output = api.transform(input_data_reader);

			// Test line response
			String response = new BufferedReader(stream_output).readLine();
			Assert.assertEquals(output2, response);
		}

	}

	@Test
	public void test_single_line_baddata() throws IOException {

		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());

		// Pattern of use for input stream data
		try (Reader input_data_reader = new InputStreamReader(new ByteArrayInputStream(source_bad.getBytes(cs)), cs);) {
			Reader stream_output = api.transform(input_data_reader);

			// Test line response
			String response = new BufferedReader(stream_output).readLine();
			Assert.assertEquals(output_bad, response);
		}

	}

	@Test
	@Ignore
	public void test_stream_api() throws IOException {
		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());
		Reader input_data_reader = new InputStreamReader(new ByteArrayInputStream(source1.getBytes(cs)), cs);

		// Pattern of use
		try (BufferedReader br = new BufferedReader(input_data_reader); Stream<String> stream_input = br.lines()) {
			// Stream stream_output = api.transform(stream_input);

			// Test line response
			Assert.fail("Not yet implemented");
		}

	}

	/**
	 * Test ZonedDateTime behaviour
	 * 
	 * @param string
	 * @return
	 */
	ZonedDateTime getUTC_Date(CSV_Timestamp_TransformAPI api, String string) {
		ZonedDateTime date_local_to_UTC = api.getLocalDate(string).atZone(ZoneId.of("UTC"));
		return date_local_to_UTC;
	}

	@Test
	public void test_date_field_transform() throws Exception {
		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());
		ZonedDateTime date = getUTC_Date(api, "2013-07-10 02:52:49");
		System.out.println(date.withZoneSameInstant(ZoneId.of("Pacific/Auckland")));
	}

	/**
	 * Modify test data to transform a January datetime
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_date_field_summertime_transform() throws Exception {
		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());
		ZonedDateTime date = getUTC_Date(api, "2013-01-10 02:52:49");
		System.out.println(date.withZoneSameInstant(ZoneId.systemDefault()));
	}

	/**
	 * Verify empty line handled
	 * @throws IOException 
	 */
	@Test
	public void test_empty() throws IOException {
		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());

		try (Reader input_data_reader = new StringReader("\n\n")) {
			Reader stream_output = api.transform(input_data_reader);
			Assert.assertFalse(stream_output.ready());
		}

	}

	/**
	 * Verify both crlf and lf both function as expected
	 */
	@Test
	@Ignore
	public void test_line_endings() {

	}

	/**
	 * CLI to run against a set of files usage
	 * mvn exec:java -q -Dexec.classpathScope=test -Dexec.mainClass="com.eroad.hr.CSV_Timestamp_TransformTest" -Dexec.arguments="src/test/resources/com/eroad/hr/TestData.csv"
	 * or
	 * ls -1  "src/test/resources/com/eroad/hr/*.csv" | xargs java -cp $base/com/eroad/java_technical_test/drsog/0.0.1-SNAPSHOT/drsog-0.0.1-SNAPSHOT.jar:$base/net/iakovlev/timeshape/2018d.1/timeshape-2018d.1.jar:$base/com/fasterxml/jackson/core/jackson-databind/2.2.3/jackson-databind-2.2.3.jar:$base/com/fasterxml/jackson/core/jackson-core/2.2.3/jackson-core-2.2.3.jar:$base/com/fasterxml/jackson/core/jackson-annotations/2.2.3/jackson-annotations-2.2.3.jar:$base/de/grundid/opendatalab/geojson-jackson/1.8/geojson-jackson-1.8.jar:$base/com/esri/geometry/esri-geometry-api/2.1.0/esri-geometry-api-2.1.0.jar: com.eroad.hr.CSV_Timestamp_TransformTest
	 * where $base points to your maven repo (~/.m2/repository)
	 * 
	 * Prints to std out.  Suggest pipe to output
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {	

		CSV_Timestamp_TransformAPI api = new CSV_Timestamp_TransformAPI(new TimeShapeTimezoneLocator());

		for (String filename : args) {
			try (PrintWriter print_writer = new PrintWriter(System.out, true); InputStream fileStream = new FileInputStream(filename);) {
				InputStreamReader input_data_reader = new InputStreamReader(fileStream); // please change
																									// encoding
																									// depending on you
																									// source - ie "UTF-8"
				api.transform(input_data_reader, print_writer);
			}
		}

	}

}
