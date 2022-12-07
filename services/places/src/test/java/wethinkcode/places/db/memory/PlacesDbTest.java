package wethinkcode.places.db.memory;

import java.util.List;

import org.junit.jupiter.api.*;
import wethinkcode.model.Municipality;
import wethinkcode.model.Province;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Uncomment the body of the test methods. They can't compile until you add appropriate code
 * to the PlacesDb class. Once you make them compile, the tests should fail at first. Now
 * make the tests green.
 */
public class PlacesDbTest
{
    public static final List<Municipality> MUNICIPALITIES = List.of(
        new Municipality( "Cape Municipality", "Western Cape" ),
        new Municipality( "Worcester", "Western Cape" ),
        new Municipality( "Riversdale", "Western Cape" ),
        new Municipality( "Gqeberha", "Eastern Cape" ),
        new Municipality( "Queenstown", "Eastern Cape" ),
        new Municipality( "Sandton-East", "Gauteng" ),
        new Municipality( "Riversdale", "Gauteng" ),
        new Municipality( "Mabopane", "Gauteng" ),
        new Municipality( "Brakpan", "Gauteng" )
    );

    public static final List<Province> PROVINCES = List.of(
            new Province("Gauteng"),
            new Province("Western Cape"),
            new Province("Eastern Cape")
    );
    @Test
    public void testProvinces() {
        final PlacesDb db = new PlacesDb(PROVINCES, MUNICIPALITIES, List.of());
        assertEquals(3, db.provinces().size());
    }

    @Test
    public void testTownsInProvince(){
        final PlacesDb db = new PlacesDb(PROVINCES, MUNICIPALITIES, List.of());
        assertEquals(4 ,  db.municipalitiesIn( "Gauteng" ).size() );
        assertEquals(2 ,  db.municipalitiesIn( "Eastern Cape" ).size() );
        assertEquals(3 ,  db.municipalitiesIn( "Western Cape" ).size() );
        assertEquals(0, db.municipalitiesIn( "Northern Cape" ).size() );
    }
}
