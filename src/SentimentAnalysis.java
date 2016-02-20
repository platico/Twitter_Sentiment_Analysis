
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;


public class SentimentAnalysis {


    Map<String,ArrayList<String>> emotionMap=new HashMap<>() ;
    HashMap<String,Float> emoticonMap = new HashMap<>() ;
    HashMap<String,Float> phrasePolarityMap = new HashMap<>();
    WordNetDatabase database = WordNetDatabase.getFileInstance() ;



    public void initialize() throws FileNotFoundException, XMLStreamException {
        readSecondary();    //loads the file of secondary emotions inside emotionMap map
        findSynonyms();     //finds secondary emotions synonyms using WordNet
        readEmoticons();    //loads the file of emoticons in emoticonMap map

        readXML();  //loads the Sentic Net XML file into phrasePolarityMap
    }

    public HashMap<String,Float> calculateSentiment(String tweet){
        HashMap<String,Integer> primaryCount = new HashMap<>();     //sinaisthima => Plithos Sinaisthimatikwn lexewn gia to sinaisthima
        HashMap<String,Float> sentimentScore = new HashMap<>();
        ArrayList<String> tokens = createAllCombinationsOfConsecutiveStrings(tweet);
        int countSentimentTokens = 0;   //plithos sinaisthimatikwn lexewn tou tweet
        String primarySentiment = "";

        for (String token : tokens){

            Float polarity = new Float(0);   //initialize the polarity
            if(emoticonMap.containsKey(token)){ //if its an emoticon
                primarySentiment = containsSecondary(token);    //takes the primary Sentiment
                updatePrimaryCount(primarySentiment,primaryCount);  //updates the count for this primary sentiment
                polarity = emoticonMap.get(token);  //takes the polarity of the emoticon
                updatePolarity(primarySentiment,polarity,sentimentScore);   //updates the polarity of the primary sentiment
                countSentimentTokens++;
            }
            else if(phrasePolarityMap.containsKey(token)){
                String stemmedToken = stemWord(token);
                if((primarySentiment = containsSecondary(stemmedToken))!=null) {  //if its contained
                    polarity = phrasePolarityMap.get(token);
                    updatePolarity(primarySentiment,polarity,sentimentScore);
                    updatePrimaryCount(primarySentiment,primaryCount);  //updates the count for this primary sentiment
                    countSentimentTokens++;
                }
                else if((primarySentiment = containsSecondary(token))!=null) {
                    polarity = phrasePolarityMap.get(token);
                    updatePolarity(primarySentiment,polarity,sentimentScore);
                    updatePrimaryCount(primarySentiment,primaryCount);  //updates the count for this primary sentiment
                    countSentimentTokens++;

                }
            }
        }

        calculateSentimentScore(primaryCount,sentimentScore,countSentimentTokens);  //calculates the overall score for each primary sentiment

        return sentimentScore;

    }

    public void calculateSentimentScore(HashMap<String,Integer> primaryCount , HashMap<String,Float> sentimentScore, int countSentimentTokens){

        for(String aKey : sentimentScore.keySet()){
            sentimentScore.put(aKey, sentimentScore.get(aKey)/ new Float(primaryCount.get(aKey)) );
        }

        for(String aKey : sentimentScore.keySet()){
            sentimentScore.put(aKey, sentimentScore.get(aKey)/ new Float(countSentimentTokens) );
        }


    }

    /**
     * Method that updates the count of tokens for each primary sentiment
     * @param primary
     * @param primaryCount
     */
    public void updatePrimaryCount(String primary,HashMap<String,Integer> primaryCount){

        if(primaryCount.containsKey(primary)){  //if its already contained
            Integer oldCount = new Integer(primaryCount.get(primary));
            primaryCount.put(primary,oldCount+1);   //we just add it
        }
        else{   //if its the first time
            primaryCount.put(primary,1);    //initialize it
        }

    }

    /**
     * Method that updates the primary sentiment score
     * @param primary
     * @param polarity
     * @param sentimentScore
     */
    public void updatePolarity(String primary,Float polarity,HashMap<String,Float> sentimentScore){
        if(sentimentScore.containsKey(primary)){    //if the primary sentiment already exists
            Float oldPolarity = new Float(sentimentScore.get(primary));     //get the secondary
            sentimentScore.put(primary,  Math.abs(oldPolarity)+Math.abs(polarity) );   //add the new polarity
        }
        else {
            sentimentScore.put(primary,Math.abs(polarity));
        }
    }

    public void readSecondary() throws FileNotFoundException {
        Scanner scan = new Scanner(new File("secondary_emotions.txt"));
        scan.useDelimiter("\n");
        while (scan.hasNext()) {
            String next = scan.next();
            ArrayList<String> aList = new ArrayList<>();
            for (String emotion : next.split("\t")) {
                aList.add(emotion);
            }
            String key;
            if (emotionMap.size() == 0) {
                key = "anger";
            } else if (emotionMap.size() == 1) {
                key = "disgust";
            } else if (emotionMap.size() == 2) {
                key = "fear";
            } else if (emotionMap.size() == 3) {
                key = "joy";
            } else if (emotionMap.size() == 4) {
                key = "sadness";
            } else {
                key = "surprise";
            }
           // System.out.println("Tha valw gia key " + key + " to " + aList);
            emotionMap.put(key, aList);
        }

    }

    /**
     *
     * @param token
     * @return null if the secondary does not exist or the primary sentiment if it exists
     */
    public String containsSecondary(String token){
        for(Map.Entry entry : emotionMap.entrySet()){
            ArrayList<String> list = new ArrayList<> ((ArrayList<String>) entry.getValue());

            if (list.contains(token)){
                return (String) entry.getKey();
            }
        }
        return null;    //if its not included in any primary sentiment , return null
    }

    public Boolean containsSecondaryForSpecificSentiment(String token,String sentiment){
            ArrayList<String> list = emotionMap.get(sentiment);
            if (list.contains(token)){
                return true;
            }
            else{
                return false;
            }

    }

    public void findSynonyms(){
        System.setProperty("wordnet.database.dir", "/usr/local/WordNet-3.0/dict");

        Iterator it=emotionMap.entrySet().iterator() ;
        while(it.hasNext())
        {
           Map.Entry entry=(Map.Entry)it.next() ;
            String key=(String)entry.getKey() ;
            ArrayList<String> Synonyms=new ArrayList<>() ;
            ArrayList<String> values=emotionMap.get(key);

            Synonyms.addAll(values);

            for(String aValue:values) {
                String wordForm = aValue ;
                Synset[] synsetAdjectives = database.getSynsets(wordForm,SynsetType.ADJECTIVE);
                if (synsetAdjectives.length > 0) {
                    for (int i = 0; i < synsetAdjectives.length; i++) {
                        String[] wordForms = synsetAdjectives[i].getWordForms();
                        for (int j = 0; j < wordForms.length; j++) {
                            if(!Synonyms.contains(wordForms[j])){
                                Synonyms.add(wordForms[j]); }
                        }
                    }
                }
                Synset[] synsetNouns = database.getSynsets(wordForm,SynsetType.NOUN);
                if (synsetNouns.length > 0) {
                    for (int i = 0; i < synsetNouns.length; i++) {
                        String[] wordForms = synsetNouns[i].getWordForms();
                        for (int j = 0; j < wordForms.length; j++) {
                            if(!Synonyms.contains(wordForms[j])){
                                Synonyms.add(wordForms[j]);
                            }
                        }
                    }
                }

                Synset[] synsetVerbs = database.getSynsets(wordForm,SynsetType.VERB);
                if (synsetVerbs.length > 0) {
                    for (int i = 0; i < synsetVerbs.length; i++) {
                        String[] wordForms = synsetVerbs[i].getWordForms();
                        for (int j = 0; j < wordForms.length; j++) {
                            if(!Synonyms.contains(wordForms[j])){
                                Synonyms.add(wordForms[j]);
                            }
                        }
                    }
                }


            }

            ArrayList<String> newList = new ArrayList<>();
            for(String word : Synonyms){
                String temp = stemWord(word);
                newList.add(temp);
            }

            entry.setValue(newList) ;


        }
    }

    public String stemWord(String word){
        PorterStemmer s = new PorterStemmer();
        s.add(word.toCharArray(),word.length());
       // for(char ch : word.toCharArray()){
         //   s.add(ch);
       // }
        s.stem();
        return s.toString();
    }

    public void readEmoticons() throws FileNotFoundException {
        Scanner scanPolarity = new Scanner(new File("emoticons.txt"));
        for (int i=0;i<4;i++)
            scanPolarity.next();
        while (scanPolarity.hasNext()) {
            String primary = scanPolarity.next();
            Float posPolarity = Float.parseFloat(scanPolarity.next());  //gets the positive polarity
            Float negPolarity = Float.parseFloat(scanPolarity.next());  //gets the negative polarity
            String emoticon = scanPolarity.next();  //gets the emoticon
            emoticonMap.put(emoticon, posPolarity - negPolarity);
            ArrayList<String> values =emotionMap.get(primary) ;
            values.add(emoticon);
            emotionMap.put(primary, values);

        }
    }

    public void readXML() throws FileNotFoundException, XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new FileReader("./senticnet3.rdf.xml"));
        Float polarity;
        String phrase = "";

        while(reader.hasNext()){
            int eventtype = reader.next();
            switch (eventtype){
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName == "text"){
                        phrase = reader.getElementText();
                    }
                    else if (elementName == "polarity"){
                        polarity = Float.parseFloat( reader.getElementText() );
                        phrasePolarityMap.put(phrase,polarity);
                    }
            }
        }

    }

    /**
     * Creates all the possible combinations of consective strings
     * @param str
     * @return
     */
    public ArrayList<String> createAllCombinationsOfConsecutiveStrings(String str){

        ArrayList<String> allCombinations = new ArrayList<>();

        String[] words = str.split(" ");
        allCombinations.addAll(Arrays.asList(words));
        for (int i=0; i < words.length; i++) {
            String temp = new String(words[i]);
            for(int j=i+1;j<words.length;j++){
                temp = temp.concat(" "+words[j]);
                allCombinations.add(temp);
            }
        }

        return allCombinations;
    }

    public void testingHashMap(Map<String,ArrayList<String>> map){

        for(Map.Entry entry : map.entrySet()){
            System.out.println("==============  KEY => "+entry.getKey()+"  ==============");
            for (String str : (ArrayList<String>)entry.getValue()){
                System.out.println(str);
            }
        }

    }

    public void testingPolarityMap(){
        System.out.println(phrasePolarityMap.size());
        for(Map.Entry entry : phrasePolarityMap.entrySet()){
            String key = (String) entry.getKey();
            Float pol = (Float) entry.getValue();
            System.out.println(key+" =========> "+pol);
        }
    }


}




