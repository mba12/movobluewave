package com.movo.wave.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/** Utility namespace for using UTC time
 * All wave devices are dealt with in UTC.
 */
public class UTC {
    final private static DateFormat dateFormat;
    final private static DateFormat dateFormatShort;
    final public static TimeZone timeZone = TimeZone.getTimeZone( "UTC" );
    static {
        dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US );
        dateFormat.setTimeZone( timeZone );

        dateFormatShort = new SimpleDateFormat( "'T'HH:mm:ss'Z'", Locale.US );
        dateFormatShort.setTimeZone( timeZone );
    }

    /** ISO-8601 date formatter.
     *
     * @param date to format
     * @return ISO 8601 formatted string
     */
    public static String isoFormat( final Date date ) {
        return dateFormat.format( date );
    }

    /** ISO-8601 date formatter.
     *
     * @param timestamp to format (long, epoch milliseconds)
     * @return ISO 8601 formatted string
     */
    public static String isoFormat( final long timestamp ) {
        return dateFormat.format( new Date( timestamp ) );
    }
    public static String isoFormatShort( final long timestamp ) {
        return dateFormatShort.format( new Date( timestamp ) );
    }

    /** Creates a new utc calendar object
     *
     * @return utc calendar
     */
    static public Calendar newCal() {
        return Calendar.getInstance( timeZone );
    }

    //prevent object creation
    private UTC() {}
}
