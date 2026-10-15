package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.ScrapeRunCoordinator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedScrapGetController {

    private final ScrapeRunCoordinator coordinator;

    public FeedScrapGetController(ScrapeRunCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping(path = "/feed/scrap", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Scrape the external front pages and save the new headlines")
    @ApiResponse(
            responseCode = "200",
            description = "The feeds saved by this run, El Mundo first, then El Español; [] when nothing is new",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = FeedResponse.class))))
    public List<FeedResponse> scrap() {
        return coordinator.runNow().stream()
                .map(feed -> FeedResponse.from(feed.toPrimitives()))
                .toList();
    }
}
