
import twitter4j.*;
import twitter4j.TwitterException;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


public class Main {

    static Twitter twitter;
    final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    final static ScheduledExecutorService preprocessingScheduler = Executors.newScheduledThreadPool(1);

    static Stream stream;
    static Configuration conf;
    //static WordNetDatabase database = WordNetDatabase.getFileInstance() ;



    public static void main(String[] args) throws TwitterException, FileNotFoundException, XMLStreamException {


     //   ResultsExporter exporter = new ResultsExporter();
     //   exporter.run();


        final Database database = new Database();
        final int TOTAL_TIME_OF_COLLECTING = 60*4320;



        database.truncate(database,"trends");   //removes all elements from "trends" collection
        database.truncate(database,"tweets");   //removes all elements from "trends" collection
        database.truncate(database,"results_of_analysis");


        ConfigurationBuilder cb = configOAuth();
        conf = cb.build();
        TwitterFactory factory = new TwitterFactory(conf);

        twitter = factory.getInstance();
        stream = new Stream(database,conf);

        final Runnable beeper = new Runnable() { //beeper runs every 10 seconds

            public void run() {
                Trends trends = null;
                try {
                    trends = twitter.getPlaceTrends(1);     //get the thends with WOEID 1 (WorldWide)
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                twitter4j.Trend[] trendsList = trends.getTrends();    //get the trends

                ArrayList<String> lastlyAddedTrends = new ArrayList<>();    //array containing the lastly added trends

                for (int i=0;i<10;i++) {    //for every trend
                    twitter4j.Trend trend = trendsList[i];

                    database.insertTrendInDb(trend);
                    lastlyAddedTrends.add(trend.getName());

                }

                database.updateTrendInfos(lastlyAddedTrends);
                database.updateActiveTrends();
                stream.updateQuery(database.activeTrends);

            }
        };


        final ScheduledFuture<?> beeperHandle = scheduler.scheduleAtFixedRate(beeper, 0, 60*5, TimeUnit.SECONDS);  //0 seconds to delay for first execution , repeat every 10 seconds

        scheduler.schedule(new Runnable() {
            public void run() {
                beeperHandle.cancel(true);
            }
        }, TOTAL_TIME_OF_COLLECTING , TimeUnit.SECONDS);    //runs for a total of 10*6 seconds



        /*
        final Runnable preprocessingBeeper = new Runnable() {
            @Override
            public void run() {


                stream.shutDownStream();    //before preprocessing starts, stream has to shutdown

                Preprocessing preprocessing = new Preprocessing(database);
                System.out.println("Preprocessing Started!");
                preprocessing.run();    //runs preprocessing
                System.out.println("Preprocessing Finished! ");
                try {
                    database.calculateSentimentForTweets();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                }
                System.out.println("Calculation Finished!");
                System.exit(1);

            }
        };

        final ScheduledFuture<?> preprocessingHandle = preprocessingScheduler.scheduleAtFixedRate(preprocessingBeeper,TOTAL_TIME_OF_COLLECTING+10,1000000000,TimeUnit.SECONDS);
*/
    }


    /**
     * Method used in order to configure the OAuth settings. Keys and secrets were generated in dev.twitter.com
     *
     * @return
     */
    public static ConfigurationBuilder configOAuth(){
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("")
                .setOAuthConsumerSecret("")
                .setOAuthAccessToken("")
                .setOAuthAccessTokenSecret("");

        cb.setJSONStoreEnabled(true);
        return cb;
    }
}




