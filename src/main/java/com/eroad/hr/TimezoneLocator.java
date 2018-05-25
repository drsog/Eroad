package com.eroad.hr;

import java.time.ZoneId;

public interface TimezoneLocator {

	/**
	 * @param latitude
	 *            decimal degrees North (positive) of equator
	 * @param longitude
	 *            decimal degrees East (positive) of Greenwich meridian
	 * @return ZoneId
	 */
	ZoneId at(Double latitude, Double longitude) throws ZoneNotFound;

	class ZoneNotFound extends Exception {

	}

}
