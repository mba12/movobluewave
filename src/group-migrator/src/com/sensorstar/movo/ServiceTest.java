package com.sensorstar.movo;

/**
 * Created by Michael Ahern on 11/13/15.
 */

public class ServiceTest implements Runnable{

//	final static Logger logger = Logger.getLogger("GM");

    /* Time between saving checkpoint time and checking for new users */
    private static long CHECKPOINT_INTERVAL = 36000;
    private static int SQL_BATCH_DELAY = 10000;

    ServiceTest(){
        System.out.println("Service Coming Alive: " + System.currentTimeMillis());
    }

    public void update(){
        System.out.println("Running an update " + System.currentTimeMillis());
    }

    public void run() {

        System.out.println("Starting queue listener loop");
        try{
            while(!Thread.currentThread().isInterrupted()) {
                Thread.sleep(SQL_BATCH_DELAY);
                System.out.println("Queue listener iteration: " + System.currentTimeMillis());
            }

        } catch (InterruptedException e) {
            System.out.println("Stopping Queue listener thread...");
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
                    System.out.println("Shutting down ...");
	                /* Save out Queue */
                    msg_thread.interrupt();
                    Thread.sleep(10000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        while(true){

            gm.update();

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
    }
}

