package com.example.sharedbikeclient;

import com.amap.api.maps2d.model.LatLng;

public class Bike {
    String key;
    public String bikeId;
    public boolean available;
    public boolean inUse;
    public LatLng position;

    public Bike(String key, String bikeId, boolean available, boolean inUse, LatLng position) {
        this.key = key;
        this.bikeId = bikeId;
        this.available = available;
        this.inUse = inUse;
        this.position = position;
    }
}
