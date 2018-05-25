/**
 * 
 */
package com.eroad.hr;

import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;

import net.iakovlev.timeshape.TimeZoneEngine;

/**
 * Transforming GIS to time zones is not trivial. Google APIs can do this, and
 * may be a more accurate source, but there is a limit to the rate of queries
 * that can be automated. Also, each request would take a webservice call, as do
 * a number of similar services.
 * 
 * I selected net.iakovlev.timeshape.TimeZoneEngine on the basis of performance.
 * https://github.com/RomanIakovlev/timeshape
 * 
 * Licensing:  
 * MIT License for code
 * Further analysis needed for the data license.
 * https://github.com/RomanIakovlev/timeshape/blob/master/DATA_LICENSE
 * 
 * 
 * I would recommend an improvement where each query tests the sensitivity of
 * the result close to timezone boundaries and make an authoritive query against the Google API for
 * those cases, using the result to update the local geometry.
 * 
 * I would recommend reviewing the indexing of TimeZoneEngine for performance.
 * 
 * Source geometric data in
 * net/iakovlev/timeshape/2018d.1/timeshape-2018d.1.jar/timezones.geojson.zip
 * 
 *
 * 
 * @author drsog
 *
 */
public class TimeShapeTimezoneLocator implements TimezoneLocator {

	static private final TimeZoneEngine engine;

	static {
		engine = TimeZoneEngine.initialize();
	}

	public TimeShapeTimezoneLocator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eroad.hr.TimezoneLocator#at(java.lang.Double, java.lang.Double)
	 */
	@Override
	public ZoneId at(Double latitude, Double longitude) throws ZoneNotFound {
		Optional<ZoneId> option = engine.query(latitude, longitude);
		try {
			return option.get();
		} catch (NoSuchElementException e) {
			throw new ZoneNotFound();
		}
	}

}
