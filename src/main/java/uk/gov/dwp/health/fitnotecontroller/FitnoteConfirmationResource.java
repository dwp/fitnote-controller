package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class FitnoteConfirmationResource {
    private static final Logger LOG = LoggerFactory.getLogger(FitnoteConfirmationResource.class.getName());
    private static final String ERROR_MSG = "Unable to process request";
    private ImageStorage imageStorage;
    private JsonValidator validator;


    public FitnoteConfirmationResource(ImageStorage imageStorage) {
        this(imageStorage, new JsonValidator());
    }

    public FitnoteConfirmationResource(ImageStorage imageStorage, JsonValidator validator) {
        this.imageStorage = imageStorage;
        this.validator = validator;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/nino")
    public Response confirmFitnote(String json) {
        Response response;
        try {
            ImagePayload imagePayload = validator.validateAndTranslateConfirmation(json);
            LOG.debug("Json validated");
            imageStorage.updateNinoDetails(imagePayload);

            LOG.info("NINO updated");
            response = createResponseOf(HttpStatus.SC_OK, createResponseFrom(imagePayload));

        } catch (ImagePayloadException e) {
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
            LOG.error("ImagePayloadException exception :: {}", e.getMessage());
            LOG.debug("Unable to process request examining payload", e);

        } catch (IOException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.error("JsonProcessingException :: {}", e.getMessage());
            LOG.debug(ERROR_MSG, e);

        } catch (CryptoException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.error("CryptoException :: {}", e.getMessage());
            LOG.debug(ERROR_MSG, e);

        }
        return response;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/mobile")
    public Response confirmMobile(String json) {
        Response response;
        try {
            ImagePayload imagePayload = validator.validateAndTranslateMobileConfirmation(json);
            LOG.debug("Json validated");

            imageStorage.updateMobileDetails(imagePayload);
            LOG.info("Mobile number updated");

            response = createResponseOf(HttpStatus.SC_OK, createResponseFrom(imagePayload));

        } catch (ImagePayloadException e) {
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
            LOG.debug("Unable to process request when examining payload", e);
            LOG.error("ImagePayloadException :: {}", e.getMessage());

        } catch (CryptoException e) {
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
            LOG.debug("Unable to encrypt payload", e);
            LOG.error("CryptoException :: {}", e.getMessage());

        } catch (IOException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.error("IOException :: {}", e.getMessage());
            LOG.debug(ERROR_MSG, e);
        }
        return response;
    }

    private String createResponseFrom(ImagePayload payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        return mapper.writerWithView(Views.SessionOnly.class).writeValueAsString(payload);
    }

    private Response createResponseOf(int status, String message) {
        return Response.status(status).entity(message).build();
    }
}
