
import DataStructure.BaseLine;
import DataStructure.CompareArea;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;

public class main {

    public static void main(String arg[]){
        System.out.println("Trying to connect to the database");
        MongoClient mongoClient = new MongoClient();
        MongoDatabase Database = mongoClient.getDatabase("testDB");




        /*
        Gson g = new Gson();
        MongoCollection<Document> coll = Database.getCollection("CompareArea");
        CompareArea one1 = new CompareArea();
        one1.FromPosX = 175;
        one1.FromPosY = 200;
        one1.Height = 50;
        one1.Width = 50;
        one1.Pos = CompareArea.Position.Absolute;


        Document d = Document.parse(g.toJson(one1));
        coll.insertOne(d);
        */

        /*
        Gson g = new Gson();
        MongoCollection<Document> coll = Database.getCollection("BaseLine");
        BaseLine one1 = new BaseLine();
        one1.Branch = "stage/R9-Major";
        one1.TestName = "Club w/o offers";
        one1.Capability = "Latest Firefox desktop";
        one1.ImageNumber = 0;
        one1.StepName = "No interatcion";

        one1.TestURL = "http://www.hej.com";
        one1.ImageName = "1.png";
        ArrayList<String> array = new ArrayList<String>();
        array.add("5a368a5553b9ad2594d9b732");
        one1.CompAreaIds = array;

        Document d = Document.parse(g.toJson(one1));
        coll.insertOne(d);
        */
        /*
        Gson g = new Gson();
        MongoCollection<Document> coll = Database.getCollection("DataStructure");
        DataStructure one1 = new DataStructure();
        one1.BaslineBranch = "stage/R9-Major";
        one1.TestBranch = "fix/GOEUXD-1234";
        one1.TestName = "Club w/o offers";
        one1.Capability = "Latest Firefox desktop";
        one1.ImageNumber = 0;
        one1.StepName = "No interatcion";

        one1.TestURL = "http://www.hej.com";
        one1.RunData = "2018-01-01 12:10";
        one1.ImageName = "c:/imagesForTest/1.png";


        Document d = Document.parse(g.toJson(one1));
        coll.insertOne(d);*/

        /*Gson g = new Gson();
        MongoCollection<Document> coll = Database.getCollection("TestResults");
        DataStructure.TestResult one1 = new DataStructure.TestResult();
        one1.BaslineBranch = "stage/R9-Major";
        one1.TestBranch = "fix/GOEUXD-1234";
        one1.TestName = "Club w/o offers";
        one1.Capability = "Latest Firefox desktop";
        one1.ImageNumber = 0;
        one1.StepName = "No interatcion";

        one1.TestURL = "http://www.hej.com";
        one1.RunData = "2018-01-01 12:10";
        one1.ImageName = "c:/imagesForTest/1.png";


        Document d = Document.parse(g.toJson(one1));
        coll.insertOne(d);
        */

        ImageComparison imageComparison = new ImageComparison(Database);

        //if image comparison returns false then we have nothing to do so we sleep for 100 ms
        if(!imageComparison.DoImageComparison()){
            try{ Thread.sleep(100);}
            catch(Exception e) {System.out.println("error when trying to fetch DataStructure records from the database " + e);}
        }


    }
}
