package wethinkcode.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

// The following nested classes are the "domain model" for this service.

// Note that a real model would be a bit more sophisticated and complex,
// and we'd want to put the source in its own package;
// this model is just a dummy "the minimum thing that could possibly work"
// so that we can stay focussed on service interconnections and communication
// without getting bogged down in modelling.
public class Schedule
{
    // Using LocalDate.now() is probably a bug! Is the server in the
    // same timezone as the client? Maybe not.
    // What if the server creates an instance one nanosecond before midnight
    // before sending it to the client who then gets it "the next day"?
    // What could you do about all that?
    private LocalDate startDate = LocalDate.now();

    private List<Day> loadSheddingDays;

    public Schedule(){
    }

    @JsonCreator
    public Schedule(
        @JsonProperty( value = "days" ) List<Day> days ){
        loadSheddingDays = days;
    }

    public List<Day> getDays(){
        return loadSheddingDays;
    }

    public int numberOfDays(){
        return getDays().size();
    }

    public LocalDate getStartDate(){
        return startDate;
    }

}
