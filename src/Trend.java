import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.lang.Math;


/**
 * Class representing the Trends
 */
public class Trend {

    ArrayList<String> sentiments = new ArrayList<String>() {{
        add("anger");
        add("disgust");
        add("fear");
        add("joy");
        add("sadness");
        add("surprise");

    }};
    String trend;
    ArrayList<TimeFrame> timeFrames;    //list that keeps the timeframes of the trend
    ArrayList<Tweet> tweets;   //the tweets of the Trend
    HashMap<String,ArrayList<Tweet>> userTweetMap; //map that keeps each user and it's tweets
    HashMap<String,HashMap<String,Integer>> representativeWordsForEachSentimentOfTrend;
    Date start_time;
    Date finish_time;


    public Trend(String trend,Date start_time,Date finish_time){
        timeFrames = new ArrayList<>();
        this.trend = trend;
        this.start_time = start_time;
        this.finish_time = finish_time;
        tweets = new ArrayList<>();
        representativeWordsForEachSentimentOfTrend = new HashMap<>();
        userTweetMap = new HashMap<>();
    }

    public void clearTweetsUsingLevenstein(){

        divideTweetsByUser();
        for(Map.Entry entry : userTweetMap.entrySet()){ //for every user in this trend
            ArrayList<Tweet> tweetsOfUser = (ArrayList<Tweet>) entry.getValue();
            ArrayList<Tweet> tweetsToBeRemoved = new ArrayList<>(); //keeps the  tweets to be removed
            ArrayList<Tweet> tweetsChecked = new ArrayList<>(); //keeps the tweets that have already been checked so that they wont be checked again
            for(Tweet tweetToCheck : tweetsOfUser){ //for every tweet

                tweetsChecked.add(tweetToCheck);    //add it so that it wont be checked again
                for(Tweet secondTweet : tweetsOfUser){
                    if(tweetsChecked.contains(secondTweet)==false) {
                        if (tweetToCheck != secondTweet && tweetsAreSimilar(tweetToCheck, secondTweet)) {  //if its not the same tweet and the 2 tweets are similar (similarity < 10/100)
                            tweetsToBeRemoved.add(secondTweet);
                        }
                    }

                }
            }
            tweetsOfUser.removeAll(tweetsToBeRemoved);  //removes all the
            userTweetMap.put((String) entry.getKey(),tweetsOfUser);
        }


    }

    /**
     * Method that checks if 2 tweets are similar
     *
     * Two tweets are concerned similar when their distance is less than 10%
     * @param firstTweet
     * @param secondTweet
     * @return
     */
    public Boolean tweetsAreSimilar(Tweet firstTweet,Tweet secondTweet){
        Levenshtein levenshtein = new Levenshtein();
        int distanceOfTwoTweets = levenshtein.distance(firstTweet.text,secondTweet.text);
        double percentage = (double) distanceOfTwoTweets / Math.max(firstTweet.text.length(),secondTweet.text.length()) ;

        if(percentage< 0.1 ){
            return true;
        }
        else{
            return false;
        }


    }

    public void divideTweetsByUser(){

        for (Tweet tweet : tweets){
            String user = tweet.username;
            ArrayList<Tweet> tweetsOfUser;
            if(userTweetMap.containsKey(user)){
                tweetsOfUser = userTweetMap.get(user);  //ig the user is already written just add the tweet
            }
            else {
                tweetsOfUser = new ArrayList<>();   //else initialize it
            }
            tweetsOfUser.add(tweet);
            userTweetMap.put(user,tweetsOfUser);
        }
    }

    public void calculateRepresentativeWordsForEachTimeframe(SentimentAnalysis analysis) throws FileNotFoundException, XMLStreamException {

        for(TimeFrame frame : timeFrames){
            frame.calculateRepresentativeWords(analysis);
        }
    }

    public void calculateRepresentativeWordsForEachSentiment(SentimentAnalysis analysis){

        for(String sent : sentiments){
            representativeWordsForEachSentimentOfTrend.put(sent,new HashMap<String, Integer>());  //contains for each sentiment, it's representative words and the amount of them
        }

        for(Tweet tweet : tweets){
            String text = tweet.text;
            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(text.split(" ")));
            for (String word : tokens){
                String primary;
                if ((primary=analysis.containsSecondary(word)) != null){    //if the token is a sentiment word

                    HashMap<String,Integer> mapOfWords = representativeWordsForEachSentimentOfTrend.get(primary);   //take the map of the sentiment
                    if(mapOfWords.containsKey(word)){
                        mapOfWords.put(word, mapOfWords.get(word)+1);   //if the word already exists just increase the count
                    }
                    else{
                        mapOfWords.put(word,1); //if the word does not already exists, initialize it
                    }


                }
            }


        }


    }

    /**
     * Method that implements the sentiment Calibration for each TimeFrame
     */
    public void calculateSentimentCalibrationForEachTimeframe(){

        for(TimeFrame frame : timeFrames){
            frame.calculateSentimentCalibration();
        }
    }

    /**
     * Method that divides the timeline in count TimeFrames
     * @param count
     */
    public void divideTimelineInTimeframes(int count){

        long differenceToAdd = (finish_time.getTime() - start_time.getTime()) / count;

        Date temp = start_time;
        for(int i=0;i<count;i++){   //for every division of timeline

            Date newDate = milliSecondsToDate(temp.getTime()+differenceToAdd);  //the new Date (end of frame) is the end of the previous fame + a difference
            TimeFrame frame = new TimeFrame(this,temp,newDate);
            timeFrames.add(frame);
            temp = newDate;

        }





    }

    public Date milliSecondsToDate(long milliSeconds){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        Date date = calendar.getTime();
        return date;
    }

    /**
     * Returns the tweets that belong in the specific frame
     * @param frame
     * @return
     */
    public ArrayList<Tweet> getTweetsInTimeframe(TimeFrame frame){

        ArrayList<Tweet> tweetsInFrame = new ArrayList<>();
        for(Tweet tweet : tweets){
            if(tweet.createdAt.after(frame.startedTime) && tweet.createdAt.before(frame.finishedTime)) {  //if the tweet belongs in this Time-frame
                tweetsInFrame.add(tweet);
            }
        }



        return tweetsInFrame;

    }


}
