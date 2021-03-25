package model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class DateConverter {
	
	private static  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssX");
	
	public static String convert(String timestamp) {
		if(timestamp == null) {
			return null;
		}
		Timestamp tm = Timestamp.valueOf(timestamp);
		return df.format(tm);
	}

}
