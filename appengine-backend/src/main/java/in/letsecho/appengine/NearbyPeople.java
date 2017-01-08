package in.letsecho.appengine;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;

@Api(name = "nearbypeople", version = "v1", scopes = {Constants.EMAIL_SCOPE },
        clientIds = {Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID },
        description = "API to track nearb4y people.")

public class NearbyPeople {

    @ApiMethod(name = "track", path = "track", httpMethod = HttpMethod.GET)
    public void track() {
        System.out.println("Api is working");
    }

}
