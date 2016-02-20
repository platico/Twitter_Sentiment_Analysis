import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class implementing the preprocessing of the tweets
 */
public class Preprocessing {

    Database database;
    ArrayList<String> stopWordsList;
    public static String[] stopwords = {"a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves", "zero"};

    public Preprocessing(Database database){
        stopWordsList = new ArrayList<>(Arrays.asList(stopwords));
        this.database = database;
    }


    /**
     * Implements the preprocessing . For each tweets, tokenizes , checks if it is legit , and finally adds it back in the DB
     */
    public void run(){
        FindIterable topic = database.db.getCollection("tweets").find();

        topic.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {

                String stringJSON = document.get("json").toString();    //takes the json
                try {
                    JSONObject jsonObject = new JSONObject(stringJSON); //converts to json object
                    String tweet = jsonObject.getString("text");
                    String[] tokens = tweet.split(" ");        //tokenizes the string
                    ArrayList<String> tokensListTemp = new ArrayList(Arrays.asList(tokens));
                    ArrayList<String> tokensList = removeSpaceFromList(tokensListTemp);     //removes blank strings
                    String tweetAfterPreprocessing = " ";
                    for(String token : tokensList){
                        if (isWhitelisted(token)){      //if the tweets is legit
                            token = token.toLowerCase();
                            token = replaceChars(token);
                            tweetAfterPreprocessing = tweetAfterPreprocessing.concat(token+" ");    //it is inserted in the "after preprocessing" string
                        }
                    }

                    database.db.getCollection("tweets").updateOne(new Document("json", document.get("json")),      //inserted back in DB
                            new Document("$set", new Document("tweet_after_preprocessing", tweetAfterPreprocessing )));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }



        });

    }

    public String replaceChars(String str){
        StringBuilder builder = new StringBuilder(str);
        int index = builder.indexOf(",");
        if (index!=-1)
            builder.deleteCharAt(index);

        index = builder.indexOf("!");
        if (index!=-1)
            builder.deleteCharAt(index);

        index = builder.indexOf(".");
        if (index!=-1)
            builder.deleteCharAt(index);

        index = builder.indexOf("-");
        if (index!=-1)
            builder.deleteCharAt(index);

        index = builder.indexOf("/");
        if (index!=-1)
            builder.deleteCharAt(index);

        index = builder.indexOf(":");
        if (index!=-1)
            builder.deleteCharAt(index);

        return builder.toString();
    }

    /**
     * Method checking if a string is whitelisted
     *
     * A string is called whitelisted if it's not a stopword or a mentrion or a number or a url
     * @param str
     * @return
     */
    public boolean isWhitelisted(String str){
        if (isMention(str) == false && isStopWord(str) == false && isNumeric(str) == false && isURL(str) == false){
            return true;
        }
        else{
            return false;
        }

    }


    /**
     * Method removing blank words from list
     * @param list
     * @return
     */
    public ArrayList<String> removeSpaceFromList(ArrayList<String> list){
        ArrayList<String> newList = new ArrayList<>();

        for(String token : list){
            if (token.matches(".*\\w.*")){
                newList.add(token);
            }

        }
        return newList;
    }

    public Boolean isMention(String str){
        if(str.charAt(0) == '@' )
            return true;
        else
            return false;
    }

    public boolean isStopWord(String str){
        if(stopWordsList.contains(str))
            return true;
        else
            return false;
    }

    public boolean isNumeric(String str){
        if (str.matches("[-+]?\\d*\\.?\\d+"))
            return true;
        else
            return false;
    }

    public boolean isURL(String str){
        try{
            URL url = new URL(str);
            return true;
        }
        catch (MalformedURLException e){
            return false;
        }
    }




}
