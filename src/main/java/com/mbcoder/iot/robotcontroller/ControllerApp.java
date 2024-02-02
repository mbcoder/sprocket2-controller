package com.mbcoder.iot.robotcontroller;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ControllerApp extends Application {

  private MapView mapView;

  private final X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[]{};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    }
  };


  public static void main(String[] args) {

    Application.launch(args);
  }

  @Override
  public void start(Stage stage) {

    // set the title and size of the stage and show it
    stage.setTitle("Sprocket 2 controller");
    stage.setWidth(800);
    stage.setHeight(700);
    stage.show();

    // create a JavaFX scene with a stack pane as the root node and add it to the scene
    BorderPane borderPane = new BorderPane();
    Scene scene = new Scene(borderPane);
    stage.setScene(scene);


    // create a MapView to display the map and add it to the stack pane
    mapView = new MapView();

    borderPane.setCenter(mapView);

    MobileMapPackage mobileMapPackage = new MobileMapPackage("EdinburghOffice.mmpk");
    mobileMapPackage.loadAsync();
    mobileMapPackage.addDoneLoadingListener(()-> {
      System.out.println("loaded " + mobileMapPackage.getLoadStatus());
      ArcGISMap map = mobileMapPackage.getMaps().get(0);

      // display the map by setting the map on the map view
      mapView.setMap(map);
    });

    VBox buttonVbox = new VBox();

    Button btnForward = new Button("Forward 100");
    btnForward.setOnAction(event -> {
      System.out.println("forward 100");
      sendCommand("forward=100");
    });

    Button btnForwardFail = new Button("Forward Fail");
    btnForwardFail.setOnAction(event -> {
      System.out.println("forward 4000 to cause fail");
      sendCommand("forward=4000");
    });

    Button btnRotate = new Button("Rotate 10");
    btnRotate.setOnAction(event -> {
      System.out.println("rotate 10");
      sendCommand("rotate=10");
    });

    buttonVbox.getChildren().addAll(btnForward, btnForwardFail, btnRotate);

    borderPane.setLeft(buttonVbox);
  }

  private void sendCommand(String command) {
    try {
      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

      var client = HttpClient.newBuilder()
          .sslContext(sslContext)
          .build();

      URI uri = URI.create("https://127.0.0.1:8080/testOne?" + command);
      var request = HttpRequest
          .newBuilder()
          .uri(uri)
          .header("accept", "application/html")
          .GET()
          .build();

      var responseAsync = client
          .sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenApply(HttpResponse::body)
          .thenAccept(System.out::println);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {

    if (mapView != null) {
      mapView.dispose();
    }
  }
}

