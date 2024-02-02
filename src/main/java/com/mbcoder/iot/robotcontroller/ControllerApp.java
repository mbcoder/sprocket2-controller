package com.mbcoder.iot.robotcontroller;

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
    });

    Button btnForwardFail = new Button("Forward Fail");
    btnForwardFail.setOnAction(event -> {
      System.out.println("forward 1000 to cause fail");
    });

    Button btnRotate = new Button("Rotate 10");
    btnRotate.setOnAction(event -> {
      System.out.println("rotate 10");
    });

    buttonVbox.getChildren().addAll(btnForward, btnForwardFail, btnRotate);

    borderPane.setLeft(buttonVbox);
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

