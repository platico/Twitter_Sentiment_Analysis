import java.util.Date;

/**
 * Class representing a tweet
 */
public class Tweet {

    String text;    //the text after the preprocessing
    String username;
    Date createdAt;
    Double angerScore;
    Double disgustScore;
    Double fearScore;
    Double joyScore;
    Double sadnessScore;
    Double surpriseScore;

    public Tweet(String username,Date createdAt,String text,Double angerScore,Double disgustScore,Double fearScore,Double joyScore,Double sadnessScore,Double surpriseScore){
        this.username = username;
        this.createdAt = createdAt;
        this.text = text;
        this.angerScore = angerScore;
        this.disgustScore = disgustScore;
        this.fearScore = fearScore;
        this.joyScore = joyScore;
        this.sadnessScore = sadnessScore;
        this.surpriseScore = surpriseScore;
    }



}
