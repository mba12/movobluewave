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

    /** ISO-8601 date formatter.
     *
     * @param calendar to format (long, epoch milliseconds)
     * @return ISO 8601 formatted string
     */
    public static String isoFormat( final Calendar calendar ) {
        return dateFormat.format( calendar.getTime() );
    }

    public static Date parse( final String iso8601 ) throws java.text.ParseException {
        return dateFormat.parse(iso8601);
    }

    /** Creates a new utc calendar object
     *
     * @return utc calendar
     */
    static public Calendar newCal() {
        return Calendar.getInstance(timeZone);
    }

    //prevent object creation
    private UTC() {}

    /**
     * Field ordering for truncating calendars.
     */
    private static final int[] fieldOrdering = new int[] {
            Calendar.YEAR,
            Calendar.MONTH,
            Calendar.DATE,
            Calendar.HOUR_OF_DAY,
            Calendar.MINUTE,
            Calendar.SECOND,
            Calendar.MILLISECOND,
    };


    /** Truncates a timestamp in UTC.
     *
     * @param timestamp to truncate.
     * @param precision maximum field to preserve (i.e. Calendar.DATE would zero HOUR_OF_DAY and below).
     * @return truncated timestamp.
     * @throws ArrayIndexOutOfBoundsException if fields is not in YEAR, MONTH, DATE, HOUR_OF_DAY, MINUTE, SECOND, or MILLISECOND.
     */
    public static long truncateTo( final long timestamp, final int precision ) throws ArrayIndexOutOfBoundsException{
        boolean begun = false;

        final Calendar ret = UTC.newCal();
        ret.clear();
        ret.setTimeInMillis( timestamp );

        for( final int field : fieldOrdering ) {
            if( begun ) {
                ret.set( field, ret.getMinimum( field ) );
            } else if( field == precision ) {
                begun = true;
            }
        }

        if( ! begun ) {
            throw new ArrayIndexOutOfBoundsException( precision );
        }
        return ret.getTimeInMillis();
    }

    /** Convenience COW truncate method for Date objects
     *
     * @see {@link #truncateTo(long, int)  truncateTo(long, int)}
     * @param date to truncate.
     * @param precision to truncate to.
     * @return new truncated Date object.
     */
    public Date truncateTo( final Date date, final int precision ) {
        return new Date( truncateTo( date.getTime(), precision ) );
    }

    /** Convenience COW truncate method for Calendar objects.
     *
     * Truncates in UTC, returns Calendar in source's time zone.
     *
     * @see {@link #truncateTo(long, int)  truncateTo(long, int)}
     * @param calendar to truncate.
     * @param precision to truncate to.
     * @return new truncated Date object.
     */
    public Calendar truncateTo( final Calendar calendar, final int precision ) {
        final Calendar ret = Calendar.getInstance( calendar.getTimeZone() );
        ret.setTimeInMillis( truncateTo( calendar.getTimeInMillis(), precision ) );
        return ret;
    }
}
