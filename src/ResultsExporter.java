import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.lang.Object;
import java.util.Map;

/**
 * Class dedicated to exporting results for the web app
 */
public class ResultsExporter {

    ArrayList<String> sentiments = new ArrayList<String>() {{
        add("anger");
        add("disgust");
        add("fear");
        add("joy");
        add("sadness");
        add("surprise");

    }};

    ArrayList<Trend> trends=null;
    ArrayList<Tweet> tweets=null;
    MongoClient mongoClient = null;
    MongoDatabase database = null;
    SentimentAnalysis analysis;


    public ResultsExporter() throws FileNotFoundException, XMLStreamException {
        analysis = new SentimentAnalysis();
        analysis.initialize();

        tweets = new ArrayList<Tweet>();
        trends = new ArrayList<>();
        mongoClient = new MongoClient();
        database = mongoClient.getDatabase("TestingDB");
    }

    public void run() throws FileNotFoundException, XMLStreamException {

        fetchTrends();  //fetches the trends from MongoDB
        fetchTweets();  //fetches the tweets from MongoDB
        findTrendsTweets();  //for each trend finds it's tweets
        divideTrendsInTimeframes(); //divides each Trend in equal timeframes

        for (Trend trend : trends){     //for every trend
            trend.calculateSentimentCalibrationForEachTimeframe();  //calculate the sentimental Calibration for each time frame
            trend.calculateRepresentativeWordsForEachTimeframe(analysis);   //find the representative words for each time frame
            trend.calculateRepresentativeWordsForEachSentiment(analysis);   //finds the representative words for each sentiment
        }

        saveResultsInMongo();

    }

    public void divideTrendsInTimeframes(){
        for (Trend trend : trends){
            trend.divideTimelineInTimeframes(5);   //NA ALLAXEI NA VRISKEI TO PLITHOS
        }
    }

    /**
     * Method that finds the tweets of each trend
     */
    public void findTrendsTweets(){

        for (Tweet tweet : tweets){     //for each tweet
            String text = tweet.text;   //gets the text of the tweet
            for(Trend trend : trends){  //for each trend
                if (text.contains(trend.trend.toLowerCase())){    //if the tweet contains the trend
                    trend.tweets.add(tweet);    //its added in the trends list
                }

            }
        }



        for(Trend trend : trends){
            trend.clearTweetsUsingLevenstein();
        }


    }

    public void fetchTweets(){

        final SimpleDateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy");
        FindIterable tweetsIterable = database.getCollection("tweets").find();

        tweetsIterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {

                    String stringJSON = document.get("json").toString();    //takes the json
                    JSONObject jsonObject = null;
                    Date dateCreated = null;
                    String username = null;
                    try {
                        jsonObject = new JSONObject(stringJSON); //converts to json object
                        String dateInJson = jsonObject.getString("created_at");
                        dateCreated = format.parse(dateInJson);

                        String jsonOfUser = jsonObject.getString("user");

                        JSONObject userJson = new JSONObject(jsonOfUser);
                        username = userJson.getString("name");  //gets the username of the tweet creator

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    Tweet newTweet = new Tweet(username, dateCreated, (String) document.get("tweet_after_preprocessing"), (Double) document.get("anger"), (Double) document.get("disgust"), (Double) document.get("fear"), (Double) document.get("joy"), (Double) document.get("sadness"), (Double) document.get("surprise"));    //creating a new Tweet object
                    tweets.add(newTweet);

            }
        });

    }

    /**
     * Fetches the trends in order to be saved in the ArrayList
     */
    public void fetchTrends(){

        final FindIterable trendsIterable = database.getCollection("trends").find();  //gets all the trends



        trendsIterable.forEach(new Block<Document>() {


            @Override
            public void apply(Document document) {  //for each trend from MongoDB
                if ((Date) document.get("finish_time") != null) {
                    Trend newTrend = new Trend((String) document.get("trend_title"), (Date) document.get("start_time"), (Date) document.get("finish_time"));   //gets the trend info and saves them in a new trend
                    trends.add(newTrend);   //adds the trend in the trends list
                }
            }

        });


    }

    public void saveResultsInMongo(){


        Mongo mongo = new Mongo("localhost");
        DB db = mongo.getDB("TestingDB");
        DBCollection collection = db.getCollection("results_of_analysis");
        collection.remove(new BasicDBObject());
        BasicDBObject jsonDocument = new BasicDBObject();
        int trendCounter =1;    //counts the trends in order to label them

        for(Trend trend : trends){

            BasicDBObject trendDocument = new BasicDBObject();
            trendDocument.put("trend_name",trend.trend);

            int counter = 1;
            for(TimeFrame timeFrame : trend.timeFrames){

                BasicDBObject timeframeDocument = new BasicDBObject();
                timeframeDocument.put("start_time",timeFrame.startedTime);
                timeframeDocument.put("finish_time",timeFrame.finishedTime);

                BasicDBObject calibrationDocument = new BasicDBObject();    //creates a document to save the calibration
                int count = 1;
                for(String sentiment : timeFrame.sentimentCalibration){
                    calibrationDocument.put(String.valueOf(count),sentiment);   //adds the position and the sentiment
                    count++;
                }
                timeframeDocument.put("calibration",calibrationDocument);   //adds the nested Document

                BasicDBObject representativeTFDocument = new BasicDBObject();   //creates a document to save the representative words of TF
                for(Map.Entry entry : timeFrame.representativeWordsBasedOnTF.entrySet()){
                    representativeTFDocument.put((String) entry.getKey(),entry.getValue());     //the format in the mongo will be word-frequency
                }
                timeframeDocument.put("representativeTF",representativeTFDocument);

                BasicDBObject representativeTFIDFDocument = new BasicDBObject();    //creates a document to save the representative words of TFIDF
                for(Map.Entry entry : timeFrame.representativeWordsBasedOnTFIDF.entrySet()){
                    representativeTFIDFDocument.put((String) entry.getKey(),entry.getValue());     //the format in the mongo will be word-frequency
                }
                timeframeDocument.put("representativeTFIDF",representativeTFIDFDocument);



                trendDocument.put("timeframe".concat(String.valueOf(counter)),timeframeDocument);
                counter++;
            }

            BasicDBObject sentimentRepresentativesDocument = new BasicDBObject();
            for(String sentiment : sentiments){
                BasicDBObject sentimentDocument = new BasicDBObject();
                for(Map.Entry entry : trend.representativeWordsForEachSentimentOfTrend.get(sentiment).entrySet()){  //gets the <word,frequency> pairs for each sentiment
                    sentimentDocument.put((String) entry.getKey(),entry.getValue());    //saves the <word,frequency> pair
                }
                sentimentRepresentativesDocument.put(sentiment,sentimentDocument);
            }
            trendDocument.put("representatives_of_trend",sentimentRepresentativesDocument);


            jsonDocument.put("trend".concat(new Integer(trendCounter).toString()),trendDocument);
            trendCounter++;
        }
        collection.insert(jsonDocument);

    }



}
