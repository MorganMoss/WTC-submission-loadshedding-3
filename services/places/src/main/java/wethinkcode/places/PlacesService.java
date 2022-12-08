package wethinkcode.places;

import java.io.*;
import java.net.URISyntaxException;

import com.google.common.io.Resources;
import picocli.CommandLine;
import wethinkcode.service.Service;


/**
 * I provide a Province-names Service_OLD for places in South Africa.
 * <p>
 * I read place-name data from a CSV file that I read and
 * parse into the objects (domain model) that I use,
 * discarding unwanted data in the file (things like mountain/river names). With my "database"
 * built, I then serve-up place-name data as JSON to clients.
 * <p>
 * Clients can request:
 * <ul>
 * <li>a list of available Provinces
 * <li>a list of all Towns/PlacesService in a given Province
 * <li>a list of all neighbourhoods in a given Municipality
 * </ul>
 * I understand the following command-line arguments:
 * <dl>
 * <dt>-c | --config &lt;configfile&gt;
 * <dd>a file pathname referring to an (existing!) configuration file in standard Java
 *      properties-file format
 * <dt>-d | --datadir &lt;datadirectory&gt;
 * <dd>the name of a directory where CSV datafiles may be found. This option <em>overrides</em>
 *      and data-directory setting in a configuration file.
 * <dt>-p | --places &lt;csvdatafile&gt;
 * <dd>a file pathname referring to a CSV file of place-name data. This option
 *      <em>overrides</em> any value in a configuration file and will bypass any
 *      data-directory set via command-line or configuration.
 * </dl>
 */
@Service.AsService
public class PlacesService {
    /**
     * Contains the path to the csv file full of places data
     */
    @CommandLine.Option(
            names = {"-d", "--data"},
            description = "The path to the csv data file used by this service"
    )
    public String data;
    /**
     * Should not be modified, I just prefer the look of SERVER.places vs SERVER.getPlaces()
     */
    public Places places;

    /**
     * Adds the additional initialisation of an in-memory Database of
     * places, municipalities and provinces.
     */
    @Service.RunBefore
    public void createPlaces() {
        places = initPlacesDb(data);
    }

    /**
     * Creates an instance of the Places database.
     * @param data_file the path to the data file
     * @return the instance loaded with data from that file
     */
    static Places initPlacesDb(String data_file) {
        File databaseFile;

        try {
            databaseFile = new File(Resources.getResource(data_file).toURI());
        } catch (IllegalArgumentException | URISyntaxException e) {
            databaseFile = new File(data_file);
        }

        try {
            return new PlacesCsvParser().parseCsvSource(databaseFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        new Service<>(new PlacesService()).execute(args);
    }
}
