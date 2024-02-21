/*
 COPYRIGHT 1995-2024 ESRI

 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */

package com.mbcoder.iot.robotcontroller;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * For sending https commands to the robot.
 */
public class RobotCommandRunner {

  /**
   * Ignore any problems with self-signed certificate by accepting all certificates.
   */
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

  public RobotCommandRunner() {

  }

  /**
   * Send a command to the robot using an HttpClient.
   *
   * @param command the command to send
   * @return null for success, a distance moved for failure.
   */
  public Double sendCommand(String command) {
    try {
      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

      var client = HttpClient.newBuilder()
          .sslContext(sslContext)
          .build();

//      URI uri = URI.create("https://localhost:8080/testOne?" + command);
      URI uri = URI.create("https://raspberrypi3-1.local:8080/testOne?" + command);
      var request = HttpRequest
          .newBuilder()
          .uri(uri)
          .header("accept", "application/html")
          .GET()
          .build();

      var responseAsync = client
          .sendAsync(request, HttpResponse.BodyHandlers.ofString());
      var httpResponse = responseAsync.get(200, TimeUnit.SECONDS);
      var body = httpResponse.body();
      Pattern responsePattern = Pattern.compile("^.*<P>Fail: *(\\d+(\\.\\d+)?)?</P>.*$");
      var responseMatcher = responsePattern.matcher(body);
      if (responseMatcher.matches()) {
        if (responseMatcher.group(1) != null) {
          // Failed - return the fail count
          return Double.valueOf(responseMatcher.group(1));
        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return 0.0;
    }
    return null;
  }
}
