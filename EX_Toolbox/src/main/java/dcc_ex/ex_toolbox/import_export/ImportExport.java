package dcc_ex.ex_toolbox.import_export;

import static dcc_ex.ex_toolbox.threaded_application.context;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import dcc_ex.ex_toolbox.R;

public class ImportExport {

    public ArrayList<String>  servoList;
    public ArrayList<Integer> servoVpinList;
    public ArrayList<Integer> servoClosedPositionList;
    public ArrayList<Integer> servoMidPositionList;
    public ArrayList<Integer> servoThrownPositionList;
    public ArrayList<Integer> servoProfileList;

    private static final String SERVO_FILENAME = "servos.txt";

    public void initialiseServoList() {
        servoList = new ArrayList<>();
        servoVpinList = new ArrayList<>();
        servoClosedPositionList = new ArrayList<>();
        servoMidPositionList = new ArrayList<>();
        servoThrownPositionList = new ArrayList<>();
        servoProfileList = new ArrayList<>();

        addSelectToList();
    }

    public void updateServoList(int vpin, int closedPosition, int midPosition, int thrownPosition, int profile) {

        if (servoVpinList.size() > 0) {
            for (int i = 0; i < servoVpinList.size(); i++) {
                if (vpin == servoVpinList.get(i)) {  // check if it is already in the list and remove it
                    servoList.remove(i);
                    servoVpinList.remove(i);
                    servoClosedPositionList.remove(i);
                    servoMidPositionList.remove(i);
                    servoThrownPositionList.remove(i);
                    servoProfileList.remove(i);
                    break;
                }
            }
        }

        // now append it to the beginning of the list
        servoList.add(0, String.format("VPIN: %s - %d,%d,%d,%d",vpin, closedPosition, midPosition, thrownPosition, profile));
        servoVpinList.add(0, vpin);
        servoClosedPositionList.add(0, closedPosition);
        servoMidPositionList.add(0, midPosition);
        servoThrownPositionList.add(0, thrownPosition);
        servoProfileList.add(0, profile);

        addSelectToList();

    }

    public void writeServoListToFile(SharedPreferences sharedPreferences) {
        //if no SD Card present then nothing else to do
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;

        File servosFile = new File(context.getExternalFilesDir(null), SERVO_FILENAME);

        PrintWriter list_output;
//        String smrl = sharedPreferences.getString("maximum_recent_locos_preference", "10"); //retrieve pref for max recent locos to show
        try {
//            int mrl = Integer.parseInt(smrl);
            int mrl =10;
            list_output = new PrintWriter(servosFile);
            if (mrl > 0) {
                for (int i = 0; i < servoVpinList.size() && i < mrl; i++) {
                    if (servoVpinList.get(i) > 0) {
                        list_output.format("%d,%d,%d,%d,%d\n",
                                servoVpinList.get(i),
                                servoClosedPositionList.get(i),
                                servoMidPositionList.get(i),
                                servoThrownPositionList.get(i),
                                servoProfileList.get(i));
                    }
                }
            }
            list_output.flush();
            list_output.close();
            Log.d("EX_Toolbox", "ImportExport: writeServosToFile: Write servos to file complete successfully");
        } catch (IOException except) {
            Log.e("EX_Toolbox",
                    "ImportExport: writeServosToFile: caught IOException: "
                            + except.getMessage());
        } catch (NumberFormatException except) {
            Log.e("EX_Toolbox",
                    "ImportExport: writeServosToFile: caught NumberFormatException: "
                            + except.getMessage());
        } catch (IndexOutOfBoundsException except) {
            Log.e("EX_Toolbox",
                    "ImportExport: writeServosToFile: caught IndexOutOfBoundsException: "
                            + except.getMessage());
        }
    }

    // ad the <select> item to tht start of the list
    void addSelectToList() {
        if (servoList.size() > 0) {
            for (int i = 0; i < servoVpinList.size(); i++) {
                if (servoList.get(i).equals(context.getResources().getString(R.string.servoListDefaultValue))) { // or it is the select item (which should always be the first)
                    servoList.remove(i);
                    servoVpinList.remove(i);
                    servoClosedPositionList.remove(i);
                    servoMidPositionList.remove(i);
                    servoThrownPositionList.remove(i);
                    servoProfileList.remove(i);
                    break;
                }
            }
        }

        servoList.add(0, context.getResources().getString(R.string.servoListDefaultValue));
        servoVpinList.add(0, -1);
        servoClosedPositionList.add(0, -1);
        servoMidPositionList.add(0, -1);
        servoThrownPositionList.add(0, -1);
        servoProfileList.add(0, -1);
    }

    public void readServoListFromFile() {
        Log.d("EX_Toolbox", "getRecentLocosListFromFile: ImportExportPreferences: Loading recent locos list from file");
        if (servoVpinList == null) { //make sure arrays are valid
            initialiseServoList();
        }

        try {
            int size = -1;
            // Populate the List with the ones saved in a file.
            File servosFile = new File(context.getExternalFilesDir(null), SERVO_FILENAME);
            if (servosFile.exists()) {
                BufferedReader list_reader = new BufferedReader(
                        new FileReader(servosFile));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    int splitPos = line.indexOf(',');
                    if (splitPos > 0) {
                        Integer vpin, closedPosition, midPosition, thrownPosition, profile = 0;
                        String [] args = line.split(",");
                        try {
                            vpin = Integer.decode(args[0]);
                            closedPosition = Integer.decode(args[1]);
                            midPosition = Integer.decode(args[2]);
                            thrownPosition = Integer.decode(args[3]);
                            profile = Integer.decode(args[4]);
                        } catch (Exception e) {
                            vpin = -1;
                            closedPosition = -1;
                            midPosition = -1;
                            thrownPosition = -1;
                            profile = -1;
                        }
                        if (vpin >= 0) {
                            size++;
                            servoList.add(size, String.format("VPIN: %d - %d,%d,%d,%d",vpin, closedPosition, midPosition, thrownPosition, profile));
                            servoVpinList.add(size, vpin);
                            servoClosedPositionList.add(size, closedPosition);
                            servoMidPositionList.add(size, midPosition);
                            servoThrownPositionList.add(size, thrownPosition);
                            servoProfileList.add(size, profile);
                        }
                    }
                }
                list_reader.close();
            }
//            Log.d("Engine_Driver", "getRecentLocosListFromFile: readServoListFromFile: Read servos from file complete successfully");

        } catch (IOException except) {
            Log.e("EX_Toolbox", "ImportExport: readServoListFromFile: Error reading servos file. "
                    + except.getMessage());
        }
    }

}