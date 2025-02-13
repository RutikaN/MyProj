package com.example.myproj

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx. appcompat. widget. Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchBar: AutoCompleteTextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private var lastKnownLocation: LatLng? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDPYy_XN9bX24PDT-qEELtktFTDSfARa38")
        }
        placesClient = Places.createClient(this)

        // Initialize FusedLocationProviderClient to get current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Search Bar
        searchBar = findViewById(R.id.search_bar)

        // Setup Google Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        // searchBar = findViewById(R.id.search_bar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_sort_by_size)

        // Handle item clicks in the navigation drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            var selectedFragment: Fragment? = null
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Handle Home click
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                }

                R.id.nav_about -> {
                    val intent = Intent(this, AboutUsActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_notification -> {
                    val intent = Intent(this, NotificationsActivity::class.java)
                    startActivity(intent)
                }
            }


            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }





        // Setup listener for search bar input (pressing enter triggers search)
        searchBar.setOnEditorActionListener { v, actionId, event ->
            val query = searchBar.text.toString()
            searchLocation(query)
            true
        }
        // Manually add menu items to the NavigationView
        navView.menu.add(0, R.id.nav_home, 0, "Home")
        navView.menu.add(0, R.id.nav_notification, 1, "Notification")
        navView.menu.add(0, R.id.nav_about, 2, "About us")
    }



    // Called when the map is ready to be used
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = false


        // Enable traffic data
        mMap.isTrafficEnabled = true



        // Request the current location of the user
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f)) // Zoom to current location
                    mMap.addMarker(MarkerOptions().position(currentLocation).title("You are here"))
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        mMap.setOnCameraIdleListener {
            val cameraPosition = mMap.cameraPosition
        }
    }

    // Handle search query and move map
    private fun searchLocation(query: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Initialize Autocomplete Session Token
            val token = AutocompleteSessionToken.newInstance()

            // Create a request for autocomplete predictions
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val prediction: AutocompletePrediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId
                    getPlaceDetails(placeId)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    // Function to fetch place details by place ID
    private fun getPlaceDetails(placeId: String) {
        val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)

        val placeRequest = com.google.android.libraries.places.api.net.FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(placeRequest)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng

                if (latLng != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    mMap.addMarker(MarkerOptions().position(latLng).title(place.name))
                    //searchBar.setText("") // Clear search bar after selecting
                } else {
                    Toast.makeText(this, "Place not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val query = searchBar.text.toString()
                searchLocation(query)
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Zoom In Function
    fun zoomIn() {
        val currentZoom = mMap.cameraPosition.zoom
        mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom + 1))
    }

    // Zoom Out Function
    fun zoomOut() {
        val currentZoom = mMap.cameraPosition.zoom
        mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom - 1))
    }

    // Handling the drawer item clicks and opening/closing
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
