package wethinkcode.places;

import java.io.*;
import java.util.List;

import org.junit.jupiter.api.*;
import wethinkcode.model.Place;

import static org.junit.jupiter.api.Assertions.*;
import static wethinkcode.places.PlacesTestData.createReaderForTest;

/**
 * Unit-test suite for the CSV parser.
 */
public class PlacesCsvParserTest
{
    private PlacesCsvParser parser;

    private Places places;

    @BeforeEach
    public void setUp(){
        parser = new PlacesCsvParser();
    }

    @AfterEach
    public void tearDown(){
        places = null;
        parser = null;
    }


    @Test
    public void firstLineGetsSkipped() {
        places = parser.parseDataLines(createReaderForTest(""));
        assertEquals(0, places.size());
    }

    @Test
    public void splitLineIntoValuesProducesCorrectNoOfValues(){
        final String testLine = "Brakpan,Non_Perennial,92797,-26.60444444,26.34,01-06-1992,,North West,66,,262,8,16,DC40,Matlosana,,,NW403,,";
        places = parser.parseDataLines(createReaderForTest(testLine));
        assertEquals(7, parser.province_column);
        assertEquals(0, parser.name_column);
        assertEquals(1, parser.type_column);
    }

    @Test
    public void urbanPlacesAreWanted(){
        final String testLine = "Brakpan,Urban Area,92799,-26.23527778,28.37,31-05-1995,,Gauteng,114,,280,3,16,EKU,Ekurhuleni Metro,,,EKU,,\n";
        places = parser.parseDataLines(createReaderForTest(testLine));
        assertEquals(
                List.of(new Place("Brakpan","Ekurhuleni Metro")),
                places.placesInProvince("Gauteng").stream().toList()
        );
    }

    @Test
    public void townsAreWanted(){
        final String testLine = "Brakpan,Town,92802,-27.95111111,26.53333333,30-05-1975,,Free State,68,,155,2,16,DC18,Matjhabeng,,,FS184,,";
        places =parser.parseDataLines(createReaderForTest(testLine));

        assertEquals(
                List.of(new Place("Brakpan","Matjhabeng")),
                places.placesInProvince("Free State").stream().toList()
        );
    }

    @Test
    public void otherFeaturesAreNotWanted(){
        final String testLine = "Amatikulu,Station,95756,-29.05111111,31.53138889,31-05-1989,,KwaZulu-Natal,79,,237,4,16,DC28,uMlalazi,,,KZ284,,";
        places = parser.parseDataLines(createReaderForTest(testLine));
        assertEquals(
                0,
                places.provinces().size()
        );

    }

    @Test
    public void parseBulkTestData(){
        places = parser.parseDataLines( createReaderForTest(PlacesTestData.CSV_DATA) );
        assertEquals( 5, places.size() );

    }

    @Test
    public void parseTestDataFromFile() throws IOException {
        File file = new File("test.csv");
        if (!file.exists()){
            if (file.createNewFile()) {
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(PlacesTestData.HEADER);
                    fw.write(PlacesTestData.CSV_DATA);
                }
            }
            file.deleteOnExit();
        }
        places = parser.parseCsvSource( file );
        assertEquals( 5, places.size() );

    }

    @Test
    public void badFile() throws IOException {
        File file = new File("bad.csv");
        file.deleteOnExit();
        try {
            places = parser.parseCsvSource( file );
            fail("Must throw fileNotFoundException");
        } catch (FileNotFoundException e){
            return;
        }
            fail("Wrong exception thrown");
    }

    @Test
    public void parseTestMissingHeaderDataFromFile() throws IOException {
        File file = new File("test_broken.csv");
        if (!file.exists()){
            if (file.createNewFile()){
                try ( FileWriter fw = new FileWriter( file)){
                    fw.write(PlacesTestData.MISSING_ITEMS_HEADER);
                    fw.write(PlacesTestData.CSV_DATA);
                }
            }
            file.deleteOnExit();
        }
        try {
            places = parser.parseCsvSource( file );
            fail("Must throw Exception");
        } catch (RuntimeException e){
            assertEquals("Bad CSV Header", e.getMessage());
            return;
        }
        fail("Wrong exception thrown");
    }

}