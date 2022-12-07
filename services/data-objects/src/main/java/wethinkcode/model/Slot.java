package wethinkcode.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

/**
 * TODO: javadoc Slot
 */
public class Slot
{
    private LocalTime start;

    private LocalTime end;

    public Slot(){
    }

    @JsonCreator
    public Slot(
        @JsonProperty( value = "from" ) LocalTime from,
        @JsonProperty( value = "to" ) LocalTime to ){
        start = from;
        end = to;
    }

    public LocalTime getStart(){
        return start;
    }

    public LocalTime getEnd(){
        return end;
    }

}
