package mainFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class ReadLogsPaxosResulsts {

    public static class WrapResults{
        List<Float> latencies;
        List<Float> throughputs;
        List<Float> clients;
        List<Float> operations;
        public WrapResults (){
            latencies=new LinkedList<>();
            throughputs=new LinkedList<>();
            clients=new LinkedList<>();
            operations=new LinkedList<>();
        }
        public WrapResults calcAverage(List<WrapResults> results){
            WrapResults wrapResults = new WrapResults();

            for (int i = 0; i < results.get(0).latencies.size(); i++) {
                float v1 = results.get(0).latencies.get(i) + results.get(1).latencies.get(i) + results.get(2).latencies.get(i);
                wrapResults.latencies.add(v1/3);
            }
            for (int i = 0; i < results.get(0).throughputs.size(); i++) {
                float v1 = results.get(0).throughputs.get(i) + results.get(1).throughputs.get(i) + results.get(2).throughputs.get(i);
                wrapResults.throughputs.add(v1/3);
            }
            for (int i = 0; i < results.get(0).clients.size(); i++) {
                float v1 = results.get(0).clients.get(i) + results.get(1).clients.get(i) + results.get(2).clients.get(i);
                wrapResults.clients.add(v1/3);
            }
            for (int i = 0; i < results.get(0).operations.size(); i++) {
                float v1 = results.get(0).operations.get(i) + results.get(1).operations.get(i) + results.get(2).operations.get(i);
                wrapResults.operations.add(v1/3);
            }

            return wrapResults;
        }
    }

    public static void main(String [] args) throws Exception {
        String [] protocolFolders = {"oldBabelResults","quicResults","tcpResulsts","udpResults"};
        //String [] protocolFolders = {"oldBabelResults"};

        for (String protocolFolder : protocolFolders) {
            List<WrapResults> wrapResultsList = new LinkedList<>();
            System.out.println("##FOLDER: "+protocolFolder);
            for (int i = 1; i < 4; i++) {
                String path = "/home/tsunami/Desktop/thesis_notes/paxosResulsts/"+protocolFolder+"/"+"test_"+i;
                //System.out.println("PATH - "+path);
                File folder = new File(path); // replace with actual folder path
                WrapResults wrapResults = new WrapResults();
                SortedMap<String,File> map = new TreeMap<>();
                for (File file : folder.listFiles()) {
                    if (file.isFile()) {
                        String name = file.getName();
                        if(name.contains("10")){
                            name="clients_99";
                        }
                        map.put(name,file);
                    }
                }
                for (Map.Entry<String,File> value : map.entrySet()) {
                    //System.out.println(value.getKey()+" "+value.getValue().getName());
                    calMetrics(wrapResults,value.getValue(),protocolFolder);
                    wrapResultsList.add(wrapResults);
                }
            }
            WrapResults metrics = wrapResultsList.get(0).calcAverage(wrapResultsList);

            System.out.println(protocolFolder+"_latencies="+ Arrays.toString(metrics.latencies.toArray()));
            System.out.println(protocolFolder+"_throughputs="+ Arrays.toString(metrics.throughputs.toArray()));
            System.out.println(protocolFolder+"_clients="+ Arrays.toString(metrics.clients.toArray()));
            System.out.println(protocolFolder+"_operations="+ Arrays.toString(metrics.operations.toArray()));
            System.out.println();
        }

    }

    public static void calMetrics(WrapResults wrapResults, File file, String protocolFolder)throws Exception{
        BufferedReader reader;
        List<Float> latencies=wrapResults.latencies;
        List<Float> throughputs=wrapResults.throughputs;
        List<Float> clients=wrapResults.clients;
        List<Float> operations=wrapResults.operations;

        //File file = new File("results.txt");
        reader = new BufferedReader(new FileReader(file));
        String line = "";
        String updateOrInsert;
        if(protocolFolder.contains("old")){
            updateOrInsert = "[UPDATE],";
        }else{
            updateOrInsert="[INSERT],";
        }
        while ( (line = reader.readLine()) != null){
            String result ="";;
            if( ( result = line.replace("[OVERALL], Throughput(ops/sec), ","")).length()<line.length()){
                throughputs.add(Float.parseFloat(result));
            }else if(( result = line.replace("[CLEANUP], Operations,","")).length()<line.length()){
                clients.add(Float.parseFloat(result));
            }else if((result = line.replace("[OVERALL], RunTime(ms),","")).length()<line.length()){
                operations.add(Float.parseFloat(result));
            }else if((result = line.replace(updateOrInsert+" AverageLatency(us), ","")).length()<line.length()){
                latencies.add(Float.parseFloat(result));
            }
        }
        reader.close();
        assert throughputs.size()==latencies.size() && latencies.size()== operations.size()&&operations.size()== clients.size();
        /**
         System.out.printf("throughputs = %s ;\n",throughputs);
         System.out.printf("latencies = %s ;\n",latencies);
         System.out.printf("operations = %s ;\n",operations);
         System.out.printf("clients = %s ;\n",clients);

         System.out.println(throughputs.size());
         **/
    }
}
