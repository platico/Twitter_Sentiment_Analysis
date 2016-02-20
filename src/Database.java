import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import twitter4j.Trend;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by platico on 6/1/2016.
 */
public class Database {

    MongoClient mongoClient = null;
    MongoDatabase db = null;
    ArrayList<Document> activeTrends = null;


    public Database(){
        mongoClient = new MongoClient();    //create a MongoDB instance
        db = mongoClient.getDatabase("TwitterProject"); //creates the db
        activeTrends = new ArrayList<Document>();
    }

    /**
     * Method that calculates sentiment for every Tweet of the MongoDB
     */
    public void calculateSentimentForTweets() throws FileNotFoundException, XMLStreamException {
        final SentimentAnalysis analysis = new SentimentAnalysis();
        analysis.initialize();


        FindIterable topic = db.getCollection("trends").find();
        topic.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                if(document.containsValue(null)){
                    db.getCollection("trends").updateOne(new Document("trend_title", document.get("trend_title")),      //it's finish time is changed to current time
                            new Document("$set", new Document("finish_time", new Date() )));
                }

            }


          });





        FindIterable tweets = db.getCollection("tweets").find();

        tweets.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {



                String tweetAfterPreprocessing =  document.get("tweet_after_preprocessing").toString();
                HashMap<String,Float> scoreMap = analysis.calculateSentiment(tweetAfterPreprocessing);

                String json = document.get("json").toString();

                Float angerScore;
                if(scoreMap.containsKey("anger")){
                    angerScore=scoreMap.get("anger");
                }
                else{
                    angerScore = new Float(0);
                }

                Float disgustScore;
                if(scoreMap.containsKey("disgust")){
                    disgustScore=scoreMap.get("disgust");
                }
                else{
                    disgustScore = new Float(0);
                }

                Float fearScore;
                if(scoreMap.containsKey("fear")){
                    fearScore=scoreMap.get("fear");
                }
                else{
                    fearScore = new Float(0);
                }

                Float joyScore;
                if(scoreMap.containsKey("joy")){
                    joyScore=scoreMap.get("joy");
                }
                else{
                    joyScore = new Float(0);
                }

                Float sadnessScore;
                if(scoreMap.containsKey("sadness")){
                    sadnessScore=scoreMap.get("sadness");
                }
                else{
                    sadnessScore = new Float(0);
                }

                Float surpriseScore;
                if(scoreMap.containsKey("surprise")){
                    surpriseScore=scoreMap.get("surprise");
                }
                else{
                    surpriseScore = new Float(0);
                }

               // if (angerScore!=0.0 || disgustScore!=0.0 || fearScore!=0.0 || sadnessScore!=0.0 || joyScore!=0.0 || surpriseScore!=0.0){
                //    System.out.println(tweetAfterPreprocessing+"    ==========>     "+angerScore+" - "+disgustScore+" - "+fearScore+" - "+joyScore+" - "+sadnessScore+" - "+surpriseScore);
               // }


                db.getCollection("tweets").replaceOne(new Document("json",json),
                        new Document()
                                .append("json",json)
                                .append("tweet_after_preprocessing",tweetAfterPreprocessing)
                                .append("anger",angerScore)
                                .append("disgust",disgustScore)
                                .append("fear",fearScore)
                                .append("joy",joyScore)
                                .append("sadness",sadnessScore)
                                .append("surprise",surpriseScore)
                );


            }
        });



    }

    /**
     * Removes all documents of a collection
     * @param collectionTitle
     */
    public void truncate(Database database,String collectionTitle){
        database.db.getCollection(collectionTitle).deleteMany(new Document());
    }

    /**
     *
     * @param lastlyAddedTrends contains the trends been added in the last parsing
     */
    public void updateTrendInfos(final ArrayList<String> lastlyAddedTrends){

        FindIterable topic = db.getCollection("trends").find();
        topic.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                if(lastlyAddedTrends.contains(document.get("trend_title"))==false && document.containsValue(null)){     //if the trend is not contained in the last appended list

                    db.getCollection("trends").updateOne(new Document("trend_title", document.get("trend_title")),      //it's finish time is changed to current time
                            new Document("$set", new Document("finish_time", new Date() )));
                }
            }
        });

    }

    public void updateActiveTrends(){

        activeTrends = new ArrayList<>();

        FindIterable topic = db.getCollection("trends").find();

        topic.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                try {
                    if (document.containsValue(null)  //if the topic has not ended
                            || date1IsBeforeDate2(addTwoHoursInDate((Date) document.get("finish_time")), new Date()) == true    // or if the finished time + 2 hours is earlier than the current time
                            && activeTrends.contains(document) == false) {    //AND the trend is not contained in the activeTrends list

                        activeTrends.add(document);     //then add it in the activeTrends list
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        });

    }

    public void insertTrendInDb(Trend trend){

        if (trendIsAlreadyInserted(trend.getName()) == false) {  //if the trend hasnt yet been inserted

            db.getCollection("trends").insertOne(    //inserts the trend (normally should check if it has finished and check if it exists)
                    new Document()
                            .append("trend_title", trend.getName()) //inserts title
                            .append("start_time", new Date() )  //inserts start time
                            .append("finish_time", null)    //inserts finish_time
            );

        }


    }

    /**
     * Checks if a trend is already inserted in the DB
     * @param trendTitle
     * @return
     */
    public boolean trendIsAlreadyInserted(String trendTitle){
        FindIterable result = db.getCollection("trends").find(new Document("trend_title",trendTitle));  //takes the results for the specific trend
        if (result.first()==null){  //if it got an empty list
            return false;   //the trend hasnt been inserted
        }
        else{   //else true
            return true;
        }
    }



    public void insertJSONInDatabase(String json){

        db.getCollection("tweets").insertOne(
                new Document()
                        .append("json",json)
                        .append("tweet_after_preprocessing",null)
                        .append("anger",0)
                        .append("disgust",0)
                        .append("fear",0)
                        .append("joy",0)
                        .append("sadness",0)
                        .append("surprise",0)
        );


        /*
        db.getCollection("tweets").insertOne(
                .append("json", json)
                .append("tweet_after_preprocessing", null),
                new Document("sentiments",
                        new Document()
                                .append("anger", 0)
                                .append("disgust", 0)
                                .append("fear", 0)
                                .append("joy", 0)
                                .append("sadness",0)
                                .append("surprise",0));

        */




    }

    /**
     * Function that checks if first date is before second date
     * @param date1
     * @param date2
     * @return
     * @throws ParseException
     */

    public Boolean date1IsBeforeDate2(Date date1, Date date2) throws ParseException {


        if(date1.before(date2)){
            return true;
        }
        else {
            return false;
        }
    }

    public Date addTwoHoursInDate(Date dateToAdd) throws ParseException {

        Calendar cal = Calendar.getInstance(); // creates calendar
        cal.setTime(dateToAdd); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, 2); // adds one hour
        return cal.getTime(); // returns new date object, one hour in the future
    }




}
