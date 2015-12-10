package com.sensorstar.movo;

import org.apache.commons.cli.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Created by Michael Ahern on 10/30/15.
 */
public class ConnectionTest {

    private static String DB_URL = "jdbc:mysql://173.194.239.157:3306/movogroups?useSSL=true&requireSSL=true";  // test
    static String keyStorePassword = "r87p-Y?72*uXqW$aSZGU"; // keystore
    static String trustStorePassword = "r87p-Y?72*uXqW$aSZGU";  // truststore
    private static String username = "movogroups";
    private static String password = "H8$E=?3*ADXFt4Ld7-jw";
    private static final String query = " select count(*) as total from steps";
    private static String FB_URL = "https://movowave.firebaseio.com/";
    private static final boolean USING_GAE_SQL = true;
    final static Logger logger = Logger.getLogger("ConnectionTest");


    public static void main(String[] args) throws org.apache.commons.cli.ParseException{

        Options options = new Options();
        options.addOption("checkpoint", 		true, 	"sets a checkpoint time to start looking for syncs (YYYY-MM-DDThh:dd:ssZ format). By default it looks for a checkpoint.ser file and if none is found it uses YYYY0000-00-00T00:00:00Z.");
        options.addOption("checkpointInterval", true, 	"Sets how often to checkpoint & look for new users (default is once per hour).");

        options.addOption("help",				false, 	"print this message" );
        options.addOption("useSSL",				false, 	"use SSL for MySQL (needed for GAE)" );
        options.addOption("keyStore", 			true, 	"sets where to look for our keyStore to access Firebase and GAE SQL instance. Default location is <current_location>/keystore.");
        options.addOption("keyStorePass",		true, 	"password for keystore" );
        options.addOption("trustStore", 		true, 	"sets where to look for our keyStore to access Firebase and GAE SQL instance. Default location is <current_location>/truststore.");
        options.addOption("trustStorePass",		true, 	"password for truststore" );

        options.addOption("FirebaseURL", 		true, 	"URL to connect to Firebase Instance. Default is:" + FB_URL);
        options.addOption("FirebaseSecret", 	true, 	"Auth Cookie to connect to Firebase Instance.");
        options.addOption("MysqlURL", 			true, 	"URL to connect to MySQL Server. Default is:" + DB_URL);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);


        try {
            // Class.forName("com.mysql.jdbc.Driver");

            if (cmd.hasOption("useSSL") || USING_GAE_SQL){

                if (cmd.hasOption("keyStore")){
                    System.setProperty("javax.net.ssl.keyStore", cmd.getOptionValue("keyStore"));
                    logger.info("javax.net.ssl.keyStore" + " :: " + cmd.getOptionValue("keyStore"));
                }else{
                    System.setProperty("javax.net.ssl.keyStore", System.getProperty("user.dir")+"/keystore");
                }

                if (cmd.hasOption("keyStorePass")){
                    System.setProperty("javax.net.ssl.keyStorePassword", cmd.getOptionValue("keyStorePass"));
                }else{
                    // System.setProperty("javax.net.ssl.keyStorePassword",	"movomovo");
                    System.setProperty("javax.net.ssl.keyStorePassword",	keyStorePassword);
                    logger.info("javax.net.ssl.keyStorePassword" + " :: " + keyStorePassword);
                }

                if (cmd.hasOption("trustStore")){
                    System.setProperty("javax.net.ssl.trustStore", cmd.getOptionValue("trustStore"));
                    logger.info("javax.net.ssl.trustStore" + " :: " + cmd.getOptionValue("trustStore"));
                }else{
                    System.setProperty("javax.net.ssl.trustStore",			System.getProperty("user.dir")+"/truststore");
                }

                if (cmd.hasOption("trustStorePass")){
                    System.setProperty("javax.net.ssl.trustStorePassword", cmd.getOptionValue("trustStorePass"));


                }else{
                    // System.setProperty("javax.net.ssl.trustStorePassword",	"movomovo");
                    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
                    logger.info("javax.net.ssl.trustStorePassword" + " :: " + trustStorePassword);
                }

                Class.forName("com.mysql.jdbc.Driver");

            }
        } catch (ClassNotFoundException e) {e.printStackTrace();}

        try {
            logger.info("Getting Connection");
            Connection conn = DriverManager.getConnection(DB_URL, username, password);
            // Connection conn = DriverManager.getConnection(DB_URL, "root", "");
            // Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            stmt.execute(query);
            logger.info("Executing");
            ResultSet rs = stmt.getResultSet();
            while(rs.next()) {
                int total = rs.getInt(1);
                logger.info("Total is: " + total);
            }
            logger.info("Done");
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe("Exeception: " + e.getMessage());
        }
    }
}
