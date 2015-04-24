package com.movo.wave.util;

import android.util.Log;

//Created by alex on 4/23/2015.

/** Lazy-logging class for easy logging
 *
 */
public class LazyLogger {

    private boolean enabled = false;
    final String TAG;
    final LazyLogger parent;

    /** Convenience constructor for no parent.
     *
     * @param TAG android log TAG.
     * @param enabled boolean initial logging state.
     */
    public LazyLogger(String TAG, boolean enabled ) {
        this(TAG, enabled, null);
    }

    /** Constructor for lazy logger.
     *
     * @param TAG android log TAG.
     * @param enabled boolean initial logging state.
     * @param parent parent logger or null.
     */
    public LazyLogger( String TAG, boolean enabled, LazyLogger parent) {
        this.TAG = TAG;
        this.enabled = enabled;
        this.parent = parent;
    }

    /** Convenience constructor for enabled logging.
     *
     * @param TAG android log TAG.
     * @param parent parent logger or null.
     */
    public LazyLogger( String TAG, LazyLogger parent) {
        this( TAG, true, parent );
    }

    /** Convenience constructor for enabled logging and no parent.
     *
     * @param TAG android log TAG.
     */
    public LazyLogger(String TAG) {
        this( TAG, true, null );
    }

    public boolean isEnabled() {
        return enabled && (parent == null || parent.isEnabled());
    }

    public void enable(boolean state) {
        enabled = state;
    }

    static public String build( final Object[] args ) {
        final String msg;
        if( args == null || args.length == 0 ) {
            msg = "null";
        } else if( args.length == 1 ) {
            msg = String.valueOf( args[ 0 ] );
        } else {
            StringBuilder buf = new StringBuilder();
            for( final Object arg : args ) {
                buf.append( arg );
            }
            msg = buf.toString();
        }

        return msg;
    }

    /** Warning level logging
     *
     * @param args variable args to log.
     */
    public void w( final Object... args ) {
        if( isEnabled() ) {
            Log.w( TAG, build( args ) );
        }
    }

    /** Verbose level logging
     *
     * @param args variable args to log.
     */
    public void v( final Object... args ) {
        if( isEnabled() ) {
            Log.v( TAG, build( args ) );
        }
    }

    /** Debug level logging
     *
     * @param args variable args to log.
     */
    public void d( final Object... args ) {
        if( isEnabled() ) {
            Log.d( TAG, build( args ) );
        }
    }

    /** Info level logging
     *
     * @param args variable args to log.
     */
    public void i( final Object... args ) {
        if( isEnabled() ) {
            Log.i( TAG, build( args ) );
        }
    }

    /** Error level logging
     *
     * @param args variable args to log.
     */
    public void e( final Object... args ) {
        if( isEnabled() ) {
            Log.e( TAG, build( args ) );
        }
    }

    /** Pass-through to android logging with enabled-checking.
     *
     * @param priority
     * @param args
     */
    public void log( final int priority, final Object... args ) {
        if( isEnabled() ) {
            Log.println( priority, TAG, build( args ));
        }
    }

    /** Assertion-style logging. Always enabled.
     *
     * @param condition assertion condition.
     * @param args variable args to log.
     * @return condition pass-through
     */
    public boolean a( final boolean condition, final Object... args ) {
        if( ! condition ) {
            Log.e( TAG, "Assertion Failed! " + build( args ) );
        }
        return condition;
    }
}
