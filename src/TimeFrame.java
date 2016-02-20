import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.*;

/**
 * Class used in order to represent each time-frame for each trend.
 */
public class TimeFrame {

    Trend trend; //the trend its coming from
    Date startedTime;   //the time it started
    Date finishedTime;  //the time it finished
    HashMap<String,Integer> representativeWordsBasedOnTF = new HashMap<>();
    HashMap<String,Double> representativeWordsBasedOnTFIDF = new HashMap<>();
    ArrayList<String> sentimentCalibration; //keeps the calibration of sentiments [0] -> most intense  [5] -> weakest
    ArrayList<Tweet> tweetsOfFrame;

    public TimeFrame(Trend trend,Date startedTime, Date finishedTime){
        this.trend = trend;
        this.startedTime = startedTime;
        this.finishedTime = finishedTime;
        sentimentCalibration = new ArrayList<>();
        tweetsOfFrame = getTweetsOfFrame(trend);
        representativeWordsBasedOnTF = new HashMap<>();
        representativeWordsBasedOnTFIDF = new HashMap<>();
    }

    /**
     * Method that finds the representative words of the most intense sentiment of this Time frame
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public void calculateRepresentativeWords(SentimentAnalysis analysis) throws FileNotFoundException, XMLStreamException {

        String firstSentiment = sentimentCalibration.get(0);    //the most intense sentiment is in index 0

        int countOfSentimentWords = 0;
        //CALCULATING FOR TF
        for (Tweet tweet : tweetsOfFrame){  //for each tweet
            String text = tweet.text;   //get it's text
            ArrayList<String> words = new ArrayList<>(Arrays.asList(text.split(" ")));   //split it to tokens
            for(String word : words){

                if (analysis.containsSecondaryForSpecificSentiment(word,firstSentiment) == true) {  //if the word is sentimental
                    countOfSentimentWords++;
                    if(representativeWordsBasedOnTF.containsKey(word)){    //if the word has been counted before
                        representativeWordsBasedOnTF.put(word, representativeWordsBasedOnTF.get(word)+1);
                    }
                    else {
                        representativeWordsBasedOnTF.put(word,1);  //else initialize it
                    }

                }

            }

        }


        for (Map.Entry entry : representativeWordsBasedOnTF.entrySet()){
            Integer temp = new Integer( (int) entry.getValue() );
            double doubleNumb = temp.doubleValue();
            Double tfidf =  doubleNumb / countOfSentimentWords ;    //calculates TFIDF
            representativeWordsBasedOnTFIDF.put((String) entry.getKey(), tfidf);     //takes every frequency of word and divides it with the count of sentimental words
        }


    }

    /**
     * Method that calculates the sentiment calibration for this TimeFrame.
     * Sentiments are sorted from the most intense to the most weak
     */
    public void calculateSentimentCalibration(){

        HashMap<String,Double> scoreCalculator = new HashMap<>();
        scoreCalculator.put("anger",new Double(0));  //HashMap initialization
        scoreCalculator.put("disgust",new Double(0));
        scoreCalculator.put("fear",new Double(0));
        scoreCalculator.put("joy",new Double(0));
        scoreCalculator.put("sadness",new Double(0));
        scoreCalculator.put("surprise",new Double(0));

        for (Tweet tweet : tweetsOfFrame){  //for every tweet of the Frame

            scoreCalculator.put("anger", scoreCalculator.get("anger") + tweet.angerScore);      //updates the values
            scoreCalculator.put("disgust", scoreCalculator.get("disgust") + tweet.disgustScore);
            scoreCalculator.put("fear", scoreCalculator.get("fear") + tweet.fearScore);
            scoreCalculator.put("joy", scoreCalculator.get("joy") + tweet.joyScore);
            scoreCalculator.put("sadness", scoreCalculator.get("sadness") + tweet.sadnessScore);
            scoreCalculator.put("surprise", scoreCalculator.get("surprise") + tweet.surpriseScore);
        }

        HashMap<String,Float> sortedMap = sortHashMapByValues(scoreCalculator);     //sorts the list by value (score)
        for(Map.Entry entry : sortedMap.entrySet()){
            sentimentCalibration.add((String) entry.getKey());    //adds the sentiment int the sentiment calibration list
        }
        Collections.reverse(sentimentCalibration);  //reverses the list in order to be in descending order

    }

    public ArrayList<Tweet> getTweetsOfFrame(Trend trend){
        return trend.getTweetsInTimeframe(this);
    }


    public LinkedHashMap sortHashMapByValues(HashMap passedMap) {
        List mapKeys = new ArrayList(passedMap.keySet());
        List mapValues = new ArrayList(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap sortedMap = new LinkedHashMap();

        Iterator valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Object val = valueIt.next();
            Iterator keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Object key = keyIt.next();
                String comp1 = passedMap.get(key).toString();
                String comp2 = val.toString();

                if (comp1.equals(comp2)){
                    passedMap.remove(key);
                    mapKeys.remove(key);
                    sortedMap.put((String)key, (Double)val);
                    break;
                }

            }

        }
        return sortedMap;
    }





}
