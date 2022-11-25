package com.example.uberclone.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.uberclone.R
import com.example.uberclone.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class HomeFragment : Fragment(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mapFragment: SupportMapFragment

    //location
    private lateinit var locationRequest:com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var  fusedLocationProviderClient: FusedLocationProviderClient                       // Helps to get the User Application.....

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
      val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
      val root=inflater.inflate(R.layout.fragment_home,container,false)

      init()

      mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
      mapFragment.getMapAsync(this)

    return root
  }

    @SuppressLint("MissingPermission")
    private fun init() {

        locationRequest=com.google.android.gms.location.LocationRequest()
        locationRequest.setPriority(LocationRequest.QUALITY_HIGH_ACCURACY)           //A quality constant indicating a location provider may choose to satisfy this request by providing very accurate locations at the expense of potentially increased power usage.
        locationRequest.setFastestInterval(3000)                                    //Explicitly set the fastest interval for location updates, in milliseconds
        locationRequest.interval=5000                                               //Set the desired interval for active location updates, in milliseconds.
        locationRequest.setSmallestDisplacement(10f)

        locationCallback =object:LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                Toast.makeText(requireContext(),"onLocation Result",Toast.LENGTH_SHORT).show()
                val newPos=LatLng(locationResult.lastLocation!!.latitude,locationResult.lastLocation!!.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

            }
        }

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap!!

       // mMap.uiSettings.isZoomControlsEnabled=true

        //requestPermission
        Dexter.withContext(context)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object:PermissionListener{
                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    //Enable the button
                    mMap.isMyLocationEnabled=true;
                    mMap.uiSettings.isMyLocationButtonEnabled=true
                    mMap.setOnMyLocationClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener{e->
                                Toast.makeText(context,e.message,Toast.LENGTH_LONG).show()
                            }.addOnSuccessListener {
                                Toast.makeText(requireContext(),"onSuccess Listener",Toast.LENGTH_SHORT).show()
                                val userlatlong=LatLng(it.latitude,it.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userlatlong,18f))

                            }
                        true
                    }


                    //Setting up the button and vies
                    val locationButton=(mapFragment.requireView()!!
                        .findViewById<View>("1".toInt())!!
                        .parent!! as View).findViewById<View>("2".toInt())
                    val params=locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin=50
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context,"Permission "+p0!!.permissionName+" was denied",Toast.LENGTH_LONG).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

        try {
            val success=googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.uber_maps_style))
            if(!success){
                Toast.makeText(context,"Something Wrong Occured",Toast.LENGTH_LONG).show()
            }
        }catch (e: Resources.NotFoundException){
            Toast.makeText(context,"Something Wrong Occured  ${e}",Toast.LENGTH_LONG).show()
        }
//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}