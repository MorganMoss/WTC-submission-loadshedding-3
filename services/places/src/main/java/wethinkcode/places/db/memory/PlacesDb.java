package wethinkcode.places.db.memory;

import java.util.List;
import java.util.Optional;

import wethinkcode.model.Place;
import wethinkcode.model.Province;
import wethinkcode.places.Places;
import wethinkcode.model.Municipality;

/**
 * TODO: javadoc PlacesDb
 */
public class PlacesDb implements Places
{
    final List<Province> provinces;
    final List<Municipality> municipalities;
    final List<Place> places;

    public PlacesDb(List<Province> provinces, List<Municipality> municipalities, List<Place> places){
        this.provinces = provinces;
        this.municipalities = municipalities;
        this.places = places;
    }

    @Override
    public List<Province> provinces(){
        return provinces;
    }

    @Override
    public List<Municipality> municipalitiesIn(String province) {
        return municipalities
                .stream()
                .filter(municipality -> municipality.province().equals(province))
                .toList();
    }

    @Override
    public List<Place> placesInMunicipality(String municipality) {
        return places.stream()
                .filter(place -> place.municipality().equals(municipality))
                .toList();
    }

    @Override
    public List<Place> placesInProvince(String province) {
        return places.stream()
            .filter(place -> {
                Optional<Municipality> m = municipality(place.municipality());
                return m
                        .map(municipality -> municipality.province().equals(province))
                        .orElse(false);
            })
            .toList();
    }

    @Override
    public Optional<Municipality> municipality(String name) {
        return municipalities.stream()
                .filter(municipality -> municipality.name().equals(name))
                .findFirst();
    }

    @Override
    public Optional<Place> place(String name) {
        return places.stream()
                .filter(place -> place.name().equals(name))
                .findFirst();
    }

    @Override
    public Optional<Province> province(String name) {
        return provinces.stream()
                .filter(place -> place.name().equals(name))
                .findFirst();
    }

    @Override
    public int size(){
        return places.size();
    }
}