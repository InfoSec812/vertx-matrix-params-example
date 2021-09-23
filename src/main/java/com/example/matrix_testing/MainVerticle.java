package com.example.matrix_testing;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    var router = Router.router(vertx);

    // Bind this handler to all routes and it will handle parsing the Matrix params
    router.route().handler(this::matrixProcessor);

    // In all subsequent handlers, the matrix params are available from routingContext.get("pathSegments")
    router.get().produces("application/json").handler(this::handleRequest);

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .<Void>mapEmpty()
      .onComplete(startPromise::handle);
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
    if (pathSegments instanceof LinkedHashMap p) {
      p.forEach((key, value) -> {
        if (value instanceof MultiMap matrixParams) {
          for (var param : matrixParams.names()) {
            LOG.info("path = {}: key = {}: values = {}", key, param, matrixParams.getAll(param));
          }
        }
      });
    }
    ctx.end("OK");
  }
}
