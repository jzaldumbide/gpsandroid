

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.jzaldumbide.test.gpsandroid.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val supabaseUrl = ""
    private val supabaseKey = ""

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocation() else showToast("Permiso denegado")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetLocation.setOnClickListener {
            checkPermissionsAndFetch()
        }
    }

    private fun checkPermissionsAndFetch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> {
                fetchLocation()
            }
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchLocation() {
        val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProvider.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                binding.txtLocation.text = "Lat: $lat\nLon: $lon"
                saveLocationToSupabase(lat, lon)
            } else {
                showToast("No se pudo obtener ubicación")
            }
        }
    }

    private fun saveLocationToSupabase(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.post("$supabaseUrl/rest/v1/locations") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(listOf(
                        LocationData(
                            id = UUID.randomUUID().toString(),
                            latitude = lat,
                            longitude = lon,
                            timestamp = Instant.now().toString()
                        )
                    ))
                }

                withContext(Dispatchers.Main) {
                    if (response.status.isSuccess()) {
                        showToast("Ubicación guardada correctamente")
                    } else {
                        showToast("Error: ${response.status}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

@Serializable
data class LocationData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)