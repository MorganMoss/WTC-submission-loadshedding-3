package wethinkcode.places;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import org.junit.jupiter.api.*;
import wethinkcode.model.Place;
import wethinkcode.service.Service;

import static org.junit.jupiter.api.Assertions.*;

/**
 * *Functional* tests of the PlacesService.
 */
public class PlacesApiTest
{
    public static final int TEST_PORT = 7377;
    private static Service<PlacesService> SERVICE;


    @BeforeAll
    public static void startServer() throws IOException{


        File data = new File("test.csv");
        if (!data.exists()){
            if (data.createNewFile()) {
                try (FileWriter fw = new FileWriter(data)) {
                    fw.write(PlacesTestData.HEADER);
                    fw.write(PlacesTestData.CSV_DATA);
                }
            }
            data.deleteOnExit();
        }

        File properties = new File("test.properties");
        if (!properties.exists()){
            if (properties.createNewFile()) {
                try (FileWriter fw = new FileWriter(properties)) {
                    fw.write("data="+data.getAbsolutePath());
                }
            }
            properties.deleteOnExit();
        }

        SERVICE = new Service<>(new PlacesService())
                .execute("-p="+TEST_PORT, "-c="+properties.getAbsolutePath());


    }

    @AfterAll
    public static void stopServer(){
        SERVICE.close();
    }

    @Test
    public void getProvincesJson(){
        HttpResponse<JsonNode> response = Unirest.get(  SERVICE.url() + "/provinces" ).asJson();
        JSONArray array = response.getBody().getArray();
        Set<String> provinces = Set.of("KwaZulu-Natal", "Western Cape", "Gauteng", "Northern Cape", "Free State");
        Set<String> actual = new HashSet<>();
        for (int i = 0; i < array.length(); i++){
           actual.add(array.getJSONObject(i).get("name").toString());
        }
        assertEquals(provinces, actual);
    }

    @Test
    public void getTownsInAProvince_provinceExistsInDb(){
        HttpResponse<JsonNode> response = Unirest.get( SERVICE.url() + "/places/province/KwaZulu-Natal").asJson();
        JSONArray array = response.getBody().getArray();
        Set<Place> places = Set.of(new Place("Amatikulu", "uMlalazi"));
        Set<Place> actual = new HashSet<>();
        for (int i = 0; i < array.length(); i++){
            actual.add(new Place(array.getJSONObject(i).get("name").toString(), array.getJSONObject(i).get("municipality").toString()));
        }
        assertEquals(places, actual);

    }

    @Test
    public void getTownsInAProvince_noSuchProvinceInDb(){
        HttpResponse<JsonNode> response = Unirest.get( SERVICE.url() + "/place/Oregon" ).asJson();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void placeExists(){
        HttpResponse<JsonNode> response = Unirest.get( SERVICE.url() + "/exists/KwaZulu-Natal/Amatikulu").asJson();
        assertEquals(HttpStatus.FOUND, response.getStatus());
    }

    @Test
    public void placeNotExists(){
        HttpResponse<JsonNode> response = Unirest.get( SERVICE.url() + "/exists/KwaZulu-Natal/MadeUp").asJson();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }


    @Test
    public void provinceNotExists(){
        HttpResponse<JsonNode> response = Unirest.get( SERVICE.url() + "/exists/MadeUp/Amatikulu").asJson();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

}
