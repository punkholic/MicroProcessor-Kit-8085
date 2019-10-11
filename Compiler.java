package sample;
import java.util.ArrayList;
import java.util.HashMap;
public class Compiler {

    private String[] allCodes;
    private HashMap<Integer, ArrayList> mnemonicsList;
    private ArrayList<String> userManagedMnemonics;

    public Compiler(String allCodes , HashMap<Integer,ArrayList> mnemonicsList, ArrayList<String> userManagedMnemonics) {
        this.allCodes = allCodes.split("\n");
        this.mnemonicsList = mnemonicsList;
        this.userManagedMnemonics = userManagedMnemonics;

    }


    public boolean hasInstructions(){
        for (String b : allCodes){
            if (onlySpace(b)){
                continue;
            }
            String[] temp = b.split("\\s");
            String a = temp[0].replaceAll(" ", "");
            if (a.equals("")){
                continue;
            }
            if (isLabelDestination(b)) {
                continue;
            }
            if (findByteValue(a)==-1){
                return false;
            }
        }
        return true;
    }
    public boolean checkSyntax(){
        String[] basicReg=new String[]{"A", "B", "C", "D", "E", "H", "L", "M"};
        for(String b: allCodes){
            if (b.equals(" ")){
                continue;
            }
            String[] temp = makeArray(b);
            //to check the query after the label is passed
            try {
                if (isLabelDestination(b)){
                    StringBuilder tempStr = new StringBuilder();
                    for (int i=1;i<temp.length;i++){
                        tempStr.append(" ").append(temp[i]);
                    }
                    temp = makeArray(tempStr.toString());
                }
                //to check if their is a label which is mentioned in the jump statement
                if (arrayContains(new String[]{"JMP", "JNZ", "JZ", "JNC", "JC", "JPE", "JPO", "CALL", "CNC", "CC", "CNZ", "CZ", "CPE", "CPO"}, temp[0]) && !findLabel(temp[1])) {
                    return false;
                }
                //to check the syntax of instructions
                if (!((temp[0].equals("MVI") && Integer.parseInt(temp[2].toUpperCase().replaceAll("H",""),16) <= 0xff && arrayContains(basicReg, temp[1]) && temp.length == 3)
                        || (temp[0].equals("MOV") && temp.length == 3 && arrayContains(basicReg, temp[1]) && arrayContains(basicReg, temp[2]))
                        || (arrayContains(new String[]{"DAA","XCHG","NOP","EI","DI","RIM","SIM", "SPHL", "XTHL", "PCHL", "HLT", "CMA", "CMC", "RLC", "RRC", "RAL", "RAR", "STC", "RET", "RC", "RNC", "RZ", "RNZ", "RPE", "RPO"}, temp[0])
                        && temp.length == 1) || ((temp[0].equals("LDAX") || temp[0].equals("STAX")) && temp.length == 2 && arrayContains(new String[]{"B", "D"}, temp[1]))
                        || (arrayContains(new String[]{"INR", "DCR", "ADD", "SBB", "ADC", "SUB", "ANA", "XRA", "ORA", "CMP"}, temp[0])
                        && temp.length == 2 && arrayContains(basicReg, temp[1])) ||
                        ((temp[0].equals("DAD") || temp[0].equals("INX") || temp[0].equals("DCX")) && temp.length == 2 && arrayContains(new String[]{"B", "D", "H", "SP"}, temp[1]))
                        || (arrayContains(new String[]{"ANI", "IN", "ADI", "ACI", "SUI", "SBI", "ANI", "XRI", "ORI", "CPI"}, temp[0]) &&
                        temp.length == 2 && Integer.parseInt(temp[1].toUpperCase().replaceAll("H",""),16) <= 0xff) ||
                        (temp[0].equals("PUSH") || temp[0].equals("POP") && temp.length == 2 && arrayContains(new String[]{"B", "D", "H", "PSW"}, temp[1]))
                        || (arrayContains(new String[]{"LDA", "STA", "LHLD", "SHLD"}, temp[0]) && temp.length == 2 && Integer.parseInt(temp[1].toUpperCase().replaceAll("H",""),16) <= 0xffff)
                        || (temp[0].equals("LXI") && arrayContains(new String[]{"B", "D", "H", "SP"}, temp[1]) && temp.length == 3 && Integer.parseInt(temp[2].toUpperCase().replaceAll("H",""),16) <= 0xffff)
                        || (arrayContains(new String[]{"JMP", "JNZ", "JZ", "JNC", "JC", "JPE", "JPO", "CALL", "CNC", "CC", "CNZ", "CZ", "CPE", "CPO"}, temp[0]) && temp.length == 2)
                || ((temp[0]).equals("OUT") && temp.length == 2 && Integer.parseInt(temp[1].replaceAll("H",""),2) <= 3)
                )) {
                    return false;
                }
            }catch (IndexOutOfBoundsException | NumberFormatException e){
                return false;
            }
        }
        return true;
    }

    public boolean onlySpace(String a) {
        if (a.equals("")){
            return true;
        }
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }
    public boolean findLabel(String label) {
        for (int i = 0; i < userManagedMnemonics.size(); i++) {
            try {
                if (userManagedMnemonics.get(i).equals(label + ":") || (userManagedMnemonics.get(i).equals(label) && userManagedMnemonics.get(i + 1).equals(":"))) {
                    return true;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }

        }
        return false;
    }
    public boolean arrayContains(String[] arr, String toCheck) {
        for (String a : arr){
            if (a.equals(toCheck)){
                return true;
            }
        }
        return false;
    }

    public boolean isLabelDestination(String b) {
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
    public String[] makeArray(String instruction){
        String[] arr = instruction.split("\\s");
        int len=0;
        for (String a : arr) {
            a = a.replaceAll(" ","");
            if (!(a.equals("") || a.equals(" ") || a.equals(":"))) {
                if (a.contains(":") && a.charAt(a.length()-1)!=':'){
                    len+=2;
                    continue;
                }
                len++;
            }

        }
        String[] newArr = new String[len];
        len=0;
        for (String a : arr) {
            a =a.replaceAll(" ","");
            if (a.equals("") || a.equals(" ") || a.equals(":")) {
                continue;
            }
            if (a.contains(":") && a.charAt(a.length() - 1) != ':') {
                String[] tempData = a.split(":");
                String values = tempData[0] + ":";
                newArr[len] = values;
                len++;
                newArr[len] = tempData[1];
                len++;
                continue;
            }
            newArr[len]=a;
            len++;
        }
        return newArr;
    }
    //returns instruction byte value checking instructions list
    public int findByteValue(String instruction){
        for(int i=1;i<=3;i++){
            for(int j=0;j<mnemonicsList.get(i).size();j++){
                if(mnemonicsList.get(i).get(j).equals(instruction)){
                    return i;
                }
            }
        }
        return -1;
    }

    public int PcValue(ArrayList<String> mnemonics, int currentLocation, int pc) {
        int tempPc = pc , pcEncountered = 0,temp=0;
        pc = 0;
        for (int i = 0; i <= currentLocation; i++) {
            if (temp==0 && pcEncountered==1){
                pc=0;
            }
            if (pcEncountered == 1 && !mnemonics.get(i).equals("PCHL")) {
                temp = 1;
                if (findByteValue(mnemonics.get(i)) != -1) {
                    pc += findByteValue(mnemonics.get(i));
                }
                continue;
            }
            if (findByteValue(mnemonics.get(i)) != -1) {
                if (mnemonics.get(i).equals("PCHL")) {
                    pcEncountered = 1;
                }
                pc += findByteValue(mnemonics.get(i));
            }
        }
        if (temp==1){
            return tempPc+pc;
        }
        return pc;
    }

}