package com.eroad.hr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.stream.Stream;

import com.eroad.hr.TimezoneLocator.ZoneNotFound;

/**
 * Purpose: Transform CSV input to generate time zone augmented output
 * Confession: when I started, I didn't pick up on the GIS level of the problem.
 * This class *appends* two new fields on the end of each CSV input line - the
 * time zone and the local time at that zone based on the GIS data. It requires
 * each line to have UTC datetime, latitude and longitude columns at positions
 * 0,1 & 2. See inner class Line for interpretation of input and output CSV
 * lines
 * 
 * *Assumption*: 
 * 
 * 1/ A fast generating source and throughput is an important factor. 
 *         
 * 2/ JDK 8 is acceptable. Chosen to prove wider knowledge of new
 * APIs applicable to problem: Streams and new date time. Code structure would
 * still lend to earlier JDKs.
 * 
 * 3/ In summer time, the test output will be adjusted for daylight savings.
 * There is an hour in autumn where the time is ambiguous. This is acceptable as
 * the UTC time is not ambiguous and data is not lost.
 * 
 * 4/ On exception the output for timezone and localised datetime are left blank
 * to prevent loss of data. (inc ArrayIndexOutOfBoundsException | NumberFormatException)
 * 
 * 5/ Adheres to GIS representation of @see com.eroad.hr.TimezoneLocator
 * 
 * 
 * Improvements: GIS data has multiple representations. This implementation only
 * uses decimal notation as a pair of Doubles.
 * 
 * @author drsog
 *
 */
public class CSV_Timestamp_TransformAPI {

	private TimezoneLocator time_zone_locator;

	// Taken from DateTimeFormatter.ISO_LOCAL_DATE_TIME; replacing T separator with
	// ':space:'
	static final DateTimeFormatter ISO_LOCAL_DATE_TIME;
	static {
		ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(' ').append(DateTimeFormatter.ISO_LOCAL_TIME)
				.toFormatter();
	}

	// Taken from PrintWriter. Applied here so class can apply to any Writer.
	static final String lineSeparator = java.security.AccessController
			.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));

	/**
	 * 
	 * @param timezoneLocator
	 *            - responsible for identifying ZoneId from GIS decimal coordinates
	 */
	public CSV_Timestamp_TransformAPI(TimezoneLocator timezoneLocator) {
		time_zone_locator = timezoneLocator;
	}

	/**
	 * Main API for reader transform Using Stream structure for extensibility
	 * Improve: make async so response blocks on input.
	 * 
	 * @param input_data_reader : source csv data
	 * @param writer updated with csv output.
	 * @throws IOException
	 */
	public void transform(Reader input_data_reader, Writer writer) throws IOException {

		Stream<String> line_stream = new BufferedReader(input_data_reader).lines();
		line_stream
				// Add filter if header in CSV
				.filter(line-> !line.isEmpty())
				.map(line -> new Line(line)) //
				.forEach(line -> line.writeOn(writer));
	}

	/**
	 * Alternative API, convenient for testing
	 * @param input_data_reader
	 * @return Output Reader
	 * @throws IOException
	 */
	public Reader transform(Reader input_data_reader) throws IOException {

		PipedWriter data_Writer = new PipedWriter();
		PipedReader response = new PipedReader(data_Writer);
		try (PrintWriter print_writer = new PrintWriter(data_Writer);) {
			transform(input_data_reader, print_writer);
		}
		return response;
	}

	/**
	 * Parsing of date field.
	 * 
	 * @param string
	 * @return DateTime without time zone
	 */
	LocalDateTime getLocalDate(String string) {
		return LocalDateTime.parse(string, ISO_LOCAL_DATE_TIME);
	}

	/**
	 * TODO: Integrate to local logging framework
	 * 
	 * @param line
	 */
	private void logFailedTransform(Line line) {
		System.err.format("%s", Arrays.toString(line.csv_line_array));
	}

	/**
	 * CSV input assumes position, possibly based on CSV header but in the current
	 * implementation the field semantics are assumed by the class in this inner
	 * class ONLY.
	 * 
	 * @author drsog
	 *
	 */
	private class Line {
		String[] csv_line_array;

		Line(String input_data) {
			csv_line_array = input_data.split(",");
		}

		ZoneId getZoneId() throws ZoneNotFound {
			return time_zone_locator.at(getLatitude(), getLongitude());
		}

		Double getLongitude() {
			return Double.valueOf(csv_line_array[2]);
		}

		Double getLatitude() {
			return Double.valueOf(csv_line_array[1]);
		}

		LocalDateTime getDateTime(ZoneId zone) {
			return getUTC_Date().withZoneSameInstant(zone).toLocalDateTime();
		}

		ZonedDateTime getUTC_Date() {
			ZonedDateTime date_local_to_UTC = getLocalDate(csv_line_array[0]).atZone(ZoneId.of("UTC"));
			return date_local_to_UTC;
		}

		/**
		 * Create a CSV output on writer. Current use cases have no risk of comma in
		 * input strings, so just concat
		 * 
		 * Exceptions result in writing out content of input csv_line_array for no data loss
		 * 
		 * @param print_writer
		 */
		public void writeOn(Writer print_writer) {
			try {
				for (String field : csv_line_array) {
					print_writer.write(field);
					print_writer.write(',');
				}
				ZoneId zone;
				try {
					zone = getZoneId();
				} catch (ZoneNotFound | ArrayIndexOutOfBoundsException | NumberFormatException  e ) {
					// Two empty fields created on exception
					print_writer.write(",");
					logFailedTransform(this);
					return;
				}
				print_writer.write(zone.getId());
				print_writer.write(',');
				print_writer.write(getDateTime(zone).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
				print_writer.write(lineSeparator);
			} catch (IOException e) {
				// Not expected as well tested standard JDK code
				logFailedTransform(this);
				throw new RuntimeException(e);
			}
		}

	}
}
