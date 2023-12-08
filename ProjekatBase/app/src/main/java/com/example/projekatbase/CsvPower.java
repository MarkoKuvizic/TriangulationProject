package com.example.projekatbase;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvPower {
    InputStream inputStream;

    public CsvPower(InputStream inputStream){
        this.inputStream = inputStream;
    }
    public List read(){
        List resultList = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
//                Log.d("CSV READER", row[0] + "....");
                resultList.add(row);
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("Greska CSV: "+ex);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Greska na input.close(): "+e);
            }
        }
        return resultList;
    }
}
