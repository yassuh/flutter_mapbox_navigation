package com.eopeter.flutter_mapbox_navigation.activity

import android.content.*
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.eopeter.flutter_mapbox_navigation.FlutterMapboxNavigationPlugin
import com.eopeter.flutter_mapbox_navigation.models.MapBoxEvents
import com.eopeter.flutter_mapbox_navigation.models.MapBoxRouteProgressEvent
import com.eopeter.flutter_mapbox_navigation.models.Waypoint
import com.eopeter.flutter_mapbox_navigation.models.WaypointSet
import com.eopeter.flutter_mapbox_navigation.utilities.PluginUtilities
import com.eopeter.flutter_mapbox_navigation.utilities.PluginUtilities.Companion.sendEvent
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.*
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.map.MapViewObserver
import com.mapbox.navigation.utils.internal.ifNonNull
import eopeter.flutter_mapbox_navigation.R
import eopeter.flutter_mapbox_navigation.databinding.NavigationActivityBinding
import com.google.gson.Gson
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.dropin.navigationview.NavigationViewListener
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue

class NavigationActivity : AppCompatActivity() {

    private var finishBroadcastReceiver: BroadcastReceiver? = null
    private var addWayPointsBroadcastReceiver: BroadcastReceiver? = null
    private var points: MutableList<Waypoint> = mutableListOf()
    private var waypointSet: WaypointSet = WaypointSet()
    private var canResetRoute: Boolean = false
    private var shouldCloseActivity: Boolean = false
    private var accessToken: String? = null
    private var lastLocation: Location? = null
    private val navigationStateListener = object : NavigationViewListener() {
        override fun onFreeDrive() {
            super.onFreeDrive()
            Log.d("YASSUH_NAVIGATION","---Free Drive")
        }

        override fun onMapClicked(point: Point) {
            super.onMapClicked(point)
            Log.d("YASSUH_NAVIGATION","---Clicky")
        }
        override fun onManeuverCollapsed() {
            super.onManeuverCollapsed()
            shouldCloseActivity = true
            Log.d("YASSUH_NAVIGATION","---Maneuver")
        }

        override fun onActiveNavigation() {
            super.onActiveNavigation()
            Log.d("YASSUH_NAVIGATION","---Active Navigation")
        }

        override fun onArrival() {
            super.onArrival()

            Log.d("YASSUH_NAVIGATION","---Arrival")
        }

        override fun onAudioGuidanceStateChanged(muted: Boolean) {
            super.onAudioGuidanceStateChanged(muted)

            Log.d("YASSUH_NAVIGATION","---Audio Guidance")
        }

        override fun onCameraPaddingChanged(padding: EdgeInsets) {
            super.onCameraPaddingChanged(padding)

            Log.d("YASSUH_NAVIGATION","---Padding Changed " + padding.bottom.toString() + padding.top.toString() + padding.right.toString())
        }

        override fun onManeuverExpanded() {
            super.onManeuverExpanded()

            Log.d("YASSUH_NAVIGATION","---Maneuver Expanded")
        }

        override fun onDestinationChanged(destination: Point?) {
            super.onDestinationChanged(destination)
            Log.d("YASSUH_NAVIGATION","---Destination")
        }

        override fun onDestinationPreview() {
            super.onDestinationPreview()
            Log.d("YASSUH_NAVIGATION","---Destination Preview")
        }

        override fun onFollowingCameraMode() {
            super.onFollowingCameraMode()

            Log.d("YASSUH_NAVIGATION","---Following camera mode")
        }

        override fun onIdleCameraMode() {
            super.onIdleCameraMode()

            Log.d("YASSUH_NAVIGATION","---Idle Camera Mode")
        }

        override fun onInfoPanelCollapsed() {
            super.onInfoPanelCollapsed()

            Log.d("YASSUH_NAVIGATION","---Info panel collapsed")
        }

        override fun onInfoPanelDragging() {
            super.onInfoPanelDragging()

            Log.d("YASSUH_NAVIGATION","---Info panel dragging")
        }

        override fun onInfoPanelExpanded() {
            super.onInfoPanelExpanded()

            Log.d("YASSUH_NAVIGATION","---Info panel expanded")
        }

        override fun onInfoPanelHidden() {
            super.onInfoPanelHidden()

            Log.d("YASSUH_NAVIGATION","---Info panel hidden")
        }

        override fun onInfoPanelSettling() {
            super.onInfoPanelSettling()
            Log.d("YASSUH_NAVIGATION","---Info panel settling")
        }

        override fun onInfoPanelSlide(slideOffset: Float) {
            super.onInfoPanelSlide(slideOffset)
            if(slideOffset.toDouble() == -1.0 && shouldCloseActivity){
                finish()
            }
            Log.d("YASSUH_NAVIGATION", "---Info panel slide offset $slideOffset")
        }

        override fun onOverviewCameraMode() {
            super.onOverviewCameraMode()
            Log.d("YASSUH_NAVIGATION","---Overview camera mode")
        }

        override fun onRouteFetchCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            super.onRouteFetchCanceled(routeOptions, routerOrigin)

            Log.d("YASSUH_NAVIGATION","---Route fetch cancelled")
        }
        
        override fun onRoutePreview() {
            super.onRoutePreview()

            Log.d("YASSUH_NAVIGATION","---Route preview")
        }

        override fun onRouteFetchFailed(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            super.onRouteFetchFailed(reasons, routeOptions)
            Log.d("YASSUH_NAVIGATION","---Route Failed")
        }

        override fun onRouteFetchSuccessful(routes: List<NavigationRoute>) {
            super.onRouteFetchSuccessful(routes)
            Log.d("YASSUH_NAVIGATION","---Route Fetch Successful")
        }

        override fun onRouteFetching(requestId: Long) {
            super.onRouteFetching(requestId)

            Log.d("YASSUH_NAVIGATION","---Route Fetching")
        }

        override fun onSpeedInfoClicked(speedInfo: SpeedInfoValue?) {
            super.onSpeedInfoClicked(speedInfo)

            Log.d("YASSUH_NAVIGATION","---Speed info")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppCompat_NoActionBar)
        binding = NavigationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.addListener(navigationStateListener)
        accessToken =
            PluginUtilities.getResourceFromContext(this.applicationContext, "mapbox_access_token")

        val navigationOptions = NavigationOptions.Builder(this.applicationContext)
            .accessToken(accessToken)
            .build()

        MapboxNavigationApp
            .setup(navigationOptions)
            .attach(this)
        if (FlutterMapboxNavigationPlugin.allowsClickToSetDestination) {
            binding.navigationView.registerMapObserver(onMapLongClick)
            binding.navigationView.customizeViewOptions {
                enableMapLongClickIntercept = false
            }
        }

        MapboxNavigationApp.current()?.registerLocationObserver(locationObserver)
        MapboxNavigationApp.current()?.registerRouteProgressObserver(routeProgressObserver)
        MapboxNavigationApp.current()?.registerArrivalObserver(arrivalObserver)
        finishBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                finish()
            }
        }

        addWayPointsBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //get waypoints
                val stops = intent.getSerializableExtra("waypoints") as? MutableList<Waypoint>
                val nextIndex = 1
                if (stops != null) {
                    //append to points
                    if (points.count() >= nextIndex)
                        points.addAll(nextIndex, stops)
                    else
                        points.addAll(stops)
                }
            }
        }

        registerReceiver(
            finishBroadcastReceiver,
            IntentFilter(NavigationLauncher.KEY_STOP_NAVIGATION)
        )

        registerReceiver(
            addWayPointsBroadcastReceiver,
            IntentFilter(NavigationLauncher.KEY_ADD_WAYPOINTS)
        )

        val p = intent.getSerializableExtra("waypoints") as? MutableList<Waypoint>
        if (p != null) points = p

        // TODO set the style Uri
        var styleUrl = FlutterMapboxNavigationPlugin.mapStyleUrlDay
        if (styleUrl == null) styleUrl = Style.MAPBOX_STREETS
        // set map style
        binding.navigationView.customizeViewStyles {}
        points.map { waypointSet.add(it) }
        requestRoutes(waypointSet)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (FlutterMapboxNavigationPlugin.allowsClickToSetDestination) {
            binding.navigationView.unregisterMapObserver(onMapLongClick)
        }
        MapboxNavigationApp.current()?.unregisterLocationObserver(locationObserver)
        MapboxNavigationApp.current()?.unregisterRouteProgressObserver(routeProgressObserver)
        MapboxNavigationApp.current()?.unregisterArrivalObserver(arrivalObserver)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {

        super.onStop()
    }

    private fun requestRoutes(waypointSet: WaypointSet) {
        sendEvent(MapBoxEvents.ROUTE_BUILDING)
        MapboxNavigationApp.current()!!.requestRoutes(
            routeOptions = RouteOptions
                .builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(waypointSet.coordinatesList())
                .waypointIndicesList(waypointSet.waypointsIndices())
                .waypointNamesList(waypointSet.waypointsNames())
                .language(FlutterMapboxNavigationPlugin.navigationLanguage)
                .alternatives(FlutterMapboxNavigationPlugin.showAlternateRoutes)
                .build(),
            callback = object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {

                    sendEvent(MapBoxEvents.ROUTE_BUILD_CANCELLED)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED)
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    sendEvent(MapBoxEvents.ROUTE_BUILT, Gson().toJson(routes))
                    if (routes.isEmpty()) {
                        sendEvent(MapBoxEvents.ROUTE_BUILD_NO_ROUTES_FOUND)
                        return
                    }
                    binding.navigationView.api.routeReplayEnabled(FlutterMapboxNavigationPlugin.simulateRoute)
                    binding.navigationView.api.startActiveGuidance(routes)
                }
            }
        )
    }


    //MultiWaypoint Navigation
    private fun addWaypoint(destination: Point, name: String?) {
        val originLocation = lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

        // we always start a route from the current location
        if (addedWaypoints.isEmpty) {
            addedWaypoints.add(Waypoint(originPoint))
        }

        if (!name.isNullOrBlank()) {
            // When you add named waypoints, the string you use here inside "" would be shown in `Maneuver` and played in `Voice` instructions.
            // In this example waypoint names will be visible in the logcat.
            addedWaypoints.add(Waypoint(name, destination))
        } else {
            // When you add silent waypoints, make sure it is followed by a regular or named waypoint, otherwise silent waypoint is treated as a regular waypoint
            addedWaypoints.add(Waypoint(destination, true))
        }

        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features
        MapboxNavigationApp.current()!!.requestRoutes(
            routeOptions = RouteOptions
                .builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(addedWaypoints.coordinatesList())
                .waypointIndicesList(addedWaypoints.waypointsIndices())
                .waypointNamesList(addedWaypoints.waypointsNames())
                .alternatives(true)
                .build(),
            callback = object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    sendEvent(MapBoxEvents.ROUTE_BUILT, Gson().toJson(routes))
                    binding.navigationView.api.routeReplayEnabled(true)
                    binding.navigationView.api.startActiveGuidance(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED)
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    sendEvent(MapBoxEvents.ROUTE_BUILD_CANCELLED)
                }

            }
        )
    }

    // Resets the current route
    private fun resetCurrentRoute() {
//        if (mapboxNavigation.getRoutes().isNotEmpty()) {
//            mapboxNavigation.setRoutes(emptyList()) // reset route
//            addedWaypoints.clear() // reset stored waypoints
//        }
    }

    private fun setRouteAndStartNavigation(routes: List<DirectionsRoute>) {
        // set routes, where the first route in the list is the primary route that
        // will be used for active guidance
        //mapboxNavigation.setRoutes(routes)
    }

    private fun clearRouteAndStopNavigation() {
        // clear
        //mapboxNavigation.setRoutes(listOf())
    }


    /**
     * Helper class that keeps added waypoints and transforms them to the [RouteOptions] params.
     */
    private val addedWaypoints = WaypointSet()


    /**
     * Bindings to the Navigation Activity.
     */
    private lateinit var binding: NavigationActivityBinding// MapboxActivityTurnByTurnExperienceBinding


    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        //Notify the client
        val progressEvent = MapBoxRouteProgressEvent(routeProgress)
        FlutterMapboxNavigationPlugin.distanceRemaining = routeProgress.distanceRemaining
        FlutterMapboxNavigationPlugin.durationRemaining = routeProgress.durationRemaining

        sendEvent(progressEvent)
    }

    private val arrivalObserver: ArrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            sendEvent(MapBoxEvents.ON_ARRIVAL)
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {

        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {

        }
    }
    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            lastLocation = locationMatcherResult.enhancedLocation
        }

        override fun onNewRawLocation(rawLocation: Location) {
            // no impl
        }

    }

    /**
     * Notifies with attach and detach events on [MapView]
     */
    private val onMapLongClick = object : MapViewObserver(), OnMapLongClickListener {

        override fun onAttached(mapView: MapView) {
            mapView.gestures.addOnMapLongClickListener(this)
        }

        override fun onDetached(mapView: MapView) {
            mapView.gestures.removeOnMapLongClickListener(this)
        }

        override fun onMapLongClick(point: Point): Boolean {
            ifNonNull(lastLocation) {
                val waypointSet = WaypointSet()
                waypointSet.add(Waypoint(Point.fromLngLat(it.longitude, it.latitude)))
                waypointSet.add(Waypoint(point))
                requestRoutes(waypointSet)
            }
            return false
        }
    }


}
