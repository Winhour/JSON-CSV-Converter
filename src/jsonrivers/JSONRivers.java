/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jsonrivers;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import org.json.*;

/**
 *
 * @author Marcin
 */
public class JSONRivers {


    public static void main(String[] args) throws IOException, JSAPException, FileNotFoundException, CsvException {
        
        JSAP jsap = new JSAP();
       
        jsap = initializeJSAP(jsap);
        
        JSAPResult config = jsap.parse(args);
        
        if(!config.success()){
            System.out.println("\nThere was an error found within command line arguments, try again\n");
            helpInfo();
        } else {
        
            if(config.getBoolean("help")){
                helpInfo();
            }
            else {        
                if(!config.getString("JSON").equals("none") && config.getString("CSV").equals("none")){
                    modifyFromJSON(config);
                }
                else if(config.getString("JSON").equals("none") && !config.getString("CSV").equals("none")){
                    parseCSV(config);
                } 
                else if(!config.getString("JSON").equals("none") && !config.getString("CSV").equals("none")){
                    System.out.println("\nPlease specify only one input file");
                    helpInfo();
                } 
                else {
                    System.out.println("\nPlease select an input file, either JSON (-j + filepath) or CSV (-c + filepath)");
                    helpInfo();
                }
            }
        }
    }
    
    private static JSAP initializeJSAP(JSAP jsap) throws JSAPException{
     
        FlaggedOption opt1 = new FlaggedOption("STEP")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setDefault("10") 
                                .setRequired(true) 
                                .setShortFlag('s') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
        
        jsap.registerParameter(opt1);
        
        FlaggedOption opt2 = new FlaggedOption("JSON")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault("none") 
                                .setRequired(true) 
                                .setShortFlag('j') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
        
        jsap.registerParameter(opt2);
        
        FlaggedOption opt3 = new FlaggedOption("LABEL")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault("none") 
                                .setRequired(true) 
                                .setShortFlag('l') 
                                .setLongFlag(JSAP.NO_LONGFLAG);     
        
        jsap.registerParameter(opt3);
        
        FlaggedOption opt4 = new FlaggedOption("OWNER")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault("RipPipPip") 
                                .setRequired(true) 
                                .setShortFlag('w') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
        
        jsap.registerParameter(opt4);
        
        FlaggedOption opt5 = new FlaggedOption("ALT")
                                .setStringParser(JSAP.DOUBLE_PARSER)
                                .setDefault("500.00") 
                                .setRequired(true) 
                                .setShortFlag('a') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
        
        jsap.registerParameter(opt5);
        
        FlaggedOption opt6 = new FlaggedOption("OUTPUT")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault("output") 
                                .setRequired(true) 
                                .setShortFlag('o') 
                                .setLongFlag("out");
        
        jsap.registerParameter(opt6);
        
        FlaggedOption opt7 = new FlaggedOption("CSV")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault("none") 
                                .setRequired(true) 
                                .setShortFlag('c') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
        
        jsap.registerParameter(opt7);
        
        Switch sw1 = new Switch("help")
                        .setShortFlag('h')
                        .setLongFlag("help");

        jsap.registerParameter(sw1);
        
        Switch sw2 = new Switch("remove_empty")
                        .setShortFlag(JSAP.NO_SHORTFLAG)
                        .setLongFlag("re");

        jsap.registerParameter(sw2);
        
        Switch sw3 = new Switch("remove_nonlatin")
                        .setShortFlag(JSAP.NO_SHORTFLAG)
                        .setLongFlag("rn");

        jsap.registerParameter(sw3);
        
        return jsap;
        
    }
    
    
    private static void modifyFromJSON(JSAPResult config) throws IOException{
        
        List <ElementData> listofElements = new ArrayList<ElementData>();
        List <ModifiedData> finallist = new ArrayList<ModifiedData>();
        //int nodeinterval = 30; 
        //String json_name = "rzeki.json";
        
        //int nodeinterval = parseInt(args[0]);
        int nodeinterval = config.getInt("STEP");
        
        //String json_name = args[1];
        String json_name = config.getString("JSON");
        
        
        String filepath = "./" + json_name;
        
        //System.out.println(filepath);
        
        String jsonstring = readFile(filepath, StandardCharsets.UTF_8);
        
        //System.out.print(jsonstring); /* test jsona */
        
        JSONObject obj = new JSONObject(jsonstring);
        JSONArray arr = obj.getJSONArray("elements");
        
        String outputfile;
        
        if (config.getString("OUTPUT").equals("output")){
            String jsn = config.getString("JSON").substring(0,config.getString("JSON").indexOf(".")+".".length());
            jsn = jsn.substring(0, jsn.length() - 1);
            outputfile = jsn + ".txt";
        }
        else {outputfile = config.getString("OUTPUT") + ".txt";}
        
        System.out.println("\nPOIOSM2FS JSON / CSV Converter\n");
                    System.out.println("Chosen parameters:");
                    System.out.println("FILE: " + filepath);
                    System.out.println("STEP: " + nodeinterval);
                    System.out.println("LABEL: " + config.getString("LABEL"));
                    System.out.println("OWNER: " + config.getString("OWNER"));
                    System.out.println("ALT: " + config.getDouble("ALT"));
                    System.out.println("OUTPUT FILE: " + outputfile);
                    System.out.println("");
        
        
        System.out.println("");
        
        for (int i=0;i<arr.length();i++){
            
            if (!arr.getJSONObject(i).getString("type").equals("way")) break;
            
            String name, nameen, type;
            long midnode;
            
            //String test1 = arr.getJSONObject(i).getString("type");
            //System.out.println(test1);
            //int test2 = arr.getJSONObject(i).getInt("id");
            //System.out.println(test2);

            JSONArray arrnodes = arr.getJSONObject(i).getJSONArray("nodes");
            //int[] numbers = new int[arrnodes.length()];
            
            //List <Integer> numbersl = new ArrayList<Integer>();
            
            midnode = arrnodes.optLong(arrnodes.length()/2);
            //System.out.println(midnode);
            

            JSONObject tags = arr.getJSONObject(i).getJSONObject("tags");
            
            if(tags.has("name")){
                name = tags.getString("name");
            //System.out.println(test4);
            }
            else {
                name = null;
            }

            //String test5 = tags.getString("waterway");
            //System.out.println(test5);
            if(tags.has("waterway")){
                type = tags.getString("waterway");
            //System.out.println(test6);
            }
            else{
                type = null;    
            }

            if(tags.has("name:en")){
                nameen = tags.getString("name:en");
            //System.out.println(test6);
            }
            else{
                nameen = null;    
            }
            
            ElementData tempElement = new ElementData(name, nameen, type);               
            
            if (arrnodes.length() > nodeinterval+1){
                               
                for (int j = 0; j < arrnodes.length(); j = j+nodeinterval) {
                    //numbers[i] = arrnodes.optInt(i);
                    tempElement.addToNodesList(arrnodes.optLong(j));
                    //System.out.println(arrnodes.optLong(j));
                    if (j+nodeinterval > arrnodes.length()) break;
                }
            } else {
                tempElement.setMiddle(midnode);
            }
            
            listofElements.add(tempElement);
        
        }
        
        
        for (ElementData x: listofElements){
            //System.out.println(x.getEnName());                    
            
            if (x.getMiddleNode() != 0){
                ModifiedData tempmod;
                tempmod = modifyElements(x, arr, x.getMiddleNode());
                finallist.add(tempmod);
            }
            else {
                List<Long> listl = x.getNodeList();
                for (int i=0;i<listl.size();i++){
                    ModifiedData tempmod;                
                    tempmod = modifyElements(x, arr, listl.get(i));
                    finallist.add(tempmod);
                }
            }
                
        }
        

        /*for (ModifiedData y: finallist){
            System.out.println(y.getType() + " " + y.getName() + " " + y.getEnName() + " lat:" + y.getLat() + " lon:" + y.getLon());
        }*/
        
        
        try {
            File myObj = new File(outputfile);
            if (myObj.createNewFile()) {
              System.out.println("File created: " + myObj.getName());
         } else {
                System.out.println("File already exists.");
         }
         } catch (IOException e) {
             System.out.println("An error occurred.");
                e.printStackTrace();
         }
        
        int linecount = 0;
        
            try {
                FileWriter myWriter = new FileWriter(outputfile);
                
                for (ModifiedData y: finallist){
                    UUID uuid = UUID.randomUUID();
                    String fname;
                    if (y.getEnName() != null){
                    fname = y.getEnName();
                    } else {fname = y.getName();}
                    
                    if (fname == null) fname = "(empty)";
                    //myWriter.write(y.getType() + "| " + y.getName() + "| " + y.getEnName() + "| lat:" + y.getLat() + " | lon:" + y.getLon() + "\n");
                    
                    if(!config.getString("LABEL").equals("none")){
                        myWriter.write("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" + config.getString("LABEL") + ": "
                                + fname + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + y.getLat() + "\" lon=\"" + y.getLon() + "\" alt=\"" 
                                + config.getDouble("ALT") +"\"/> \n");
                        linecount++;
                    } else {
                        myWriter.write("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" 
                                + fname + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + y.getLat() + "\" lon=\"" + y.getLon() + "\" alt=\"" 
                                + config.getDouble("ALT") +"\"/> \n");
                        linecount++;
                    }   
                }
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            
            System.out.println("Number of lines: " + linecount);
        
    }
    
    
    private static void parseCSV(JSAPResult config) throws FileNotFoundException, IOException, CsvException{
        
        String csv_name = config.getString("CSV");
        
        
        String filepath = "./" + csv_name;  
        
        String csvstring = readFile(filepath, StandardCharsets.UTF_8);
        
        //System.out.println(filepath);
        
        //File file = new File(filepath);
        
        String outputfile;
        
        if (config.getString("OUTPUT").equals("output")){
            String cs = config.getString("CSV").substring(0,config.getString("CSV").indexOf(".")+".".length());
            cs = cs.substring(0, cs.length() - 1);
            outputfile = cs + ".txt";
        }
        else outputfile = config.getString("OUTPUT") + ".txt";
        
        System.out.println("\nPOIOSM2FS JSON / CSV Converter\n");
                    System.out.println("Chosen parameters:");
                    System.out.println("FILE: " + filepath);
                    System.out.println("LABEL: " + config.getString("LABEL"));
                    System.out.println("OWNER: " + config.getString("OWNER"));
                    System.out.println("ALT: " + config.getDouble("ALT"));
                    System.out.println("OUTPUT FILE: " + outputfile);
                    System.out.println("REMOVE_EMPTY: " + config.getBoolean("remove_empty"));
                    System.out.println("REMOVE_NONLATIN: " + config.getBoolean("remove_nonlatin"));
                    System.out.println("");
        
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        int lines = 0;
        while (reader.readLine() != null) lines++;
        reader.close();
        
        
        String FINALSTRING = "";
        
        String lat, lon, name, nameEn, eleValue;
        
        int linecount = 0;
        double currentline_in = 0;
        double progressPercentage = 0;
        
        System.out.println("");
        
        String eleFlag;
        
        try (Scanner sc = new Scanner(csvstring).useDelimiter("\\s*\\|\\s*"))
        {
            sc.next();
            sc.next();
            sc.next();
            sc.next();
            sc.next();
            eleFlag = sc.next();
            //System.out.println(eleFlag);
            
            sc.close();
        }
        
        if (!eleFlag.equals("ele")){
        
            try (Scanner sc = new Scanner(csvstring).useDelimiter("\\s*\\|\\s*"))
            {
                sc.nextLine();
                while (sc.hasNext()) {
                    //System.out.println(sc.next());
                    sc.next();
                    lat = sc.next();
                    lon = sc.next();
                    name = sc.next();
                    nameEn = sc.next();
                    sc.next();


                    UUID uuid = UUID.randomUUID();

                    String fname;
                        if (!nameEn.replaceAll("\\s","").equals("")){
                        fname = nameEn;
                        } else {fname = name;}

                        if (fname.replaceAll("\\s","").equals("")) fname = "(empty)";

                    String clean = Normalizer.normalize(fname, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                    boolean valid = (clean.substring(0,1).matches("\\w+") || clean.substring(0,1).matches("[0-9]") || clean.substring(0,1).matches("\"")
                            || clean.substring(0,1).matches("\\(") || clean.substring(0,1).matches("\\["));
                    
                    

                    //System.out.println(fname + " " + clean.substring(0,1) + " " + valid);

                    //System.out.println(config.getBoolean("remove_empty") + " " + fname.equals("(empty)") + " " + config.getBoolean("remove_nonlatin") + " " + valid);


                    if(!(config.getBoolean("remove_empty") && fname.equals("(empty)"))){  
                        if(!(config.getBoolean("remove_nonlatin") && !valid)){
                            if (!config.getString("LABEL").equals("none")) {   
                                FINALSTRING += ("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" + config.getString("LABEL") + ": "
                                                + fname + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + lat + "\" lon=\"" + lon + "\" alt=\"" 
                                                + config.getDouble("ALT") +"\"/> \n");
                                linecount++;
                            }
                            else{
                                FINALSTRING += ("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" 
                                                + fname + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + lat + "\" lon=\"" + lon + "\" alt=\"" 
                                                + config.getDouble("ALT") +"\"/> \n");
                                linecount++;
                            }
                        }
                    }

                    currentline_in++;

                    progressPercentage = currentline_in/lines;
                    //System.out.println(progressPercentage);

                    updateProgress(progressPercentage);

                    if(sc.hasNextLine())
                        sc.nextLine();

                }
                sc.close();
                System.out.print("\r");
                System.out.print("[..................................................] 100%");
                System.out.println("");
                System.out.println("");
            }
            
        } else {
            
            try (Scanner sc = new Scanner(csvstring).useDelimiter("\\s*\\|\\s*"))
            {
                sc.nextLine();
                while (sc.hasNext()) {
                    //System.out.println(sc.next());
                    sc.next();
                    lat = sc.next();
                    lon = sc.next();
                    name = sc.next();
                    nameEn = sc.next();
                    eleValue = sc.next();
                    sc.next();


                    UUID uuid = UUID.randomUUID();

                    String fname;
                        if (!nameEn.replaceAll("\\s","").equals("")){
                        fname = nameEn;
                        } else {fname = name;}

                        if (fname.replaceAll("\\s","").equals("")) fname = "(empty)";

                    String clean = Normalizer.normalize(fname, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                    boolean valid = (clean.substring(0,1).matches("\\w+") || clean.substring(0,1).matches("[0-9]") || clean.substring(0,1).matches("\"")
                            || clean.substring(0,1).matches("\\(") || clean.substring(0,1).matches("\\["));

                    //System.out.println(fname + " " + clean.substring(0,1) + " " + valid);

                    //System.out.println(config.getBoolean("remove_empty") + " " + fname.equals("(empty)") + " " + config.getBoolean("remove_nonlatin") + " " + valid);

                    if (!eleValue.replaceAll("\\s","").equals("")){
                            

                        if(!(config.getBoolean("remove_empty") && fname.equals("(empty)"))){  
                            if(!(config.getBoolean("remove_nonlatin") && !valid)){
                                if (!config.getString("LABEL").equals("none")) {   
                                    FINALSTRING += ("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" + config.getString("LABEL") + ": "
                                                    + fname + " " + eleValue + "m"
                                            + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + lat + "\" lon=\"" + lon + "\" alt=\"" 
                                                    + config.getDouble("ALT") +"\"/> \n");
                                    linecount++;
                                }
                                else{
                                    FINALSTRING += ("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" 
                                                    + fname + " " + eleValue + "m"
                                            + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + lat + "\" lon=\"" + lon + "\" alt=\"" 
                                                    + config.getDouble("ALT") +"\"/> \n");
                                    linecount++;
                                }
                            }
                        }
                    } else {
                        if(!(config.getBoolean("remove_empty") && fname.equals("(empty)"))){  
                            if(!(config.getBoolean("remove_nonlatin") && !valid)){
                                if (!config.getString("LABEL").equals("none")) {   
                                    FINALSTRING += ("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" + config.getString("LABEL") + ": "
                                                    + fname + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + lat + "\" lon=\"" + lon + "\" alt=\"" 
                                                    + config.getDouble("ALT") +"\"/> \n");
                                    linecount++;
                                }
                                else{
                                    FINALSTRING += ("<LandmarkLocation instanceId=\"{" + uuid + "}\" type=\"POI\" name=\"" 
                                                    + fname + "\" owner=\""+ config.getString("OWNER") + "\" lat=\"" + lat + "\" lon=\"" + lon + "\" alt=\"" 
                                                    + config.getDouble("ALT") +"\"/> \n");
                                    linecount++;
                                }
                            }
                        }
                    }

                    currentline_in++;

                    progressPercentage = currentline_in/lines;
                    //System.out.println(progressPercentage);

                    updateProgress(progressPercentage);

                    if(sc.hasNextLine())
                        sc.nextLine();

                }
                sc.close();
                System.out.print("\r");
                System.out.print("[..................................................] 100%");
                System.out.println("");
                System.out.println("");
            
            }
            
        }
        
        
        //System.out.println(FINALSTRING);
             
        try {
            File myObj = new File(outputfile);
            if (myObj.createNewFile()) {
              System.out.println("File created: " + myObj.getName());
         } else {
                System.out.println("File already exists.");
         }
         } catch (IOException e) {
             System.out.println("An error occurred.");
                e.printStackTrace();
         }
        
            try {
                FileWriter myWriter = new FileWriter(outputfile);
                
                myWriter.write(FINALSTRING);
                
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            
            System.out.println("Number of lines: " + linecount + "\n");
        
        
    }
    
    
    static String readFile(String path, Charset encoding)
    throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
    
    private static ModifiedData modifyElements(ElementData ed, JSONArray arr, long id){
        
        double lat = 0, lon = 0;
        
        for (int i=0;i<arr.length();i++){
            
            if (arr.getJSONObject(i).getString("type").equals("node")){
                if(arr.getJSONObject(i).getLong("id") == id){
                    lat = arr.getJSONObject(i).getDouble("lat");
                    lon = arr.getJSONObject(i).getDouble("lon");
                }
            }
            
        }
        
        
        ModifiedData modifiedElement = new ModifiedData(ed.getName(), ed.getEnName(), ed.getType(), lat, lon);
        
        //System.out.println(modifiedElement.getType() + " " + modifiedElement.getName() + " " + modifiedElement.getEnName() + " lat:" + modifiedElement.getLat() + " lon:" + modifiedElement.getLon());
        
        return modifiedElement;
    }
    
    
    static void updateProgress(double progressPercentage) {
        final int width = 50; // progress bar width in chars

        System.out.print("\r[");
        int i = 0;
        for (; i <= (int)(progressPercentage*width); i++) {
          System.out.print(".");
        }
        for (; i < width; i++) {
          System.out.print(" ");
        }
        System.out.print("] " + (int)(progressPercentage*102) + "%");
  }
    
    
    
    
    private static void helpInfo(){
        System.out.println("\nHow to use:\n");
        System.out.println("-j (json_file_path) selects a JSON file to use");
        System.out.println("-c (csv_file_path) selects a CSV file to use");
        System.out.println("-s (Integer) selects the interval between chosen nodes");
        System.out.println("-l (String) alows to add a label in front of element's name");
        System.out.println("-w (String) specifies the owner");
        System.out.println("-a (Double) specifies the altitude");
        System.out.println("-o (filename) allows the user to choose the output file");
        System.out.println("--rn removes lines with names made up of nonlatin characters");
        System.out.println("--re removes lines with empty names");
        System.out.println("\nExample:\n");
        System.out.println("java -jar \"POIOSM2FS.jar\" -c ruinsplus.csv -l Ruins -w Winhour -a 356.7890 -o ruins -s 20 --rn --re");
        System.out.println("java -jar \"POIOSM2FS.jar\" -s 25 -j rzeki_IL.json -l Rzeki -w Winhour -a 421.3358 -o rzeki\n");
    }
    
    
    
    }
