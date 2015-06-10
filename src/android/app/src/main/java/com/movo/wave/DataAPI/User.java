package com.movo.wave.DataAPI;

import android.content.ContentValues;

import java.util.Date;
import java.util.Observable;

//Created by alex on 6/9/2015.

/** Observable user class.
 *
 * Issues events for update of
 *   User Info
 *   User Photos
 *   User Steps
 *
 * @author Alexander Haase on 6/9/2015
 */
public class User extends Observable {

    final public String UID;
    final public String email;
    private String password;

    /** Base event class for notifying user-data changes
     *
     */
    static abstract class Event {
        final long timestamp;
        final User user;

        public Event( final User user ) {
            this.user = user;
            timestamp = new Date().getTime();
        }
    }

    /** Info describes strings for ContentValue keys
     *
     */
    public enum Info {
        DATE_OF_BIRTH,
        GENDER,
        WEIGHT,
        HEIGHT,
        FULL_NAME,
        USER_NAME
    }

    private ContentValues info;

    /** Info update settings
     *
     * new values will be broadcast via observer pattern
     *
     * @param values new Info values
     */
    public void replaceInfo( ContentValues values ) {
        info = values;
        setChanged();
        //FIXME: store to sql
        //FIXME: store to firebase
        notifyObservers( new InfoEvent( this, info ) );
    }

    /** event for step info
     *
     * # FIXME! Firebase notify....
     */
    static class StepEvent extends Event {
        final long begin;
        final long end;
        final int steps;

        public StepEvent( final User user, final long begin, final long end, final int steps ) {
            super( user );
            this.begin = begin;
            this.end = end;
            this.steps = steps;
        }
    }

    static class InfoEvent extends Event {
        final ContentValues info;

        public InfoEvent( final User user, final ContentValues info ) {
            super( user );
            this.info = info;
        }
    }
}
