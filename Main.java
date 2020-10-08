package advisor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


public class Main {
    public static Scanner scanner = new Scanner(System.in);


    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("-access")) {
                Auth.SERVER_PATH = args[1];
            }
            if (args[2].equals("-resource")) {
                Auth.RESOURCE_URL = args[3];
            }
        }


        auth();
    }

    public static void auth() {
        Auth auth = new Auth();
        boolean end = false;
        String last = "";

        do {
            String arg;
            if (scanner.hasNext()) {
                arg = scanner.next();
            } else {
                break;
            }
            if ("auth".equals(arg)) {
                end = true;
            } else {
                System.out.println("Please, provide access for application.");
            }
            last = arg;
        } while (!end);

        if (last.equals("auth")) {
        auth.getAccessCode();
        auth.getAccessToken();
        menu();
        }
    }

    public static void menu() {
        boolean end = false;
        do {
            String arg = "";
            if (scanner.hasNext()) {
                arg = scanner.next();
            }
            switch (arg) {
                case "featured":
                    featured();
                    break;
                case "new":
                    newReleases();
                    break;
                case "categories":
                    categories();
                    break;
                case "playlists":
                    playlists();
                    break;
                case "exit":
                    end = true;
                    exit();
                    break;
            }
        } while (!end);
    }

    public static void featured() {
        String apiLink = Auth.RESOURCE_URL + "/v1/browse/featured-playlists";
        Request request = new Request();
        String json = request.request(apiLink);
        try {
            JsonObject playlists = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("playlists");
            JsonArray ja = playlists.getAsJsonArray("items");
            for (JsonElement pa : ja) {
                JsonObject name = pa.getAsJsonObject();
                JsonObject href = pa.getAsJsonObject().getAsJsonObject("external_urls");
                String nameString = name.get("name").getAsString();
                String hrefString = href.get("spotify").getAsString();
                System.out.println(nameString + "\n" + hrefString + "\n");
            }
        } catch (Exception e) {
            JsonObject errorMessage = JsonParser.parseString(json).getAsJsonObject();
            String errorMessageString = errorMessage.get("message").getAsString();
            System.out.println(errorMessageString);
        }

        menu();
    }

    public static void newReleases() {
        String apiLink = Auth.RESOURCE_URL + "/v1/browse/new-releases";
        Request request = new Request();
        String json = request.request(apiLink);
        try {
            JsonObject playlists = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("albums");
            JsonArray ja = playlists.getAsJsonArray("items");
            for (JsonElement pa : ja) {
                JsonObject name = pa.getAsJsonObject();
                JsonArray artistArray = pa.getAsJsonObject().getAsJsonArray("artists");
                List<String> artistArrayString = new ArrayList<>();
                for (JsonElement paa : artistArray) {
                    artistArrayString.add(paa.getAsJsonObject().get("name").getAsString());
                }
                JsonObject href = pa.getAsJsonObject().getAsJsonObject("external_urls");
                String nameString = name.get("name").getAsString();
                String artistString = artistArrayString.toString();
                String hrefString = href.get("spotify").getAsString();
                System.out.println(nameString + "\n" + artistString + "\n" + hrefString + "\n");
            }
        } catch (Exception e) {
            JsonObject errorMessage = JsonParser.parseString(json).getAsJsonObject();
            String errorMessageString = errorMessage.get("message").getAsString();
            System.out.println(errorMessageString);
        }
        menu();
    }

    public static void categories() {
        String apiLink = Auth.RESOURCE_URL + "/v1/browse/categories";
        Request request = new Request();
        String json = request.request(apiLink);
        //System.out.println(json);
        try {
            JsonObject playlists = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("categories");
            JsonArray ja = playlists.getAsJsonArray("items");
            for (JsonElement pa : ja) {
                JsonObject name = pa.getAsJsonObject();
                String categoryId = name.get("id").getAsString();
                String categoryName = name.get("name").getAsString();
                Request.categoriesAndIds.put(categoryName, categoryId);
                System.out.println(categoryName);
            }
        } catch (Exception e) {
            try {
                JsonObject errorMessage = JsonParser.parseString(json).getAsJsonObject();
                String errorMessageString = errorMessage.get("message").getAsString();
                System.out.println(errorMessageString);
            } catch (Exception e2) {
                e.printStackTrace();
            }
        }
        menu();
    }

    public static void playlists() {
        Request.fetchCategories();
        String category = scanner.nextLine();
        category = category.replaceFirst(" ", "");
        if (!Request.categoriesAndIds.containsKey(category)) {
        System.out.println("Unknown category name");
        }
        else {
            String categoryId = Request.categoriesAndIds.get(category);
            String apiLink = Auth.RESOURCE_URL + "/v1/browse/categories/" + categoryId + "/playlists";
            Request request = new Request();
            String json = request.request(apiLink);
            try {
                JsonObject playlists = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("playlists");
                JsonArray ja = playlists.getAsJsonArray("items");
                for (JsonElement pa : ja) {
                    JsonObject name = pa.getAsJsonObject();
                    JsonObject href = pa.getAsJsonObject().getAsJsonObject("external_urls");
                    String playlistName = name.get("name").getAsString();
                    String playlistLink = href.get("spotify").getAsString();
                    System.out.println(playlistName + "\n" + playlistLink + "\n");
                }
            } catch (Exception e) {
                try {
                    System.out.println(json);
                    JsonObject errorMessage = JsonParser.parseString(json).getAsJsonObject();
                    String errorMessageString = errorMessage.get("message").getAsString();
                    System.out.println(errorMessageString);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
        menu();

    }

    public static void exit() {
        System.out.println("---GOODBYE!---");
        System.exit(0);
    }
}

class Request {
    private String jsonRequest = "";
    public static HashMap<String, String> categoriesAndIds = new HashMap<>();

    public String request(String linkToApi) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + Auth.ACCESS_TOKEN)
                .uri(URI.create(linkToApi))
                .GET()
                .build();

        try {

            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            assert response != null;
            jsonRequest = response.body();

        } catch (InterruptedException | IOException e) {
            System.out.println("Error response");
        }
        return jsonRequest;
    }


    public static void fetchCategories() {
        String apiLink = Auth.RESOURCE_URL + "/v1/browse/categories";
        Request request = new Request();
        String json = request.request(apiLink);
        //System.out.println(json);
        try {
            JsonObject playlists = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("categories");
            JsonArray ja = playlists.getAsJsonArray("items");
            for (JsonElement pa : ja) {
                JsonObject name = pa.getAsJsonObject();
                String categoryId = name.get("id").getAsString();
                String categoryName = name.get("name").getAsString();
                categoriesAndIds.put(categoryName, categoryId);
                System.out.println(categoryName);
            }
        } catch (Exception e) {
            try {
                JsonObject errorMessage = JsonParser.parseString(json).getAsJsonObject();
                String errorMessageString = errorMessage.get("message").getAsString();
                System.out.println(errorMessageString);
            } catch (Exception e2) {
                e.printStackTrace();
            }
        }

    }


}


class Auth {
    public boolean end = false;
    public static String SERVER_PATH = "https://accounts.spotify.com";
    public static String REDIRECT_URI = "http://localhost:8080";
    public static String CLIENT_ID = "556d5601795442069fdf24e5129cf220";
    public static String CLIENT_SECRET = "2af9c6ba66134c698bc2e8cfd171fbc6";
    public static String ACCESS_TOKEN = "";
    public static String ACCESS_CODE = "";
    public static String responseFinal = "";
    public static String RESOURCE_URL = "https://api.spotify.com";

    //Getting access code
    public void getAccessCode() {
        //Creating a line to go to in the browser
        String uri = SERVER_PATH + "/authorize"
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + REDIRECT_URI
                + "&response_type=code";
        System.out.println("use this link to request the access code:");
        System.out.println(uri);

        //Creating a server and listening to the request.
        try {
            HttpServer server = HttpServer.create();
            server.bind(new InetSocketAddress(8080), 0);
            server.start();
            server.createContext("/",
                    exchange -> {
                        String query = exchange.getRequestURI().getQuery();
                        String request;
                        if (query != null && query.contains("code")) {
                            ACCESS_CODE = query.substring(5);
                            System.out.println("code received");
                            //System.out.println(ACCESS_CODE);
                            request = "Got the code. Return back to your program.";
                        } else {
                            request = "Not found authorization code. Try again.";
                        }
                        exchange.sendResponseHeaders(200, request.length());
                        exchange.getResponseBody().write(request.getBytes());
                        exchange.getResponseBody().close();
                    });

            System.out.println("waiting for code...");
            while (ACCESS_CODE.length() == 0) {
                Thread.sleep(100);
            }
            server.stop(5);

        } catch (IOException | InterruptedException e) {
            System.out.println("Server error");
        }
    }


      //Getting access_token based on access_code

    public void getAccessToken() {

        System.out.println("making http request for access_token...");
        System.out.println("response:");

        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(SERVER_PATH + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=authorization_code"
                                + "&code=" + ACCESS_CODE
                                + "&client_id=" + CLIENT_ID
                                + "&client_secret=" + CLIENT_SECRET
                                + "&redirect_uri=" + REDIRECT_URI))
                .build();

        try {

            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assert response != null;
            responseFinal = response.body();
            //System.out.println(responseFinal);
            System.out.println("---SUCCESS---");
            getAccessTokenFinal();
            end = true;

        } catch (InterruptedException | IOException e) {
            System.out.println("Error response");
        }
    }

    public void getAccessTokenFinal() {
        String json = responseFinal;
        JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
        ACCESS_TOKEN = jo.get("access_token").getAsString();
    }
}