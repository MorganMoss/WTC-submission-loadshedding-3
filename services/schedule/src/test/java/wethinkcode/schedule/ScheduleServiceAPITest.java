package wethinkcode.schedule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;
import wethinkcode.model.Schedule;
import wethinkcode.places.PlacesService;
import wethinkcode.service.Service;
import wethinkcode.stage.StageService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * I am an API / functional test of the ScheduleService. I am not a unit test.
 */
@Tag( "expensive" )
//@Disabled( "Enable this to test your ScheduleService. DO NOT MODIFY THIS FILE.")
public class ScheduleServiceAPITest
{
    public static final int TEST_PORT = 8876;
    private static Service<PlacesService> places;
    private static Service<ScheduleService> schedule;
    private static Service<StageService> stage;

    @BeforeAll
    public static void initJsonMapper(){
        final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule( new JavaTimeModule() );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        Unirest.config().setObjectMapper( new kong.unirest.jackson.JacksonObjectMapper( mapper ) );
    }


    @BeforeAll
    public static void initTestScheduleFixture() throws IOException {
        places = new Service<>(new PlacesService()).execute("-o=false", "-p=3221");

        stage = new Service<>(new StageService()).execute("-o=false","-p=1234");

        schedule = new Service<>(new ScheduleService()).execute("-p=" + TEST_PORT, "--places=http://localhost:3221");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void destroyTestFixture(){
        places.close();
        stage.close();
        schedule.close();
    }

    @Test
    public void getSchedule_someTown(){
        HttpResponse<Schedule> response = Unirest
            .get(schedule.url() + "/Eastern%20Cape/Gqeberha" )
            .asObject( Schedule.class );
        assertEquals( HttpStatus.OK, response.getStatus());

        Schedule schedule = response.getBody();
        assertEquals( 4, schedule.numberOfDays() );
        assertEquals( LocalDate.now(), schedule.getStartDate() );
    }

    @Test
    public void getSchedule_nonexistentTown(){
        HttpResponse<Schedule> response = Unirest
            .get(schedule.url() + "/Mars/Elonsburg" )
            .asObject( Schedule.class );
        assertEquals( HttpStatus.NOT_FOUND, response.getStatus() );
        assertEquals( 0, response.getBody().numberOfDays() );
    }
}
