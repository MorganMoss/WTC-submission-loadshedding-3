package wethinkcode.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * TODO: javadoc Day
 */
public class Day
{
    private List<Slot> loadSheddingSlots;

    public Day(){
    }

    @JsonCreator
    public Day(
        @JsonProperty( value = "slots" ) List<Slot> slots ){
        loadSheddingSlots = slots;
    }

    public List<Slot> getSlots(){
        return loadSheddingSlots;
    }

    public int numberOfSlots(){
        return getSlots().size();
    }

}
