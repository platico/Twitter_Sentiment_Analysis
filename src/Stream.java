import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;


public class Stream {


    TwitterStream twitterStream;

    StatusListener listener;
    Database db = null;

    public Stream(Database database,Configuration conf){
            db = database;
            twitterStream  = new TwitterStreamFactory(conf).getInstance();
            twitterStream.addListener( new StatusListener() {

                    @Override
                    public void onStatus(Status status) {
                        String json = TwitterObjectFactory.getRawJSON(status);  //gets the json for the specific status
                      //  System.out.println(status.getText());
                        db.insertJSONInDatabase(json);
                    }

                    @Override
                    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                        System.out.println("onDeletionNotice");
                    }

                    @Override
                    public void onTrackLimitationNotice(int i) {
                        System.out.println("onTrackLimitationNotice");

                    }

                    @Override
                    public void onScrubGeo(long l, long l1) {
                        System.out.println("onScrubGeo");

                    }

                    @Override
                    public void onStallWarning(StallWarning stallWarning) {
                        System.out.println("onStallWarning");

                    }

                    @Override
                    public void onException(Exception e) {
                        System.out.println("onException");

                    }

                });

    }

    /**
     * Method that updates the query with the keywords to be filtered
     * @param trends
     */
    public void updateQuery(ArrayList<Document> trends){
        FilterQuery query = new FilterQuery();
        String trendsInList[] = new String[trends.size()];
        int i=0;
        for (Document doc : trends){    //inserts keywords in Array of String
            trendsInList[i] = doc.get("trend_title").toString();
            i++;
        }
        query.language(new String[]{"en"});
        query.track(trendsInList);  //tracks the array

        twitterStream.filter(query);    //starts the filtering
    }

    public void shutDownStream(){
        twitterStream.cleanUp();
        twitterStream.shutdown();
        System.out.println("Stream shutdown...");
    }

}
