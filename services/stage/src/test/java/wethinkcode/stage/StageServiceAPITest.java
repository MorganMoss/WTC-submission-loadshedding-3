package wethinkcode.stage;

import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONException;
import org.junit.jupiter.api.*;
import wethinkcode.BadStageException;
import wethinkcode.model.Stage;
import wethinkcode.service.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * I contain functional tests of the Stage Service_OLD.
 */
@Tag( "expensive" )
//@Disabled( "Enable this to test your ScheduleService. DO NOT MODIFY THIS FILE.")
public class StageServiceAPITest
{
    public static final int TEST_PORT = 7777;

    private static Service<StageService> service;

    @BeforeAll
    public static void startServer() {
        service = new Service<>(new StageService()).execute("-p="+TEST_PORT);
    }

    @AfterAll
    public static void stopServer(){
        service.close();
    }

    @Test
    public void setNewStage_validStage() throws BadStageException {
        final int NEW_STAGE = 4;
        HttpResponse<JsonNode> post = Unirest.post( serverUrl() + "/stage" )
            .header( "Content-Type", "application/json" )
            .body(Stage.stageFromNumber(NEW_STAGE))
            .asJson();
        assertEquals( HttpStatus.OK, post.getStatus() );

        HttpResponse<JsonNode> response = Unirest.get( serverUrl() + "/stage" ).asJson();
        assertEquals( HttpStatus.OK, response.getStatus() );
        assertEquals( "application/json", response.getHeaders().getFirst( "Content-Type" ) );

        final int stage = getStageFromResponse( response );
        assertEquals( NEW_STAGE, stage );
    }

    @Test
    public void setNewStage_illegalStageValue() {
        HttpResponse<JsonNode> response = Unirest.get( serverUrl() + "/stage" ).asJson();
        assertEquals( HttpStatus.OK, response.getStatus() );
        assertEquals( "application/json", response.getHeaders().getFirst( "Content-Type" ) );
        final int oldStage = getStageFromResponse( response );

        final int NEW_STAGE = -1;
        final HttpResponse<JsonNode> post = Unirest.post( serverUrl() + "/stage" )
            .header( "Content-Type", "application/json" )
            .body( "{\"stage\": "+ NEW_STAGE+ "}")
            .asJson();
        assertEquals( HttpStatus.BAD_REQUEST, post.getStatus() );

        final HttpResponse<JsonNode> check = Unirest.get( serverUrl() + "/stage" ).asJson();
        assertEquals( HttpStatus.OK, check.getStatus() );
        assertEquals( "application/json", check.getHeaders().getFirst( "Content-Type" ) );

        final int stage = getStageFromResponse( check );
        assertEquals( oldStage, stage );
    }

    @Test
    public void getStageJson() throws BadStageException {
        HttpResponse<JsonNode> response = Unirest.get( serverUrl() + "/stage" ).asJson();
        assertEquals(HttpStatus.OK ,response.getStatus());
        Stage res = Stage.stageFromNumber(response.getBody().getObject().getInt("stage"));
        assertEquals(service.instance.stage, res);
    }

    @Test
    public void postStageOK() throws BadStageException {
        HttpResponse<JsonNode> response = Unirest.post( serverUrl() + "/stage" ).body("{\"stage\" : 4}").asJson();
        assertEquals(HttpStatus.OK ,response.getStatus());

        response = Unirest.get( serverUrl() + "/stage" ).asJson();
        assertEquals(HttpStatus.OK ,response.getStatus());
        Stage stage = Stage.stageFromNumber(response.getBody().getObject().getInt("stage"));
        assertEquals(Stage.STAGE4, stage);

    }

    @Test
    public void postStageBadNumber(){
        HttpResponse<JsonNode> response = Unirest.post( serverUrl() + "/stage" ).body("{\"stage\" : 10}").asJson();
        assertEquals(HttpStatus.BAD_REQUEST ,response.getStatus());
    }


    @Test
    public void postStageBadNumberType(){
        HttpResponse<JsonNode> response = Unirest.post( serverUrl() + "/stage" ).body("{\"stage\" : \"4\"}").asJson();
        assertEquals(HttpStatus.BAD_REQUEST ,response.getStatus());
    }

    @Test
    public void postStageNoBody(){
        HttpResponse<JsonNode> response = Unirest.post( serverUrl() + "/stage" ).body("{}").asJson();
        assertEquals(HttpStatus.BAD_REQUEST ,response.getStatus());
    }


    private static int getStageFromResponse( HttpResponse<JsonNode> response ) throws JSONException{
        return response.getBody().getObject().getInt( "stage" );
    }

    private String serverUrl(){
        return "http://localhost:" + TEST_PORT;
    }
}
