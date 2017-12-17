import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


import org.apache.commons.io.FileUtils;
import org.bson.Document;
import DataStructure.*;
import org.bson.types.ObjectId;
import org.im4java.core.*;

import java.io.File;
import java.io.IOException;

public class ImageComparison {
    private  MongoDatabase Database;

    public ImageComparison(MongoDatabase Database){
        this.Database = Database;
    }

    public boolean DoImageComparison()
    {
        ToDo toDo = FetchTodo();
        if(FetchTodo() == null)
            return false;

        BaseLine base = GetBaselineTestResult(toDo);

        //TODO save directly to result list
        if(base == null)
            return true;

        if(!MoveToTempLocation(toDo, base)) {
            return false;
        }
        if(!UpdateSizeData(toDo))
            return false;

        if(!ResizeImagesToBeAbleToDoCompare(toDo, base))
            return false;

        if(!AddComparingAreas(toDo, base))
            return false;

        if(CompareImages(toDo, base)){
            return false;
        }

        SaveTestResultToDB();


        return true;
    }



    private ToDo FetchTodo(){
        //getting data but if there is non the return null
        MongoCollection<Document> toDos = Database.getCollection("ToDo");
        if(toDos.count() == 0)
            return null;
        Document d = toDos.find().first();
        Gson g = new Gson();
        ToDo todo = g.fromJson(d.toJson(), ToDo.class);
        return todo;
    }


    private BaseLine GetBaselineTestResult(ToDo toDo) {
        MongoCollection<Document> baselines = Database.getCollection("BaseLine");
        if(baselines.count() == 0)
            return null;


        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("Branch", toDo.BaslineBranch);
        whereQuery.put("TestName", toDo.TestName);
        whereQuery.put("StepName", toDo.StepName);
        whereQuery.put("ImageNumber", toDo.ImageNumber);
        whereQuery.put("Capability", toDo.Capability);

        FindIterable<Document> result = baselines.find(whereQuery);

        if(result.first() == null)
            return null;

        Document d = result.first();
        Gson g = new Gson();
        BaseLine baseline = g.fromJson(d.toJson(), BaseLine.class);
        return baseline;

    }

    private boolean MoveToTempLocation(ToDo todo, BaseLine bl) {

        //if we dont have a baseline then we can just add the to-do to latest run
        if(bl == null)
            return true;


        //moving To-do image to the work in progress area
        File toDoFile = new File(Configuration.PathToToDos+"\\"+todo.ImageName);
        File wipToDoFile = new File(Configuration.PathToWorkingArea+"\\toDo.png");
        //break if file dose not exist
        if(!toDoFile.isFile()) {
            System.out.println("Could not open file with path: " + toDoFile.getAbsolutePath());
            return false;
        }
        //handel if image is already there - removing if we find one
        if(wipToDoFile.exists()) {
            try {
                FileUtils.forceDelete(wipToDoFile);
            }
            catch (IOException e){
                System.out.println("Found a file already in ToDo WIP so trying to delete but failed: " + e);
                return false;
            }
        }
        //move file
        try {
            FileUtils.copyFile(toDoFile, wipToDoFile);
        }
        catch (IOException e){
            System.out.println("something went wrong when trying to copy file to WIP folder " + e.toString());
            return false;
        }




        File baselineFile = new File(Configuration.PathToBaseLine +"\\"+ bl.ImageName);
        File wipBaselineFile = new File(Configuration.PathToWorkingArea + "\\bl.png");
        if(!baselineFile.isFile()) {
            System.out.println("Could not open file with path: " + baselineFile.getAbsolutePath());
            return false;
        }
        if(wipBaselineFile.exists()) {
            try {
                FileUtils.forceDelete(wipBaselineFile);
            }
            catch (IOException e){
                System.out.println("Found a file already in Baseline WIP so trying to delete but failed: " + e);
                return false;
            }
        }
        try {
            FileUtils.copyFile(baselineFile, wipBaselineFile);
        }
        catch (IOException e){
            System.out.println("something went wrong when trying to copy file to WIP folder " + e.toString());
            return false;
        }


        return true;
    }


    private boolean UpdateSizeData(ToDo toDo) {

        File f = new File(Configuration.PathToWorkingArea+"\\toDo.png");
        String toDoWipPath = f.getAbsolutePath();

        Info imageInfo;
        try {
            imageInfo = new Info(toDoWipPath, true);
            if(imageInfo.getImageHeight() == 0 || imageInfo.getImageWidth() == 0){
                System.out.println("The todo image has 0 height of width - failing");
                return false;
            }

            toDo.SizeX = imageInfo.getImageWidth();
            toDo.SizeY = imageInfo.getImageHeight();

        }
        catch(Exception e){
            System.out.println("Error when trying to figure out image size: " + e);
            return false;
        }

        return true;
    }
    private boolean ResizeImagesToBeAbleToDoCompare(ToDo todo, BaseLine bl) {
        //if x size differs
        if(todo.SizeX != bl.SizeX){
            if(todo.SizeX < bl.SizeX)
                if(!ResizeImage(new File(Configuration.PathToWorkingArea+"//toDo.png"), bl.SizeX, todo.SizeY))
                    return false;
            else
                if(!ResizeImage(new File(Configuration.PathToWorkingArea+"//bl.png"), todo.SizeX, bl.SizeY))
                    return false;
        }
        //if y size differ
        if(todo.SizeY != bl.SizeY){
            if(todo.SizeY < bl.SizeY){
                if(!ResizeImage(new File(Configuration.PathToWorkingArea+"//toDo.png"), todo.SizeX, bl.SizeY))
                    return false;
            }
            else
                if(!ResizeImage(new File(Configuration.PathToWorkingArea+"//bl.png"), bl.SizeX, todo.SizeY))
                    return false;
        }

        return true;
    }

    private boolean ResizeImage(File f, int x, int y){
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.addImage(f.getAbsolutePath());
        op.extent(x,y);
        op.addImage(f.getAbsolutePath());
        try {
            cmd.run(op);
        }
        catch(Exception e){
            System.out.println("failed when tying to resize image: " + f.getAbsolutePath() + " " + e);
            return false;
        }
        return true;
    }

    private boolean AddComparingAreas(ToDo toDo, BaseLine base) {
        MongoCollection<Document> compAreas = Database.getCollection("CompareArea");
        if(compAreas.count() == 0)
            return true;
        for(String id : base.CompAreaIds){
            BasicDBObject whereQuery = new BasicDBObject();
            whereQuery.put("_id", new ObjectId(id));
            Document result = compAreas.find(whereQuery).first();
            if(result ==  null|| result.size() == 0){
                System.out.println("Found a comp area id that did not have a comp area row in the db: " + id);
                return false;
            }
            Gson g = new Gson();
            CompareArea ca =  g.fromJson(result.toJson(), CompareArea.class);

            File blFile = new File(Configuration.PathToWorkingArea+"//bl.png");
            File toDoFile = new File(Configuration.PathToWorkingArea+"//toDo.png");

            if(ca.Pos == CompareArea.Position.Absolute){
                if(!DrawRectangle(blFile, ca.FromPosX, ca.FromPosY, ca.Width, ca.Height)
                || !DrawRectangle(toDoFile, ca.FromPosX, ca.FromPosY, ca.Width, ca.Height))
                    return false;
            }
        }
        return true;

    }

    private boolean DrawRectangle(File f, int x, int y, int w, int h) {
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.addImage(f.getAbsolutePath());
        op.fill("Cyan");
        String text = "rectangle "+ x+","+y+" "+(x+w)+","+(y+h);
        op.draw(text);
        op.addImage(f.getAbsolutePath());
        try{
            cmd.run(op);
        }
        catch (Exception e){
            System.out.println("Failed to draw area on image + " + e);
            return false;
        }
        return true;
    }

    private boolean CompareImages(ToDo todo, BaseLine bl) {
        File todoFile = new File(Configuration.PathToWorkingArea+"//toDo.png");
        File blFile = new File(Configuration.PathToWorkingArea+"//bl.png");
        File resultFile = new File(Configuration.PathToWorkingArea+"//diff.png");

        CompareCmd cmd = new CompareCmd();
        IMOperation op = new IMOperation();
        op.highlightColor("Magenta");
        op.lowlightColor("transparent");
        op.addImage(todoFile.getAbsolutePath());
        op.addImage(blFile.getAbsolutePath());
        op.compose("src");
        op.colorspace("sRGB");
        op.addImage(resultFile.getAbsolutePath());

        try {
            cmd.run(op);
        }
        catch (CommandException e){
            //0 = diff
            //-1 = no diff
            if(e.getReturnCode() == 0){
                todo.HadDiff = true;
            }
            else if(e.getReturnCode() == 1){
                todo.HadDiff = false;
            }
            else{
                System.out.println("The comparison ended with response code (Fail) "+e);
                return false;
            }

        }
        catch (InterruptedException e){
            return false;
        }
        catch (IM4JavaException e){
            return false;
        }
        catch (IOException e){
            return false;
        }
        return true;
    }

    private void SaveTestResultToDB() {
        //fetch testresult from before
        //update
        //if not found the remove
    }


}
