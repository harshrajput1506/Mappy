package `in`.hypernation.mappy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.common.location.LocationProvider
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationSourceOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.data.OverviewViewportStateOptions
import com.mapbox.maps.plugin.viewport.state.FollowPuckViewportState
import com.mapbox.maps.plugin.viewport.state.OverviewViewportState
import com.mapbox.maps.plugin.viewport.viewport
import com.mapbox.maps.viewannotation.annotatedLayerFeature
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

class MainActivity : AppCompatActivity(), OnMoveListener, OnMapLongClickListener{
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mapView : MapView
    private lateinit var locationFab : FloatingActionButton
    private lateinit var styleFab : FloatingActionButton
    private lateinit var dialog: AlertDialog
    private lateinit var markerDialog : AlertDialog

    private var checkedStyle : Int = 0

    private companion object {
        const val TAG = "MainActivity"
    }

    private var permissionsListener: PermissionsListener = object : PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: List<String>) {

        }

        override fun onPermissionResult(granted: Boolean) {
            if (granted) {
                showMapBox()
                // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location
            } else {
                // User denied the permission
                Log.d(TAG, "onPermissionResult:  User denied the permission")
                Toast.makeText(applicationContext, "User denied the permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Get the user's location as coordinates
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(it)


    }


        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mapView = findViewById(R.id.mapView)
        locationFab = findViewById(R.id.location_fab)
        styleFab = findViewById(R.id.style_fab)


        if(PermissionsManager.areLocationPermissionsGranted(this)){
            // To Show Device loacation on the map
            showMapBox()
        } else {
            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager.requestLocationPermissions(this)
        }

        locationFab.setOnClickListener {
            goToCurrentLocation()
        }

        styleFab.setOnClickListener{
            showStyleDialog()
        }

    }

    private fun goToCurrentLocation(){

        val viewportPlugin = mapView.viewport
        // transition to followPuckViewportState with default transition
        val followPuckViewportState: FollowPuckViewportState = viewportPlugin.makeFollowPuckViewportState(
            FollowPuckViewportStateOptions.Builder()
                .bearing(FollowPuckViewportStateBearing.Constant(0.0))
                .zoom(16.0)
                .padding(EdgeInsets(200.0 * resources.displayMetrics.density, 0.0, 0.0, 0.0))
                .build()
        )
        viewportPlugin.transitionTo(followPuckViewportState) { success ->
            // the transition has been completed with a flag indicating whether the transition succeeded
            locationFab.setImageResource(R.drawable.my_location)
            locationFab.isClickable = false
        }

        mapView.gestures.scrollEnabled = true
        mapView.gestures.addOnMoveListener(this)
    }
    fun showMapBox(){
        mapView.mapboxMap.loadStyle(
            Style.MAPBOX_STREETS
        )
        mapView.location.locationPuck = createDefault2DPuck(true)
        /*mapView.location.locationPuck = LocationPuck2D(
            // ImageHolder also accepts Bitmap
            bearingImage = ImageHolder.from(R.drawable.location_on),
            scaleExpression = interpolate {
                linear()
                zoom()
                stop {
                    literal(0.0)
                    literal(0.6)
                }
                stop {
                    literal(20.0)
                    literal(1.0)
                }
            }.toJson()
        )*/
        goToCurrentLocation()
        // Add Click Listeners to the map
        mapView.gestures.addOnMapLongClickListener(this)
        Toast
            .makeText(this@MainActivity, "Press long click to add a marker", Toast.LENGTH_LONG)
            .show()


    }

    private fun addAnnotationToMap(point : Point, label : String?) {
        // Create an instance of the Annotation API and get the PointAnnotationManage
        var pointAnnotation : PointAnnotation? = null
        bitmapFromDrawableRes(
            this@MainActivity,
            R.drawable.red_marker
        )?.let {
            val pointAnnotationManager =  mapView.annotations.createPointAnnotationManager().apply {
                addClickListener(
                    OnPointAnnotationClickListener {
                        // define camera position
                        val cameraPosition = CameraOptions.Builder()
                            .zoom(16.0)
                            .center(it.point)
                            .build()
                        // set camera position
                        mapView.mapboxMap.flyTo(cameraPosition)
                        locationChanged()
                        true
                    }
                )

            }
           // Set options for the resulting symbol layer.
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                // Define a geographic coordinate.
                .withPoint(point)
                // Specify the bitmap you assigned to the point annotation
                // The bitmap will be added to map style automatically.
                .withIconImage(it)
            // Add the resulting pointAnnotation to the map.
            pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

        }
        val viewAnnotationManager = mapView.viewAnnotationManager

        label?.let {
            // add view annotation attached to the polyline
            val viewAnnotation = viewAnnotationManager.addViewAnnotation(
                resId = R.layout.layout_annotation,
                options = viewAnnotationOptions {
                    geometry(point)
                    annotationAnchor {
                        anchor(ViewAnnotationAnchor.BOTTOM)
                        offsetY((pointAnnotation?.iconImageBitmap?.height!!.toDouble()))
                    }
                }
            )

            val annotationLabel : TextView = viewAnnotation.findViewById(R.id.annotation_lbl)
            val annotationClose : ImageView = viewAnnotation.findViewById(R.id.annotation_close)
            annotationLabel.text = it
            annotationClose.setOnClickListener{
                viewAnnotationManager.removeViewAnnotation(viewAnnotation)
            }
        }

    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            // copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun locationChanged(){
        locationFab.isClickable = true
        locationFab.setImageResource(R.drawable.location_searching)
    }

    private fun showStyleDialog(){
        var selectedStyle : Int = checkedStyle
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setTitle("Select Your Map Style")
            .setPositiveButton("Apply") { dialog, which ->
                // Do something.
                when(selectedStyle){
                    0 -> mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)
                    1 -> mapView.mapboxMap.loadStyle(Style.SATELLITE)
                    2 -> mapView.mapboxMap.loadStyle(Style.SATELLITE_STREETS)
                    else -> mapView.mapboxMap.loadStyle(Style.STANDARD)
                }
                checkedStyle = selectedStyle
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Do something else.
            }
            .setSingleChoiceItems(
                arrayOf("Street", "Satellite", "Hybrid"), checkedStyle
            ) { dialog, which ->
                selectedStyle = which
            }

        dialog = builder.create()
        dialog.show()
    }

    private fun createMarkerDialog(point : Point){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater;
        val dialogView = inflater.inflate(R.layout.layout_marker_dialog, null)
        builder
            .setTitle("Add a Marker")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, which ->
                // Do something.
                val editText : EditText = dialogView.findViewById(R.id.marker_editlbl)
                val label = editText.text.toString()
                addAnnotationToMap(point, if(label == "") null else label)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Do something else.
            }

        markerDialog = builder.create()
        markerDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode,
            permissions as Array<String>, grantResults)
    }

    override fun onMove(detector: MoveGestureDetector): Boolean {
        TODO("Not yet implemented")
    }

    override fun onMoveBegin(detector: MoveGestureDetector) {
        locationChanged()
        mapView.gestures.removeOnMoveListener(this)
    }

    override fun onMoveEnd(detector: MoveGestureDetector) {
        TODO("Not yet implemented")
    }

    override fun onMapLongClick(point: Point): Boolean {
        createMarkerDialog(point)
        return true
    }
}
