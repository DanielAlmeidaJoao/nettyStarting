package mainFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class ReadResults {
    Map<String, SortedMap<String,Float>> results;

    public static final String FAULT_0 ="faults_0";
    public static final String FAULT_12 ="faults_12";
    public static final String FAULT_24 ="faults_24";
    public static final String FAULT_52 ="faults_52";

    public ReadResults(){
        results = initMap2();
        start();
    }
    private void start(){
        File folder = new File("/home/tsunami/Desktop/thesis_projects/experimentsResults/tcpResults"); // replace with actual folder path
        for (File file : folder.listFiles()) {
            if (file.isFile()&&!(file.getName().contains("extra"))) {
                Map<String,Float> map = initMap();
                System.out.println(file.getName());

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    Float value;
                    while ((line = br.readLine()) != null) {

                        if(file.getName().contains("5")||file.getName().contains("6")){
                            value = getNumOther(line);
                        }else{
                            value = getNum(line);
                        }

                        String fault = br.readLine();
                        if(fault.contains(FAULT_0)){
                            fault = FAULT_0;
                        } else if (fault.contains(FAULT_12)) {
                            fault = FAULT_12;
                        } else if (fault.contains(FAULT_24)){
                            fault = FAULT_24;
                        }else{
                            fault = FAULT_52;
                        }
                        value = map.get(fault)+value;
                        map.put(fault,value);
                    }
                    for (Map.Entry<String, Float> stringLongEntry : map.entrySet()) {
                        results.get(stringLongEntry.getKey()).put(file.getName(),stringLongEntry.getValue());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (Map.Entry<String, SortedMap<String,Float>> stringListEntry : results.entrySet()) {
            System.out.printf(" %s = [ ",stringListEntry.getKey());
            boolean g = false;
            for (Float aLong : stringListEntry.getValue().values()) {
                if(g){
                    System.out.printf(",");
                }
                if(aLong>1){
                    System.out.printf(" %s ",aLong/3);
                }else{
                    System.out.printf(" %s ",aLong);
                }
                g = true;
            }
            System.out.printf(" ] ; \n");
        }
        String times = String.format("times = [ 4, 5, 6, 7 ] ;");
        System.out.println(times);
    }
    private Map<String,Float> initMap(){
        Map<String,Float> mp = new HashMap<>();
        mp.put(FAULT_0,0f);
        mp.put(FAULT_12,0f);
        mp.put(FAULT_24,0f);
        mp.put(FAULT_52,0f);
        return mp;
    }
    private Map<String,SortedMap<String,Float>> initMap2(){
        Map<String,SortedMap<String,Float>> mp = new HashMap<>();
        mp.put(FAULT_0,new TreeMap<>());
        mp.put(FAULT_12,new TreeMap<>());
        mp.put(FAULT_24,new TreeMap<>());
        mp.put(FAULT_52,new TreeMap<>());
        return mp;
    }
    private Float getNum(String input){
        try{
            return Float.parseFloat(input);
        }catch (Exception e){
            return -1f;
        }
    }
    private Float getNumOther(String input){
        try{
            String [] vals = input.split("-");
            return Float.parseFloat(vals[0]);
        }catch (Exception e){
            return -1f;
        }
    }

    private Float getNumOther2(String input){
        try{
            String [] vals = input.split("-");
            return Float.parseFloat(vals[1]);
        }catch (Exception e){
            return -1f;
        }
    }

    public static void main(String [] args){
        new ReadResults();
    }
}
