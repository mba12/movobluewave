package com.movo.wave;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.movo.wave.util.LazyLogger;

//Created by alex on 5/1/2015.

/** Centralized animation enum
 *
 * @author Alexander Haase
 */
public enum LaunchAnimation {
    INHERIT {
        @Override
        public void animate(Activity activity) {}
    },
    SLIDE_LEFT {
        @Override
        public void animate(Activity activity) {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    },
    SLIDE_RIGHT {
        @Override
        public void animate(Activity activity) {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        }
    };

    public final static LazyLogger lazyLog = new LazyLogger( "LaunchAnimation");

    public final static String EXTRA_ANIMATE = "SLIDE_ANIMATE";

    /** Parse intent animate request, or return INHERIT as default
     *
     *  @param intent to parse
     *  @return Enum object to animate.
     */
    public static LaunchAnimation fromIntent( final Intent intent ) {
        final int index = intent.getIntExtra( EXTRA_ANIMATE, INHERIT.ordinal() );
        if( index >= 0 && index < values().length ) {
            return values()[ index ];
        } else {
            lazyLog.w( "Unrecognized LaunchAnimation ordinal: " + index);
            return INHERIT;
        }
    }


    /** Store animation enum in intent
     *
     * @param intent to update.
     */
    public void setIntent( final Intent intent ) {
        intent.putExtra( EXTRA_ANIMATE, ordinal() );
    }

    /** OVERRIDE to animate an activity
     *
     * @param activity to animate
     */
    public void animate( Activity activity ) {
        lazyLog.d( "No animation programmed for " + this );
    }

    /** pull incoming intent and animate
     *
     * @param activity to animate.
     */
    public static void apply( Activity activity ) {
        apply( activity, activity.getIntent() );
    }


    /** animate activity as indicated by intent
     *
     * @param activity to animate.
     * @param intent to read.
     */
    public static void apply( Activity activity, Intent intent ) {
        LaunchAnimation.fromIntent( intent ).animate( activity );
    }
}