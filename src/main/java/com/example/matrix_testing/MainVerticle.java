package com.example.matrix_testing;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.out;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);

    // Bind this handler to all routes and it will handle parsing the Matrix params
    router.route().handler(this::matrixProcessor);

    // In all subsequent handlers, the matrix params are available from routingContext.get("pathSegments")
    router.get().produces("application/json").handler(this::handleRequest);

    vertx.createHttpServer().requestHandler(router).listen(8080)
      .mapEmpty()
      .onFailure(startPromise::fail)
      .onSuccess(v -> startPromise.complete());
  }

  /**
   * Use the `path` from the RoutingContext and parse out matrix parameters
   * @param ctx The {@link RoutingContext} of the request
   */
  private void matrixProcessor(RoutingContext ctx) {
    var pathParts = ctx.request().path().split("/");

    LinkedHashMap<String, MultiMap> pathSegments = new LinkedHashMap<>();

    for (var pathPart: pathParts) {
      MultiMap matrixParams = MultiMap.caseInsensitiveMultiMap();
      var pathSegmentParts = pathPart.split(";");
      var pathName = pathSegmentParts[0];
      for (int i = 1; i < pathSegmentParts.length; i++) {
        var keyValue = pathSegmentParts[i].split("=");
        var key = (CharSequence) keyValue[0];
        var values = keyValue[1].split(",");
        matrixParams.add(key, List.of(values));
      }
      pathSegments.put(pathName, matrixParams);
    }

    ctx.put("pathSegments", pathSegments);
    ctx.next();
  }

  private void handleRequest(RoutingContext ctx) {
    var pathSegments = ctx.get("pathSegments");
    if (pathSegments instanceof LinkedHashMap) {
      LinkedHashMap<String, MultiMap> p = (LinkedHashMap<String, MultiMap>) pathSegments;
      for (var path: p.entrySet()) {
        MultiMap matrixParams = path.getValue();
        for (var param: matrixParams.names()) {
          LOG.info("path = {}: key = {}: values = {}", path.getKey(), param, matrixParams.getAll(param));
        }
      }
    }
    ctx.end("OK");
  }
}
