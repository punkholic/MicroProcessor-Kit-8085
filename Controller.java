package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Controller {
    @FXML
    private Button runButton, addMemory, debugMode, stopButton;
    @FXML
    private TextArea allCodes;
    @FXML
    private TableView<MemoryAddress> tableView;
    @FXML
    private Text AReg, BReg, CReg, DReg, EReg, HReg, LReg, MReg, SPReg, PCReg, ioOne, ioTwo, ioThree, ioFour, SFlag, CYFlag, PFlag, ZFlag, ACFlag, runningInstruct;
    @FXML
    private TextField operandValue, memoryAddress;
    @FXML
    private TextField pcInitialValue;
    @FXML
    BorderPane mainPane;
    //local variable
    private Compiler compiler;
    private ArrayList<String> mnemonics;
    private ArrayList<Text> ioList = new ArrayList<>();
    private HashMap<String, Text> registers = new HashMap<>();
    private static int currentLocation = 0;
    private HashMap<String, Boolean> flags = new HashMap<>();
    private HashMap<String, Text> flagsTextObj = new HashMap<>();
    private int max = 0xFF;
    private int maxAddress = 0xFFFF;
    private int firstValue, secondValue, returnValue = -1, pcValue = 0;
    private ObservableList<MemoryAddress> tableData = FXCollections.observableArrayList();
    private ArrayList<Integer> callTrackers = new ArrayList<>();
    private int FIFOCall = 0;
    private HashMap<Integer, ArrayList> mnemonicsList = new HashMap<>();
    private int loop = 0;
    private HashMap<String, String> labelLocation = new HashMap<>();
    private List<MemoryAddress> userInputtedList = new ArrayList<>();
    private List<Integer> PCHLValues = new ArrayList<>();
    private int hahaPc ;

    @FXML
    private GridPane getOs;

    private void OSCheck(){
            if (!System.getProperty("os.name").equals("Linux")){
                getOs.hgapProperty().setValue(800);

            }
    }

    public void initialize() {
        OSCheck();
        hahaPc = Integer.parseInt(pcInitialValue.getText(), 16);
        tableData.addAll(tableView.getItems());
        ArrayList<String> tempArray = new ArrayList<>();

        //oneByteInstructions
        String tempInstruction = "DAA,NOP,SIM,RIM,DI,EI,MOV,XCHG,LDAX,STAX,ADD,ADC,SUB,SBB,DAD,INR,DCR,INX,DCX,ANA,XRA,CMA,ORA,CMP,CMC,RLC,RCC,RAL,RAR,STC,RET,RC,RNC,RNZ,RZ,RPE,RPO,PUSH," +
                "PCHL,POP,XTHL,SPHL,HLT";
        Collections.addAll(tempArray, tempInstruction.split(","));
        mnemonicsList.put(1, tempArray);
        //twoByteInstructions
        tempArray = new ArrayList<>();
        tempInstruction = "MVI,IN,OUT,ADI,ACI,SUI,SBI,ANI,XRI,ORI,CPI";
        Collections.addAll(tempArray, tempInstruction.split(","));
        mnemonicsList.put(2, tempArray);
        //threeByteInstructions
        tempArray = new ArrayList<>();
        tempInstruction = "LDA,STA,LHLD,SHLD,LXI,JMP,JNZ,JZ,JNC,JC,JPE,JPO,CALL,CNC,CC,CNZ,CZ,CPE,CPO";
        Collections.addAll(tempArray, tempInstruction.split(","));
        mnemonicsList.put(3, tempArray);

        ioList.add(0, ioOne);
        ioList.add(1, ioTwo);
        ioList.add(2, ioThree);
        ioList.add(3, ioFour);

        registers.put("A", AReg);
        registers.put("B", BReg);
        registers.put("C", CReg);
        registers.put("D", DReg);
        registers.put("E", EReg);
        registers.put("H", HReg);
        registers.put("L", LReg);
        registers.put("M", MReg);
        registers.put("SP", SPReg);
        registers.put("PC", PCReg);

        flags.put(SFlag.getText(), false);
        flags.put(CYFlag.getText(), false);
        flags.put(PFlag.getText(), false);
        flags.put(ZFlag.getText(), false);
        flags.put(ACFlag.getText(), false);

        flagsTextObj.put(SFlag.getText(), SFlag);
        flagsTextObj.put(CYFlag.getText(), CYFlag);
        flagsTextObj.put(PFlag.getText(), PFlag);
        flagsTextObj.put(ZFlag.getText(), ZFlag);
        flagsTextObj.put(ACFlag.getText(), ACFlag);

        runButton.setOnAction(actionEvent -> {
            runningInstruct.setText("");
            if (!whenStart()) {
                return;
            }
            resetEverything();
//                Thread t1 = new Thread(new Mul());
//                t1.start();

            while (currentLocation < mnemonics.size()) {
                runProgram();
                if (loop > 50000) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Warning!!");
                    alert.setContentText("Please check you logic once more and try again!");
                    alert.setHeaderText("Infinite Loop");
                    alert.showAndWait();
                    break;
                }
            }
            updateHexCode();
            currentLocation = 0;
            loop = 0;
        });
        stopButton.setOnAction(actionEvent -> currentLocation = mnemonics.size());
        debugMode.setOnAction(actionEvent -> {
            if (loop == 0) {
                resetEverything();
                if (!whenStart()) {
                    return;
                }
            }
            if (currentLocation < mnemonics.size()) {
                runningInstruct.setText("" + matchPcAndCurrentLocation(currentLocation));
                runProgram();
            }

        });
        addMemory.setOnAction(actionEvent -> modifyTable());
    }
    private void resetEverything(){
        disableFlags();
        for (Text a : registers.values()) {
            if (a.getText().equals("SP") || a.getText().equals("PC")){
                a.setText(makeAddress("0"));
                continue;
            }
            a.setText(makeReg("0"));
        }
        for (Text a : ioList){
            a.setText(makeReg("0"));
        }
    }
    private boolean whenStart(){
        pcValue = Integer.parseInt(pcInitialValue.getText(),16);
        mnemonics = getMnemonics(allCodes.getText().toUpperCase());
        compiler = new Compiler(allCodes.getText().toUpperCase(), mnemonicsList, mnemonics);
        boolean hasInstrust = compiler.hasInstructions(), checkSyntax = compiler.checkSyntax();
        if ((!hasInstrust) || (!checkSyntax)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please check your syntax and try again!");
            alert.setTitle("ERROR!!");
            alert.setHeaderText("Compilation Error");
            alert.showAndWait();
            return false;
        }
        return true;
    }
    private void setAddressInCall(){
        String tempString;
        tempString = (makeAddress(Integer.toHexString(pcValue-findByteValue( mnemonics.get(currentLocation ))).toUpperCase()));
        secondValue = Integer.parseInt(SPReg.getText(), 16);
        setValue(addMemory(secondValue, 1), Integer.parseInt(tempString.substring(0, 2), 16));
        setValue(secondValue, Integer.parseInt(tempString.substring(2,4),16));
    }
    //check this
    private boolean isLabelDestination(String b) {
        ArrayList<String > temp = new ArrayList<>();
        String[] a = b.split("\\s");

        for (int i = 0; i < a.length; i++) {
            String abc = a[i].replaceAll(" ","");
            if (abc.equals("")){
                continue;
            }
            if (a[i].contains(":")){
                if (a[i].charAt(a[i].length()-1)!=':'){
                    String[] value = a[i].split(":");
                    String label = value[0]+":";
                    temp.add(label);
                    temp.add(value[1]);
                    continue;
                }
            }
            temp.add(a[i]);
        }
        if (a.length - 1 < 1) {
            return false;
        }
        try {
            if (temp.get(0).contains(":")) {
                if (findByteValue(temp.get(1)) != -1) {
                    return true;
                }
            } else if (temp.get(1).equals(":")) {
                if (findByteValue(temp.get(2)) != -1) {
                    return true;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        return false;
    }
    //check this
    private void updateHexCode() {
        int pcLoop=0;
        pcValue = Integer.parseInt(pcInitialValue.getText(),16);
        tableView.getItems().clear();
        ArrayList<String> gotHexInstrust = hexCodeInstruct();
        int totalRemove = 0;
        tableView.getItems().clear();
        HexCode hexCode = new HexCode(labelLocation);
        for (String a : gotHexInstrust){
            if (a.equals("PCHL")){
                pcValue = PCHLValues.get(pcLoop);
                totalRemove = 0;
                pcLoop++;
            }
            ArrayList<String> hexCodes = hexCode.getHexCode(a);
            for (String hex : hexCodes) {
                addDataInTable(new MemoryAddress(hex, makeAddress(Integer.toHexString(((totalRemove++) + pcValue)).toUpperCase())));
            }
        }
        for (MemoryAddress a : userInputtedList){
            if (!tableView.getItems().contains(a)){
                a.setAddressValue(makeAddress(a.getAddressValue()));
                a.setDataValue(makeReg(a.getDataValue()));
                addDataInTable(a);
            }
        }
    }
   private int pcEncountered= 0;
    private void runProgram() {
        String tempString;
        boolean tempBool;
        int added;
        char tempChar;
        pcValue = compiler.PcValue(mnemonics, currentLocation, hahaPc);
        if(pcValue!=1 && pcEncountered==0){
            hahaPc=Integer.parseInt(pcInitialValue.getText(),16);
            PCReg.setText(makeAddress(Integer.toHexString(hahaPc + pcValue).toUpperCase()));
        }else {
            PCReg.setText(makeAddress(Integer.toHexString(pcValue).toUpperCase()));
        }
        if (compiler.arrayContains(new String[]{"CALL", "CNC", "CC", "CNZ", "CZ", "CPE", "CPO"}, mnemonics.get(currentLocation).toUpperCase())) {
            callTrackers.add(currentLocation + 2);
        }

        switch (mnemonics.get(currentLocation).toUpperCase()) {
            case "MVI":
                registers.get(mnemonics.get(currentLocation + 1)).setText((mnemonics.get(currentLocation + 2)));
                if (mnemonics.get(currentLocation + 1).equals("H") || mnemonics.get(currentLocation + 1).equals("M")) {
                    updateMRegValue();
                }
                currentLocation += 3;
                break;
            case "MOV":
                registers.get(mnemonics.get(currentLocation + 1)).setText(registers.get(mnemonics.get(currentLocation + 2)).getText());
                currentLocation += 3;
                break;
            case "ADD":
                added = (Integer.parseInt(registers.get("A").getText(), 16) + Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(), 16));
                flags.put(ACFlag.getText(), isAuxiliaryCarry(Integer.parseInt(registers.get("A").getText(),16), Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16), '+'));
                flags.put(PFlag.getText(), isEvenParity(added));
                added = updateCarryAndSignAddFlag(added);
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "SUB":
                added = sub(Integer.parseInt(registers.get("A").getText(),16), Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16));
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "CMP":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                sub(firstValue, Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16));
                registers.get("A").setText(Integer.toHexString(firstValue).toUpperCase());
                currentLocation += 2;
                break;
            case "CPI":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                sub(firstValue, Integer.parseInt(mnemonics.get(currentLocation + 1),16));
                registers.get("A").setText(Integer.toHexString(firstValue).toUpperCase());
                currentLocation += 2;
                break;
            case "LDA":
                updateHexCode();
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1),16);
                registers.get("A").setText(Integer.toHexString(foundMemoryData(firstValue)).toUpperCase());
                currentLocation += 2;
                break;
            case "STA":
                updateHexCode();
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1),16);
                secondValue = Integer.parseInt(registers.get("A").getText(),16);
                setValue(firstValue, secondValue);
                currentLocation += 2;
                break;
            case "LHLD":
                updateHexCode();
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1),16);
                if (firstValue == maxAddress) {
                    secondValue = 0;
                } else {
                    secondValue = firstValue + 1;
                }
                firstValue = foundMemoryData(firstValue);
                secondValue = foundMemoryData(secondValue);
                LReg.setText(Integer.toHexString(firstValue).toUpperCase());
                HReg.setText(Integer.toHexString(secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "SHLD":
                updateHexCode();
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1),16);
                if (firstValue == maxAddress) {
                    secondValue = 0;
                } else {
                    secondValue = firstValue + 1;
                }
                setValue(firstValue, Integer.parseInt(LReg.getText(),16));
                setValue(secondValue, Integer.parseInt(HReg.getText(),16));
                currentLocation += 2;
                break;
            case "LXI":
                tempString = mnemonics.get(currentLocation + 1);
                registers.get(tempString).setText(mnemonics.get(currentLocation + 2).charAt(0) + "" + mnemonics.get(currentLocation + 2).charAt(1));
                tempChar = tempString.charAt(0);
                tempChar++;
                if (tempString.equals("H")) {
                    tempString = "L";
                } else {
                    tempString = "" + tempChar;
                }
                //can only put B D H Sp
                registers.get(tempString).setText(mnemonics.get(currentLocation + 2).charAt(2) + "" + mnemonics.get(currentLocation + 2).charAt(3));
                currentLocation += 3;
                break;
            case "XCHG":
                firstValue = Integer.parseInt(registers.get("D").getText(),16);
                secondValue = Integer.parseInt(registers.get("E").getText(),16);
                registers.get("D").setText(registers.get("H").getText());
                registers.get("E").setText(registers.get("L").getText());
                registers.get("H").setText(Integer.toHexString(firstValue).toUpperCase());
                registers.get("L").setText(Integer.toHexString(secondValue).toUpperCase());
                currentLocation += 1;
                break;
            case "LDAX":
                //B and D
                tempString = mnemonics.get(currentLocation + 1);
                tempChar = tempString.charAt(0);
                tempChar++;
                tempString = registers.get(mnemonics.get(currentLocation + 1)).getText();
                tempString += registers.get("" + tempChar).getText();
                firstValue = foundMemoryData(Integer.parseInt(tempString,16));
                registers.get("A").setText(Integer.toHexString(firstValue).toUpperCase());
                currentLocation += 2;
                break;
            case "STAX":
                tempString = mnemonics.get(currentLocation + 1);
                tempChar = tempString.charAt(0);
                tempChar++;
                tempString = registers.get(mnemonics.get(currentLocation + 1)).getText();
                tempString += registers.get("" + tempChar).getText();
                setValue(Integer.parseInt(tempString,16), Integer.parseInt(registers.get("A").getText(),16));
                currentLocation += 2;
                break;
            case "IN":
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1), 2);
                registers.get("A").setText(ioList.get(firstValue).getText());
                currentLocation += 2;
                break;
            case "OUT":
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1), 2);
                ioList.get(firstValue).setText(registers.get("A").getText());
                currentLocation += 2;
                break;
            case "ADI":
                firstValue = Integer.parseInt(mnemonics.get(currentLocation + 1),16);
                secondValue = Integer.parseInt(registers.get("A").getText(),16);
                added = firstValue + secondValue;
                added = updateCarryAndSignAddFlag(added);
                updateZeroFlag(added);
                flags.put(ACFlag.getText(), isAuxiliaryCarry(firstValue, secondValue, '+'));
                flags.put(PFlag.getText(), isEvenParity(added));
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "ADC":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                secondValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                added = firstValue + secondValue;
                if (flags.get(CYFlag.getText())) {
                    added = updateCarryAndSignAddFlag(added + 1);
                } else {
                    added = updateCarryAndSignAddFlag(added);
                }
                updateZeroFlag(added);
                flags.put(PFlag.getText(), isEvenParity(added));
                flags.put(ACFlag.getText(), isAuxiliaryCarry(firstValue, secondValue, '+'));
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "ACI":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                secondValue = Integer.parseInt(mnemonics.get(currentLocation + 1));
                added = firstValue + secondValue;
                if (flags.get(CYFlag.getText())) {
                    added = updateCarryAndSignAddFlag(added + 1);
                } else {
                    added = updateCarryAndSignAddFlag(added);
                }
                updateZeroFlag(added);
                flags.put(PFlag.getText(), isEvenParity(added));
                flags.put(ACFlag.getText(), isAuxiliaryCarry(firstValue, secondValue, '+'));
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "SUI":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                added = sub(firstValue, Integer.parseInt(mnemonics.get(currentLocation + 1),16));
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "SBB":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                secondValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                added = sub(firstValue, secondValue);
                if (flags.get(CYFlag.getText())) {
                    if ((added - 1) < 0) {
                        added = max + (added - 1);
                    }
                }
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "SBI":
                firstValue = Integer.parseInt(registers.get("A").getText(),16);
                secondValue = Integer.parseInt(mnemonics.get(currentLocation + 1),16);
                added = firstValue - secondValue;
                if (flags.get(CYFlag.getText())) {
                    added = firstValue - secondValue - 1;
                }
                if ((added) < 0) {
                    added = max + (added);
                }
                updateCarryAndSignSubFlag(firstValue, secondValue);
                updateZeroFlag(added);
                flags.put(PFlag.getText(), isEvenParity(added));
                flags.put(ACFlag.getText(), isAuxiliaryCarry(firstValue, secondValue, '-'));
                registers.get("A").setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "INR":
                firstValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                added = firstValue + 1;
                tempBool = flags.get(CYFlag.getText());
                added = updateCarryAndSignAddFlag(added);
                flags.put(CYFlag.getText(), tempBool);
                updateZeroFlag(added);
                flags.put(PFlag.getText(), isEvenParity(added));
                flags.put(ACFlag.getText(), isAuxiliaryCarry(firstValue, 1, '+'));
                registers.get(mnemonics.get(currentLocation + 1)).setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "DCR":
                firstValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                tempBool = flags.get(CYFlag.getText());
                added = updateCarryAndSignSubFlag(firstValue, 1);
                flags.put(CYFlag.getText(), tempBool);
                updateZeroFlag(added);
                flags.put(PFlag.getText(), isEvenParity(added));
                flags.put(ACFlag.getText(), isAuxiliaryCarry(firstValue, 1, '-'));
                registers.get(mnemonics.get(currentLocation + 1)).setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "INX":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = Integer.parseInt(registers.get(tempString).getText(),16);
                if (tempString.equals("SP")) {
                    registers.get(tempString).setText(Integer.toHexString(addMemory(firstValue, 1)).toUpperCase());
                    currentLocation += 2;
                    break;
                }
                tempChar = tempString.charAt(0);
                if (tempString.equals("H")) {
                    tempChar = 'L';
                } else {
                    tempChar++;
                }
                secondValue = Integer.parseInt(registers.get("" + tempChar).getText(),16);
                if ((secondValue + 1) > max) {
                    if ((firstValue + 1) > max) {
                        firstValue = 0;
                        secondValue = 0;
                    } else {
                        firstValue++;
                        secondValue = 0;
                    }
                } else {
                    secondValue++;
                }
                registers.get(tempString).setText("" + Integer.toHexString(firstValue).toUpperCase());
                registers.get("" + tempChar).setText(Integer.toHexString(secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "DCX":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = Integer.parseInt(registers.get(tempString).getText(),16);
                if (tempString.equals("SP")) {
                    registers.get(tempString).setText(Integer.toHexString(subMemory(firstValue, 1)).toUpperCase());
                    currentLocation += 2;
                    break;
                }
                tempChar = tempString.charAt(0);
                tempChar++;
                if (tempString.equals("H")) {
                    tempChar = 'L';
                } else {
                    tempChar++;
                }
                secondValue = Integer.parseInt(registers.get("" + tempChar).getText(),16);
                if ((secondValue - 1) < 0) {
                    if ((firstValue - 1) < 0) {
                        firstValue = max;
                        secondValue = max;
                    } else {
                        firstValue--;
                        secondValue = max;
                    }
                } else {
                    secondValue--;
                }
                registers.get(tempString).setText(Integer.toHexString(firstValue).toUpperCase());
                registers.get("" + tempChar).setText(Integer.toHexString(secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "DAD":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = Integer.parseInt(registers.get(tempString).getText(),16);
                tempChar = tempString.charAt(0);
                tempChar++;
                secondValue = Integer.parseInt(HReg.getText(),16);
                added = firstValue + secondValue;
                if ((firstValue + secondValue) > max) {
                    tempBool = flags.get(SFlag.getText());
                    added = updateCarryAndSignAddFlag(firstValue + secondValue);
                    flags.put(SFlag.getText(), tempBool);
                }
                HReg.setText(Integer.toHexString(added).toUpperCase());
                firstValue = Integer.parseInt(registers.get("" + tempChar).getText(),16);
                secondValue = Integer.parseInt(LReg.getText(),16);
                added = firstValue + secondValue;
                if ((firstValue + secondValue) > max) {
                    tempBool = flags.get(SFlag.getText());
                    added = updateCarryAndSignAddFlag(firstValue + secondValue);
                    flags.put(SFlag.getText(), tempBool);
                }
                LReg.setText(Integer.toHexString(added).toUpperCase());
                currentLocation += 2;
                break;
            case "ANA":
                firstValue = Integer.parseInt(AReg.getText(),16);
                secondValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                flags.put(ACFlag.getText(), true);
                flags.put(PFlag.getText(), isEvenParity(firstValue & secondValue));
                updateZeroFlag(firstValue & secondValue);
                AReg.setText(Integer.toHexString(firstValue & secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "ANI":
                firstValue = Integer.parseInt(AReg.getText(),16);
                secondValue = Integer.parseInt((mnemonics.get(currentLocation + 1)),16);
                flags.put(ACFlag.getText(), true);
                updateZeroFlag(firstValue & secondValue);
                flags.put(PFlag.getText(), isEvenParity(firstValue & secondValue));
                updateZeroFlag(firstValue & secondValue);
                AReg.setText(Integer.toHexString(firstValue & secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "XRA":
                firstValue = Integer.parseInt(AReg.getText(),16);
                secondValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                flags.put(PFlag.getText(), isEvenParity(firstValue ^ secondValue));
                updateZeroFlag(firstValue ^ secondValue);
                AReg.setText(Integer.toHexString(firstValue ^ secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "XRI":
                firstValue = Integer.parseInt(AReg.getText(),16);
                secondValue = Integer.parseInt((mnemonics.get(currentLocation + 1)),16);
                flags.put(PFlag.getText(), isEvenParity(firstValue ^ secondValue));
                updateZeroFlag(firstValue ^ secondValue);
                AReg.setText(Integer.toHexString(firstValue ^ secondValue).toUpperCase());
                updateZeroFlag(firstValue ^ secondValue);
                currentLocation += 2;
                break;
            case "ORA":
                firstValue = Integer.parseInt(AReg.getText(),16);
                secondValue = Integer.parseInt(registers.get(mnemonics.get(currentLocation + 1)).getText(),16);
                flags.put(PFlag.getText(), isEvenParity(firstValue | secondValue));
                updateZeroFlag(firstValue | secondValue);
                AReg.setText(Integer.toHexString(firstValue | secondValue).toUpperCase());
                currentLocation += 2;
                break;
            case "ORI":
                firstValue = Integer.parseInt(AReg.getText(),16);
                secondValue = Integer.parseInt((mnemonics.get(currentLocation + 1)),16);
                flags.put(PFlag.getText(), isEvenParity(firstValue | secondValue));
                updateZeroFlag(firstValue | secondValue);
                AReg.setText(Integer.toHexString(firstValue | secondValue).toUpperCase());
                updateZeroFlag(firstValue | secondValue);
                currentLocation += 2;
                break;
            case "CMA":
                firstValue = Integer.parseInt(AReg.getText(),16);
                AReg.setText(Integer.toHexString(Integer.parseInt(makeComplement(firstValue), 2)).toUpperCase());
                currentLocation += 1;
                break;
            case "CMC":
                tempBool = flags.get(CYFlag.getText());
                flags.put(CYFlag.getText(), !tempBool);
                currentLocation += 1;
                break;
            case "RLC":
                firstValue = Integer.parseInt(AReg.getText(),16);
                tempString = leftShiftData(firstValue);
                if (tempString.charAt(7) == '0') {
                    flags.put(CYFlag.getText(), false);
                } else {
                    flags.put(CYFlag.getText(), true);
                }
                AReg.setText(Integer.toHexString(Integer.parseInt(tempString, 2)).toUpperCase());
                currentLocation += 1;
                break;

                //done till here
            case "RAL":
                firstValue = Integer.parseInt(AReg.getText(),16);
                if (flags.get(CYFlag.getText())) {
                    tempChar = '1';
                } else {
                    tempChar = '0';
                }
                tempString = leftShiftData(firstValue);
                if (tempString.charAt(7) == '0') {
                    flags.put(CYFlag.getText(), false);
                } else {
                    flags.put(CYFlag.getText(), true);
                }
                tempString = tempString.substring(0, 7) + "" + tempChar;
                AReg.setText(Integer.toHexString(Integer.parseInt(tempString, 2)).toUpperCase());
                currentLocation += 1;
                break;
            case "RRC":
                firstValue = Integer.parseInt(AReg.getText(),16);
                tempString = rightShiftData(firstValue);
                if (tempString.charAt(0) == '0') {
                    flags.put(CYFlag.getText(), false);
                } else {
                    flags.put(CYFlag.getText(), true);
                }
                AReg.setText(Integer.toHexString(Integer.parseInt(tempString, 2)).toUpperCase());
                currentLocation += 1;
                break;
            case "RAR":
                firstValue = Integer.parseInt(AReg.getText(),16);
                if (flags.get(CYFlag.getText())) {
                    tempChar = '1';
                } else {
                    tempChar = '0';
                }
                tempString = rightShiftData(firstValue);
                if (tempString.charAt(0) == '0') {
                    flags.put(CYFlag.getText(), false);
                } else {
                    flags.put(CYFlag.getText(), true);
                }
                tempString = tempChar + "" + tempString.substring(1, 8);
                AReg.setText(Integer.toHexString(Integer.parseInt(tempString, 2)).toUpperCase());
                currentLocation += 1;
                break;
            case "STC":
                flags.put(CYFlag.getText(), true);
                currentLocation += 1;
                break;
            case "JMP":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "JNZ":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && !flags.get(ZFlag.getText())) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "JZ":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && flags.get(ZFlag.getText())) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "JC":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && flags.get(CYFlag.getText())) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "JNC":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && !flags.get(CYFlag.getText())) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "JPE":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && flags.get(PFlag.getText())) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "JPO":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && !flags.get(PFlag.getText())) {
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;

            case "CALL":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }

                break;
            case "CZ":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && !flags.get(ZFlag.getText())) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "CNZ":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && flags.get(ZFlag.getText())) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "CNC":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && !flags.get(CYFlag.getText())) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "CC":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && flags.get(CYFlag.getText())) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "CPE":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && flags.get(PFlag.getText())) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "CPO":
                tempString = mnemonics.get(currentLocation + 1);
                firstValue = branchLocation(tempString);
                if (firstValue != -1 && !flags.get(PFlag.getText())) {
                    updateSpCAll();
                    setAddressInCall();
                    returnValue = currentLocation + 1;
                    currentLocation = firstValue;
                } else {
                    currentLocation += 2;
                }
                break;
            case "RET":
                if (returnValue != -1) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                    FIFOCall++;
                } else {
                    currentLocation += 1;
                }
                break;
            case "RNZ":
                if (returnValue != -1 && !flags.get(ZFlag.getText())) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                    FIFOCall++;
                } else {
                    currentLocation += 1;
                }
                break;
            case "RZ":
                if (returnValue != -1 && flags.get(ZFlag.getText())) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                } else {
                    FIFOCall++;
                    currentLocation += 1;
                }
                break;
            case "RNC":
                if (returnValue != -1 && !flags.get(CYFlag.getText())) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                } else {
                    currentLocation += 1;
                }
                break;
            case "RC":
                if (returnValue != -1 && flags.get(CYFlag.getText())) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                    FIFOCall++;
                } else {
                    currentLocation += 1;
                }
                break;
            case "RPE":
                if (returnValue != -1 && flags.get(PFlag.getText())) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                    FIFOCall++;
                } else {
                    currentLocation += 1;
                }
                break;
            case "RPO":
                if (returnValue != -1 && !flags.get(PFlag.getText())) {
                    updateSpReturn();
                    currentLocation = callTrackers.get(FIFOCall);
                    FIFOCall++;
                } else {
                    currentLocation += 1;
                }
                break;

            case "HLT":
                currentLocation = mnemonics.size();
                break;
            case "PCHL":
                hahaPc = Integer.parseInt(HReg.getText() + "" + LReg.getText(),16);
                PCReg.setText(makeAddress(Integer.toHexString(hahaPc).toUpperCase()));
                PCHLValues.add(hahaPc);
                pcEncountered=1;
                currentLocation += 1;
                break;
            case "PUSH":
                tempString = mnemonics.get(currentLocation + 1);
                tempChar = tempString.charAt(0);
                tempChar++;
                firstValue = Integer.parseInt(registers.get(tempString).getText(),16);
                updateSpCAll();
                secondValue = Integer.parseInt(SPReg.getText(), 16);
                setValue(addMemory(secondValue, 1), firstValue);
                firstValue = Integer.parseInt(registers.get("" + tempChar).getText(),16);
                setValue(secondValue, firstValue);
                currentLocation += 2;
                break;
            case "POP":
                tempString = mnemonics.get(currentLocation + 1);
                tempChar = tempString.charAt(0);
                tempChar++;
                updateSpReturn();
                firstValue = Integer.parseInt(SPReg.getText(),16);
                registers.get(tempString).setText(makeReg(Integer.toHexString(foundMemoryData(subMemory(firstValue, 1))).toUpperCase()));
                registers.get("" + tempChar).setText(makeReg(Integer.toHexString(foundMemoryData(subMemory(firstValue, 2))).toUpperCase()));
                currentLocation += 2;
                break;
            case "XTHL":
                XTHL();
                currentLocation += 1;
                break;
            case "SPHL":
                SPReg.setText(makeReg(HReg.getText()) + "" + makeReg(LReg.getText()));
                currentLocation += 1;
                break;
            case "DAA":
                String value = DAA();
                flags.put(PFlag.getText(),isEvenParity(Integer.parseInt(value,16)));
                AReg.setText(value);
                currentLocation+=1;
                break;
            default:
                if (compiler.arrayContains(new String[]{"NOP","EI","DI","RIM","SIM"},mnemonics.get(currentLocation).toUpperCase())){
                    currentLocation+=1;
                    break;
                }
                labelLocation.put(mnemonics.get(currentLocation),""+compiler.PcValue(mnemonics, currentLocation,pcValue));
                currentLocation += 1;
                //if encountered label it will increase 1 then start from next instruction corresponding to the label
                runProgram();
                break;
        }
        loop++;
        updateFlag();
        updateMRegValue();
    }
    private String makeAddress(String value){
        int len = 4 - value.length();
        StringBuilder valueBuilder = new StringBuilder(value);
        for (int i = 0; i<len; i++){
            valueBuilder.insert(0, "0");
        }
        value = valueBuilder.toString();
        return value;
    }
    private String makeReg(String value){
        int len = 2 - value.length();
        StringBuilder valueBuilder = new StringBuilder(value);
        for (int i = 0; i<len; i++){
            valueBuilder.insert(0, "0");
        }
        value = valueBuilder.toString();
        return value;
    }
//    private ArrayList<String> getAllCodesForPc() {
//        String[] allCode = allCodes.getText().toUpperCase().split("\n");
//        ArrayList<String> a = new ArrayList<>();
//        for (String ab : allCode) {
//            if (ab.equals("") || ab.equals(" ")) {
//                continue;
//            }
//            a.add(ab);
//            loop++;
//        }
//        return a;
//    }

    //think about this
    private String DAA(){
        int aRegValue = Integer.parseInt(AReg.getText(), 16);
        StringBuilder binaryValue = new StringBuilder(Integer.toBinaryString(aRegValue));
        int len = 8 - binaryValue.length(), firstValue, secondValue;
        for (int i=0;i<len;i++){
            binaryValue.insert(0, "0");
        }
        firstValue = Integer.parseInt(binaryValue.substring(4, 8), 2);
        secondValue = Integer.parseInt(binaryValue.substring(0, 4), 2);
        //last nibble binaryValue.substring(4, 8)
        //first nibble binaryValue.substring(0, 4)
        if (Integer.parseInt(binaryValue.substring(4, 8),2)>9 || flags.get(ACFlag.getText())){
            if (isAuxiliaryCarry(firstValue,6,'+')){
                firstValue+=6;
                firstValue = Integer.parseInt(getValidBinary(firstValue).substring(4, 8), 2);
                flags.put(ACFlag.getText(),true);
                if (isAuxiliaryCarry(secondValue, 1, '+')) {
                    secondValue+=1;
                    secondValue = Integer.parseInt(getValidBinary(secondValue).substring(4,8),2);
                    flags.put(CYFlag.getText(), true);
                }
            }
        }
        if (secondValue > 9 || flags.get(CYFlag.getText())){
            secondValue += 6;
            secondValue = Integer.parseInt(getValidBinary(secondValue).substring(4,8),2);
        }
        return (secondValue+""+firstValue);
    }

    private String getValidBinary(int num) {
        StringBuilder binaryValue = new StringBuilder(Integer.toBinaryString(num));
        int len = 8 - binaryValue.length();
        for (int i = 0; i < len; i++) {
            binaryValue.insert(0, "0");
        }
        return binaryValue.toString();
    }
    private String matchPcAndCurrentLocation(int currentLocationOfProgram) {
        StringBuilder tempStringOK;
        int found=0;
        try {
            found = 0;
//            if (geda==1){
//                for (int i=0;;i++){
//                    if (findByteValue(mnemonics.get(currentLocationOfProgram+i))==-1){
//                        continue;
//                    }else{
//                      found[0]=i;
//                      geda=0;
//                      break;
//                    }
//                }
//            }
            if (isLabelDestination(mnemonics.get(currentLocationOfProgram))) {
                for (int i = currentLocationOfProgram; ; i++) {
                    if (!(findByteValue(mnemonics.get(currentLocationOfProgram + i)) == -1)) {
                        found = i;
                        break;
                    }
                }
            }
            tempStringOK = new StringBuilder(mnemonics.get(currentLocationOfProgram + found));
            int loop = currentLocationOfProgram + 1 + found;
            if (findByteValue(tempStringOK.toString()) == -1) {
                if (mnemonics.get(currentLocationOfProgram).contains(":")) {
                    loop = currentLocationOfProgram + 2;
                    tempStringOK = new StringBuilder(mnemonics.get(currentLocationOfProgram + 1));
                } else if (mnemonics.get(currentLocationOfProgram + 2).contains(":")) {
                    tempStringOK = new StringBuilder(mnemonics.get(currentLocationOfProgram + 2));
                    loop = currentLocationOfProgram + 3;
                }
            }

            for (int i = loop; i < mnemonics.size(); i++) {

                if (findByteValue(mnemonics.get(i)) != -1 || mnemonics.get(i).contains(":")) {
                    break;
                } else {
                    tempStringOK.append(" ").append(mnemonics.get(i));
                }
            }
        } catch (IndexOutOfBoundsException e) {
            return "EOF";
        }
        return tempStringOK.toString();
    }

    private int subMemory(int memAddress, int operand) {
        int finalValue;
        if ((memAddress - operand) < 0) {
            finalValue = (maxAddress + 1) + (memAddress - operand);
        } else {
            finalValue = memAddress - operand;
        }
        return finalValue;
    }

    private int addMemory(int memAddress, int operand) {
        int finalValue;
        if ((operand + memAddress) > maxAddress) {
            finalValue = (operand + memAddress) - (memAddress + 1);
        } else {
            finalValue = memAddress + operand;
        }
        return finalValue;
    }

    private void XTHL() {
        int firstValue, secondValue, thirdValue, fourthValue;
        firstValue = Integer.parseInt(SPReg.getText(),16);
        secondValue = firstValue + 1;
        if (firstValue == maxAddress) {
            secondValue = 0;
        }
        thirdValue = Integer.parseInt(HReg.getText());//H value
        fourthValue = Integer.parseInt(LReg.getText());//L value
        LReg.setText("" + foundMemoryData(firstValue));//4050
        HReg.setText("" + foundMemoryData(secondValue));//4051
        setValue(secondValue, thirdValue);
        setValue(firstValue, fourthValue);
    }

    private void updateSpReturn() {
        firstValue = Integer.parseInt(SPReg.getText(),16);
        if ((firstValue + 2) > maxAddress - 1) {
            firstValue = (firstValue + 2) - (maxAddress + 1);
        } else {
            firstValue = firstValue + 2;
        }
        returnValue = currentLocation + 1;
        SPReg.setText(makeAddress(Integer.toHexString(firstValue).toUpperCase()));
    }

    private void updateSpCAll() {
        secondValue = Integer.parseInt(SPReg.getText(),16);
        if (secondValue < 2) {
            secondValue = (maxAddress + 1) + (secondValue - 2);
        } else {
            secondValue -= 2;
        }
        SPReg.setText(makeAddress(Integer.toHexString(secondValue).toUpperCase()));
    }

    private int branchLocation(String label) {
        label = label + ":";
        for (int i = 0; i < mnemonics.size(); i++) {
            if (mnemonics.get(i).equals(label)) {
                return i;
            }
        }
        return -1;
    }

    private String leftShiftData(int a) {
        StringBuilder value = new StringBuilder(Integer.toBinaryString(a));
        char tempChar;
        int zeroS = (8 - value.length());
        for (int i = 0; i < zeroS; i++) {
            value.insert(0, "0");
        }
        char[] b = value.toString().toCharArray();
        tempChar = b[0];
        for (int i = 0; i < b.length - 1; i++) {
            b[i] = b[i + 1];
        }
        b[7] = tempChar;
        return new String(b);
    }

    private String rightShiftData(int a) {
        StringBuilder value = new StringBuilder(Integer.toBinaryString(a));
        char tempChar;
        int zeroS = (8 - value.length());
        for (int i = 0; i < zeroS; i++) {
            value.insert(0, "0");
        }
        char[] b = new char[value.length()];
        char[] c = value.toString().toCharArray();
        tempChar = c[c.length - 1];
        for (int i = 0; i < b.length - 1; i++) {
            b[i + 1] = c[i];
        }
        b[0] = tempChar;
        return new String(b);
    }

    private String makeComplement(int a) {
        StringBuilder value = new StringBuilder(Integer.toBinaryString(a));
        StringBuilder comp = new StringBuilder();
        int zeroS = (8 - value.length());
        for (int i = 0; i < zeroS; i++) {
            value.insert(0, "0");
        }
        for (int i = 0; i < 8; i++) {
            if (value.charAt(i) == '0') {
                comp.append("1");
            } else {
                comp.append("0");
            }
        }
        return comp.toString();

    }

    private void updateMRegValue() {
        int firstValue = Integer.parseInt(HReg.getText() + "" + LReg.getText(),16);
        MReg.setText(makeReg(Integer.toHexString(foundMemoryData(firstValue)).toUpperCase()));
    }

    private void modifyTable() {
        if (operandValue.getText().equals("")){
            operandValue.setText("0");
        }
        MemoryAddress a =
                new MemoryAddress(makeReg(operandValue.getText()),
                        makeAddress(memoryAddress.getText()));
        userInputtedList.add(a);
        addDataInTable(a);
    }

    private ArrayList<String> getMnemonics(String datas) {
        ArrayList<String> orginalData = new ArrayList<>();
        String[] splited = datas.split("\\s");
        for (String a : splited) {
            String b = a.replace(" ", "");
            if (b.equals("")) {
                continue;
            }
            if (b.contains(":")){
                if (b.charAt(b.length()-1)!=':'){
                    String[] value = b.split(":");
                    String label = value[0]+":";
                    orginalData.add(label);
                    orginalData.add(value[1]);
                    continue;
                }
            }
            if (findByteValue(b)==-1 && !isLabelDestination(b) && !registers.containsKey(b)){
                b = b.toUpperCase().replaceAll("H", "");
            }
            orginalData.add(b);
        }
        return orginalData;
    }

    private boolean isEvenParity(int a) {
        if (a == 0) {
            return false;
        }
        String value = Integer.toBinaryString(a);
        int oneCount = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '1') {
                oneCount++;
            }
        }
        return (oneCount % 2 == 0);
    }

    private boolean isAuxiliaryCarry(int a, int b, char operation) {
        StringBuilder first = new StringBuilder(Integer.toBinaryString(a));
        StringBuilder second = new StringBuilder(Integer.toBinaryString(b));
        int carry = 0;
        int firLen = (8 - first.length());
        int secLen = (8 - second.length());
        for (int i = 0; i < firLen; i++) {
            first.insert(0, "0");
        }
        for (int i = 0; i < secLen; i++) {
            second.insert(0, "0");
        }
        if (operation == '+') {
            for (int i = 7; i >= 4; i--) {
                if (first.charAt(i) == second.charAt(i)) {
                    if (first.charAt(i) == '1' && carry != 1) {
                        carry = 1;
                    } else if (first.charAt(i) == '1' && carry == 1) {
                        carry = 1;
                    } else if (first.charAt(i) == '0') {
                        carry = 0;
                    }
                } else if ((first.charAt(i) != second.charAt(i))) {
                    if ((first.charAt(i)=='1' && carry==1) || (second.charAt(i)=='1' && carry==1) ){
                        carry=1;
                    }else{
                        carry = 0;
                    }
                }
            }
        } else if (operation == '-') {
            for (int i = 7; i >= 4; i--) {
                if (first.charAt(i) == '0' && second.charAt(i) == '1') {
                    carry = 1;
                }
                if (carry == 1 && first.charAt(i) == '1') {
                    carry = 0;
                    if (second.charAt(i) == '1') {
                        carry = 1;
                    }
                }
            }
        }
        return (carry == 1);
    }

    private void updateFlag() {
        for (String a : flags.keySet()) {
            if (flags.get(a)) {
                flagsTextObj.get(a).setFill(Color.RED);
            } else {
                flagsTextObj.get(a).setFill(Color.BLACK);
            }
        }
    }

    private void updateZeroFlag(int a) {
        if (a == 0) {
            flags.put(ZFlag.getText(), true);
            flags.put(ACFlag.getText(), false);
        } else {
            flags.put(ZFlag.getText(), false);
        }
    }

//    public boolean isLabel(String a) {
//        if (findByteValue(a) == -1 && !compiler.arrayContains(new String[]{"A", "B", "C", "D", "E", "H", "L", "SP", "PC"}, a)) {
//            try {
//                Integer.parseInt(a, 16);
//                return true;
//            } catch (NumberFormatException e) {
//                return false;
//            }
//        }
//        return false;
//    }

    //for add
    private int updateCarryAndSignAddFlag(int a) {
        int value = a;
        if (a > max) {
            value = a - max - 1;
            flags.put(CYFlag.getText(), true);
        } else {
            flags.put(CYFlag.getText(), false);
        }
        if (flags.get(SFlag.getText()) && max > a) {
            flags.put(SFlag.getText(), true);
        } else {
            flags.put(SFlag.getText(), false);
        }
        if (flags.get(SFlag.getText())) {
            if (max > a) {
                flags.put(SFlag.getText(), true);
            }
        }
        return value;
    }

    //    private int decimalToBCD(int a) {
//        String mainValue="";
//        while (a != 0) {
//            int r = a % 10;
//            String tempData = Integer.toBinaryString(r);
//            int len = 4-tempData.length();
//            for (int i=0;i<len;i++){
//                tempData="0"+tempData;
//            }
//            mainValue=tempData+mainValue;
//            a/=10;
//        }
//        return Integer.parseInt(mainValue,2);
//    }
    //for sub
    private int updateCarryAndSignSubFlag(int a, int b) {
        int sub = a - b;
        if (sub < 0) {
            sub = max + sub + 1;
        }
        if (a < b) {
            flags.put(SFlag.getText(), true);
            flags.put(CYFlag.getText(), true);
        } else {
            flags.put(SFlag.getText(), false);
            flags.put(CYFlag.getText(), false);
        }
        return sub;
    }

    private int sub(int a, int b) {
        int added;
        added = updateCarryAndSignSubFlag(a, b);
        updateZeroFlag(added);
        flags.put(ACFlag.getText(), isAuxiliaryCarry(a, b, '-'));
        flags.put(PFlag.getText(), isEvenParity(added));
        return added;
    }

    private int foundMemoryData(int a) {
        List<MemoryAddress> aList = new ArrayList<>(tableView.getItems());
        for (MemoryAddress memoryAddress : aList) {
            if (Integer.parseInt(memoryAddress.getAddressValue(),16) == a) {
                return Integer.parseInt(memoryAddress.getDataValue(),16);
            }
        }
        return 0;
    }private void setValue(int address, int value) {
        if (tableView == null) {
            return;
        }
//        List<MemoryAddress> a = new ArrayList<>(tableView.getItems());
//        for (MemoryAddress memoryAddress : a) {
//            if (Integer.parseInt(memoryAddress.getAddressValue(),16) == address) {
                addDataInTable(new MemoryAddress(makeReg(Integer.toHexString(value).toUpperCase()), makeAddress(Integer.toHexString(address).toUpperCase())));
                userInputtedList.add(new MemoryAddress(makeReg(Integer.toHexString(value).toUpperCase()),makeAddress(Integer.toHexString(address).toUpperCase())));
//            }
//        }
        tableView.refresh();
    }

    //table work
    @FXML
    private void deleteData(){
        if (tableView.getSelectionModel().getSelectedItem() != null) {
            List<MemoryAddress> list = new ArrayList<>(tableView.getItems());
            MemoryAddress toRemove = tableView.getSelectionModel().getSelectedItem();
            userInputtedList.remove(toRemove);
            tableView.getItems().clear();
            for (MemoryAddress a : list){
                if (!a.equals(toRemove)){
                    tableView.getItems().add(a);
                }
            }
            tableView.refresh();
        }
    }
    @FXML
    private void updateData(){
        if (tableView.getSelectionModel().getSelectedItem() != null) {
            List<MemoryAddress> list = new ArrayList<>(tableView.getItems());
            MemoryAddress modify = tableView.getSelectionModel().getSelectedItem();
            userInputtedList.get(userInputtedList.indexOf(modify)).setDataValue(makeReg(operandValue.getText()));
            userInputtedList.get(userInputtedList.indexOf(modify)).setAddressValue(makeAddress(memoryAddress.getText()));
            tableView.getItems().clear();
            for (MemoryAddress a : list){
                if (!a.equals(modify)){
                    tableView.getItems().add(a);
                }else{
                    tableView.getItems().add(userInputtedList.get(userInputtedList.indexOf(modify)));
                }
            }
            tableView.refresh();
        }
    }
    //table modifing above
    private void addDataInTable(MemoryAddress memoryAddress) {
        if (userInputtedList.contains(memoryAddress)){
            for (MemoryAddress a: userInputtedList){
                if (a.equals(memoryAddress)){
                    a.setDataValue(memoryAddress.getDataValue());
                    a.setAddressValue(memoryAddress.getAddressValue());
                }
            }
        }
        if (tableView.getItems().isEmpty()) {
            tableView.getItems().add(memoryAddress);
            return;
        }
        List<MemoryAddress> list = new ArrayList<>(tableView.getItems());
        tableView.getItems().clear();
        ObservableList<MemoryAddress> observableList = FXCollections.observableList(list);
        int temp = -1;
        for (MemoryAddress b : observableList) {
            if (memoryAddress.equals(b)) {
                MemoryAddress newMemoryAddress = new MemoryAddress(memoryAddress.getDataValue(),
                        memoryAddress.getAddressValue());
                tableView.getItems().add(newMemoryAddress);
                temp = 0;
                continue;
            }
            tableView.getItems().add(b);
        }
        if (temp == -1) {
            tableView.getItems().add(memoryAddress);
        }
        tableView.refresh();
    }

    public int findByteValue(String instruction) {
        for (int i = 1; i <= 3; i++) {
            for (int j = 0; j < mnemonicsList.get(i).size(); j++) {
                if (mnemonicsList.get(i).get(j).equals(instruction)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void disableFlags() {
        flags.replaceAll((a, v) -> false);
    }

    //for menu items *******************************************************************************************************************************************
    private File oldFile = null;

    @FXML
    private void onNewMenu(){
        allCodes.setText("");
    }

    @FXML
    private void onOpenMenu(){
        String a="";
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("NRJ","*.nrj"));
        File chooseFile = fileChooser.showOpenDialog(mainPane.getScene().getWindow());
        if (chooseFile==null){
            System.out.println("ERROR!!");
            return;
        }
        oldFile = chooseFile;
        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(chooseFile.toPath(),StandardOpenOption.READ))){
            a = objectInputStream.readUTF();
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        allCodes.setText(a);
    }
    @FXML
    private void onSaveMenu(){
        if (oldFile == null){
            onSaveAsMenu();
        }else{
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(oldFile.toPath(), StandardOpenOption.CREATE))) {
                objectOutputStream.reset();
                objectOutputStream.writeUTF(allCodes.getText());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @FXML
    public void showDialog() throws IOException {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainPane.getScene().getWindow());
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("FXMLs/DialogBox.fxml"));
        dialog.getDialogPane().setContent(fxmlLoader.load());
        dialog.setTitle("About Me");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();

    }
    @FXML
    private void onSaveAsMenu(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("NRJ","*.nrj"));
        File chooseFile = fileChooser.showSaveDialog(mainPane.getScene().getWindow());
        if (chooseFile==null){
            System.out.println("ERROR!!");
            return;
        }
        File mainFile = new File(chooseFile.toString() + ".nrj");
        oldFile = mainFile;
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(mainFile.toPath(), StandardOpenOption.CREATE_NEW))) {
            objectOutputStream.writeUTF(allCodes.getText());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void onExit(){
        System.exit(0);
    }

    //Clipboard clipboard =Toolkit.getDefaultToolkit().getSystemClipboard();
    //clipboard.setContents(new StringSelection("hahaha bro funny"), null);

    //end of the code of menu items *****************************************************************************************************************************

    private ArrayList<String> hexCodeInstruct(){
        ArrayList<String> tempList = new ArrayList<>();
        int firstTime=0;
        String[] temp = allCodes.getText().toUpperCase().split("\n");
        for (String a:temp){
            if (compiler.onlySpace(a)){
                continue;
            }
            String[] data = a.split("\\s+");
            if (a.contains(":")){
                String[] tempData = a.split(":");
                data = tempData[1].split("\\s+");
            }
            StringBuilder mainData= new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                //their is an index element with a blank space in the "data" string array
                //i used the following code to omit it but it is still printing why?
                String d = data[i].replaceAll(" ","");
                if (compiler.onlySpace(d)) {
                    continue;
                }
                if (firstTime == 0) {
                    mainData = new StringBuilder(data[i]);
                    firstTime=-1;
                } else {
                    if (findByteValue(data[i])==-1 && !isLabelDestination(data[i]) && !registers.containsKey(data[i])){
                        data[i] = data[i].toUpperCase().replaceAll("H", "");
                    }
                    mainData.append(" ").append(data[i]);
                }
            }
            firstTime=0;
            tempList.add(mainData.toString());
        }
        return tempList;
    }
}


//fix AC while adding it test sub data, also enables on first nibble increment(Done)
//update PC according to byte instruction(Done)
//tempChar++ m paxi l aauxa;(Done)
//Add DAA,EI,DI,EI,DI,RST,RIM,SIM,NOP(add only DDA)
//update PC in branching conditions(while coding compiler)