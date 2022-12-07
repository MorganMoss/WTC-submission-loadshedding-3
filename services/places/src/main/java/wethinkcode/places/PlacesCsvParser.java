package wethinkcode.places;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import wethinkcode.places.db.memory.PlacesDb;
import wethinkcode.model.Municipality;
import wethinkcode.model.Place;
import wethinkcode.model.Province;


/**
 * PlacesCsvParser : I parse a CSV file with each line containing the fields (in order):
 * <code>Name, Feature_Description, pklid, Latitude, Longitude, Date, MapInfo, Province,
 * fklFeatureSubTypeID, Previous_Name, fklMagisterialDistrictID, ProvinceID, fklLanguageID,
 * fklDisteral, Local Municipality, Sound, District Municipality, fklLocalMunic, Comments, Meaning</code>.
 * <p>
 * For the PlacesService we're only really interested in the <code>Name</code>,
 * <code>Feature_Description</code> and <code>Province</code> fields.
 * <code>Feature_Description</code> allows us to distinguish towns and urban areas from
 * (e.g.) rivers, mountains, etc. since our PlacesService is only concerned with occupied places.
 */
public class PlacesCsvParser
{
    @VisibleForTesting
    int name_column;
    @VisibleForTesting
    int province_column;
    @VisibleForTesting
    int type_column;
    @VisibleForTesting
    int municipality_column;
    @VisibleForTesting
    int columns;

    private static final List<String> types = List.of("town", "neighbourhood", "populated area", "settled place", "urban area");

    private ArrayList<String> split(String s, String delimiter, String encloser){
        int e = s.indexOf(encloser);
        int d = s.indexOf(delimiter);

        if (e > d) {
            int e2 = s.indexOf(encloser, e+1);
            if (e2 > -1) {
                d = s.indexOf(delimiter, e2+1);
            }
        }


        if (d == -1){
            return new ArrayList<>() {{
                add(s);
            }};
        }

        String item = s.substring(0, d);

        if (d == 0){
            item = "";
        }

        String left = s.substring(d+1);

        String finalItem = item;
        ArrayList<String> sl = new ArrayList<>() {{
            add(finalItem);
        }};

        sl.addAll(split(left, delimiter, encloser));

        return sl;
    }

    /**
     * Takes into account the type_column,
     * Filters out feature types that are irrelevant
     * @param s - the full line from the csv being parsed
     * @return true if the right type
     */
    boolean isCorrectType(ArrayList<String> s){
        return types.contains(s.get(type_column).toLowerCase());
    }

    private Place convertToPlace(String[] data){
        return new Place(data[name_column], data[municipality_column]);
    }

    private Municipality convertToMunicipality(String[] data) {
        return new Municipality(data[municipality_column], data[province_column]);
    }

    private Province convertToProvince(String[] data) {
        return new Province(data[province_column]);
    }

    private boolean removeNulls(String[] data){
        return !(
            data[name_column].equals("")
            ||   data[province_column].equals("")
            ||   data[type_column].equals("")
            ||   data[municipality_column].equals("")
        );
    }

    public Places parseCsvSource( File csvFile ) throws IOException {
        if (!csvFile.exists()){
            throw new FileNotFoundException(csvFile.getPath());
        }

        FileReader fileReader = new FileReader(csvFile);

        LineNumberReader csvReader = new LineNumberReader(fileReader);

        return parseDataLines(csvReader);
    }

    private void parseHeader(List<String> header){
        columns = header.size();
        name_column = header.indexOf("Name");
        province_column = header.indexOf("Province");
        type_column = header.indexOf("Feature_Description");
        municipality_column = header.indexOf("Local Municipality");
        if (name_column == -1 || province_column == -1 || type_column == -1 || municipality_column == -1){
            throw new RuntimeException("Bad CSV Header");
        }
    }

    @VisibleForTesting
    Places parseDataLines( final LineNumberReader in ){
        List<String> lines = in.lines().toList();

        parseHeader(List.of(lines.get(0).split(",", -1)));

        Stream<String[]> placeStream = lines
                .stream()
                .map((s) -> split(s,",", "\""))
                .filter((s) -> s.size() >= columns)
                .filter(this::isCorrectType)
                .map(strings -> strings.toArray(String[]::new))
                .filter(this::removeNulls);

        List<String[]> placesRaw = placeStream.toList();

        List<Province> provinces = placesRaw
                .stream()
                .map(this::convertToProvince)
                .distinct()
                .toList();

        List<Municipality> municipalities = placesRaw
                .stream()
                .map(this::convertToMunicipality)
                .distinct()
                .toList();

        List<Place> places = placesRaw
                .stream()
                .map(this::convertToPlace)
                .toList();

        return new PlacesDb(provinces, municipalities, places);
    }


}
