package com.sensorstar.movo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Created by Michael Ahern on 11/13/15.
 */

public class ServiceTest implements Runnable{

    final static private Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /* Time between saving checkpoint time and checking for new users */
    private static long CHECKPOINT_INTERVAL = 36000;
    private static int SQL_BATCH_DELAY = 10000;
    private static File db_log = new File("/home/ahern/realtime/test_dbheartbeat.txt");
    private static File main_log = new File("/home/ahern/realtime/test_mainheartbeat.txt");


    ServiceTest(){
        logger.log(Level.INFO, "Service Coming Alive: " + System.currentTimeMillis());
    }

    public static void dbConnectionHeartBeat(boolean status){

        try{
            if(!db_log.exists()){
                logger.log(Level.INFO, "Created new heartbeat file.");
                db_log.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(db_log, false);

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( status?String.valueOf(System.currentTimeMillis()):"0" ); // date +"%s"
            bufferedWriter.close();

        } catch(IOException e) {
            logger.log(Level.INFO, "COULD NOT LOG HEARTBEAT!!");
        }
    }

    public static void mainThreadHeartBeat(boolean status){

        try{
            if(!main_log.exists()){
                logger.log(Level.INFO, "Created new main thread file.");
                main_log.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(main_log, false);

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( status?String.valueOf(System.currentTimeMillis()):"0" ); // date +"%s"
            bufferedWriter.close();

        } catch(IOException e) {
            logger.log(Level.INFO, "COULD NOT LOG HEARTBEAT!!");
        }
    }


    public void update(){
        logger.log(Level.INFO, "Running an update " + System.currentTimeMillis());
    }

    public void run() {

        logger.log(Level.INFO, "Starting queue listener loop");
        try{
            while(!Thread.currentThread().isInterrupted()) {
                Thread.sleep(SQL_BATCH_DELAY);
                logger.log(Level.INFO, "Queue listener iteration: " + System.currentTimeMillis());
            }

        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Stopping Queue listener thread...");
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws org.apache.commons.cli.ParseException{

        ServiceTest gm = new ServiceTest();

        // start SQL Queue Thread
        final Thread msg_thread = new Thread(gm);
        msg_thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    logger.log(Level.INFO, "Shutting down ...");
	                /* Save out Queue */
                    msg_thread.interrupt();
                    Thread.sleep(10000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        ServiceTest.mainThreadHeartBeat(true);
        long mainThreadTime = System.currentTimeMillis();

        while(true){

            gm.update();

            // Heartbeat check every minute
            if (System.currentTimeMillis() - mainThreadTime > 60000) {
                GroupMigrator.mainThreadHeartBeat(true);
                mainThreadTime = System.currentTimeMillis();
            }

            try {
                Thread.sleep(CHECKPOINT_INTERVAL);
                boolean alive = msg_thread.isAlive();
                if(!alive) {
                    msg_thread.start();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        ServiceTest.mainThreadHeartBeat(false);
    }
}

