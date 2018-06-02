import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Utils {
    public static ArrayList<String> fileToLineList(File file){

        ArrayList<String> list = new ArrayList<String>();
        try {
            Scanner input = new Scanner(file);

            while (input.hasNextLine()) {
                list.add(input.nextLine());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static ArrayList<String> fileToLineList(String filePath){
        return fileToLineList(new File(filePath));
    }
}
