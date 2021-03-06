package com.gmail.mooman219.pokemongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class UserToken {

    /**
     * Niantic client ID.
     */
    public static final String CLIENT_ID = "848232511240-73ri3t7plvk96pj4f85uj8otdat2alem.apps.googleusercontent.com";
    /**
     * Niantic client secret.
     */
    public static final String CLIENT_SECRET = "NCjF1TLi2CcY6t5mt0ZveuL7";
    /**
     * Google authentication URL for getting the code.
     */
    public static final String URL_GOOGLE_AUTH = "https://accounts.google.com/o/oauth2/auth?";
    /**
     * Google authentication URL for getting the token.
     */
    public static final String URL_GOOGLE_TOKEN = "https://accounts.google.com/o/oauth2/token";

    /**
     * The token that can be sent to a Google API.
     */
    private final String accessToken;
    /**
     * Identifies the type of token returned. Currently, this field always has
     * the value Bearer.
     */
    private final String tokenType;
    /**
     * The remaining lifetime of the access token.
     */
    private final int expiresIn;
    /**
     * A JWT that contains identity information about the user that is digitally
     * signed by Google. If your request included an identity scope such as
     * openid, profile, or email.
     */
    private final String idToken;
    /**
     * A token that may be used to obtain a new access token, included by
     * default for installed applications. Refresh tokens are valid until the
     * user revokes access.
     */
    private final String refreshToken;

    /**
     * Represents an user token response from Google.
     *
     * @param accessToken the token that can be sent to a Google API.
     * @param tokenType identifies the type of token returned. Currently, this
     * field always has the value Bearer.
     * @param expiresIn the remaining lifetime of the access token.
     * @param idToken a JWT that contains identity information about the user
     * that is digitally signed by Google. If your request included an identity
     * scope such as openid, profile, or email.
     * @param refreshToken a token that may be used to obtain a new access
     * token, included by default for installed applications. Refresh tokens are
     * valid until the user revokes access.
     */
    private UserToken(String accessToken, String tokenType, int expiresIn, String idToken, String refreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public String toString() {
        return "UserToken{" + "accessToken=" + accessToken + ", tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", idToken=" + idToken + ", refreshToken=" + refreshToken + '}';
    }

    /**
     * Creates a new UserToken from the refresh token of the current UserToken.
     * This performs a http request to the authentication endpoint.
     *
     * @return a UserToken upon success, null if there was an issue with the
     * code.
     */
    public UserToken refresh() {
        byte[] payload = ("grant_type=authorization_code"
                + "&refresh_token=" + this.refreshToken
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8);

        Map<String, Object> res = queryUserTokenApi(payload);
        if (res == null) {
            System.out.println("Error: Null UserToken response");
            return null;
        }

        return new UserToken(
                (String) res.get("access_token"),
                (String) res.get("token_type"),
                (int) res.get("expires_in"),
                (String) res.get("id_token"),
                this.refreshToken);
    }

    /**
     * Creates a new UserToken from the given one time use code.
     *
     * @param code the one time use code used to create a UserToken.
     * @param redirectUrl the url that's redirected to upon completion.
     * @return a UserToken upon success, null if there was an issue with the
     * code.
     */
    public static UserToken createUserToken(String code, String redirectUrl) {
        byte[] payload = ("grant_type=authorization_code"
                + "&code=" + code
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&redirect_uri=" + redirectUrl).getBytes(StandardCharsets.UTF_8);

        Map<String, Object> res = queryUserTokenApi(payload);
        if (res == null) {
            System.out.println("Error: Null UserToken response");
            return null;
        }

        return new UserToken(
                (String) res.get("access_token"),
                (String) res.get("token_type"),
                (int) res.get("expires_in"),
                (String) res.get("id_token"),
                (String) res.get("refresh_token"));
    }

    /**
     * Queries the authentication endpoint with the payload.
     *
     * @param payload the post data to send to the authentication endpoint.
     * @return the mapped json response.
     * @throws IOException
     */
    private static Map<String, Object> queryUserTokenApi(byte[] payload) {
        try {
            URL url = new URL(URL_GOOGLE_TOKEN);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            con.setRequestProperty("Content-Length", payload.length + "");
            con.setDoOutput(true);
            con.setDoInput(true);

            try (DataOutputStream output = new DataOutputStream(con.getOutputStream());) {
                output.write(payload);
            }

            if (con.getResponseCode() != 200) {
                System.out.println("Error: Non 200 status code on UserToken\n"
                        + "Response: " + con.getResponseCode() + " " + con.getResponseMessage() + "\n"
                        + "Payload: " + new String(payload, StandardCharsets.UTF_8));
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(con.getInputStream(), Map.class);
        } catch (IOException ex) {
            System.out.println("Error: Unable to create authorization\n"
                    + "Stack trace:");
            ex.printStackTrace();
        }
        return null;
    }
}
