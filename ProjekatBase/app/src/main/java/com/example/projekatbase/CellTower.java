package com.example.projekatbase;

import android.telephony.CellInfoGsm;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.List;

public class CellTower {
    //0.00000663766646
    protected double phoneGain = 2.2; //dbi
    protected double towerGain = 11.3; //dbi
    protected double power = 50.23515955366738; //watts
    protected List<Double> powers = new ArrayList<Double>();
    protected List<Double> distances = new ArrayList<Double>();
    protected CellInfoGsm cellInfo;
    protected WGS84 coords;
    protected boolean accurate = true;


//    protected double latitude;
//    protected double longitude;

    public CellTower(CellInfoGsm cellInfo, List<String[]> res, List<String[]> powers, Calendar cal){
        this.cellInfo = cellInfo;

//        Log.d("TowerConstructorCsvOut ", res.get(1)[0]);
//        Log.d("TowerConstructorCsvOut ", "FFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        double latitude;
        double longitude;
        for(String[] row : res){
            if(row[0].contains("GSM") && row[1].contains(String.valueOf(this.cellInfo.getCellIdentity().getMcc())) && row[2].contains(String.valueOf(this.cellInfo.getCellIdentity().getMnc()))
            && row[3].contains(String.valueOf(this.cellInfo.getCellIdentity().getLac())) && row[4].contains(String.valueOf(this.cellInfo.getCellIdentity().getCid()))){
                    latitude = Double.valueOf(row[7]);
                    longitude = Double.valueOf(row[6]);
                    this.coords = new WGS84(latitude, longitude);
                    Log.d("Tower Constructor Csv: ", "cellTower: Found");
            }

        }
        for(String[] row : powers){
            if(row[0].contains(String.valueOf(this.cellInfo.getCellIdentity().getCid()))){
                for(String cell : row){
                    if(cell.contains("0")){
                        this.powers.add(Double.valueOf(cell));
                    }
                }
                Log.d("___________________________________________________________________", powers.get(7)[1]);
                if(this.powers.size()!=0){
                    this.powers.remove(0);
                }else{
                    for(String cell : powers.get(this.powers.size() - 1)){
                        if(cell.contains("0")){
                            this.powers.add(Double.valueOf(cell));//default vrednosti kada nemamo merenja za datu baznu stanicu
                        }
                    }
                    if(this.powers.size()!=0){
                        this.powers.remove(0);
                        this.accurate = false;
                    }
                }


                Log.d("fFFFFFFFFF", String.valueOf(this.power));
                Log.d("fFFFFFFFFF", String.valueOf(this.cellInfo.getCellIdentity().getCid()));
            }

        }



    }
    public double getLatitude(){
        return this.coords.getLatitude();
    }
    public double getLongitude(){
        return this.coords.getLongitude();
    }
    public WGS84 getCoords(){
        return coords;
    }
    public double distanceCalculator(double modifiedPower){

        //Funkcija ce racunati razdaljinu telefona od ovog tornja (FSPL formula, iz Friis equation)

        double squared = (modifiedPower * this.phoneGain * this.towerGain * 0.11095742873)/(Math.pow(4, 2) * Math.pow(Math.PI, 2) * CellTower.dbmToWattConverter(this.cellInfo.getCellSignalStrength().getDbm()));
        this.distances.add(Math.pow(squared, 1/2.6));
        return this.distances.get(this.distances.size()-1);
    }
    protected static double dbmToWattConverter(double dbm){
        Log.d("CONVERTER", String.valueOf(dbm) + "->" + String.valueOf(Math.pow(10.0, ((dbm - 30.0)/10.0))) );
        return Math.pow(10.0, ((dbm - 30.0)/10.0));
    }

    protected List<Double> distanceCalcWrapper(){
        for(Double modifiedPower : this.powers){
            this.distanceCalculator(modifiedPower);
        }
        Log.d("TAG", "distanceCalcWrapper: " + this.distances);
        return this.distances;
    }
}
