package com.bluedot.pointapp;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import au.com.bluedot.application.model.geo.Fence;
import au.com.bluedot.model.geo.BoundingBox;
import au.com.bluedot.model.geo.Circle;
import au.com.bluedot.model.geo.LineString;
import au.com.bluedot.model.geo.Point;
import au.com.bluedot.model.geo.Polygon;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.ZoneInfo;

import com.bluedot.pointapp.model.BoundingBoxItem;
import com.bluedot.pointapp.model.CircleItem;
import com.bluedot.pointapp.model.LineStringItem;
import com.bluedot.pointapp.model.MapItem;
import com.bluedot.pointapp.model.PolygonItem;
import com.bluedotinnovation.android.pointapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class PointMapFragment extends SupportMapFragment implements LocationListener, View.OnTouchListener, ClusterManager.OnClusterClickListener, ClusterManager.OnClusterItemClickListener<MapItem>, GoogleMap.OnMapClickListener{

    private static final String TAG = PointMapFragment.class.getSimpleName();
	private GoogleMap mMap;
	private MainActivity mActivity;
	private static boolean mIsInBackbround = true;

	// Indicates if power consumption dialog has been shown
	private boolean mIsPowerConsumptionDialogShown = false;

	// GoogleMaps cluster manager
	private ClusterManager<MapItem> clusterManager;

	// LocationManager to receive location updates
	private LocationManager mLocationManager;

	// Last fence drawn by clicking on map marker
	private Object lastDrawFence;

	// Starting from how many of markers organising them into clusters
	private final static int CLUSTER_MARKERS_IN_CLUSTER = 5;

	// Acceptable accuracy for location updates, in meters
	private final static float ACCURACY_LEVEL_m = 50.0f;

	// Animation timeout for zooming-in clicked cluster
	private final static int CAMERA_ANIMATION_ms = 500;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (getActivity() == null) {
			mActivity = (MainActivity) activity;
		} else {
			mActivity = (MainActivity) getActivity();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
		View mapView = super.onCreateView(inflater, viewGroup, bundle);
		RelativeLayout view = new RelativeLayout(getActivity());
		view.addView(mapView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

		// Adding "hold for MyLocation" button
		// programmatically hence SupportMapFragment holds layouts and views internally
		Button button_view=new Button(mActivity);
		RelativeLayout.LayoutParams button_view_layout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		button_view_layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		button_view_layout.addRule(RelativeLayout.CENTER_IN_PARENT);
		button_view.setLayoutParams(button_view_layout);
		button_view.setText(mActivity.getResources().getText(R.string.hold_for_gps));
		button_view.setOnTouchListener(this);
		view.addView(button_view);

		return view;
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mIsInBackbround = false;
		mMap = getMap();
		if (mMap != null) {
			mMap.setBuildingsEnabled(true);
			CameraPosition cameraPosition = CameraPosition.fromLatLngZoom(
					getLastKnownPosition(), 18);
			mMap.moveCamera(CameraUpdateFactory
					.newCameraPosition(cameraPosition));

			// Initialise cluster manager
			clusterManager = new ClusterManager<MapItem>(mActivity, mMap);
			mMap.setOnCameraChangeListener(clusterManager);
			clusterManager.setRenderer(new FenceRender(mActivity, mMap, clusterManager));
			clusterManager.setOnClusterItemClickListener(this);
			clusterManager.setOnClusterClickListener(this);
			mMap.setOnMarkerClickListener(clusterManager);
			mMap.setOnMapClickListener(this);
			mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
		}
	}

	/**
	 * Tracks holding down of "Hold for MyLocation" button
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		switch (event.getAction() & MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_DOWN:
				mMap.setMyLocationEnabled(true);
				mLocationManager.requestSingleUpdate(LocationManager.PASSIVE_PROVIDER, this, null);
				break;
			case MotionEvent.ACTION_UP:
				mMap.setMyLocationEnabled(false);
				if (!mIsPowerConsumptionDialogShown)
					showPowerConsumptionDialog();
				break;
		}
		return false;
	}

	/**
	 * Forms and shows a dialog informing user on high battery consumption
	 * then "Hold for MyLocation" button is held
	 */
	private void showPowerConsumptionDialog()
	{
		AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
		alertDialog.setTitle(mActivity.getResources().getText(R.string.power_consumption_title));
		alertDialog.setMessage(mActivity.getResources().getText(R.string.power_consumption_message));
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, mActivity.getResources().getText(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alertDialog.show();
		mIsPowerConsumptionDialogShown = true;
	}

	/**
	 * Put the fence on the map
	 */
    private void displayFenceOnMap(Fence fence, ZoneInfo zoneInfo) {
        int color = 0x55880000;
        if (fence.getGeometry() instanceof Circle) {
            Circle circle = (Circle) fence.getGeometry();
            LatLng latLong = new LatLng(circle.getCenter().getLatitude(),
                    circle.getCenter().getLongitude());
            CircleOptions circleOptions = new CircleOptions().center(latLong)
                    .radius(circle.getRadius()).fillColor(color).strokeWidth(2)
                    .strokeColor(0x88888888);
            CircleItem circleItem = new CircleItem();
            circleItem.setGeometry(circleOptions);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(
                            new LatLng(circle.getCenter().getLatitude(),
                                    circle.getCenter().getLongitude()))
                    .title("Zone Name : " + zoneInfo.getZoneName())
                    .snippet("Fence Name : " + fence.getName());

            circleItem.setMarkerOption(markerOptions);

            clusterManager.addItem(circleItem);

        } else if (fence.getGeometry() instanceof BoundingBox) {
            BoundingBox bbox = (BoundingBox) fence.getGeometry();
            PolygonOptions polygon = new PolygonOptions()
                    .add(new LatLng(bbox.getNorth(), bbox.getEast()))
                    .add(new LatLng(bbox.getNorth(), bbox.getWest()))
                    .add(new LatLng(bbox.getSouth(), bbox.getWest()))
                    .add(new LatLng(bbox.getSouth(), bbox.getEast()))
                    .fillColor(color).strokeWidth(2).strokeColor(0x88888888);
            BoundingBoxItem boundingBoxItem = new BoundingBoxItem();
            boundingBoxItem.setGeometry(polygon);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(
                            new LatLng(bbox.getNorthEast().getLatitude(), bbox
                                    .getNorthEast().getLongitude()))
                    .title("Zone Name : " + zoneInfo.getZoneName())
                    .snippet("Fence Name : " + fence.getName());

            boundingBoxItem.setMarkerOption(markerOptions);

            clusterManager.addItem(boundingBoxItem);
        } else if (fence.getGeometry() instanceof Polygon) {

            Polygon truePolygon = (Polygon) fence.getGeometry();
            List<Point> points = truePolygon.getVertices();
            PolygonOptions truePolygonOptions = new PolygonOptions()
                    .fillColor(color).strokeWidth(2).strokeColor(0x88888888);
            for (Point p : points) {
                truePolygonOptions.add(new LatLng(p.getLatitude(), p
                        .getLongitude()));
            }
            PolygonItem polygonItem = new PolygonItem();
            polygonItem.setGeometry(truePolygonOptions);
            Point marker_position = ((Polygon) fence.getGeometry()).getVertices().get(0);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(
                            new LatLng(marker_position.getLatitude(), marker_position.getLongitude()))
                    .title("Zone Name : " + zoneInfo.getZoneName())
                    .snippet("Fence Name : " + fence.getName());
            polygonItem.setMarkerOption(markerOptions);

            clusterManager.addItem(polygonItem);
        } else if (fence.getGeometry() instanceof LineString) {
            LineString trueLineString = (au.com.bluedot.model.geo.LineString) fence.getGeometry();
            List<Point> points = trueLineString.getVertices();
            PolylineOptions truePolylineOptions = new PolylineOptions()
                    .width(6).color(0x55880000);
            for (Point p : points) {
                truePolylineOptions.add(new LatLng(p.getLatitude(), p
                        .getLongitude()));
            }
            LineStringItem lineStringItem = new LineStringItem();
            lineStringItem.setGeometry(truePolylineOptions);
            Point marker_position = ((LineString) fence.getGeometry()).getStart();
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(
                            new LatLng(marker_position.getLatitude(), marker_position.getLongitude()))
                    .title("Zone Name : " + zoneInfo.getZoneName())
                    .snippet("Fence Name : " + fence.getName());
            lineStringItem.setMarkerOption(markerOptions);
            clusterManager.addItem(lineStringItem);
        }
    }

    /**
     * Put beacon on the map
     * @param beacon
     * @param zoneInfo
     */
    private void displayBeaconOnMap(BeaconInfo beacon, ZoneInfo zoneInfo) {
        int color = 0x550000aa;
        MarkerOptions markerOptions = null;

        LatLng latLong = new LatLng(beacon.getLocation().getLatitude(),
                beacon.getLocation().getLongitude());
        CircleOptions circleOptions = new CircleOptions().center(latLong)
                .radius(beacon.getRange()).fillColor(color).strokeWidth(2)
                .strokeColor(0x88888888);

        CircleItem circleItem = new CircleItem();
        circleItem.setGeometry(circleOptions);
        // map.addCircle(circleOptions);
        markerOptions = new MarkerOptions()
                .position(new LatLng(beacon.getLocation().getLatitude(),beacon.getLocation().getLongitude()))
                .title("Zone Name : " + zoneInfo.getZoneName())
                .snippet("Beacon Name : " + beacon.getName());

        circleItem.setMarkerOption(markerOptions);
        circleItem.setIcon(BitmapDescriptorFactory.HUE_BLUE);

        clusterManager.addItem(circleItem);

    }

	private void loadDetails(ArrayList<ZoneInfo> zonesInfo) {
		mMap.clear();
		clusterManager.clearItems();
        Log.i(TAG, "Zone size: " + zonesInfo.size());
        for (ZoneInfo zoneInfo : zonesInfo) {
            //	printMessage("Zone Name ::: " + zoneInfo.getZoneName());
            try {
                if (mMap != null) {
                    for (Fence fence : zoneInfo.getFences()) {
                        displayFenceOnMap(fence, zoneInfo);
                    }
                    for (BeaconInfo beacon : zoneInfo.getBeacons()) {
                        displayBeaconOnMap(beacon, zoneInfo);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "loadDetails():");
                Log.e(TAG, "Zone name:" + zoneInfo.getZoneName());
                Log.e(TAG, "Fence size: " + zoneInfo.getFences().size());
                Log.e(TAG, "Beacon size: " + zoneInfo.getBeacons().size());
                e.printStackTrace();
            }
        }
        // Push cluster manager to update map
        clusterManager.cluster();
	}

	@Override
	public void onPause() {
		super.onPause();
		mIsInBackbround = true;
	}

	@Override
	public void onResume() {
		super.onResume();
		loadDetails(mActivity.getZones());
		mIsInBackbround = false;
	}

	public LatLng getLastKnownPosition() {
        LatLng result = null;
        if (mActivity != null) {
            LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    result = new LatLng(location.getLatitude(), location.getLongitude());
                }
            }
        }
		return result;
	}

	public void refresh() {
		loadDetails(mActivity.getZones());
	}

	@Override
	public boolean onClusterClick(Cluster cluster){
		CameraPosition cameraPosition = mMap.getCameraPosition();
		mMap.animateCamera(CameraUpdateFactory.zoomTo(cameraPosition.zoom + 1), CAMERA_ANIMATION_ms, null);
		return true;
	}

    private void deleteLastDrawFence()
    {
        if(lastDrawFence != null){
            if(lastDrawFence instanceof com.google.android.gms.maps.model.Circle){
                ((com.google.android.gms.maps.model.Circle) lastDrawFence).remove();
            }else if(lastDrawFence instanceof com.google.android.gms.maps.model.Polygon){
                ((com.google.android.gms.maps.model.Polygon) lastDrawFence).remove();
            }else if(lastDrawFence instanceof com.google.android.gms.maps.model.Polyline){
                ((com.google.android.gms.maps.model.Polyline) lastDrawFence).remove();
            }
            lastDrawFence = null;
        }
    }

	@Override
	public boolean onClusterItemClick(MapItem item) {

		deleteLastDrawFence();

		if(item instanceof CircleItem){
			com.google.android.gms.maps.model.Circle c = mMap.addCircle((CircleOptions) item.getGeometry());
			lastDrawFence = c;
		}else if(item instanceof PolygonItem){
			com.google.android.gms.maps.model.Polygon p = mMap.addPolygon((PolygonOptions)item.getGeometry());
			lastDrawFence = p;
		}else if(item instanceof BoundingBoxItem){
			com.google.android.gms.maps.model.Polygon box =  mMap.addPolygon((PolygonOptions) item.getGeometry());
			lastDrawFence = box;
        }else if(item instanceof LineStringItem){
            com.google.android.gms.maps.model.Polyline ls =  mMap.addPolyline((PolylineOptions) item.getGeometry());
            lastDrawFence = ls;
        }
		return false;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		deleteLastDrawFence();
	}

	@Override
	public void onLocationChanged(Location location) {
		boolean requestAnotherUpdate = true;
		if (location != null)
		{
			if (location.getAccuracy() < ACCURACY_LEVEL_m) {
				mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())), CAMERA_ANIMATION_ms, null);
				requestAnotherUpdate = false;
			}
		}
		if (requestAnotherUpdate){
			mLocationManager.requestSingleUpdate(LocationManager.PASSIVE_PROVIDER, this, null);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {

	}

	private class FenceRender extends DefaultClusterRenderer<MapItem> {

		private final GoogleMap map;

		public FenceRender(Context context, GoogleMap map, ClusterManager<MapItem> clusterManager) {
			super(context, map, clusterManager);
			this.map = map;
		}

		@Override
		protected boolean shouldRenderAsCluster(Cluster<MapItem> cluster) {
			return cluster.getSize() > CLUSTER_MARKERS_IN_CLUSTER;
		}

		@Override
		protected void onClusterItemRendered(MapItem item, Marker marker) {
			MarkerOptions markerOptions = item.getMarkerOptions();
			marker.setSnippet(markerOptions.getSnippet());
			marker.setTitle(markerOptions.getTitle());
		}

        @Override
        protected void onBeforeClusterItemRendered(MapItem item, MarkerOptions markerOptions) {
            if (item.getIcon() != BitmapDescriptorFactory.HUE_RED){
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(item.getIcon()));
            }
        }
	}
}
