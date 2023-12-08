package com.example.projekatbase;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    protected TelephonyManager tel;
    protected List<CellInfoGsm> cellsLte = new ArrayList<CellInfoGsm>();
    protected List<CellTower> towers = new ArrayList<CellTower>();
    protected List<Double[]> points = new ArrayList<Double[]>();
    protected Calendar cal = Calendar.getInstance();
    protected GoogleMap map;
    protected SupportMapFragment mapFragment;
    protected WGS84 avgCoords;
    protected Double[] sumAvg = {0.0, 0.0};
    protected static ArrayList<UTM> averages;
    protected int numOfAvgs = 1;

    private InputStream inputStream;
    private InputStream powerInput;
    private CsvFile csvFile;
    private CsvPower csvPower;
    private List<String[]> res;
    private List<String[]> powers;

    private PhoneStateListen phoneSTL = new PhoneStateListen(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new ButtonListener(this));
        this.locationSetup();

    }

    public void locationSetup() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE};
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, perms, 0);
        }
        averages = new ArrayList<UTM>();
        inputStream = getResources().openRawResource(R.raw.cells);
        powerInput = getResources().openRawResource(R.raw.cellwatts);

        csvFile = new CsvFile(inputStream);
        csvPower = new CsvPower(powerInput);

        res = csvFile.read();
        powers = csvPower.read();


        tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);



        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        new LocationTask().execute(phoneSTL);

        this.locationUpdate();

    }
    public void locationUpdate(){

        this.cellsLte.clear();
        this.towers.clear();


//        tel.listen(phoneSTL, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);



        List<CellInfo> cellsTemp = new ArrayList<CellInfo>();

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
            cellsTemp = this.tel.getAllCellInfo();
        }else{
            return;
        }

        CellTower ctTemp;

        for (CellInfo ci : cellsTemp) {

            if (ci instanceof CellInfoGsm) {
                ctTemp = new CellTower((CellInfoGsm) ci, res, powers, cal);
                if(ctTemp.getCoords() != null){
                    this.towers.add(new CellTower((CellInfoGsm) ci, res, powers, cal));
                }
                this.cellsLte.add((CellInfoGsm) ci);

            }
        }
        List<CellTower> ctsToRemove = new ArrayList<CellTower>();
        for(int i = 0; i < this.towers.size(); i++){
            if(this.towers.get(i).getLongitude() != 0.0 && this.towers.get(i).getLatitude() != 0.0){
                this.towers.get(i).distanceCalcWrapper();
            }
        }
        for(int i = 0; i < this.towers.size(); i++){
            if(this.towers.get(i).distances.size() == 0){
                this.towers.remove(i);
                Log.d("TAG", "I JUST REMOVED A TOWER: ");
            }
        }
//        for(CellTower ct : this.towers){
//            if(ct.getLongitude() != 0.0 && ct.getLatitude() != 0.0){
//                ct.distanceCalcWrapper();
//            }
//            Log.d("LAT LONG", String.valueOf(ct.getLatitude()) + String.valueOf(ct.getLongitude()));
//        }

//        for(CellTower temp : this.towers){
//            Log.d("SORTING", temp.cellInfo.getCellIdentity().getCid() + " " + temp.distances.toString());
//            if(temp.distances.size() == 0){
//                ctsToRemove.add(temp);
//                Log.d("REMOOOVEEEEEED", String.valueOf(temp.cellInfo.getCellIdentity().getCid()));
//            }
//        }
//        this.towers.removeAll(ctsToRemove);

        if(this.towers.size() >= 2){
            sortByAvgDistance(this.towers);
            this.intersectCallerWrapper();
        }else{
            Log.d("OOFFF", "locationSetup: PROBLEM");

            this.locationUpdate();
            return;
        }




        Double[] avg = averageCalc(this.points);

        UTM avgMeters = new UTM(34, 'T', avg[0], avg[1]);

        this.numOfAvgs +=1;
        if(avgMeters.getEasting()!=0.0 && !Double.isNaN(avgMeters.getEasting()) &&
                avgMeters.getNorthing()!=0.0 && !Double.isNaN(avgMeters.getNorthing())) {

            this.sumAvg[0] += avgMeters.getEasting();
            this.sumAvg[1] += avgMeters.getNorthing();

            UTM pinLocation = new UTM(34, 'T', this.sumAvg[0] / (this.numOfAvgs), this.sumAvg[1] / (this.numOfAvgs));

            this.avgCoords = new WGS84(pinLocation);
            if (this.map != null) {
                LatLng avgC = new LatLng(this.avgCoords.getLatitude(), this.avgCoords.getLongitude());
                this.map.clear();
                this.map.addMarker(new MarkerOptions().position(avgC));

            }
        }
        new LocationTask().execute(phoneSTL);
    }

    public void intersectCallerWrapper(){
        for(int i = 0; i < this.towers.size(); i++){
            for(int j = 1; j < this.towers.size(); j++){
                if(i != j){
                    if(this.intersectCaller(this.towers.get(i), this.towers.get(j)) == true){
                        Log.d("TO JE SASVIM UREDU KOLEGA", "intersectCallerWrapper: ");
                        return;
                    }
                }
            }
        }
        return;
    }

    private static void sortByAvgDistance(List<CellTower> list) {
        sortByAvgDistance(list, 0, list.size() - 1);

        ArrayList<CellTower> ctsToShuffle = new ArrayList<CellTower>();
        for(CellTower ct : list){
            if(ct.accurate == false){
                ctsToShuffle.add(ct);
                Log.d("SHUFFLED CT", String.valueOf(ct.cellInfo.getCellIdentity().getCid()));
            }
        }
        Log.d("SSSSHUUUUUUFFFFFFLEEEEEEE", "sortByAvgDistance: ");
        list.removeAll(ctsToShuffle);
        list.addAll(ctsToShuffle);
        // dodajemo sve stanice koje koriste neprecizne default vrednosti na kraj sa idejom da se one koriste samo ako nema preciznih sacuvanih
    }

    private static void sortByAvgDistance(List<CellTower> list, int from, int to) {
        if (from < to) {
            int pivot = from;
            int left = from + 1;
            int right = to;
            Double pivotValue = list.get(pivot).distances.get(0);
            while (left <= right) {

                while (left <= to && pivotValue >= list.get(left).distances.get(0)) {
                    left++;
                }

                while (right > from && pivotValue < list.get(right).distances.get(0)) {
                    right--;
                }
                if (left < right) {
                    Collections.swap(list, left, right);
                }
            }
            Collections.swap(list, pivot, left - 1);
            sortByAvgDistance(list, from, right - 1);
            sortByAvgDistance(list, right + 1, to);
        }
    }
    public double[][] intersect(CellTower ct1, CellTower ct2, double d1, double d2){
        UTM utm1 = new UTM(ct1.getCoords());
        UTM utm2 = new UTM(ct2.getCoords());
        double p1 = utm1.getEasting();
        double p2 = utm2.getEasting();
        double q1 = utm1.getNorthing();
        double q2 = utm2.getNorthing();
        double r1 = d1;
        double r2 = d2;

        //konstante za izvedenu formulu za presek dva kruga
        double f = -(q1 - q2)/(p1 - p2); // a u originalnom izvodu
        double t = ((r2 * r2) - (r1 * r1) + (p1 * p1) - (p2 * p2) + (q1 * q1) - (q2 * q2))/(2 *(p1 - p2));

        double a = (f * f) + 1;
        double b = (2 * t * f) - (2 * p1 * f) - (2 * q1);
        double c = (t * t) - (2 * p1 * t) + (p1 * p1) + (q1 * q1) - (r1 * r1);

        double y1 = (-b + Math.sqrt((b * b) - (4 * a * c)))/(2 * a);
        double x1 = t + (f * y1);

        double x2 = (-b - Math.sqrt((b * b) - (4 * a * c)))/(2 * a);
        double y2 =  t + (f * y1);
//        Log.d("FROM INTERSECT b * b", String.valueOf(b * b));
//        Log.d("FROM INTERSECT 4ac", String.valueOf(4 * a * c));
//        Log.d("FROM INTERSECT", String.valueOf(x2) + " " + String.valueOf(y2));
//        Log.d("FROM INTERSECT P1", String.valueOf(p1));
//        Log.d("FROM INTERSECT Q1", String.valueOf(q1));
//        Log.d("FROM INTERSECT P2 Q2", String.valueOf(p2) + " " + String.valueOf(q2));
        Log.d("FROM INTERSECT R1 R2", String.valueOf(r1) + " " +String.valueOf(r2));
//        Log.d("LETTERS", utm1.getLetter() + " " +utm2.getLetter());

        Double [] tmp = {x1, y1};
        Log.d("TAG", "intersect points: " + tmp[0] + " " + tmp[1]);
        if(!Double.isNaN(tmp[0])){
            this.points.add(tmp);
        }

        Double [] tmp2 = {x2, y2};
        if(!Double.isNaN(tmp[1])){
            this.points.add(tmp2);
        }



        return new double[][] {{x1, y1}, {x2, y2}};
    }
    public boolean intersectCaller(CellTower ct1, CellTower ct2){
        for(int i = 0; i < ct1.distances.size(); i++){
            for(int j = 0; j < ct2.distances.size(); j++){
                Log.d("TAG", "intersectCaller Distances: " + ct1.distances.get(i) + " " + ct2.distances.get(j));
                this.intersect(ct1, ct2, ct1.distances.get(i), ct2.distances.get(j));
            }
        }
        this.minimumDistance(this.towers.get(2));
        for(Double[] a : this.points){
           if(!Double.isNaN(a[0]) && !Double.isNaN(a[1])){
               return true;
           }
        }
        return false;

//        Log.d("AVG", );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private static Double[] averageCalc(List<Double[]> d){
        Double avg1 = new Double(0);
        Double avg2 = new Double(0);
        int i = 0;
        for(Double[] da : d){
            if(!Double.isNaN(da[0]) && !Double.isNaN(da[1]) && da[0] < 500000){
                avg1 += da[0];
                avg2 += da[1];
            }else{
                i++;
            }
        }
        return new Double[]{avg1/(d.size() - i), avg2/(d.size() - i)};
    }
    private void minimumDistance(CellTower ct3){
        Double minDistance = 9999.0;
        UTM utm = new UTM(ct3.getCoords());
        Double[] utmDouble = {utm.getEasting(), utm.getNorthing()};
        List<Double[]> pointsToRemove = new ArrayList<Double[]>();
        for(int i = 1; i < this.points.size(); i+=2){
            if(Math.abs(distanceTwoPoints(this.points.get(i), utmDouble) - ct3.distances.get(0))
            <Math.abs(distanceTwoPoints(this.points.get(i-1), utmDouble) - ct3.distances.get(0))){
                pointsToRemove.add(this.points.get(i - 1));
            }else{
                pointsToRemove.add(this.points.get(i));
            }
        }
        this.points.removeAll(pointsToRemove);
    }
    private static Double distanceTwoPoints(Double[] d1, Double[] d2){
        return (Math.sqrt(Math.pow((d1[0] - d2[0]), 2) + Math.pow((d1[1] - d2[1]), 2)));
    }
    private void filter(){

        if(this.points.size() == 0){return;}
        for(int i = 0; i < this.points.size(); i++){
            Log.d("POINTS", "POINT: " + this.points.get(i)[0] + " " + this.points.get(i)[1]);
            if(Double.isNaN(this.points.get(i)[0]) || Double.isNaN(this.points.get(i)[1])){
                this.points.remove(i);
            }
        }

        if(this.towers.size()>2){
            this.minimumDistance(this.towers.get(2));
        }
        //ne mozemo samo da radimo na d zbog ConcurrentModificationException
    }
    private static void sort(List<Double> list) {
        sort(list, 0, list.size() - 1);
    }

    private static void sort(List<Double> list, int from, int to) {
        if (from < to) {
            int pivot = from;
            int left = from + 1;
            int right = to;
            Double pivotValue = list.get(pivot);
            while (left <= right) {

                while (left <= to && pivotValue >= list.get(left)) {
                    left++;
                }

                while (right > from && pivotValue < list.get(right)) {
                    right--;
                }
                if (left < right) {
                    Collections.swap(list, left, right);
                }
            }
            Collections.swap(list, pivot, left - 1);
            sort(list, from, right - 1);
            sort(list, right + 1, to);
        }
    }
    private static Double[] medianCalc(List<Double[]> d){
        Double[] res = {0.0, 0.0};
        List <Double> x = new ArrayList<Double>();
        List <Double> y = new ArrayList<Double>();
        for(Double[] da : d){
            x.add(da[0]);
            y.add(da[1]);
        }
        sort(x);
        sort(y);
        if(x.size() % 2 == 0){
            res[0] = (x.get((x.size()/2) - 1) + x.get((x.size()/2) - 1))/2;
        }else{
            res[0] = x.get((x.size() - 1)/2);
        }
        if(y.size() % 2 == 0){
            res[1] = (y.get((y.size()/2) - 1) + y.get((y.size()/2) - 1))/2;
        }else{
            res[1] = y.get((y.size() - 1)/2);
        }
        return res;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("AAFFFFFFFFFFWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW", "onMapReady: ");
        map = googleMap;

        Double[] avg = averageCalc(this.points);
        Log.d("AVG", String.valueOf(avg[0]) + " " + String.valueOf(avg[1]));

        UTM avgMeters = new UTM(34, 'T', avg[0], avg[1]);

//        averages.add(avgMeters);

        if(avgMeters.getEasting()!=0.0 && !Double.isNaN(avgMeters.getEasting()) &&
                avgMeters.getNorthing()!=0.0 && !Double.isNaN(avgMeters.getNorthing())){

            Log.d("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDSUMavg",  this.sumAvg[0] + " " + this.sumAvg[1]);

            this.sumAvg[0] += avgMeters.getEasting();
            this.sumAvg[1] += avgMeters.getNorthing();

            UTM pinLocation = new UTM(34, 'T', this.sumAvg[0]/(this.numOfAvgs), this.sumAvg[1]/(this.numOfAvgs));
            Log.d("____________________Pinning At", this.averages.size() + " " + this.averages.size());

            this.avgCoords = new WGS84(pinLocation);
            if(!Double.isNaN(this.avgCoords.getLatitude()) && !Double.isNaN(this.avgCoords.getLongitude())){
                LatLng avgPoint = new LatLng(this.avgCoords.getLatitude(), this.avgCoords.getLongitude());
                map.addMarker(new MarkerOptions().position(avgPoint));
                map.moveCamera(CameraUpdateFactory.newLatLng(avgPoint));
                map.moveCamera(CameraUpdateFactory.zoomBy(4));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(avgPoint,15));

    //            googleMap.animateCamera(CameraUpdateFactory.zoomIn());
                }
        }
//        map.moveCamera(CameraUpdateFactory.zoomBy(50));
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        double[] tmp = new double[] {0.0, 0.0};
        savedInstanceState.putDoubleArray("sumAvg", tmp);
        savedInstanceState.putInt("numOfAvgs", 1);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        this.numOfAvgs = savedInstanceState.getInt("numOfAvgs");
        double[] tmp = (savedInstanceState.getDoubleArray("sumAvg"));
        this.sumAvg = new Double[]{tmp[0], tmp[1]};
        Log.d("I CALLED THE THING", "onRestoreInstanceState: ");
    }
    public TelephonyManager getTel(){
        return this.tel;
    }
    public PhoneStateListen getPhoneSTL(){
        return this.phoneSTL;
    }
}