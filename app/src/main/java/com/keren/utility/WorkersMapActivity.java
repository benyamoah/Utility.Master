package com.keren.utility;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class WorkersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location LastLocation;
    LocationRequest locationRequest;

    private Button LogoutWorkerBtn;
    private Button SettingsWorkerButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogOutUserStatus = false;

    //getting request customer's id
//    private String customerID = "";
    private String workerID, customerID = "";
    private DatabaseReference AssignedCustomerRef;
    private DatabaseReference AssignedCustomerWorkRef;
    Marker WorkMarker;

    private ValueEventListener AssignedCustomerWorkRefListener;

    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //notice
        setContentView(R.layout.activity_workers_map);


        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        workerID = mAuth.getCurrentUser().getUid();


        LogoutWorkerBtn = (Button) findViewById(R.id.logout_worker_btn);
        SettingsWorkerButton = (Button) findViewById(R.id.settings_worker_btn);

        txtName = findViewById(R.id.name_customer);
        txtPhone = findViewById(R.id.phone_customer);
        profilePic = findViewById(R.id.profile_image_customer);
        relativeLayout = findViewById(R.id.rel2);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(WorkersMapActivity.this);




        SettingsWorkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(WorkersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Workers");
                startActivity(intent);
            }
        });

        LogoutWorkerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
               currentLogOutUserStatus = true;
//                DisconnectWorker();
                mAuth.signOut();
                LogOutUser();
            }
        });


        getAssignedCustomersRequest();
    }



    private void getAssignedCustomersRequest()
    {
        AssignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Workers").child(workerID).child("CustomerWorkID");

        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    customerID = dataSnapshot.getValue().toString();
                    GetAssignedCustomerWorkLocation();

//                    getting assigned customer location
                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedCustomerInformation();
                }
                else
                {
                    customerID = "";

                    if (WorkMarker != null)
                    {
                        WorkMarker.remove();
                    }

                    if (AssignedCustomerWorkRefListener != null)
                    {
                        AssignedCustomerWorkRef.removeEventListener(AssignedCustomerWorkRefListener);
                    }

                    relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    private void GetAssignedCustomerWorkLocation()
    {
        AssignedCustomerWorkRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests")
                .child(customerID).child("l");

        AssignedCustomerWorkRefListener = AssignedCustomerWorkRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if(customerLocationMap.get(0) != null)
                    {
                        LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if(customerLocationMap.get(1) != null)
                    {
                        LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    LatLng WorkerLatLng = new LatLng(LocationLat, LocationLng);
                    WorkMarker = mMap.addMarker(new MarkerOptions().position(WorkerLatLng).title("Customer Work Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                }
            }
//
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // now let set user location enable
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState)
    {
        super.onCreate(savedInstanceState, persistentState);





    }

    @Override
    public void onLocationChanged(Location location)
    {
        if(getApplicationContext() != null)
        {
           //5 getting the updated location
            LastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));


            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference WorkersAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Workers Available");
            GeoFire geoFireAvailability = new GeoFire(WorkersAvailabilityRef);

            DatabaseReference WorkersWorkingRef = FirebaseDatabase.getInstance().getReference().child("Workers Working");
            GeoFire geoFireWorking = new GeoFire(WorkersWorkingRef);



//           switch (customerID)
//            {
//                case "":
//                    geoFireWorking.removeLocation(userID);
//                    geoFireAvailability.setLocation(userID,new GeoLocation(location.getLatitude(), location.getLongitude()));
//                    break;
//
//                default:
//                    geoFireAvailability.removeLocation(userID);
//                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
//                    break;
//            }
        }
    }

//paste here

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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


    @Override
    protected void onStop()
    {
        super.onStop();

        if(!currentLogOutUserStatus)
        {
//            DisconnectWorker();
        }
    }


//    private void DisconnectWorker()
//    {
//        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference WorkersAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Workers Available");
//
//        GeoFire geoFire = new GeoFire(WorkersAvailabilityRef);
//        geoFire.removeLocation(userID);
//    }


    public void LogOutUser()
    {
        Intent startPageIntent = new Intent(WorkersMapActivity.this, WelcomeActivity.class);
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }



    private void getAssignedCustomerInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(customerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);

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
