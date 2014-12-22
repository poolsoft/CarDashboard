package ru.max314.cardashboard.view;


import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import ru.max314.cardashboard.R;
import ru.max314.cardashboard.model.ApplicationModelFactory;
import ru.max314.cardashboard.model.ModelData;
import ru.max314.util.LogHelper;
import ru.max314.util.threads.TimerUIHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class GMapFragment extends Fragment implements IBackgroudMapFrame {
    protected static LogHelper Log = new LogHelper(GMapFragment.class);

    private GoogleMap googleMap; // Might be null if Google Play services APK is not available.
    private ModelData modelData; // model
    private boolean mapBussy = true; // map ready ?
    TimerUIHelper timerUIHelper; // auto update data from model


    private MapView mapView;

    public GMapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gmap, container, false);
        Log.d("get mapview init");
        mapView = (MapView) view.findViewById(R.id.frGMapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                InitMap(map);
            }
        });
        Log.d("run asynck get map from mapview");
        modelData = ApplicationModelFactory.getModel().getModelData();

        try {
            MapsInitializer.initialize(this.getActivity());
        } catch (Throwable e) {
           Log.e("Error init gogole map service leyer",e);
        }

        return view;
    }

    /**
     * init gmap on create fragment
     */
    private void InitMap(GoogleMap map) {
        Log.d("InitMap map ready for working");
        googleMap = map;
        googleMap.setTrafficEnabled(true); // трафик у нас не показываеть
        googleMap.getUiSettings().setCompassEnabled(true); // компас показать
        googleMap.getUiSettings().setMyLocationButtonEnabled(false); // кнопку позиционирования скрыть
        googleMap.setMyLocationEnabled(true); // показывать текущее местоположение
        // кнопки приближения - свои
        mapBussy = false; // карта готова страдать
    }

    /**
     * Данные были изменены в модели чейнить делаем
     */
    private void updateData() {
        if (mapBussy) // карта не готова уходим отседова
            return;
        Location location = modelData.getCurrentLocation();
        if (location == null)
            return;

        LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(loc)
                .zoom(modelData.getCurrentZoom())
                .bearing(location.getBearing())
                .tilt(30)
                .build();
        mapBussy = true;
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GMapCancell());

    }

    /**
     * Приблизить карту
     */
    @Override
    public void ZoomIn() {
        float zoom = googleMap.getCameraPosition().zoom + 1;
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), new GMapCancellZoom());
    }

    /**
     * Отдалить карту
     */
    @Override
    public void ZoomOut() {
        float zoom = googleMap.getCameraPosition().zoom - 1;
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), new GMapCancellZoom());
    }

    //region Overrides
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (timerUIHelper != null) {
            timerUIHelper.cancel();
        }
        timerUIHelper = new TimerUIHelper(500, new Runnable() {
            @Override
            public void run() {
                updateData();
            }
        });
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
        if (timerUIHelper != null) {
            timerUIHelper.cancel();
            timerUIHelper = null;
        }
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        modelData.setCurrentZoom(googleMap.getCameraPosition().zoom);
        mapView.onSaveInstanceState(b);
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }
    //endregion


    //region Implement GoogleMap.CancelableCallback class

    /**
     * Implement GoogleMap.CancelableCallback on end set zoom in model
     */
    private class GMapCancell implements GoogleMap.CancelableCallback {

        private GMapCancell() {
            mapBussy = true;
        }

        @Override
        public void onFinish() {
            mapBussy = false;
        }

        @Override
        public void onCancel() {
            mapBussy = false;
        }
    }

    /**
     * Implement GoogleMap.CancelableCallback on end set zoom in model
     */
    private class GMapCancellZoom extends GMapCancell {
        @Override
        public void onFinish() {
            super.onFinish();
            modelData.setCurrentZoom(googleMap.getCameraPosition().zoom);
        }

        @Override
        public void onCancel() {
            super.onCancel();
            modelData.setCurrentZoom(googleMap.getCameraPosition().zoom);
        }
    }
    //endregion
}
