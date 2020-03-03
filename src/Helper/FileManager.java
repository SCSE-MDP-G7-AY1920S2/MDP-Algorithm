package Helper;
import javafx.scene.image.Image;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Logger;

public class FileManager {

    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());
    private static final String realstr = "{'com': 'M'," +
            " 'imgRaw': [\"b'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'\"]}";
    private final String tiledImageFilename = "tiled_image.jpg";
    private String folderPath;

    public FileManager(){
        this.folderPath = "";
    }

    public void jsonToImage (String jsonStr) {

        File f;
        if(this.folderPath == "") {
            f = this.generateNewFolder();
            if (f == null)
                return;
        }
        else
            f = new File(this.folderPath);


        JSONArray arr = new JSONArray(jsonStr);
//        JSONArray arr = msg.getJSONArray("imgRaw");

        if(arr.isEmpty())
            return;

        for(int i=0; i<arr.length(); i++){
            int id = i;
            String image = arr.get(i).toString();

            try {
                LOGGER.info("Saving image: " + jsonStr);
                String tmp = image.split("'")[1];
                byte[] decodedString = Base64.getDecoder().decode(tmp.getBytes(StandardCharsets.UTF_8));
                File image_f = new File(f, id+".jpg");
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(image_f));
                bos.write(decodedString);
                bos.flush();
                bos.close();

            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



//        JSONObject msg = new JSONObject(new JSONTokener(jsonStr));
//        // tokenize the data
//        if (msg.has("imgRaw")) {
//            JSONArray arr = msg.getJSONArray("imgRaw");
//
//            for(int i=0; i<arr.length(); i++){
//                int id = i;
//                String image = arr.get(i).toString();
//
//                try {
//                    LOGGER.info("Saving image: " + msg);
//                    String tmp = image.split("'")[1];
//                    byte[] decodedString = Base64.getDecoder().decode(tmp.getBytes(StandardCharsets.UTF_8));
//                    File image_f = new File(f, id+".jpg");
//                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(image_f));
//                    bos.write(decodedString);
//                    bos.flush();
//                    bos.close();
//
//                } catch (UnsupportedEncodingException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }

    }

    public void saveMdf(String mapMdf, String exploredMdf){

        if(this.folderPath == "")
            this.generateNewFolder();

        // tokenize the data
        if (mapMdf != null && exploredMdf != null && this.folderPath != "") {

            try (PrintWriter out = new PrintWriter( this.folderPath + File.separator +"savedMdf.txt")) {
                out.println(mapMdf + "\n" + exploredMdf);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
        else
            return;
    }

    private File generateNewFolder(){
        String folderPath = "images";
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String path = System.getProperty("user.dir");
        path += File.separator + folderPath + File.separator + timeStamp;
        LOGGER.info("Creating new folder:" + path);

        File file = new File(path);
        //Creating the directory
        boolean isCreated = file.mkdirs();
        if(isCreated){
            LOGGER.info("Directory created successfully");
            this.folderPath = path;
            return file;
        }else{
            LOGGER.info("Could not create specified directory");
            return null;
        }
    }

    public Image generateImageFromPath(String img){
        File f = new File(this.folderPath + File.separator + img);
        if (f.isFile() && f.canRead()) {
            try {
                return new Image(new FileInputStream(this.folderPath + File.separator + img));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public void generateTileImage() throws IOException {
        LOGGER.info("Generating tiled image...");

        int width = 1000;
        int height = 550;
        int x = 0;
        int y = 0;
        int imageWidth = width / 3;

        String resultPath = this.folderPath + File.separator + tiledImageFilename;
        File folder = new File(this.folderPath.replace("\\", "/"));
        File[] listOfFiles = folder.listFiles();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        // Set background color to blue
        g.setPaint ( new Color (52, 171, 235) );
        g.fillRect ( 0, 0, result.getWidth(), result.getHeight() );

        for (int i = 0; i < listOfFiles.length; i++) {
            BufferedImage bi = ImageIO.read(listOfFiles[i]);
            g.drawImage(bi, x, y, null);
            x += imageWidth;
            if(i == 2){
                x = width/6;
                y = height/2;
            }
        }
//        x = width / 6;
//        y = y_inc;
//        for (int i = 3; i < 5; i++) {
//            BufferedImage bi = ImageIO.read(listOfFiles[i]);
//            g.drawImage(bi, x, y, null);
//            x += x_inc;
//        }

        ImageIO.write(result,"jpg",new File(resultPath));
        System.out.println(resultPath);
    }

    public String getTiledImageFilename() {
        return tiledImageFilename;
    }

}
