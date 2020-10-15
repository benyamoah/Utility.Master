package com.keren.utility;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location LastLocation;
    LocationRequest locationRequest;

    private Button Logout;
    private Button SettingsButton;
    private Button CallWorkerButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference CustomerDatabaseRef;
    private LatLng CustomerWorkLocation;

    private DatabaseReference WorkerAvailableRef;
    private DatabaseReference WorkerLocationRef;
    private DatabaseReference WorkersRef;
    private int radius = 1;

    private Boolean workerFound = false, requestType = false;
    private String workerFoundID;
    private String customerID;
    Marker WorkerMarker, WorkMarker;
    GeoQuery geoQuery;

    private ValueEventListener WorkerLocationRefListener;


    private TextView txtName, txtPhone, txtWorkName;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);



        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        WorkerAvailableRef = FirebaseDatabase.getInstance().getReference().child("Workers Available");
        WorkerLocationRef = FirebaseDatabase.getInstance().getReference().child("Workers Working");


        Logout = (Button) findViewById(R.id.logout_customer_btn);
        SettingsButton = (Button) findViewById(R.id.settings_customer_btn);
        CallWorkerButton =  (Button) findViewById(R.id.call_a_worker_button);

        txtName = findViewById(R.id.name_worker);
        txtPhone = findViewById(R.id.phone_worker);
        txtWorkName = findViewById(R.id.work_name_worker);
        profilePic = findViewById(R.id.profile_image_worker);
        relativeLayout = findViewById(R.id.rel1);




        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        SettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                mAuth.signOut();
                LogOutUser();
            }
        });


        CallWorkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (requestType)
                {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    WorkerLocationRef.removeEventListener(WorkerLocationRefListener);

                    if (workerFound != null)
                    {
                        WorkersRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Workers").child(workerFoundID).child("CustomerWorkID");
//                        WorkersRef.setValue(true);
                        WorkersRef.removeValue();
                        workerFoundID = null;
                    }

                    workerFound = false;
                    radius = 1;

                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.removeLocation(customerID);

                    if (WorkMarker != null)
                    {
                        WorkMarker.remove();
                    }
                    if (WorkerMarker != null)
                    {
                        WorkerMarker.remove();
                    }

                    CallWorkerButton.setText("Call a Worker");
                    relativeLayout.setVisibility(View.GONE);
                }
                else
                {
                    requestType = true;
//
//                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
//                    geoFire.setLocation(customerID, new GeoLocation(LastLocation.getLatitude(), LastLocation.getLongitude()));

                    CustomerWorkLocation = new LatLng(LastLocation.getLatitude(), LastLocation.getLongitude());
                    WorkMarker = mMap.addMarker(new MarkerOptions().position(CustomerWorkLocation).title("My Work Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    CallWorkerButton.setText("Getting your Worker...");
                    getClosetWorker();
                }
            }
        });
    }




    private void getClosetWorker()
    {
        GeoFire geoFire = new GeoFire(WorkerAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerWorkLocation.latitude, CustomerWorkLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location)
            {
                //anytime the driver is called this method will be called
                //key=driverID and the location
                if(!workerFound && requestType)
                {
                    workerFound = true;
                    workerFoundID = key;


                    //we tell driver which customer he is going to have
//
                    WorkersRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Workers").child(workerFoundID);
                    HashMap workersMap =  new HashMap();
                    workersMap.put("CustomerWorkID", customerID);
                    WorkersRef.updateChildren(workersMap);
//
//                    //Show driver location on customerMapActivity
                    GettingWorkerLocation();
                    CallWorkerButton.setText("Looking for Worker Location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady()
            {
                if(!workerFound)
                {
                    radius = radius + 1;
                    getClosetWorker();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError databaseError) {

            }
        });
    }





    //and then we get to the driver location - to tell customer where is the driver
    private void GettingWorkerLocation()
    {
//        WorkerLocationRef.child(workerFoundID).child("l");
        WorkerLocationRefListener = WorkerLocationRef.child(workerFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists()  &&  requestType)
                        {
                            List<Object> workerLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            CallWorkerButton.setText("Worker Found");


                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedWorkerInformation();


                            if(workerLocationMap.get(0) != null)
                            {
                                LocationLat = Double.parseDouble(workerLocationMap.get(0).toString());
                            }
                            if(workerLocationMap.get(1) != null)
                            {
                                LocationLng = Double.parseDouble(workerLocationMap.get(1).toString());
                            }

                            //adding marker - to pointing where driver is - using this lat lng
                            LatLng WorkerLatLng = new LatLng(LocationLat, LocationLng);
                            if(WorkerMarker != null)
                            {
                                WorkerMarker.remove();
                            }


                            Location location1 = new Location("");
                            location1.setLatitude(CustomerWorkLocation.latitude);
                            location1.setLongitude(CustomerWorkLocation.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(WorkerLatLng.latitude);
                            location2.setLongitude(WorkerLatLng.longitude);

                            float Distance = location1.distanceTo(location2);
                            CallWorkerButton.setText("Worker Found:" + String.valueOf(Distance));

                            if (Distance < 90)
                            {
                                CallWorkerButton.setText("Worker's Reached");
                            }
                            else
                            {
                                CallWorkerButton.setText("Worker Found:" + String.valueOf(Distance));
                            }

                            WorkerMarker = mMap.addMarker(new MarkerOptions().position(WorkerLatLng).title("your worker is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.work)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }




    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        // now let set user location enable
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //

            return;
        }
        //it will handle the refreshment of the location
        //if we dont call it we will get location only once
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {

    }

    @Override
    public void onLocationChanged(Location location)
    {
        //getting the updated location
        LastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
    }


    //create this method -- for useing apis
    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }


//    @Override
//    protected void onStop()
//    {
//        super.onStop();
//    }


    public void LogOutUser()
    {
        Intent startPageIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }



    private void getAssignedWorkerInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Workers").child(workerFoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String work = dataSnapshot.child("work").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtWorkName.setText(work);

                    if (dataSnapshot.hasChild("image"))
                    {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
