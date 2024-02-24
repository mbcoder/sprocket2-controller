package com.mbcoder.iot.robotcontroller;

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISVectorTiledLayer;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ControllerApp extends Application {

  private MapView mapView;

  private Point currentLocation;
  private double currentBearing;
  private Graphic currentGraphic;
  PictureMarkerSymbol arrowMarker;

  private PointCollection routePoints;
  private GraphicsOverlay routeGraphicsOverlay;
  private SimpleLineSymbol lineSymbol;

  private enum modes {SET_CURRENT, DRAW_ROUTE, IGNORE_CLICK}

  private modes appMode = modes.IGNORE_CLICK;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final RobotCommandRunner robotCommandRunner = new RobotCommandRunner();
  private final AtomicBoolean cancelRoutePlaying = new AtomicBoolean();

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

    // create the web view for livestream
    //WebView webView = new WebView();
    //webView.getEngine().load("http://192.168.68.109:5000");
    //borderPane.setRight(webView);

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

    /*
    //MobileMapPackage mobileMapPackage = new MobileMapPackage("EdinburghOffice.mmpk");
    MobileMapPackage mobileMapPackage = new MobileMapPackage("PSConventionMap.mmpk");
    mobileMapPackage.loadAsync();
    mobileMapPackage.addDoneLoadingListener(()-> {
      System.out.println("loaded " + mobileMapPackage.getLoadStatus());
      ArcGISMap map = mobileMapPackage.getMaps().get(0);

      // display the map by setting the map on the map view
      mapView.setMap(map);
    });

     */

    ArcGISVectorTiledLayer vectorTiledLayer = new ArcGISVectorTiledLayer("PSConvention.vtpk");
    vectorTiledLayer.loadAsync();
    vectorTiledLayer.addDoneLoadingListener(() -> {
      System.out.println("tiles loaded " + vectorTiledLayer.getLoadStatus());

      ArcGISMap map = new ArcGISMap();
      Basemap basemap = new Basemap(vectorTiledLayer);
      System.out.println("Name " + basemap.getBaseLayers().size());
      map.setBasemap(basemap);

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
    btnDrawRoute.setOnAction(event -> appMode = modes.DRAW_ROUTE);

    Button btnClearRoute = new Button("Clear route");
    btnClearRoute.setOnAction(event -> {
      appMode = modes.DRAW_ROUTE;

      // clear route but add the first point to start a new one
      if (!routePoints.isEmpty()) {
        routePoints.clear();
        routePoints.add(currentLocation);
        routeGraphicsOverlay.getGraphics().clear();

        //reinstate the initial current bearing
        currentBearing = sliderDirection.getValue();
      }
    });

    Button btnFinishRoute = new Button("Finish route");
    btnFinishRoute.setOnAction(event -> appMode = modes.IGNORE_CLICK);

    Button btnPlayRoute = new Button("Play route");
    btnPlayRoute.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;

      executorService.execute(this::playRoute);
    });

    Button btnStopRoute = new Button("Stop route");
    btnStopRoute.setOnAction(event -> {
      appMode = modes.IGNORE_CLICK;
      cancelRoutePlaying.set(true);
    });

    Button btnForward = new Button("^");
    btnForward.setOnAction(event -> {
      robotCommandRunner.sendCommand("forward=" + (int) 100);
    });

    HBox hBox = new HBox();
    hBox.setAlignment(Pos.CENTER);

    Button btnRotateLeft = new Button("<-");
    btnRotateLeft.setOnAction(event -> {
      robotCommandRunner.sendCommand("rotate=" + (int) -10);
    });

    Button btnRotateRight = new Button("->");
    btnRotateRight.setOnAction(event -> {
      robotCommandRunner.sendCommand("rotate=" + (int) 10);
    });
    hBox.getChildren().addAll(btnRotateLeft, btnRotateRight);

    buttonVbox.setAlignment(Pos.CENTER);
    buttonVbox.getChildren().addAll(
        btnSetInitLocation,
        sliderDirection,
        btnDrawRoute,
        btnFinishRoute,
        btnClearRoute,
        btnPlayRoute,
        btnStopRoute,
        btnForward,
        hBox);

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

  /**
   * Plays the entire route. It is expected this method will be run on a separate thread.
   */
  private void playRoute() {
    System.out.println("playing route...");
    cancelRoutePlaying.set(false);

    Point originPoint = null;

    // loop through the route points
    for (Point point: routePoints) {
      if (cancelRoutePlaying.get()) {
        System.out.println("Cancel route");
        cancelRoutePlaying.set(false);
        break;
      }

      // Only send commands if we have already set an originPoint (first time through the loop
      // the originPoint will still be null)
      if (originPoint != null) {
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

        // fix rotation angle if +/- 180
        if (rotateAngle > 180) rotateAngle-=360;
        if (rotateAngle < -180) rotateAngle+=360;

        // perform rotation
        robotCommandRunner.sendCommand("rotate=" + (int) Math.round(rotateAngle));
        System.out.println("rotate=" + (int) Math.round(rotateAngle));

        // update map with position
        Platform.runLater(() -> arrowMarker.setAngle((float) currentBearing));

        if (cancelRoutePlaying.get()) {
          System.out.println("Cancel route");
          cancelRoutePlaying.set(false);
          break;
        }

        // perform forward
        var result = robotCommandRunner.sendCommand("forward=" + (int) Math.round(distance*1000));
        if (result != null) {
          // Command failed
          double distanceCompleted =  (result / 1000.0);
          Point partialMovePoint = new Point(originPoint.getX() + (xDiff / distance * distanceCompleted),
              originPoint.getY() + (yDiff / distance * distanceCompleted), point.getSpatialReference());
          Platform.runLater(() -> currentGraphic.setGeometry(partialMovePoint));
          System.out.printf("Stopped playing commands due to failure (moved %f)%n", distanceCompleted);
          originPoint = partialMovePoint;
          break;
        }
        System.out.println("forward=" + (int) Math.round(distance*1000));

        // update map with position
        Platform.runLater(() -> currentGraphic.setGeometry(point));
      }
      // set origin for next step
      originPoint = point;
    }

    if (originPoint != null) {
      currentLocation = originPoint;
      routePoints.clear();
      routePoints.add(currentLocation);
    }
    System.out.println("Stopped playing commands");
  }

  private void updateRouteGraphic() {
    routeGraphicsOverlay.getGraphics().clear();

    Polyline polyline = new Polyline(routePoints);
    Graphic graphic = new Graphic(polyline, lineSymbol);
    routeGraphicsOverlay.getGraphics().add(graphic);
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {
    executorService.shutdown();

    if (mapView != null) {
      mapView.dispose();
    }
  }
}

