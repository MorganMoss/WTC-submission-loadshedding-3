package wethinkcode.schedule;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.*;
import wethinkcode.model.Schedule;
import wethinkcode.places.PlacesService;
import wethinkcode.service.Service;
import wethinkcode.stage.StageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScheduleServiceTest
{
    private static Service<PlacesService> places;
    private static Service<ScheduleService> schedule;
    private static Service<StageService> stage;



    @BeforeAll
    static void startPlacesService() throws IOException {
        places = new Service<>(new PlacesService()).execute("--port=3222");
        stage = new Service<>(new StageService()).execute("--port=1234");

        schedule = new Service<>(new ScheduleService()).execute("--places=http://localhost:3222", "--port=5678");
    }

    @AfterAll
    static void closeAll() {
        stage.stop();
        schedule.stop();
        places.stop();
    }

    @Test
    public void testSchedule_someTown() {
        final Optional<Schedule> scheduleOptional = schedule.instance.scheduleDAO.getSchedule( "Eastern Cape", "Gqeberha", 4 );
        assertTrue( scheduleOptional.isPresent() );
        assertEquals( 4, scheduleOptional.get().numberOfDays() );
    }

    @Test
    public void testSchedule_nonexistentTown() {
        final Optional<Schedule> scheduleOptional = schedule.instance.scheduleDAO.getSchedule( "Mars", "Elonsburg", 2 );
        assertTrue( scheduleOptional.isEmpty() );
    }
}
