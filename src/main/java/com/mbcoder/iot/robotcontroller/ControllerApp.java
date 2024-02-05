package com.mbcoder.iot.robotcontroller;

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.MultilayerPointSymbol;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SymbolLayer;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import java.util.List;
import javafx.geometry.Point2D;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
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

  private Point currentLocation;
  private double currentBearing;
  private Graphic currentGraphic;
  PictureMarkerSymbol arrowMarker;

  PointCollection routePoints;
  GraphicsOverlay routeGraphicsOverlay;
  SimpleLineSymbol lineSymbol;

  private enum modes {SET_CURRENT, DRAW_ROUTE, IGNORE_CLICK};

  private modes appMode = modes.IGNORE_CLICK;

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

    // graphics overlay for route
    routeGraphicsOverlay = new GraphicsOverlay();
    mapView.getGraphicsOverlays().add(routeGraphicsOverlay);

    // empty points collection for route
    routePoints = new PointCollection(SpatialReferences.getWebMercator());

    // line marker for route
    lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 3f);

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

    Button btnSetInitLocation = new Button("Current location");
    btnSetInitLocation.setOnAction(event -> {
      System.out.println("setting current location");
      appMode = modes.SET_CURRENT;
    });

    Slider sliderDirection = new Slider(-180, 180, 0);
    sliderDirection.valueProperty().addListener(event -> {
      currentBearing = sliderDirection.getValue();

      // if the marker symbol exists update the direction
      if (arrowMarker!=null) {
        arrowMarker.setAngle((float) currentBearing);
      }
    });

    Button btnDrawRoute = new Button("Draw route");
    btnDrawRoute.setOnAction(event -> {
      appMode = modes.DRAW_ROUTE;

    });

    Button btnClearRoute = new Button("Clear route");
    btnClearRoute.setOnAction(event -> {
      appMode = modes.DRAW_ROUTE;

      // clear route but add the first point to start a new one
      if (routePoints.size() > 0) {
        routePoints.clear();
        routePoints.add(currentLocation);
        routeGraphicsOverlay.getGraphics().clear();

        //reinstate the initial current bearing
        currentBearing = sliderDirection.getValue();
      }
    });

    Button btnFinishRoute = new Button("Finish route");
    btnFinishRoute.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;
    });

    Button btnPlayRoute = new Button("Play route");
    btnPlayRoute.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;

      playRoute();
    });

    Button btnStopRoute = new Button("Stop route");
    btnStopRoute.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;
    });

    Button btnForward = new Button("Forward 100");
    btnForward.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;
      System.out.println("forward 100");
      sendCommand("forward=100");
    });

    Button btnForwardFail = new Button("Forward Fail");
    btnForwardFail.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;
      System.out.println("forward 4000 to cause fail");
      sendCommand("forward=4000");
    });

    Button btnRotate = new Button("Rotate 10");
    btnRotate.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;
      System.out.println("rotate 10");
      sendCommand("rotate=10");
    });

    buttonVbox.getChildren().addAll(
        btnSetInitLocation,
        sliderDirection,
        btnDrawRoute,
        btnFinishRoute,
        btnClearRoute,
        btnPlayRoute,
        btnStopRoute,
        btnForward,
        btnForwardFail,
        btnRotate);

    // handle map click events
    mapView.setOnMouseClicked(event -> {

      Point2D clickedPoint = new Point2D(event.getX(), event.getY());
      Point mapLocation = mapView.screenToLocation(clickedPoint);

      // handle new location
      if (appMode == modes.SET_CURRENT) {

        // update current location
        currentLocation = mapLocation;

        // clear the route points and graphic
        routePoints.clear();
        routePoints.add(mapLocation);
        routeGraphicsOverlay.getGraphics().clear();

        // update location on screen
        if (currentGraphic != null) {

          currentGraphic.setGeometry(mapLocation);

        } else {
          // set up graphic for 1st time
          arrowMarker = new PictureMarkerSymbol("Arrow.png");
          arrowMarker.loadAsync();
          arrowMarker.addDoneLoadingListener(() -> {
            System.out.println("arrow loaded");


            currentGraphic = new Graphic(mapLocation, arrowMarker);

            GraphicsOverlay locationOverlay = new GraphicsOverlay();
            mapView.getGraphicsOverlays().add(locationOverlay);

            locationOverlay.getGraphics().add(currentGraphic);
          });
        }
      }

      // handle adding points to route
      if (appMode == modes.DRAW_ROUTE) {
        // add new points and refresh graphic
        routePoints.add(mapLocation);

        updateRouteGraphic();

      }
    });

    borderPane.setLeft(buttonVbox);
  }

  private void playRoute() {
    System.out.println("playing route...");

    Point originPoint = null;

    // loop through the route points
    for (Point point: routePoints) {
      // first point do nothing, just collect the point
      if (originPoint == null) {
        // store the origin
        originPoint = point;
      } else {
        // process the 2 points

        var xDiff = point.getX() - originPoint.getX();
        var yDiff = point.getY() - originPoint.getY();

        var distance = GeometryEngine.distanceBetween(originPoint,point);

        var bearing = Math.toDegrees(Math.atan(xDiff / yDiff));
        // correct if heading South West
        if (yDiff < 0 && xDiff < 0) {
          bearing = bearing -180;
        }

        // correct if heading South East
        if (yDiff < 0 && xDiff > 0) {
          bearing = 180 + bearing;
        }

        // angle to rotate robot
        System.out.println("current bearing " + currentBearing);
        var rotateAngle = bearing - currentBearing;

        // update the current angle as it will be after it has rotated
        currentBearing = bearing;

        System.out.println("diff x=" + xDiff + " y=" + yDiff + " bearing=" + bearing + " distance=" + distance + " rotate=" + rotateAngle);

        //reset origin for next step
        originPoint = point;
      }

    }

  }

  private void updateRouteGraphic() {
    routeGraphicsOverlay.getGraphics().clear();

    Polyline polyline = new Polyline(routePoints);
    Graphic graphic = new Graphic(polyline, lineSymbol);
    routeGraphicsOverlay.getGraphics().add(graphic);

  }

  private void sendCommand(String command) {
    try {
      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

      var client = HttpClient.newBuilder()
          .sslContext(sslContext)
          .build();

      URI uri = URI.create("https://raspberrypi.local:8080/testOne?" + command);
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

