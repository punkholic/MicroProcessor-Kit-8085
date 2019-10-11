package sample;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

public class HexCode {
    private HashMap<String, String> data, labelValues;

    public HexCode(HashMap<String, String> labelValues) {
        data = new HashMap<>();
        this.labelValues = labelValues;
        setData();
    }

    private void setData() {
        File mainPath = new File("");
        try {
            File currentPath = new java.io.File(".").getCanonicalFile();
            mainPath = new File(currentPath.getPath() + "/hexData.nrj");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(mainPath.toPath(), StandardOpenOption.READ))) {
            String value = "";
            while ((value = objectInputStream.readUTF()) != null) {
                String[] mniAndHex = value.split(",");
                data.put(mniAndHex[0], mniAndHex[1]);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

//    public void writeData() throws IOException {
//        File currentPath = new java.io.File(".").getCanonicalFile();
//        File mainPath = new File(currentPath.getPath()+"/hexData.nrj");
//        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(mainPath.toPath(), StandardOpenOption.CREATE_NEW))) {
//            for (String a : data.keySet()) {
//                objectOutputStream.writeUTF(a + "," + data.get(a));
//            }
//        }
//    }

    public ArrayList<String> getHexCode(String search) {
        ArrayList<String> tempData = new ArrayList<>();
        String[] splitedData = search.split("\\s");
        if (arrayContains(new String[]{"JMP","JNZ", "JZ", "JNC", "JC", "JPE", "JPO",
                "CALL", "CC", "CNC", "CNZ", "CZ", "CPE", "CPO"},splitedData[0])){
            tempData.add(data.get(splitedData[0]));
            String value = getLabelPosition(splitedData[1]);
            value = make16Bit(value);
            tempData.add(value.substring(2,4));
            tempData.add(value.substring(0, 2));
            return tempData;

        }
        else if (arrayContains(new String[]{ "RC", "RNC", "RZ", "RNZ", "RPE", "RPO", "RST"}, splitedData[0])) {
            for (String a : data.keySet()) {
                if (search.contains(a)) {
                    tempData.add(data.get(a));
                    return tempData;
                }
            }
        } else if (arrayContains(new String[]{"IN", "OUT", "ADI", "ACI", "SUI", "SBI", "ANI", "XRI", "ORI", "CPI"}, splitedData[0])) {
            for (String a : data.keySet()) {
                if (search.contains(a)) {
                    tempData.add(data.get(a));
                    tempData.add(splitedData[1]);
                    return tempData;
                }
            }
        }else if (arrayContains(new String[]{"LDA","STA","LHLD","SHLD"},splitedData[0])){
            for (String a : data.keySet()) {
                if (search.contains(a)) {
                    tempData.add(data.get(a));
                    String value = make16Bit(splitedData[1]);
                    tempData.add(value.substring(2,4));
                    tempData.add(value.substring(0,2));
                    return tempData;
                }
            }
        }else if (splitedData[0].equals("MVI")){
            for (String a : data.keySet()) {
                if (search.contains(a)) {
                    tempData.add(data.get(a));
                    tempData.add(splitedData[2]);
                    return tempData;
                }
            }
        }else if (splitedData[0].equals("LXI")){
            for (String a : data.keySet()) {
                if (search.contains(a)) {
                    tempData.add(data.get(a));
                    String value = make16Bit(splitedData[2]);
                    tempData.add(value.substring(2,4));
                    tempData.add(value.substring(0,2));
                    return tempData;
                }
            }
        }else {
            for (String a : data.keySet()) {
                if (a.equals(search)) {
                    tempData.add(data.get(a));
                    return tempData;
                }
            }
        }
        return null;
    }

    public String getLabelPosition(String search) {
        for (String a : labelValues.keySet()){
            if (a.contains(search)){
                return labelValues.get(a);
            }
        }
        return null;
    }

    public boolean arrayContains(String arr[], String toCheck) {
        for (String a : arr){
            if (a.equals(toCheck)){
                return true;
            }
        }
        return false;
    }
    public String make16Bit(String a){
        int l = 4 - a.length();
        for (int i=0;i<l;i++){
            a="0"+a;
        }
        return a;
    }
}
